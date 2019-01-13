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

import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;


public class ZmqTopicSender implements ServiceFactory<Publisher> {

    private final BundleContext ctx;
    private final String topic;
    private final String scope;
    private final Map<Long, Publisher> publishers = new HashMap<>();
    private final ZMQ.Socket zmqSocket;
    private final String epUrl;
    private final String bindAddress;
    private final ZFrame filterFrame;
    private final Serializer serializer;

    private ServiceRegistration<ServiceFactory<Publisher>> registration;


    public ZmqTopicSender(BundleContext ctx, ZContext zmqContext, Properties topicProperties, String scope, String topic, Serializer serializer) {
        super();
        this.ctx = ctx;
        this.scope = scope == null ? "default" : scope;
        this.topic = topic;
        this.serializer = serializer;

        String filter = this.scope.length() >= 2 ? this.scope.substring(0, 2) : "EE";
        filter += this.topic.length() >= 2 ? this.topic.substring(0, 2) : "EE";
        filterFrame = new ZFrame(filter);

      /* MOVE
      tracker = new ServiceTracker<Serializer,Serializer>(ctx, serializer, new ServiceTrackerCustomizer<Serializer, Serializer>() {
        @Override
        public Serializer addingService(ServiceReference<Serializer> serviceReference) {
          Serializer ser = ctx.getService(serviceReference);
          setSerializer(tracker.getService()); //set highest ranking
          return ser;
        }

        @Override
        public void modifiedService(ServiceReference<Serializer> serviceReference, Serializer serializer) {
          //nop
        }

        @Override
        public void removedService(ServiceReference<Serializer> serviceReference, Serializer serializer) {
          setSerializer(tracker.getService()); //set highest ranking
        }
      });*/

        this.zmqSocket = zmqContext.createSocket(ZMQ.PUB);

        boolean secure = Boolean.parseBoolean(ctx.getProperty(ZmqConstants.ZMQ_SECURE));
        if (secure) {
            ZCert serverCert = new ZCert(); //TODO: Load the actual private key
            serverCert.apply(zmqSocket);
            zmqSocket.setCurveServer(true);
        }

        String strMinPort = ctx.getProperty(ZmqConstants.ZMQ_BASE_PORT);
        int minPort = ZmqConstants.ZMQ_BASE_PORT_DEFAULT;
        if (strMinPort != null) {
            minPort = Integer.parseInt(strMinPort.trim());
        }

        String strMaxPort = ctx.getProperty(ZmqConstants.ZMQ_MAX_PORT);
        int maxPort = ZmqConstants.ZMQ_MAX_PORT_DEFAULT;
        if (strMaxPort != null) {
            maxPort = Integer.parseInt(strMaxPort.trim());
        }

        Random r = new Random();

        int port = r.nextInt(maxPort - minPort + 1) + minPort;
        this.epUrl = "tcp://127.0.0.1:" + port;
        this.bindAddress = "tcp://*:" + port;

    }

    public String getTopic() {
        return topic;
    }

    public String getScope() {
        return scope;
    }

    public String getConnectionUrl() {
        return epUrl;
    }

    public void start() {
        Dictionary properties = new Hashtable<>();
        properties.put(Publisher.PUBSUB_TOPIC, topic);

        this.zmqSocket.bind(bindAddress);

        registration = ctx.registerService(Publisher.class, this, properties);
    }

    public void stop() {
        this.zmqSocket.unbind(bindAddress);
        registration.unregister();
    }

    public void addSubscriberEndpoint(Map<String, String> endpoint) {
        //TODO
    }

    public void removeSubscriberEndpoint(Map<String, String> endpoint) {
        //TODO
    }

    public void addPublisherEndpoint(Map<String, String> endpoint) {
        // Not needed for a publisher
    }

    public void removePublisherEndpoint(Map<String, String> endpoint) {
        // Not needed for a publisher
    }

    public boolean isActive() {
        return !publishers.isEmpty();
    }

    @Override
    public Publisher getService(Bundle bundle, ServiceRegistration<Publisher> registration) {
        Publisher result = null;
        synchronized (this.publishers) {
            if (!publishers.containsKey(bundle.getBundleId())) {
                //new
                Publisher pub = new Publisher() {
                    @Override
                    public void send(Object msg) {
                        sendMsg(msg);
                    }
                };
                publishers.put(bundle.getBundleId(), pub);
            }
            return publishers.get(bundle.getBundleId());
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Publisher> registration, Publisher service) {
        synchronized (this.publishers) {
            publishers.remove(bundle.getBundleId());
        }
    }


    public void connectTo(String url) {
        zmqSocket.connect(url);
    }

    public void disconnectFrom(String url) {
        zmqSocket.disconnect(url);
    }

    public void sendMsg(Object msg) {
        boolean success = sendRawMsg(serializer.serialize(msg), msg.getClass());
        if (!success) {
            //TODO log warning
        }
    }


    private boolean sendRawMsg(byte[] msg, Class<?> msgClazz) {

        boolean success = true;

        //Then a header containing type hash (unsigned int) and version info (major/minor) as two chars.
        ZFrame headerFrame = headerFrameFor(msgClazz);

        //Then the actual payload
        ZFrame payloadFrame = new ZFrame(msg);

        success = filterFrame.send(this.zmqSocket, ZFrame.MORE);
        if (success) {
            success = headerFrame.send(this.zmqSocket, ZFrame.MORE);
        }
        if (success) {
            success = payloadFrame.send(this.zmqSocket, 0);
        }

        return success;
    }

    private ZFrame headerFrameFor(Class<?> clazz) {
        byte[] headerBytes = new byte[6]; //first 4 hashcode of the class
        int hash = Utils.stringHash(clazz.getName());
        headerBytes[0] = (byte) (hash & 0xFF);
        headerBytes[1] = (byte) ((hash >> 8) & 0xFF);
        headerBytes[2] = (byte) ((hash >> 16) & 0xFF);
        headerBytes[3] = (byte) ((hash >> 24) & 0xFF);
        headerBytes[4] = 0; /*TODO major version*/
        headerBytes[4] = 0; /*TODO minor version*/
        return new ZFrame(headerBytes);
    }
}
