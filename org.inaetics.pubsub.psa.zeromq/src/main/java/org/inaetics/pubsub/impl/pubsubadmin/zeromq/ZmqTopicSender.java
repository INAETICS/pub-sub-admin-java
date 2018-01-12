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
import org.inaetics.pubsub.spi.discovery.DiscoveryManager;
import org.inaetics.pubsub.spi.pubsubadmin.TopicSender;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.zeromq.ZCert;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;

public class ZmqTopicSender extends TopicSender {

  private BundleContext bundleContext = FrameworkUtil.getBundle(ZmqTopicSender.class).getBundleContext();
  private final String topic;

  private ServiceTracker tracker;
  private ServiceRegistration<ServiceFactory<Publisher>> registration;
  private final Serializer serializer;
  private final String serializerString;

  private final Map<Bundle, Publisher> publishers = new HashMap<>();

  private ZMQ.Socket zmqSocket;
  private String ep;

  public ZmqTopicSender(ZContext zmqContext, Map<String, String> zmqProperties, String topic, String serializer) {

    super();
    this.topic = topic;

    Filter filter = null;
    try {
      if (serializer != null) {
        filter = bundleContext.createFilter("(&(objectClass=" + Serializer.class.getName() + ")"
                + "(" + Serializer.SERIALIZER + "=" + serializer + "))");
      } else {
        filter = bundleContext.createFilter("(objectClass=" + Serializer.class.getName() + ")");
      }

    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    tracker = new ServiceTracker<>(bundleContext, filter, null);
    tracker.open();

    ServiceReference<Serializer> serviceReference = tracker.getServiceReference();
    this.serializer = (Serializer) bundleContext.getService(serviceReference);
    this.serializerString = (String) serviceReference.getProperty(Serializer.SERIALIZER);

    this.zmqSocket = zmqContext.createSocket(ZMQ.PUB);

    boolean secure = Boolean.parseBoolean(bundleContext.getProperty(ZmqConstants.ZMQ_SECURE));
    if (secure){
      ZCert serverCert = new ZCert(); //TODO: Load the actual private key
      serverCert.apply(zmqSocket);
      zmqSocket.setCurveServer(true);
    }

    String strMinPort = bundleContext.getProperty(ZmqConstants.ZMQ_BASE_PORT);
    int minPort = ZmqConstants.ZMQ_BASE_PORT_DEFAULT;
    if (strMinPort != null){
      minPort = Integer.parseInt(strMinPort.trim());
    }

    String strMaxPort = bundleContext.getProperty(ZmqConstants.ZMQ_MAX_PORT);
    int maxPort = ZmqConstants.ZMQ_MAX_PORT_DEFAULT;
    if (strMaxPort != null){
      maxPort = Integer.parseInt(strMaxPort.trim());
    }

    Random r = new Random();

    int port = r.nextInt(maxPort - minPort + 1) + minPort;
    this.ep = "tcp://127.0.0.1:" + port;
    String bindAddress = "tcp://*:" + port;

    this.zmqSocket.bind(bindAddress);

    System.out.println("Bind to address: " + ep);
  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public Map<String, String> getEndpointProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put(DiscoveryManager.SERVICE_ID, Integer.toString(getId()));
    properties.put(Publisher.PUBSUB_TOPIC, topic);
    properties.put(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_TYPE, Constants.PUBLISHER);
    properties.put(Serializer.SERIALIZER, serializerString);
    properties.put(PUBSUB_ADMIN_TYPE, ZmqConstants.ZMQ);

    return properties;
  }

  @Override
  public void open() {
    Dictionary properties = new Hashtable<>();
    properties.put(Publisher.PUBSUB_TOPIC, topic);

    registration = bundleContext.registerService(Publisher.class, this, properties);
  }

  @Override
  public void close() {
    tracker.close();
    registration.unregister();
  }

  @Override
  public void addSubscriberEndpoint(Map<String, String> endpoint) {
    //TODO
  }

  @Override
  public void removeSubscriberEndpoint(Map<String, String> endpoint) {
    //TODO
  }

  @Override
  public void addPublisherEndpoint(Map<String, String> endpoint) {
    // Not needed for a publisher
  }

  @Override
  public void removePublisherEndpoint(Map<String, String> endpoint) {
    // Not needed for a publisher
  }

  @Override
  public boolean isActive() {
    return !publishers.isEmpty();
  }

  @Override
  public Publisher getService(Bundle bundle, ServiceRegistration<Publisher> registration) {
    if (publishers.get(bundle) == null) {
      Publisher publisher = new ZmqPublisher(topic, zmqSocket, serializer);
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
