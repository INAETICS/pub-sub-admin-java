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
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

public class ZmqPublisher implements org.inaetics.pubsub.api.pubsub.Publisher {

  private String topic;
  private ZMQ.Socket socket;
  private Serializer serializer;
  private MultipartContainer multipartContainer;

  public ZmqPublisher(String topic, ZMQ.Socket socket, Serializer serializer) {
    this.topic = topic;
    this.serializer = serializer;
    this.socket = socket;
  }

  @Override
  public void send(Object msg) {

    MultipartContainer container = new MultipartContainer();
    container.addObject(msg);
    send_pubsub_msg(serializer.serialize(container), true);

  }

  @Override
  public synchronized void sendMultipart(Object msg, int flags) throws MultipartException {

    boolean objectAdded = false;

    if ((flags & (Publisher.PUBLISHER_FIRST_MSG | Publisher.PUBLISHER_LAST_MSG)) > 0) {
      //Normal send case
      MultipartContainer container = new MultipartContainer();
      container.addObject(msg);
      send_pubsub_msg(serializer.serialize(container), true);
    } else {

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
        send_pubsub_msg(serializer.serialize(multipartContainer), true);
        multipartContainer = null;
      }

    }

  }

  private synchronized boolean send_pubsub_msg(byte[] msg, boolean last){

    boolean success = true;

    ZFrame headerMsg = new ZFrame(this.topic); // TODO: Change header message to be compliant with Celix
    ZFrame payloadMsg = new ZFrame(msg);

    if (!headerMsg.send(this.socket, ZFrame.MORE)) success = false;

    if (!last){
      if (!payloadMsg.send(this.socket, ZFrame.MORE)) success = false;
    } else {
      if (!payloadMsg.send(this.socket, 0)) success = false;
    }

    if (!success){
      headerMsg.destroy();
      payloadMsg.destroy();
    }

    return success;
  }

  public ZMQ.Socket getSocket() {
    return this.socket;
  }

}
