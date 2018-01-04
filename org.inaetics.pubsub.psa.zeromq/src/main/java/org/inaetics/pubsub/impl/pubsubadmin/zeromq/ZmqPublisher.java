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

import org.inaetics.pubsub.api.pubsub.MultipartException;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.zeromq.ZMQ;

public class ZmqPublisher implements org.inaetics.pubsub.api.pubsub.Publisher {

  private String topic;
  private ZMQ.Socket socket;
  private Serializer serializer;

  public ZmqPublisher(String topic, ZMQ.Socket socket, Serializer serializer) {
    this.topic = topic;
    this.serializer = serializer;
    this.socket = socket;
  }

  @Override
  public void send(Object msg) {

  }

  @Override
  public synchronized void sendMultipart(Object msg, int flags) throws MultipartException {

  }

  public ZMQ.Socket getSocket() {
    return this.socket;
  }

}
