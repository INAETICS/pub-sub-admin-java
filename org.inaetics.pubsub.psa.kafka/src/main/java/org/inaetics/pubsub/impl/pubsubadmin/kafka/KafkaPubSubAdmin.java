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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.dm.annotation.api.*;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.spi.pubsubadmin.TopicSender;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

@Component
public class KafkaPubSubAdmin implements PubSubAdmin, ManagedService {

  public static final String SERVICE_PID = KafkaPubSubAdmin.class.getName();

  private KafkaProducerFactory producerFactory = new KafkaProducerFactory();

  private Map<String, String> defaultSubscriberProperties = new HashMap<>();
  private Map<String, String> defaultPublisherProperties = new HashMap<>();

  private static final Set<String> kafkaProperties = new HashSet<>(Arrays.asList(
      "bootstrap.servers,acks,buffer.memory,compression.type,retries,ssl.key.password,ssl.keystore.location,ssl.keystore.password,ssl.truststore.location,ssl.truststore.password,batch.size,client.id,connections.max.idle.ms,linger.ms,max.block.ms,max.request.size,partitioner.class,receive.buffer.bytes,request.timeout.ms,sasl.kerberos.service.name,security.protocol,send.buffer.bytes,ssl.enabled.protocols,ssl.keystore.type,ssl.protocol,ssl.provider,ssl.truststore.type,timeout.ms,block.on.buffer.full,max.in.flight.requests.per.connection,metadata.fetch.timeout.ms,metadata.max.age.ms,metric.reporters,metrics.num.samples,metrics.sample.window.ms,reconnect.backoff.ms,retry.backoff.ms,sasl.kerberos.kinit.cmd,sasl.kerberos.min.time.before.relogin,sasl.kerberos.ticket.renew.jitter,sasl.kerberos.ticket.renew.window.factor,ssl.cipher.suites,ssl.endpoint.identification.algorithm,ssl.keymanager.algorithm,ssl.trustmanager.algorithm,group.id,zookeeper.connect,consumer.id,socket.timeout.ms,socket.receive.buffer.bytes,fetch.message.max.bytes,num.consumer.fetchers,auto.commit.enable,auto.commit.interval.ms,queued.max.message.chunks,rebalance.max.retries,fetch.min.bytes,fetch.wait.max.ms,rebalance.backoff.ms,refresh.leader.backoff.ms,auto.offset.reset,consumer.timeout.ms,exclude.internal.topics,client.id,zookeeper.session.timeout.ms,zookeeper.connection.timeout.ms,zookeeper.sync.time.ms,offsets.storage,offsets.channel.backoff.ms,offsets.channel.socket.timeout.ms,offsets.commit.max.retries,dual.commit.enabled,partition.assignment.strategy"
          .split(",")));
  private static final Set<String> generalProperties =
      new HashSet<>(Arrays.asList("serializer,pubsub.topic,pubsub.scope".split(",")));
  
  
  private volatile LogService m_LogService;

  @Init
  void init(){
    System.out.println("INITIALIZED " + this.getClass().getName());
  }

  @Start
  protected final void start() throws Exception {
    System.out.println("STARTED " + this.getClass().getName());
  }

  @Stop
  protected final void stop() throws Exception {
    System.out.println("STOPPED " + this.getClass().getName());
  }

  @Destroy
  void destroy() {
    System.out.println("DESTROYED " + this.getClass().getName());
  }

  private Map<String, String> getPublisherProperties(Bundle bundle, Filter filter) {
    Map<String, String> filterProperties = Utils.verySimpleLDAPFilterParser(filter);

    String topic = Utils.getTopicFromProperties(filterProperties);
    Map<String, String> properties = getPublisherProperties(bundle, topic);
    
    properties.putAll(filterProperties);
    return properties;
  }

  private Map<String, String> getSubscriberProperties(Bundle bundle,
      ServiceReference<Subscriber> reference) {
    Map<String, String> referenceProperties = Utils.getPropertiesFromReference(reference);

    String topic = Utils.getTopicFromProperties(referenceProperties);
    Map<String, String> properties = getSubscriberProperties(bundle, topic);

    properties.putAll(referenceProperties);
    return properties;
  }

  private Map<String, String> getPublisherProperties(Bundle bundle, String topic) {
    Map<String, String> properties = getProperties(bundle, topic, "pub");
    properties.putAll(getProperties(bundle, topic, "kafkapub"));
    return properties;
  }

  private Map<String, String> getSubscriberProperties(Bundle bundle, String topic) {
    Map<String, String> properties = getProperties(bundle, topic, "sub");
    properties.putAll(getProperties(bundle, topic, "kafkasub"));
    return properties;
  }

  private Map<String, String> putDefaultPublisherProperties(Map<String, String> properties) {
    for (String key : defaultPublisherProperties.keySet()) {
      if (!properties.containsKey(key)) {
        properties.put(key, defaultPublisherProperties.get(key));
      }
    }
    return properties;
  }

  private Map<String, String> putDefaultSubscriberProperties(Map<String, String> properties) {
    for (String key : defaultSubscriberProperties.keySet()) {
      if (!properties.containsKey(key)) {
        properties.put(key, defaultSubscriberProperties.get(key));
      }
    }
    return properties;
  }

  @Override
  public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    System.out.println("UPDATED " + this.getClass().getName());

    if (properties != null) {
      defaultPublisherProperties = new HashMap<>();
      defaultSubscriberProperties = new HashMap<>();
      Enumeration<String> keys = properties.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        if (key.startsWith("pub:")) {
          defaultPublisherProperties.put(key.substring(4), (String) properties.get(key));
        } else if (key.startsWith("sub:")) {
          defaultSubscriberProperties.put(key.substring(4), (String) properties.get(key));
        }
      }
    }
  }

  @Override
  public synchronized double matchPublisher(Bundle requester, Filter filter) {
    double score = 0;
    Map<String, String> properties = getPublisherProperties(requester, filter);
    return match(properties);
  }

  @Override
  public synchronized TopicSender createTopicSender(Bundle requester, Filter filter) {
    Map<String, String> properties = getPublisherProperties(requester, filter);
    putDefaultPublisherProperties(properties);

    KafkaTopicSender topicSender =
        new KafkaTopicSender(producerFactory, getKafkaProperties(properties),
            Utils.getTopicFromProperties(properties), properties.get(Serializer.SERIALIZER));
    
    return topicSender;
  }

  @Override
  public synchronized double matchSubscriber(ServiceReference ref) {
    double score = 0;
    Map<String, String> properties = getSubscriberProperties(ref.getBundle(), ref);
    return match(properties);
  }
  
  private static double match(Map<String, String> properties) {
    double score = 0;
    for (String key : properties.keySet()) {
      if (generalProperties.contains(key)) {
        score += 1;
      } else if (kafkaProperties.contains(key)) {
        score += 2;
      } else if (key.equals(PUBSUB_ADMIN_CONFIG) && properties.get(key).equals("kafka")) {
        score += 100;
      } else if (key.equals(PUBSUB_INTENT_LATE_JOINER_SUPPORT) && properties.get(key).equals("late-joiner-support")) {
        score += 25;
      } else if (key.equals(PUBSUB_INTENT_RELIABLE) && properties.get(key).equals("reliable")) {
        score += 25;
      }
    }
    return score;
  }

  @Override
  public synchronized TopicReceiver createTopicReceiver(ServiceReference reference) {
    Map<String, String> properties = getSubscriberProperties(reference.getBundle(), reference);
    putDefaultSubscriberProperties(properties);
    String topic = Utils.getTopicFromProperties(properties);
    KafkaTopicReceiver topicReceiver = new KafkaTopicReceiver(properties, topic);
    return topicReceiver;
  }

  private static Map<String, String> getKafkaProperties(Map<String, String> properties) {
    Map<String, String> result = new HashMap<>();

    for (String key : properties.keySet()) {
      if (kafkaProperties.contains(key)) {
        result.put(key, properties.get(key));
      }
    }

    return result;
  }

  private Map<String, String> getProperties(Bundle bundle, String topic, String extension) {
    URL url = bundle.getResource(Constants.PUBSUB_CONFIG_PATH + topic + "." + extension);
    Properties properties = new Properties();
    try {
      if (url != null) {
        InputStream stream = url.openStream();
        properties.load(stream);
      }
    } catch (IOException e) {
      m_LogService.log(LogService.LOG_WARNING, e.getMessage(), e);
    }
    return Utils.propertiesToMap(properties);
  }
}
