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
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ZmqTopicReceiver extends TopicReceiver {

  private final BundleContext bundleContext = FrameworkUtil.getBundle(ZmqTopicReceiver.class).getBundleContext();

  private final Set<Subscriber> subscribers = new HashSet<>();
  private final Set<ZmqSubscriber> zmqSubscribers = new HashSet<>();
  private final Map<String, Map<String, String>> zmqConnectedPublisherEndpoints = new HashMap<>();

  private ZContext zmqContext;
  private Map<String, String> zmqProperties;
  private final String topic;
  private ZMQ.Socket socket;

  private boolean open = false;

  public ZmqTopicReceiver(ZContext zmqContext, Map<String, String> zmqProperties, String topic) {

    this.zmqContext = zmqContext;
    this.zmqProperties = zmqProperties;
    this.topic = topic;
    this.socket = zmqContext.createSocket(ZMQ.SUB);

    boolean secure = Boolean.parseBoolean(bundleContext.getProperty(ZmqConstants.ZMQ_SECURE));
    if (secure){
      ZCert publicServerCert = new ZCert(); //TODO: Load the actual server public key
      byte[] serverKey = publicServerCert.getPublicKey();

      ZCert clientCert = new ZCert(); //TODO: Load the actual client private key
      clientCert.apply(socket);

      socket.setCurveServerKey(serverKey);
    }

  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public Map<String, String> getEndpointProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put(DiscoveryManager.SERVICE_ID, Integer.toString(getId()));
    properties.put(Subscriber.PUBSUB_TOPIC, topic);
    properties.put(Constants.PUBSUB_TYPE, Constants.SUBSCRIBER);
    properties.put(PUBSUB_ADMIN_TYPE, ZmqConstants.ZMQ);

    return properties;
  }

  @Override
  public void open() {
    for (String bindUrl : zmqConnectedPublisherEndpoints.keySet()) {
      ZmqSubscriber zmqSubscriber = new ZmqSubscriber(bindUrl, zmqProperties, topic, zmqConnectedPublisherEndpoints.get(bindUrl).get(Serializer.SERIALIZER), socket);

      System.out.println("Connecting to : " + topic + " / " + bindUrl);
      this.socket.connect(bindUrl);
      this.socket.subscribe(topic);

      zmqSubscriber.start();
      for (Subscriber subscriber : subscribers) {
        zmqSubscriber.connect(subscriber);
      }
      zmqSubscribers.add(zmqSubscriber);
    }
    this.open = true;
  }

  @Override
  public void close() {
    for (ZmqSubscriber zmqSubscriber : zmqSubscribers) {
      zmqSubscriber.stopZmqSubscriber();
    }
    this.open = false;
  }

  @Override
  public void addSubscriberEndpoint(Map<String, String> endpoint) {
    // Not needed for a subscriber
  }

  @Override
  public void removeSubscriberEndpoint(Map<String, String> endpoint) {
    // Not needed for a subscriber
  }

  @Override
  public void addPublisherEndpoint(Map<String, String> endpoint) {
    if (endpoint.get(PUBSUB_ADMIN_TYPE).equals(ZmqConstants.ZMQ)
            && endpoint.get(Publisher.PUBSUB_TOPIC).equals(topic)) {

      String zmqTopic = endpoint.get(Subscriber.PUBSUB_TOPIC);
      String serializer = endpoint.get(Serializer.SERIALIZER);
      String bindUrl = endpoint.get(Publisher.PUBSUB_ENDPOINT_URL);

      if (!zmqConnectedPublisherEndpoints.containsKey(bindUrl)) {
          zmqConnectedPublisherEndpoints.put(bindUrl, endpoint);

        if (open) {
          System.out.println("Connecting to : " + zmqTopic + " / " + bindUrl);
          this.socket.connect(bindUrl);
          this.socket.subscribe(this.topic);

          ZmqSubscriber zmqSubscriber = new ZmqSubscriber(bindUrl, zmqProperties, zmqTopic, serializer, socket);

          for (Subscriber subscriber : subscribers) {
            zmqSubscriber.connect(subscriber);
          }
          zmqSubscribers.add(zmqSubscriber);
          zmqSubscriber.start();
        }
      }
    }
  }

  @Override
  public void removePublisherEndpoint(Map<String, String> endpoint) {
    if (endpoint.get(PUBSUB_ADMIN_TYPE).equals(ZmqConstants.ZMQ)
            && endpoint.get(Publisher.PUBSUB_TOPIC).equals(topic)) {

      String bindUrl = endpoint.get(Publisher.PUBSUB_ENDPOINT_URL);
      zmqConnectedPublisherEndpoints.remove(bindUrl);

      for (ZmqSubscriber zmqSubscriber : zmqSubscribers) {
        if (zmqSubscriber.getBindUrl().equals(bindUrl)) {
          zmqSubscriber.stopZmqSubscriber();
        }
      }

      System.out.println("Disconnecting from : " + topic + " / " + bindUrl);
      this.socket.unsubscribe(this.topic);
      this.socket.disconnect(bindUrl);
    }

  }

  @Override
  public void connectSubscriber(ServiceReference reference) {
    Subscriber subscriber = (Subscriber) bundleContext.getService(reference);
    subscribers.add(subscriber);
    for (ZmqSubscriber zmqSubscriber : zmqSubscribers) {
      zmqSubscriber.connect(subscriber);
    }
    bundleContext.ungetService(reference);
  }

  @Override
  public void disconnectSubscriber(ServiceReference reference) {
    Subscriber subscriber = (Subscriber) bundleContext.getService(reference);
    subscribers.remove(subscriber);
    for (ZmqSubscriber zmqSubscriber : zmqSubscribers) {
      zmqSubscriber.disconnect(subscriber);
    }
    bundleContext.ungetService(reference);
  }

  @Override
  public boolean isActive() {
    return !subscribers.isEmpty();
  }

}
