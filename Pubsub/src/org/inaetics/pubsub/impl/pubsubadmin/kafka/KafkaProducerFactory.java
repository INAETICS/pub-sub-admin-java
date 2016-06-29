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
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.producer.KafkaProducer;

import com.sun.org.apache.regexp.internal.recompile;

public class KafkaProducerFactory {
  private Map<Map<String, String>, KafkaProducer<byte[], byte[]>> producerPropertiesMap = new HashMap<>();
  private Map<KafkaProducer<byte[], byte[]>, Set<KafkaTopicSender>> producerUsersMap = new HashMap<>();

  public KafkaProducer<byte[], byte[]> getKafkaProducer(Map<String, String> properties, KafkaTopicSender user) {
   try {
    
  
    String keySerializer = "org.apache.kafka.common.serialization.ByteArraySerializer";
    String valueSerializer = "org.apache.kafka.common.serialization.ByteArraySerializer";
    
    Map<String, String> producerProperties = new HashMap<>();
    producerProperties.putAll(properties);
    producerProperties.put("key.serializer", keySerializer);
    producerProperties.put("value.serializer", valueSerializer);
    
    for (Map<String, String> p : producerPropertiesMap.keySet()) {
      if (p.equals(producerProperties)) {
        producerUsersMap.get(producerPropertiesMap.get(p)).add(user);
        return producerPropertiesMap.get(p);
      }
    }
    
    Properties kafkaProperties = new Properties();
    kafkaProperties.putAll(producerProperties);
    KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(kafkaProperties);
    
    Set<KafkaTopicSender> senders = new HashSet<>();
    senders.add(user);
    producerUsersMap.put(producer, senders);
    producerPropertiesMap.put(producerProperties, producer);
    
    return producer;
    } catch (Exception e) {
    e.printStackTrace();
  }
    return null;
  }
  
  public void removeProducer(KafkaProducer<byte[], byte[]> producer, KafkaTopicSender user) {
    producerUsersMap.get(producer).remove(user);
    if (producerUsersMap.get(producer).isEmpty()) {
      producerPropertiesMap.values().remove(producer);
    }
  }

}
