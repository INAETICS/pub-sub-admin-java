/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package org.inaetics.pubsub.impl.discovery.etcd;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.pubsub.spi.discovery.AnnounceEndpointListener;
import org.inaetics.pubsub.spi.discovery.DiscoveredEndpointListener;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EtcdDiscovery implements AnnounceEndpointListener, ManagedService {

    private volatile LogService log;

    private class AnnounceEntry {
        public String key = null; //etcd key
        public boolean isSet = false; //whether the value is already set (in case of unavailable etcd server this can linger)
        public int refreshCount = 0;
        public int setCount = 0;
        public int errorCount = 0;
        public Properties endpoint = null;
        public long setTime = -1L;
    }

    private final ScheduledExecutorService refreshScheduler = Executors.newScheduledThreadPool(1);
    private final Thread watchThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                watch();
            }
        }
    });

    private final Map<String, AnnounceEntry> announcedEndpoints = new HashMap(); //key = etcd key
    private final Map<String, Properties> discoveredEndpoints = new HashMap<>(); //key = etcd key
    private final Set<DiscoveredEndpointListener> discoveredEndpointListeners = new HashSet<>();

    private final static String ROOT_PATH_DEFAULT = "/pubsub/";

    private final int ttl;
    private final int refreshDelay;
    private final EtcdWrapper etcd;
    private final String rootPath;

    private long nextWaitForIndex = -1L; //lock with this

    public EtcdDiscovery(EtcdWrapper etcd, int ttl) {
        this.ttl = ttl;
        this.refreshDelay = (int) (ttl / 2);
        this.etcd = etcd;
        this.rootPath = ROOT_PATH_DEFAULT;
    }

    public void start() {
        refreshScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshAnnouncedEndpoints();
            }
        }, refreshDelay, refreshDelay, TimeUnit.SECONDS);
        watchThread.start();
    }

    public void stop() {
        refreshScheduler.shutdown();
        try {
            watchThread.interrupt();
            watchThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String keyForEndpoint(Properties endpoint) {
        String result = null;
        String scope = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE);
        String topic = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME);
        String uuid = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID);
        String adminType = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE);
        if (scope != null && topic != null && uuid != null && adminType != null) {
            result = String.format("%s/%s/%s/%s/%s", this.rootPath, adminType, scope, topic, uuid);
        } else {
            log.log(LogService.LOG_WARNING, "Found an endpoint without the mandatory property entries");
        }
        return result;
    }

    private void refreshAnnouncedEndpoints() {
        synchronized (this.announcedEndpoints) {
            long updateTime = System.currentTimeMillis();
            for (AnnounceEntry entry : this.announcedEndpoints.values()) {
                if (entry.isSet) {
                    try {
                        etcd.refreshTTL(entry.key, this.ttl);
                        entry.refreshCount += 1;
                    } catch (IOException e) {
                        entry.isSet = false;
                        entry.errorCount += 1;
                        entry.setTime = -1L;
                    }
                } else {
                    String json = Utils.endpointToJson(entry.endpoint);
                    try {
                        etcd.put(entry.key, json, this.ttl);
                        entry.isSet = true;
                        entry.setTime = updateTime;
                        entry.setCount += 1;
                    } catch (IOException e) {
                        entry.errorCount += 1;
                        entry.setTime = -1L;
                    }
                }
            }
        }
    }

    @Override
    public void announceEndpoint(Properties endpoint) {
        String key = this.keyForEndpoint(endpoint);
        AnnounceEntry entry = null;
        if (key == null) {
            return;
        } else {
            entry = new AnnounceEntry();
            entry.key = key;
            entry.endpoint = endpoint;
        }
        synchronized (this.announcedEndpoints) {
            this.announcedEndpoints.put(key, entry);
            String jsonProps = Utils.endpointToJson(endpoint);
            try {
                etcd.put(key, jsonProps, this.ttl);
                entry.isSet = true;
                entry.setCount += 1;
                entry.setTime = System.currentTimeMillis();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void revokeEndpoint(Properties endpoint) {
        String key = this.keyForEndpoint(endpoint);
        if (key == null) {
            return;
        }
        AnnounceEntry entry = null;
        synchronized (this.announcedEndpoints) {
            entry = this.announcedEndpoints.remove(key);
        }
        if (entry != null) {
            try {
                etcd.delete(entry.key);
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {

    }

    public void discoveredEndpointListenerAdded(DiscoveredEndpointListener listener) {
        ArrayList<Properties> endpoints = new ArrayList<>();
        synchronized (this.discoveredEndpoints) {
            endpoints.addAll(this.discoveredEndpoints.values());
        }
        for (Properties endpoint : endpoints) {
            listener.addDiscoveredEndpoint(endpoint);
        }
        synchronized (this.discoveredEndpointListeners) {
            this.discoveredEndpointListeners.add(listener);
        }
    }

    public void discoveredEndpointListenerRemoved(DiscoveredEndpointListener listener) {
        synchronized (this.discoveredEndpointListeners) {
            this.discoveredEndpointListeners.remove(listener);
        }
    }

    private long setupConnection() {
        long nextWaitIndex = -1L;
        try {
            EtcdWrapper.EtcdResult result = etcd.get(this.rootPath, false, true, 0);
            nextWaitIndex = result.nextWaitIndex;

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Properties> endpoints = new HashMap<>();
            for (EtcdWrapper.ResultEntry entry : result.entries) {
                JsonNode node = mapper.readTree(entry.value);
                Properties endpoint = Utils.endpointFormJson(node);
                endpoints.put(entry.key, endpoint);
            }

            synchronized (this.discoveredEndpoints) {
                this.discoveredEndpoints.putAll(endpoints);
            }

            synchronized (this.discoveredEndpointListeners) {
                for (DiscoveredEndpointListener listener : this.discoveredEndpointListeners) {
                    for (Properties endpoint : endpoints.values()) {
                        listener.addDiscoveredEndpoint(endpoint);
                    }
                }
            }
        } catch (IOException e) {
            //ignore
        }
        return nextWaitIndex;
    }

    private long waitForChange(long waitFor) {
        long nextWaitIndex = -1L;
        try {
            EtcdWrapper.EtcdResult result = etcd.get(this.rootPath, true, true, waitFor);
            nextWaitIndex = result.nextWaitIndex;

            if ("delete".equals(result.action) || "expire".equals(result.action) ) {
                //remove endpoints
                List<Properties> endpoints = new ArrayList<>();
                synchronized (this.discoveredEndpoints) {
                    for (EtcdWrapper.ResultEntry entry : result.entries) {
                        Properties ep = discoveredEndpoints.remove(entry.key);
                        if (ep != null) {
                            endpoints.add(ep);
                        }
                    }
                }
                synchronized (this.discoveredEndpointListeners) {
                    for (DiscoveredEndpointListener listener : this.discoveredEndpointListeners) {
                        for (Properties ep : endpoints) {
                            listener.removeDiscoveredEndpoint(ep);
                        }
                    }
                }
            } else if ("set".equals(result.action) || "update".equals(result.action)) {
                ObjectMapper mapper = new ObjectMapper();
                List<Properties> endpoints = new ArrayList<>();
                for (EtcdWrapper.ResultEntry entry : result.entries) {
                    JsonNode node = mapper.readTree(entry.value);
                    Properties endpoint = Utils.endpointFormJson(node);
                    endpoints.add(endpoint);
                }

                synchronized (this.discoveredEndpoints) {
                    for (Properties endpoint : endpoints) {
                        String uuid = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID);
                        this.discoveredEndpoints.put(uuid, endpoint);
                    }
                }
                synchronized (this.discoveredEndpointListeners) {
                    for (DiscoveredEndpointListener listener : this.discoveredEndpointListeners) {
                        for (Properties endpoint : endpoints) {
                            listener.addDiscoveredEndpoint(endpoint);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //ignore
        }
        return nextWaitIndex;
    }

    private void cleanupDiscoveredEndpoints() {
        Map<String, Properties> endpoints = new HashMap<>();
        synchronized (this.discoveredEndpoints) {
            endpoints.putAll(this.discoveredEndpoints);
            this.discoveredEndpoints.clear();
        }
        synchronized (this.discoveredEndpointListeners) {
            for (DiscoveredEndpointListener listener : this.discoveredEndpointListeners) {
                for (Properties ep : endpoints.values()) {
                    listener.removeDiscoveredEndpoint(ep);
                }
            }
        }
    }

    public void watch() {
        long waitFor = -1L;
        synchronized (this) {
            waitFor = this.nextWaitForIndex;
        }

        if (waitFor < 0 /*not connected*/) {
            waitFor = setupConnection();
        }
        if (waitFor >= 0 /*connected*/) {
            waitFor = waitForChange(waitFor);
        }
        if (waitFor < 0 /*not connected*/) {
            cleanupDiscoveredEndpoints();
        }

        synchronized (this) {
            this.nextWaitForIndex = waitFor;
        }
    }

    //TODO gogo shell
}
