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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.api.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.api.serialization.Serializer;
import org.inaetics.pubsub.impl.utils.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class KafkaTopicReceiver extends TopicReceiver {

  private final Set<Subscriber> subscribers = new HashSet<>();
  private final Map<String, String> kafkaTopicsToSerializer = new HashMap<>();
  private final Set<KafkaSubscriber> kafkaSubscribers = new HashSet<>();
  private final Map<String, String> kafkaProperties;
  private final BundleContext bundleContext =
      FrameworkUtil.getBundle(KafkaTopicReceiver.class).getBundleContext();
  private final String topic;
  private boolean open = false;

  public KafkaTopicReceiver(Map<String, String> kafkaProperties, String topic) {
    this.kafkaProperties = kafkaProperties;
    this.topic = topic;
    
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
    properties.put(PUBSUB_ADMIN_TYPE, KafkaConstants.KAFKA);

    return properties;
  }

  @Override
  public void open() {
    for (String kafkaTopic : kafkaTopicsToSerializer.keySet()) {
      KafkaSubscriber kafkaSubscriber =
          new KafkaSubscriber(kafkaProperties, kafkaTopic, kafkaTopicsToSerializer.get(kafkaTopic));
      kafkaSubscriber.start();
      for (Subscriber subscriber : subscribers) {
        kafkaSubscriber.connect(subscriber);
      }
      kafkaSubscribers.add(kafkaSubscriber);
    }
    this.open = true;
  }

  @Override
  public void close() {
    for (KafkaSubscriber kafkaSubscriber : kafkaSubscribers) {
      kafkaSubscriber.stopKafkaSubscriber();
    }
  }

  @Override
  public void addSubcriberEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public void removeSubscriberEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public void addPublisherEndpoint(Map<String, String> endpoint) {
    if (endpoint.get(PUBSUB_ADMIN_TYPE).equals(KafkaConstants.KAFKA)
        && endpoint.get(Publisher.PUBSUB_TOPIC).equals(topic)) {
      String kafkaTopic = endpoint.get(KafkaConstants.KAFKA_TOPIC);
      String serializer = endpoint.get(Serializer.SERIALIZER);
      
      if (!kafkaTopicsToSerializer.containsKey(kafkaTopic)) {
        kafkaTopicsToSerializer.put(kafkaTopic, serializer);

        if (open) {
          KafkaSubscriber kafkaSubscriber =
              new KafkaSubscriber(kafkaProperties, kafkaTopic, serializer);

          for (Subscriber subscriber : subscribers) {
            kafkaSubscriber.connect(subscriber);
          }
          kafkaSubscribers.add(kafkaSubscriber);
          kafkaSubscriber.start();
        }
      }
    }
  }

  @Override
  public void removePublisherEndpoint(Map<String, String> endpoint) {
    if (endpoint.get(PUBSUB_ADMIN_TYPE).equals(KafkaConstants.KAFKA)
        && endpoint.get(Publisher.PUBSUB_TOPIC).equals(topic)) {
      String kafkaTopic = endpoint.get(KafkaConstants.KAFKA_TOPIC);
      kafkaTopicsToSerializer.remove(kafkaTopic);

      for (KafkaSubscriber kafkaSubscriber : kafkaSubscribers) {
        if (kafkaSubscriber.getKafkaTopic().equals(kafkaTopic)) {
          kafkaSubscriber.stopKafkaSubscriber();
        }
      }
    }

  }

  @Override
  public void connectSubscriber(ServiceReference reference) {
    Subscriber subscriber = (Subscriber) bundleContext.getService(reference);
    subscribers.add(subscriber);
    for (KafkaSubscriber kafkaSubscriber : kafkaSubscribers) {
      kafkaSubscriber.connect(subscriber);
    }
    bundleContext.ungetService(reference);
  }

  @Override
  public void disconnectSubscriber(ServiceReference reference) {
    Subscriber subscriber = (Subscriber) bundleContext.getService(reference);
    subscribers.remove(subscriber);
    for (KafkaSubscriber kafkaSubscriber : kafkaSubscribers) {
      kafkaSubscriber.disconnect(subscriber);
    }
    bundleContext.ungetService(reference);

  }

  @Override
  public boolean isActive() {
    return !subscribers.isEmpty();
  }

}
