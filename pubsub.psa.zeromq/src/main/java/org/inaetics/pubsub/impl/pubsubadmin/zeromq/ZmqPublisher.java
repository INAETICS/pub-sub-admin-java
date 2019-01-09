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
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

public class ZmqPublisher implements org.inaetics.pubsub.api.pubsub.Publisher {

  private final String scope;
  private final String topic;
  private final ZMQ.Socket socket;
  private final Serializer serializer;
  private final ZFrame filterFrame;

  private final int FIRST_SEND_DELAY = 2; // in seconds
  private boolean firstSend = true;

  public ZmqPublisher(String scope, String topic, ZMQ.Socket socket, Serializer serializer) {
    this.scope = scope == null ? "default" : scope;
    this.topic = topic;
    this.socket = socket;
    this.serializer = serializer;

    String filter = this.scope.length() >= 2 ? this.scope.substring(0,2) : "EE";
    filter += this.topic.length() >= 2 ? this.topic.substring(0, 2) : "EE";
    filterFrame = new ZFrame(filter);
  }

  public void connectTo(String url) {
      socket.connect(url);
  }

  public void disconnectFrom(String url) {
      socket.disconnect(url);
  }

  @Override
  public void send(Object msg) {
    boolean success = sendRawMsg(serializer.serialize(msg), msg.getClass());
    if (!success) {
      //TODO log warning
    }
  }


  private boolean sendRawMsg(byte[] msg, Class<?> msgClazz) {

    boolean success = true;

    //Then a header containing type hash (unsigned int) and version info (major/minor) as two chars.
    ZFrame headerFrame = headerFrameFor(msgClazz);

    //Then the actual payload
    ZFrame payloadFrame = new ZFrame(msg);

    delayFirstSendForLateJoiners();

    success = filterFrame.send(this.socket, ZFrame.MORE);
    if (success) {
      success = headerFrame.send(this.socket, ZFrame.MORE);
    }
    if (success) {
      success = payloadFrame.send(this.socket, 0);
    }

    return success;
  }

  private ZFrame headerFrameFor(Class<?> clazz) {
    byte[] headerBytes = new byte[6]; //first 4 hashcode of the class
    int hash = Utils.stringHash(clazz.getName());
    headerBytes[0] = (byte)(hash & 0xFF);
    headerBytes[1] = (byte)((hash >> 8) & 0xFF);
    headerBytes[2] = (byte)((hash >> 16) & 0xFF);
    headerBytes[3] = (byte)((hash >> 24) & 0xFF);
    headerBytes[4] = 0; /*TODO major version*/
    headerBytes[4] = 0; /*TODO minor version*/
    return new ZFrame(headerBytes);
  }

  private void delayFirstSendForLateJoiners() {
    if(firstSend){
      System.out.println(this.getClass().getSimpleName() + ": Delaying first send for late joiners...");
      try {
        Thread.sleep(FIRST_SEND_DELAY * 1000);
      } catch (InterruptedException e) {
        System.out.println(this.getClass().getSimpleName() + ": Something wrong with sleeping!");
      }
      firstSend = false;
    }
  }

}
