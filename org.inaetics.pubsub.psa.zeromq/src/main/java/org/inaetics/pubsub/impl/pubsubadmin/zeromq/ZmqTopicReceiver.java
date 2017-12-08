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

import org.inaetics.pubsub.spi.pubsubadmin.TopicReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.zeromq.ZContext;

import java.util.Map;

public class ZmqTopicReceiver extends TopicReceiver {

  private final BundleContext bundleContext =
          FrameworkUtil.getBundle(ZmqTopicReceiver.class).getBundleContext();
  private final String topic;

  private ZContext zmqContext;

  public ZmqTopicReceiver(ZContext zmqContext, Map<String, String> zmqProperties, String topic) {

    this.topic = topic;
    this.zmqContext = zmqContext;

    //TODO

  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public Map<String, String> getEndpointProperties() {
    //TODO
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
    //TODO
  }

  @Override
  public void removeSubscriberEndpoint(Map<String, String> endpoint) {
    //TODO
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
    //TODO
  }

}
