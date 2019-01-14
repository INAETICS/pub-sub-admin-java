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
package org.inaetics.pubsub.impl.pubsubadmin.zeromq;

import org.inaetics.pubsub.api.Subscriber;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.osgi.framework.Constants.OBJECTCLASS;

public class ZmqTopicReceiver {

    private final BundleContext bundleContext = FrameworkUtil.getBundle(ZmqTopicReceiver.class).getBundleContext();

    private class SubscriberEntry {
        private final long svcId;
        private final Subscriber subscriber;
        private final Class<?> receiveClass;
        boolean initialized = false;

        public SubscriberEntry(long svcId, Subscriber subscriber, Class<?> receiveClass) {
            this.svcId = svcId;
            this.subscriber = subscriber;
            this.receiveClass = receiveClass;
        }
    }

    private final UUID uuid;
    private final Map<Long, SubscriberEntry> subscribers = new Hashtable<>();
    private final Map<Integer, Class<?>> typeIdMap = new Hashtable<>();
    private final Set<String> connections = new HashSet<>();

    private ZContext zmqContext;
    private final ZMQ.Socket socket;
    private final String zmqFilter;

    private final Properties topicProperties;
    private final String scope;
    private final String topic;

    private final Serializer serializer;
    private final ServiceTracker<Subscriber, Subscriber> tracker;

    private final Thread receiveThread = new Thread(new Runnable() {
        @Override
        public void run() {
            receiveLoop();
        }
    });

    public ZmqTopicReceiver(ZContext zmqContext, Serializer serializer, Properties topicProperties, String scope, String topic) {

        this.uuid = UUID.randomUUID();
        this.zmqContext = zmqContext;
        this.serializer = serializer;
        this.topicProperties = topicProperties;
        this.scope = scope == null ? "default" : scope;
        this.topic = topic;
        this.socket = zmqContext.createSocket(ZMQ.SUB);

        String zfilter = this.scope.length() >= 2 ? this.scope.substring(0, 2) : "EE";
        zfilter += this.topic.length() >= 2 ? this.topic.substring(0, 2) : "EE";
        this.zmqFilter = zfilter;
        this.socket.subscribe(this.zmqFilter);

        boolean secure = Boolean.parseBoolean(bundleContext.getProperty(ZmqConstants.ZMQ_SECURE));
        if (secure) {
            ZCert publicServerCert = new ZCert(); //TODO: Load the actual server public key
            byte[] serverKey = publicServerCert.getPublicKey();

            ZCert clientCert = new ZCert(); //TODO: Load the actual client private key
            clientCert.apply(socket);

            socket.setCurveServerKey(serverKey);
        }

        String filter = String.format("(&(%s=%s)(%s=%s))", OBJECTCLASS, Subscriber.class.getName(), org.inaetics.pubsub.api.Constants.TOPIC_KEY, topic);
        Filter f = null;
        try {
            f = bundleContext.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        tracker = new ServiceTracker<Subscriber, Subscriber>(bundleContext, f, new ServiceTrackerCustomizer<Subscriber, Subscriber>() {
            @Override
            public Subscriber addingService(ServiceReference<Subscriber> serviceReference) {
                Subscriber sub = bundleContext.getService(serviceReference);
                addSubscriber(serviceReference);
                return sub;
            }

            @Override
            public void modifiedService(ServiceReference<Subscriber> serviceReference, Subscriber subscriber) {
                //nop
            }

            @Override
            public void removedService(ServiceReference<Subscriber> serviceReference, Subscriber subscriber) {
                Long svcId = (Long) serviceReference.getProperty("service.id");
                removeSubscriber(svcId);
            }
        });
    }

    private void addSubscriber(ServiceReference<Subscriber> ref) {
        String subScope = (String)ref.getProperty(Subscriber.PUBSUB_SCOPE);
        boolean match = subScope != null && this.scope.equals(subScope);
        if (subScope == null && this.scope.equals("default")) {
            match = true; //for default scope a subscriber can leave out the scope property
        }

        if (match) {
            synchronized (subscribers) {
                Long svcId = (Long) ref.getProperty("service.id");
                Subscriber<?> sub = bundleContext.getService(ref);
                SubscriberEntry entry = new SubscriberEntry(svcId, sub, sub.receiveClass());
                subscribers.put(svcId, entry);
            }
            synchronized (typeIdMap) {
                Subscriber<?> sub = bundleContext.getService(ref);
                int typeId = Utils.typeIdForClass(sub.receiveClass());
                typeIdMap.put(typeId, sub.receiveClass());
            }
        }
    }

    private void removeSubscriber(long svcId) {
        synchronized (subscribers) {
            subscribers.remove(svcId);
        }
        //TODO clean up typeIdMap if class is not used anymore, i.e. loop over all classes in typeIdMap and see if there are still subscribers for
    }

    public void start() {
        tracker.open();
        receiveThread.start();
    }

    @SuppressWarnings("unchecked")
    private void receiveLoop() {
        while (!Thread.interrupted()) {
            //TODO recvFrame does not quit on interrupt. improve.
            ZFrame filterMsg = ZFrame.recvFrame(this.socket);
            ZFrame headerMsg = ZFrame.recvFrame(this.socket);
            ZFrame payloadMsg = ZFrame.recvFrame(this.socket);

            if (filterMsg != null && headerMsg != null && payloadMsg != null) {
                int typeId = typeIdFromHeader(headerMsg);
                Class<?> msgClass;
                synchronized (this.typeIdMap) {
                    msgClass = typeIdMap.get(typeId);
                }
                if (msgClass != null) {
                    Object msg = serializer.deserialize(msgClass.getName(), payloadMsg.getData());
                    synchronized (subscribers) {
                        for (SubscriberEntry entry : subscribers.values()) {
                            if (!entry.initialized) {
                                entry.subscriber.init();
                                ;
                            }
                            if (entry.receiveClass.equals(msgClass)) {
                                entry.subscriber.receive(msg);
                            }
                        }
                    }
                } else {
                    //nop. no active subscribers for class
                }
            } else {
                //TODO log
            }
        }
    }

    private int typeIdFromHeader(ZFrame frame) {
        int hash = ByteBuffer.wrap(frame.getData()).getInt();
        return hash;
    }

    public void stop() {
        tracker.close();
        this.receiveThread.interrupt();
        try {
            this.receiveThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectTo(String url) {
        synchronized (this.connections) {
            connections.add(url);
        }
        socket.connect(url);
    }

    public void disconnectFrom(String url) {
        socket.disconnect(url);
        synchronized (this.connections) {
            connections.remove(url);
        }
    }

    public String getTopic() {
        return this.topic;
    }

    public String getScope() {
        return this.scope;
    }

    public String getUUID() {
        return uuid.toString();
    }

    public Collection<String> getConnections() {
        synchronized (this.connections) {
            List<String> conns = new ArrayList<>(connections.size());
            conns.addAll(this.connections);
            return conns;
        }
    }
}
