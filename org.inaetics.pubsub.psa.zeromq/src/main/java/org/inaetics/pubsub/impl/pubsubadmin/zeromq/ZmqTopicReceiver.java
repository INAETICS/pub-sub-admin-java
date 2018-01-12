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

import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.discovery.DiscoveryManager;
import org.inaetics.pubsub.spi.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ZmqTopicReceiver extends TopicReceiver {

  private final BundleContext bundleContext = FrameworkUtil.getBundle(ZmqTopicReceiver.class).getBundleContext();

  private final Set<Subscriber> subscribers = new HashSet<>();

  private ZContext zmqContext;
  private Map<String, String> zmqProperties;
  private final String topic;
  private ZMQ.Socket socket;

  public ZmqTopicReceiver(ZContext zmqContext, Map<String, String> zmqProperties, String topic) {

    this.zmqContext = zmqContext;
    this.zmqProperties = zmqProperties;
    this.topic = topic;
    this.socket = zmqContext.createSocket(ZMQ.SUB);

    //TODO

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
    //TODO
  }

  @Override
  public void close() {
    //TODO
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
    //TODO
  }

  @Override
  public void removePublisherEndpoint(Map<String, String> endpoint) {
    //TODO
  }

  @Override
  public void connectSubscriber(ServiceReference reference) {
    //TODO

  }

  @Override
  public void disconnectSubscriber(ServiceReference reference) {
    //TODO
  }

  @Override
  public boolean isActive() {
    return !subscribers.isEmpty();
  }

}
