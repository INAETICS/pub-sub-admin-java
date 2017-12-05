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
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class ZmqSubscriber extends Thread {

  private BundleContext bundleContext =
          FrameworkUtil.getBundle(ZmqSubscriber.class).getBundleContext();
  private Serializer serializer;

  public ZmqSubscriber(Map<String, String> zmqProperties, String zmqTopic, String serializer) {
    //TODO
  }

  @Override
  public void run() {
    while (!this.isInterrupted()) {
      //TODO
    }
  }

private class SubscriberCaller extends Thread {

  public SubscriberCaller(Subscriber subscriber, BlockingQueue<MultipartContainer> queue) {
    //TODO
  }

  @Override
  public void run() {
    //TODO
  }

  }

  public void connect(Subscriber subscriber) {
    //TODO
  }

  public void disconnect(Subscriber subscriber) {
    //TODO
  }

  public boolean hasSubscribers() {
    //TODO
  }

  public void stopKafkaSubscriber() {
    //TODO
  }

  private KafkaStream<byte[], byte[]> getStream(String kafkaTopic) {
    //TODO
  }

  private ConsumerConfig createConsumerConfig(Map<String, String> properties) {
    //TODO
  }


  public String getKafkaTopic() {
    //TODO
  }


}
