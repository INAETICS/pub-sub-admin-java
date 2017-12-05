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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.inaetics.pubsub.spi.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.spi.pubsubadmin.TopicSender;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class KafkaTopicSender extends TopicSender {

  private final String topic;
  private final String kafkaTopic;
  private final String serializerString;;
  private final KafkaProducerFactory factory;
  private KafkaProducer<byte[], byte[]> producer;
  private final Serializer serializer;
  private final Map<String, String> kafkaProperties;
  private ServiceTracker tracker;
  private final Map<Bundle, Publisher> publishers = new HashMap<>();
  private BundleContext bundleContext =
      FrameworkUtil.getBundle(KafkaTopicSender.class).getBundleContext();
  private ServiceRegistration<ServiceFactory<Publisher>> registration;

  public KafkaTopicSender(KafkaProducerFactory factory, Map<String, String> kafkaProperties,
      String topic, String serializer) {
    super();
    this.topic = topic;
    this.factory = factory;
    this.kafkaProperties = kafkaProperties;


    BundleContext bundleContext =
        FrameworkUtil.getBundle(KafkaTopicSender.class).getBundleContext();
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

    this.kafkaTopic = topic + "." + serializerString;

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
    properties.put(Constants.PUBSUB_TYPE, Constants.PUBLISHER);
    properties.put(Serializer.SERIALIZER, serializerString);
    properties.put(KafkaConstants.KAFKA_TOPIC, kafkaTopic);
    properties.put(PUBSUB_ADMIN_TYPE, KafkaConstants.KAFKA);

    return properties;
  }

  @Override
  public void open() {
    producer = factory.getKafkaProducer(kafkaProperties, this);
    
    Dictionary properties = new Hashtable<>();
    properties.put(Publisher.PUBSUB_TOPIC, topic);

    registration = bundleContext.registerService(Publisher.class, this, properties);
  }

  @Override
  public void close() {
    factory.removeProducer(producer, this);
    tracker.close();
    registration.unregister();
  }

  @Override
  public void addSubscriberEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public void removeSubscriberEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public void addPublisherEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public void removePublisherEndpoint(Map<String, String> endpoint) {
    // Not used for Kafka
  }

  @Override
  public boolean isActive() {
    return !publishers.isEmpty();
  }

  @Override
  public Publisher getService(Bundle bundle, ServiceRegistration<Publisher> registration) {
    if (publishers.get(bundle) == null) {
      Publisher publisher = new KafkaPublisher(kafkaTopic, producer, serializer);
      publishers.put(bundle, publisher);
      return publisher;
    } else {
      return publishers.get(bundle);
    }
  }

  @Override
  public void ungetService(Bundle bundle, ServiceRegistration<Publisher> registration,
      Publisher service) {

    publishers.remove(bundle);

  }


}
