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
package org.inaetics.pubsub.impl.topologymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.api.Subscriber;
import org.inaetics.pubsub.spi.discovery.AnnounceEndpointListener;
import org.inaetics.pubsub.spi.discovery.DiscoveredEndpointListener;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

import javax.xml.ws.Service;

import static org.osgi.framework.Constants.SERVICE_ID;


public class PubSubTopologyManager implements ListenerHook, DiscoveredEndpointListener, ManagedService {

    private class TopicSenderOrReceiverEntry {
        public String key = null; //(sender-/receiver-)${scope}-${topic}
        public String scope = null;
        public String topic = null;
        public Properties endpoint = null;

        public int usageCount = 1; //nr of subscriber service for the topic receiver (matching scope & topic)
        public Properties topicProperties;
        public long selectedPsaSvcId = -1L;
        public long selectedSerializerSvcId = -1L;
        public long bndId = -1L;
        public boolean rematch = true;

        //for sender entry
        public String publisherFilter = null;

        //for receiver entry
        public Properties subscriberProperties = null;

        public boolean isForSender() {
            return publisherFilter != null;
        }

        public boolean isForReceiver() {
            return subscriberProperties != null;
        }
    }

    private class DiscoveredEndpointEntry {
        public String uuid = null;
        public long selectedPsaSvcId = -1L; // -1L, indicates no selected psa
        public int usageCount = 0; //note that discovered endpoints can be found multiple times by different pubsub discovery components
        public Properties endpoint = null;
    }

    public final static String SERVICE_PID = PubSubTopologyManager.class.getName();

    private final int PSA_UPDATE_DELAY = 15;

    private volatile boolean verbose = true;

    private final Map<Long, PubSubAdmin> pubSubAdmins = new HashMap<>();
    private final List<AnnounceEndpointListener> announceEndpointListeners = new ArrayList<>();

    private volatile LogService m_LogService;

    private final Map<String, TopicSenderOrReceiverEntry> sendersAndReceivers = new HashMap<>();

    private final Map<String, DiscoveredEndpointEntry> discoveredEndpoints = new HashMap<>();

    private final Thread psaHandlingThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                psaHandling();
                try {
                    Thread me = Thread.currentThread();
                    synchronized (me) {
                        me.wait(PSA_UPDATE_DELAY * 1000);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }, "PubSub TopologyManager");

    public void start() {
        psaHandlingThread.start();
    }

    public void stop() {
        try {
            psaHandlingThread.interrupt();
            psaHandlingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void psaHandling() {
        this.teardownTopicSenderAndReceivers();
        this.setupTopicSendersAndReceivers();
        this.findPsaForEndpoint();
    }

    public void adminAdded(ServiceReference<PubSubAdmin> adminRef, PubSubAdmin admin) {
        Long svcId = (Long)adminRef.getProperty(SERVICE_ID);

        synchronized (this.pubSubAdmins) {
            pubSubAdmins.put(svcId, admin);
        }

        synchronized (this.sendersAndReceivers) {
            for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                entry.rematch = true; //new PSA -> run a rematch and if needed teardown and setup a new TopicSender/Receiver
            }
        }

        synchronized (psaHandlingThread) { psaHandlingThread.notifyAll(); }

    }

    public void adminRemoved(ServiceReference<PubSubAdmin> adminRef, PubSubAdmin admin) {
        Long svcId = (Long)adminRef.getProperty(SERVICE_ID);

        //NOTE psa shutdown should teardown topic receivers / topic senders
        //de-setup all topic receivers/senders for the removed psa.
        //the next psaHandling run will try to find new psa.

        synchronized (this.pubSubAdmins) {
            pubSubAdmins.remove(svcId);
        }

        List<Properties> revokes = new ArrayList<Properties>();

        synchronized (this.sendersAndReceivers) {
            for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                if (entry.selectedPsaSvcId == svcId) {
                    revokes.add(entry.endpoint);
                    entry.endpoint = null;
                    entry.selectedPsaSvcId = -1L;
                    entry.rematch = true;
                }
            }
        }

        synchronized (this.announceEndpointListeners) {
            for (Properties ep : revokes) {
                for (AnnounceEndpointListener listener : this.announceEndpointListeners) {
                    listener.revokeEndpoint(ep);
                }
            }
        }
    }

    @Override
    public void addDiscoveredEndpoint(Properties endpoint) {

        String uuid = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID);

        if (uuid == null) {
            m_LogService.log(LogService.LOG_WARNING, "Invalid endpoint. No UUID found!");
            return;
        }

        // 1) See if endpoint is already discovered, if so increase usage count.
        // 1) If not, find matching psa using the matchEndpoint
        // 2) if found call addEndpoint of the matching psa

        if (verbose) {
            m_LogService.log(LogService.LOG_DEBUG, String.format("PSTM: Discovered endpoint added for topic %s with scope %s [fwUUID=%s, epUUID=%s]\n",
                    endpoint.get(Constants.PUBSUB_ENDPOINT_TOPIC_NAME),
                    endpoint.get(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE),
                    endpoint.get(Constants.PUBSUB_ENDPOINT_FRAMEWORK_UUID),
                    uuid));
        }


        boolean newEntry = false;
        synchronized (discoveredEndpoints) {
            DiscoveredEndpointEntry entry = discoveredEndpoints.get(uuid);
            if (entry != null) {
                entry.usageCount += 1;
            } else {
                newEntry = true;
                entry = new DiscoveredEndpointEntry();
                entry.usageCount = 1;
                entry.endpoint = endpoint;
                discoveredEndpoints.put(uuid, entry);
                //note selectedPsaSvcId is stil -1 -> no psa selected yet -> psa handling thread should try to find psa
                //for psa handling
            }
        }
        if (newEntry) {
            synchronized (psaHandlingThread) { psaHandlingThread.notifyAll(); }
        }
    }

    @Override
    public void removeDiscoveredEndpoint(Properties endpoint) {
        String uuid = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID);
        assert (uuid != null);

        // 1) See if endpoint is already discovered, if so decrease usage count.
        // 1) If usage count becomes 0, find matching psa using the matchEndpoint
        // 2) if found call disconnectEndpoint of the matching psa

        if (verbose) {
            m_LogService.log(LogService.LOG_DEBUG, String.format("PSTM: Discovered endpoint removed for topic %s with scope %s [fwUUID=%s, epUUID=%s]\n",
                    endpoint.get(Constants.PUBSUB_ENDPOINT_TOPIC_NAME),
                    endpoint.get(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE),
                    endpoint.get(Constants.PUBSUB_ENDPOINT_FRAMEWORK_UUID),
                    uuid));
        }

        DiscoveredEndpointEntry entry = null;
        synchronized (discoveredEndpoints) {
            entry = discoveredEndpoints.remove(uuid);
        }

        if (entry != null) {
            if (entry.selectedPsaSvcId >= 0) {
                synchronized (this.pubSubAdmins) {
                    PubSubAdmin admin = this.pubSubAdmins.get(entry.selectedPsaSvcId);
                    if (admin != null) {
                        admin.removeDiscoveredEndpoint(entry.endpoint);
                    } else {
                        m_LogService.log(LogService.LOG_WARNING, "Cannot find configured psa for endpoint " + uuid);

                    }
                }
            } else {
                m_LogService.log(LogService.LOG_DEBUG, "No selected psa for endpoint " + uuid);
            }
        }
    }

    public void announceEndointListenerAdded(AnnounceEndpointListener listener) {
        synchronized (this.announceEndpointListeners) {
            this.announceEndpointListeners.add(listener);
        }
        synchronized (this.sendersAndReceivers) {
            for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                if (entry.endpoint != null) {
                    listener.announceEndpoint(entry.endpoint);
                }
            }
        }
    }

    public void announceEndointListenerRemoved(AnnounceEndpointListener listener) {
        synchronized (this.announceEndpointListeners) {
            this.announceEndpointListeners.remove(listener);
        }
    }

    private String topicReceiverKey(final String scope, final String topic) {
        return "receiver-" + scope + "-" + topic;

    }

    private String topicSenderKey(final String scope, final String topic) {
        return "sender-" + scope + "-" + topic;

    }

    public void subscriberAdded(ServiceReference<Subscriber> ref, Subscriber svc) {
        //NOTE new local subscriber service register
        //1) First trying to see if a TopicReceiver already exists for this subscriber, if found
        //2) update the usage count. if not found
        //3) signal psaHandling thread to setup topic receiver

        Bundle svcOwner = ref.getBundle();
        String topic = (String)ref.getProperty(Subscriber.PUBSUB_TOPIC);
        String scope = (String)ref.getProperty(Subscriber.PUBSUB_SCOPE);
        if (topic == null) {
            this.m_LogService.log(LogService.LOG_WARNING, "[PSTM] Warning subscriber service without mandatory %s property.");
            return;
        }
        if (scope == null) {
            scope = "default";
        }

        Properties props = new Properties();
        for (String key : ref.getPropertyKeys()) {
            props.put(key, ref.getProperty(key));
        }

        long bndId = svcOwner.getBundleId();
        String key = topicReceiverKey(scope, topic);

        boolean newEntry = false;
        synchronized (this.sendersAndReceivers) {
            TopicSenderOrReceiverEntry entry = this.sendersAndReceivers.get(key);
            if (entry != null) {
                entry.usageCount += 1;
            } else {
                entry = new TopicSenderOrReceiverEntry();
                entry.key = key;
                entry.scope = scope;
                entry.topic = topic;
                entry.bndId = bndId;
                entry.rematch = true;
                entry.subscriberProperties = props;
                this.sendersAndReceivers.put(key, entry);
            }

            newEntry = entry.usageCount == 1;
        }

        if (newEntry) {
            synchronized (psaHandlingThread) { psaHandlingThread.notifyAll(); }
        }
    }

    public void subscriberRemoved(ServiceReference<Subscriber<?>> ref, Subscriber svc) {
        //NOTE local subscriber service unregister
        //1) Find topic receiver and decrease count

        String topic = (String)ref.getProperty(Subscriber.PUBSUB_TOPIC);
        String scope = (String)ref.getProperty(Subscriber.PUBSUB_SCOPE);
        if (topic == null) {
            this.m_LogService.log(LogService.LOG_WARNING, "[PSTM] Warning subscriber service without mandatory %s property.");
            return;
        }
        if (scope == null) {
            scope = "default";
        }

        String key = topicReceiverKey(scope, topic);
        synchronized (this.sendersAndReceivers) {
            TopicSenderOrReceiverEntry entry = this.sendersAndReceivers.get(key);
            if (entry != null) {
                entry.usageCount -= 1;
                //note no notify needed, because the topicreceiver does not have to go away immediately
            }
        }
    }


    @Override
    public void added(Collection<ListenerInfo> collection) {
        boolean newEntry = false;
        for (ListenerInfo info : collection) {
            //TODO improve -> parse filter
            if (!info.getFilter().contains("objectClass=" + Publisher.class.getName())) {
                continue;
            }

            //NOTE new local publisher service requested
            //1) First trying to see if a TopicSender already exists for this publisher, if found
            //2) update the usage count. if not found
            //3) signal psaHandling thread to find a psa and setup TopicSender

            String scope = Utils.getAttributeValueFromFilter(info.getFilter(), Publisher.PUBSUB_SCOPE);
            String topic = Utils.getAttributeValueFromFilter(info.getFilter(), Publisher.PUBSUB_TOPIC);
            scope = scope != null ? scope : "default";

            if (topic == null) {
                m_LogService.log(LogService.LOG_WARNING, "[PSTM] Cannot find topic for publisher request");
                continue;
            }

            String key = this.topicSenderKey(scope, topic);
            synchronized (this.sendersAndReceivers) {
                TopicSenderOrReceiverEntry entry = this.sendersAndReceivers.get(key);
                if (entry != null) {
                    entry.usageCount += 1;
                } else {
                    entry = new TopicSenderOrReceiverEntry();
                    entry.key = key;
                    entry.topic = topic;
                    entry.scope = scope;
                    entry.rematch = true;
                    entry.publisherFilter = info.getFilter();
                    this.sendersAndReceivers.put(key, entry);
                }

                newEntry = entry.usageCount == 1;
            }
        }


        if (newEntry) {
            synchronized (psaHandlingThread) { psaHandlingThread.notifyAll(); }
        }
    }

    @Override
    public void removed(Collection<ListenerInfo> collection) {
        for (ListenerInfo info : collection) {
            if (!info.getFilter().contains("objectClass=" + Publisher.class.getName())) {
                continue;
            }

            String scope = Utils.getAttributeValueFromFilter(info.getFilter(), Publisher.PUBSUB_SCOPE);
            String topic = Utils.getAttributeValueFromFilter(info.getFilter(), Publisher.PUBSUB_TOPIC);
            scope = scope != null ? scope : "default";
            if (topic == null) {
                m_LogService.log(LogService.LOG_WARNING, "[PSTM] Cannot find topic for removed publisher request");
                continue;
            }

            //NOTE local publisher request removed
            //1) Find topic sender and decrease count

            String key = this.topicSenderKey(scope, topic);
            synchronized (this.sendersAndReceivers) {
                TopicSenderOrReceiverEntry entry = this.sendersAndReceivers.get(key);
                if (entry != null) {
                    entry.usageCount -= 1;
                }
            }
        }
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        //TODO
    }

    private void teardownTopicSenderAndReceivers() {
        List<Properties> revokes = new ArrayList<>();

        synchronized (this.sendersAndReceivers) {
            synchronized (this.pubSubAdmins) {
                for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                    if (entry.usageCount == 0 && entry.endpoint != null) {
                        revokes.add(entry.endpoint);
                        if (entry.selectedPsaSvcId >= 0) {
                            PubSubAdmin admin = this.pubSubAdmins.get(entry.selectedPsaSvcId);
                            if (entry.isForSender()) {
                                admin.teardownTopicSender(entry.scope, entry.topic);
                            } else {
                                admin.teardownTopicReceiver(entry.scope, entry.topic);
                            }
                        }

                        entry.endpoint = null;
                        entry.selectedPsaSvcId = -1L;
                        entry.selectedSerializerSvcId = -1L;
                        entry.rematch = true;
                    }
                }
            }
        }

        synchronized (this.announceEndpointListeners) {
            for (AnnounceEndpointListener listener : this.announceEndpointListeners) {
                for (Properties endpoint : revokes) {
                    listener.revokeEndpoint(endpoint);
                }
            }
        }
    }

    private void setupTopicSendersAndReceivers() {
        List<Properties> revokes = new ArrayList<>();
        List<Properties> announces = new ArrayList<>();
        synchronized (this.sendersAndReceivers) {
            synchronized (this.pubSubAdmins) {
                for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                    if (entry.rematch && entry.usageCount > 0) {
                        double bestScore = PubSubAdmin.PUBSUB_ADMIN_NO_MATCH_SCORE;
                        long bestPsa = -1L;
                        long serializerSvcId = -1L;
                        Properties topicProperties = null;
                        for (Map.Entry<Long, PubSubAdmin> psaEntry : this.pubSubAdmins.entrySet()) {
                            if (entry.isForSender()) {
                                PubSubAdmin.MatchResult mr = psaEntry.getValue().matchPublisher(entry.bndId, entry.publisherFilter);
                                if (mr.score > bestScore) {
                                    bestScore = mr.score;
                                    serializerSvcId = mr.selectedSerializerSvcId;
                                    topicProperties = mr.topicProperties;
                                    bestPsa = psaEntry.getKey();
                                }
                            } else {
                                PubSubAdmin.MatchResult mr = psaEntry.getValue().matchSubscriber(entry.bndId, entry.subscriberProperties);
                                if (mr.score > bestScore) {
                                    bestScore = mr.score;
                                    serializerSvcId = mr.selectedSerializerSvcId;
                                    topicProperties = mr.topicProperties;
                                    bestPsa = psaEntry.getKey();
                                }
                            }
                        }
                        if (bestPsa >= 0 && entry.selectedPsaSvcId != bestPsa) {
                            if (entry.endpoint != null) {
                                revokes.add(entry.endpoint);
                            }
                            entry.endpoint = null;
                            entry.selectedPsaSvcId = -1L;
                            entry.selectedSerializerSvcId = serializerSvcId;
                            entry.topicProperties = topicProperties;

                            entry.selectedPsaSvcId = bestPsa;
                            entry.selectedSerializerSvcId = serializerSvcId;
                            PubSubAdmin admin = this.pubSubAdmins.get(bestPsa);
                            if (entry.isForSender()) {
                                entry.endpoint = admin.setupTopicSender(entry.scope, entry.topic, entry.topicProperties, entry.selectedSerializerSvcId);
                            } else {
                                entry.endpoint = admin.setupTopicReceiver(entry.scope, entry.topic, entry.topicProperties, entry.selectedSerializerSvcId);
                            }
                            announces.add(entry.endpoint);
                        }
                    }
                }
            }
        }

        //revoke and announce removed topic sender/receiver endpoints and announce new ones
        synchronized (this.announceEndpointListeners) {
            for (AnnounceEndpointListener listener : this.announceEndpointListeners) {
                for (Properties endpoint : revokes) {
                    listener.revokeEndpoint(endpoint);
                }
                for (Properties endpoint : announces) {
                    listener.announceEndpoint(endpoint);
                }
            }
        }
    }

    private void findPsaForEndpoint() {
        synchronized (this.discoveredEndpoints) {
            for (DiscoveredEndpointEntry entry : this.discoveredEndpoints.values()) {
                if (entry != null && entry.selectedPsaSvcId < 0) {
                    long psa = -1L;

                    synchronized (this.pubSubAdmins) {
                        for (Map.Entry<Long, PubSubAdmin> psaEntry : this.pubSubAdmins.entrySet()) {
                            if (psaEntry.getValue().matchDiscoveredEndpoint(entry.endpoint)) {
                                psa = psaEntry.getKey();
                                break;
                            }
                        }
                        if (psa >= 0) {
                            entry.selectedPsaSvcId = psa;
                            this.pubSubAdmins.get(psa).addDiscoveredEndpoint(entry.endpoint);
                        } else {
                            this.m_LogService.log(LogService.LOG_DEBUG, "Cannot find psa for endpoint " + entry.uuid);
                        }
                    }
                }
            }
        }
    }

    private void printTopicSenderOrReceivers(boolean senders) {
        System.out.println("");
        if (senders) {
            System.out.println("Active Topic Senders:");
        } else {
            System.out.println("Active Topic Receivers:");
        }
        synchronized (this.sendersAndReceivers) {
            for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                boolean print = senders ? entry.isForSender() : entry.isForReceiver();
                if (print) {
                    String uuid = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID, "!Error!");
                    String scope = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, "!Error!");
                    String topic = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, "!Error!");
                    String adminType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, "!Error!");
                    String serType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_SERIALIZER, "!Error!");

                    if (senders) {
                        System.out.println("|- Topic Sender for endpoint " + uuid);
                    } else {
                        System.out.println("|- Topic Receiver for endpoint " + uuid);
                    }
                    System.out.println("   |-  scope         = " + scope);
                    System.out.println("   |-  topic         = " + topic);
                    System.out.println("   |-  admin type    = " + adminType);
                    System.out.println("   |-  serializer    = " + serType);
                    if (this.verbose) {
                        System.out.println("   |-  psa svc id    = " + entry.selectedPsaSvcId);
                        System.out.println("   |-  usage count   = " + entry.usageCount);
                    }
                }
            }
        }
    }

    //GOGO shell command
    public void pstm() {
        System.out.println("");


        System.out.println("Discovered Endpoints:");
        synchronized (this.discoveredEndpoints) {
            for (DiscoveredEndpointEntry entry : this.discoveredEndpoints.values()) {
                String cn = entry.endpoint.getProperty("container_name", "!Error!");
                String fwuuid = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_FRAMEWORK_UUID, "!Error!");
                String type = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TYPE, "!Error!");
                String scope = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, "!Error!");
                String topic = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, "!Error!");
                String adminType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, "!Error!");
                String serType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, "!Error!");
                System.out.println("|- Discovered Endpoint " + entry.uuid);
                System.out.println("   |- container name = " + cn);
                System.out.println("   |- fw uuid        = " + fwuuid);
                System.out.println("   |-  type          = " + type);
                System.out.println("   |-  scope         = " + scope);
                System.out.println("   |-  topic         = " + topic);
                System.out.println("   |-  admin type    = " + adminType);
                System.out.println("   |-  serializer    = " + serType);
                if (this.verbose) {
                    System.out.println("   |-  psa svc id    = " + entry.selectedPsaSvcId);
                    System.out.println("   |-  usage conut   = " + entry.usageCount);
                }
            }
        }

        printTopicSenderOrReceivers(true);
        printTopicSenderOrReceivers(false);

        System.out.println("");
        System.out.println("Pending Endpoints:");
        synchronized (this.sendersAndReceivers) {
            for (TopicSenderOrReceiverEntry entry : this.sendersAndReceivers.values()) {
                if (entry.selectedPsaSvcId < 0) {
                    String uuid = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_UUID, "!Error!");
                    String scope = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, "!Error!");
                    String topic = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, "!Error!");
                    String adminType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, "!Error!");
                    String serType = entry.endpoint.getProperty(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, "!Error!");

                    System.out.println("|- Pending endpoint " + uuid);
                    System.out.println("   |-  scope         = " + scope);
                    System.out.println("   |-  topic         = " + topic);
                    System.out.println("   |-  admin type    = " + adminType);
                    System.out.println("   |-  serializer    = " + serType);
                }
            }
        }
    }
}
