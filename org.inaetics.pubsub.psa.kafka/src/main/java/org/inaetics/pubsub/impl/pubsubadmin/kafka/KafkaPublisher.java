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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.inaetics.pubsub.api.pubsub.MultipartException;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Utils;

public class KafkaPublisher implements org.inaetics.pubsub.api.pubsub.Publisher {
  private final String topic;
  private final KafkaProducer<byte[], byte[]> producer;
  private Serializer serializer;
  private MultipartContainer multipartContainer;


  public KafkaPublisher(String topic, KafkaProducer<byte[], byte[]> producer,
      Serializer serializer) {
    
    this.topic = topic;
    this.producer = producer;
    this.serializer = serializer;
  }

  @Override
  public void send(Object msg) {
    send(msg, 0);
  }

  @Override
  public void send(Object msg, int msgTypeId) {
    MultipartContainer container = new MultipartContainer();
    container.addObject(msg);
    producer.send(new ProducerRecord<byte[], byte[]>(topic, serializer.serialize(container)));
  }

  @Override
  public synchronized void sendMultipart(Object msg, int flags) throws MultipartException {
    sendMultipart(msg,0, flags);
  }

  @Override
  public void sendMultipart(Object msg, int msgTypeId, int flags) throws MultipartException {
    boolean objectAdded = false;

    if ((flags & Publisher.PUBLISHER_FIRST_MSG) > 0) {
      if (multipartContainer == null) {
        multipartContainer = new MultipartContainer();
        multipartContainer.addObject(msg);
        objectAdded = true;
      } else {
        throw new MultipartException("Can't have 2 first messages in a multipart send.");
      }
    }

    if (multipartContainer == null) {
      throw new MultipartException("No first message sent");
    }

    if ((flags & Publisher.PUBLISHER_PART_MSG) > 0) {
      if (!objectAdded) {
        multipartContainer.addObject(msg);
        objectAdded = true;
      }
    }

    if ((flags & Publisher.PUBLISHER_LAST_MSG) > 0) {
      if (!objectAdded) {
        multipartContainer.addObject(msg);
        objectAdded = true;
      }
      byte[] data = serializer.serialize(multipartContainer);
      producer.send(new ProducerRecord<byte[], byte[]>(topic, data));
      multipartContainer = null;
    }
  }

  @Override
  public int localMsgTypeIdForMsgType(String msgType) {
    // Not used for Kafka
    return 0;
  }


  public KafkaProducer<byte[], byte[]> getProducer() {
    return producer;
  }

}
