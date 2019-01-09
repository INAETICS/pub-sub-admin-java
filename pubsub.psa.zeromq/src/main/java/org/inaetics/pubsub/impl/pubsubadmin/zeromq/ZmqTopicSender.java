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

import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;

public class ZmqTopicSender implements ServiceFactory<Publisher> {

  private final BundleContext ctx;
  private final String topic;
  private final String scope;

  private final ServiceReference<Serializer> serializer;
  private final String serializerName;

  private final Map<Bundle, Publisher> publishers = new HashMap<>();

  private final ZMQ.Socket zmqSocket;
  private final String epUrl;
  private final String bindAddress;

  private ServiceRegistration<ServiceFactory<Publisher>> registration;


    public ZmqTopicSender(BundleContext ctx, ZContext zmqContext, Map<String, String> zmqProperties, String topic, ServiceReference<Serializer> serializer) {
      super();
      this.ctx = ctx;
      this.scope = "TODO"; //TODO
      this.topic = topic;
      this.serializer = serializer;
      this.serializerName = serializer.getProperty(Serializer.SERIALIZER_NAME_KEY).toString();

//    Filter filter = null;
//    try {
//      if (serializer != null) {
//        filter = ctx.createFilter("(&(objectClass=" + Serializer.class.getName() + ")"
//                + "(" + Serializer.SERIALIZER + "=" + serializer + "))");
//      } else {
//        filter =  ctx.createFilter("(objectClass=" + Serializer.class.getName() + ")");
//      }
//
//    } catch (InvalidSyntaxException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }

//    ServiceTrackerCustomizer<Serializer, Serializer> cust = new ServiceTrackerCustomizer<Serializer, Serializer>() {
//
//        private Serializer current = null;
//
//        @Override
//        public synchronized Serializer addingService(ServiceReference<Serializer> serviceReference) {
//            Serializer s = ctx.getService(serviceReference);
//            if (current == null) {
//                setSerializer(s);
//                current = s;
//            }
//            return s;
//        }
//
//        @Override
//        public void modifiedService(ServiceReference<Serializer> serviceReference, Serializer ser) {
//            //ignore
//        }
//
//        @Override
//        public synchronized void removedService(ServiceReference<Serializer> serviceReference, Serializer ser) {
//            if (current == ser) {
//                setSerializer(null);
//                current = null;
//            }
//        }
//    };
//
//    tracker = new ServiceTracker<Serializer, Serializer>(ctx, filter, cust);
//    tracker.open();

    this.zmqSocket = zmqContext.createSocket(ZMQ.PUB);

    boolean secure = Boolean.parseBoolean(ctx.getProperty(ZmqConstants.ZMQ_SECURE));
    if (secure){
      ZCert serverCert = new ZCert(); //TODO: Load the actual private key
      serverCert.apply(zmqSocket);
      zmqSocket.setCurveServer(true);
    }

    String strMinPort = ctx.getProperty(ZmqConstants.ZMQ_BASE_PORT);
    int minPort = ZmqConstants.ZMQ_BASE_PORT_DEFAULT;
    if (strMinPort != null){
      minPort = Integer.parseInt(strMinPort.trim());
    }

    String strMaxPort = ctx.getProperty(ZmqConstants.ZMQ_MAX_PORT);
    int maxPort = ZmqConstants.ZMQ_MAX_PORT_DEFAULT;
    if (strMaxPort != null){
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

  public Map<String, String> getEndpointProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, topic);
    properties.put(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, scope);
    properties.put(Constants.PUBSUB_ENDPOINT_TYPE, Constants.PUBSUB_PUBLISHER_ENDPOINT_TYPE);
    properties.put(Serializer.SERIALIZER_NAME_KEY, serializerName);
    properties.put(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, ZmqConstants.ZMQ_ADMIN_TYPE);
    properties.put(ZmqConstants.ZMQ_CONNECTION_URL, this.epUrl);

    return properties;
  }

  public void open() {
    Dictionary properties = new Hashtable<>();
    properties.put(Publisher.PUBSUB_TOPIC, topic);

    this.zmqSocket.bind(bindAddress);

    registration = ctx.registerService(Publisher.class, this, properties);
  }

  public void close() {
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
    if (publishers.get(bundle) == null) {
      Publisher publisher = new ZmqPublisher(scope, topic, zmqSocket, serializer);
      publishers.put(bundle, publisher);
      return publisher;
    } else {
      return publishers.get(bundle);
    }
  }

  @Override
  public void ungetService(Bundle bundle, ServiceRegistration<Publisher> registration, Publisher service) {
    publishers.remove(bundle);
  }

}
