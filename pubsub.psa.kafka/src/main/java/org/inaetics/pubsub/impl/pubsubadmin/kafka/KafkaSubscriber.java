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
package org.inaetics.pubsub.impl.pubsubadmin.kafka;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

public class KafkaSubscriber extends Thread {
  private final Map<Subscriber, BlockingQueue<MultipartContainer>> subscribers =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<Subscriber, SubscriberCaller> subscriberCallers =
      Collections.synchronizedMap(new HashMap<>());
  private ConsumerConnector consumer;
  private KafkaStream<byte[], byte[]> stream;
  private final String kafkaTopic;
  private ConsumerIterator<byte[], byte[]> iterator;
  private Serializer serializer;
  private BundleContext bundleContext =
      FrameworkUtil.getBundle(KafkaSubscriber.class).getBundleContext();

  public KafkaSubscriber(Map<String, String> kafkaProperties, String kafkaTopic,
      String serializer) {
    
    try {
      consumer = kafka.consumer.Consumer
          .createJavaConsumerConnector(createConsumerConfig(kafkaProperties));

    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    this.kafkaTopic = kafkaTopic;
    this.stream = getStream(kafkaTopic);
    this.iterator = stream.iterator();

    try {
      ServiceTracker tracker = new ServiceTracker<>(bundleContext,
          bundleContext.createFilter("(&(objectClass=" + Serializer.class.getName() + ")" + "("
              + Serializer.SERIALIZER + "=" + serializer + "))"),
          null);
      tracker.open();
      this.serializer = (Serializer) tracker.waitForService(0);

    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    while (!this.isInterrupted()) {
      try {
        iterator.hasNext();
      } catch (Exception e) {
        // Catch the interrupted exception and quit.
        return;
      }

      byte[] message = iterator.next().message();
      MultipartContainer container = serializer.deserialize(message);
      synchronized (subscribers) {
        for (BlockingQueue<MultipartContainer> queue : subscribers.values()) {
          queue.add(container);
        }
      }
    }
  }

private class SubscriberCaller extends Thread {
  private BlockingQueue<MultipartContainer> queue;
  private Subscriber subscriber;

  public SubscriberCaller(Subscriber subscriber, BlockingQueue<MultipartContainer> queue) {
    this.subscriber = subscriber;
    this.queue = queue;
  }

  @Override
  public void run() {
    while (!this.isInterrupted()) {
      try {
        MultipartContainer container = queue.take();
        subscriber.receive(container.getObjects().get(0), container);
      } catch (InterruptedException e) {

      }
    }
  }

}

  public void connect(Subscriber subscriber) {
    BlockingQueue<MultipartContainer> queue = new LinkedBlockingQueue<MultipartContainer>();
    SubscriberCaller caller = new SubscriberCaller(subscriber, queue);
    this.subscribers.put(subscriber, queue);
    this.subscriberCallers.put(subscriber, caller);
    caller.start();
  }

  public void disconnect(Subscriber subscriber) {
    this.subscribers.remove(subscriber);
    this.subscriberCallers.get(subscriber).interrupt();
    this.subscriberCallers.remove(subscriber);
  }

  public boolean hasSubscribers() {
    return !subscribers.isEmpty();
  }

  public void stopKafkaSubscriber() {
    this.interrupt();
    for (Thread thread : subscriberCallers.values()) {
      thread.interrupt();
    }
    consumer.shutdown();
  }

  private KafkaStream<byte[], byte[]> getStream(String kafkaTopic) {
    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(kafkaTopic, new Integer(1));
    Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
        consumer.createMessageStreams(topicCountMap);
    List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(kafkaTopic);
    return streams.get(0);
  }

  private ConsumerConfig createConsumerConfig(Map<String, String> properties) {
    SecureRandom random = new SecureRandom();

    Properties props = new Properties();
    props.putAll(properties);
    String groupId =
        Utils.getFrameworkUUID(bundleContext) + "-" + new BigInteger(60, random).toString(32);
    props.put("group.id", groupId);
    return new ConsumerConfig(props);
  }


  public String getKafkaTopic() {
    return kafkaTopic;
  }


}
