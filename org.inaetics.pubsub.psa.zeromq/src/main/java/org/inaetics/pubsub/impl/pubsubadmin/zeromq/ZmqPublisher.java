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

public class ZmqPublisher implements org.inaetics.pubsub.api.pubsub.Publisher {

  private String topic;
  private ZMQ.Socket socket;
  private Serializer serializer;
  private MultipartContainer multipartContainer;

  private final int FIRST_SEND_DELAY = 2; // in seconds
  private boolean firstSend = true;

  public ZmqPublisher(String topic, ZMQ.Socket socket, Serializer serializer) {
    this.topic = topic;
    this.serializer = serializer;
    this.socket = socket;
  }

  @Override
  public void send(Object msg) {
    send(msg, 0);
  }

  @Override
  public void send(Object msg, int msgTypeId) {

    try {
      sendMultipart(msg, msgTypeId, Publisher.PUBLISHER_FIRST_MSG | Publisher.PUBLISHER_LAST_MSG);
    } catch (MultipartException e) {
      System.out.println("Error in send() method: " + e.getMessage());
    }

  }

  @Override
  public void sendMultipart(Object msg, int flags) throws MultipartException {
    sendMultipart(msg,0, flags);
  }

  @Override
  public synchronized void sendMultipart(Object msg, int msgTypeId, int flags) throws MultipartException {

    boolean objectAdded = false;

    switch(flags){
      case Publisher.PUBLISHER_FIRST_MSG:
        if (multipartContainer == null) {
          multipartContainer = new MultipartContainer();
          multipartContainer.addObject(msg);
          objectAdded = true;
        } else {
          throw new MultipartException("Can't have 2 first messages in a multipart send.");
        }
        break;

      case Publisher.PUBLISHER_PART_MSG:
        if (multipartContainer == null) {
          throw new MultipartException("No first message sent");
        }
        if (!objectAdded) {
          multipartContainer.addObject(msg);
          objectAdded = true;
        }

        break;

      case Publisher.PUBLISHER_LAST_MSG:
        if (!objectAdded) {
          multipartContainer.addObject(msg);
          objectAdded = true;
        }
        send_pubsub_mp_msg(multipartContainer, msgTypeId);
        multipartContainer = null;

        break;

      case Publisher.PUBLISHER_FIRST_MSG | Publisher.PUBLISHER_LAST_MSG: //Normal send case
        MultipartContainer container = new MultipartContainer();
        container.addObject(msg);
        send_pubsub_msg(serializer.serialize(container), msgTypeId, msg.getClass().getName(), true);
        break;

      default:
        System.out.println(this.getClass().getSimpleName() + ": ERROR: Invalid MP flags combination");
        break;
    }

  }

  @Override
  public int localMsgTypeIdForMsgType(String msgType) {
    return Utils.stringHash(msgType);
  }

  private synchronized boolean send_pubsub_msg(byte[] msg, int msgTypeId, String msgTypeClassName, boolean last){

    boolean success = true;

    ZFrame headerMsg = new ZFrame(createHdrMsg(msgTypeId, msgTypeClassName));
    ZFrame payloadMsg = new ZFrame(msg);

    delay_first_send_for_late_joiners();

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

  private synchronized boolean send_pubsub_mp_msg(MultipartContainer container, int msgTypeId) {

    boolean success = true;

    int containerSize = container.getObjects().size();
    for (int i = 0; i < containerSize; i++){
      success = success && send_pubsub_msg(serializer.serialize(container.getObjects().get(i)),
              msgTypeId, container.getClasses().get(i), (i==containerSize-1));
    }

    return success;

  }

  private byte[] createHdrMsg(int msgTypeId, String msgTypeClassName) {

    // Given the following struct in C:
    // struct pubsub_msg_header{
    //   char topic[MAX_TOPIC_LEN];
    //   unsigned int type;
    //   unsigned char major;
    //   unsigned char minor;
    //   char className[MAX_CLASS_LEN]; // TODO: Not in Celix (yet?)
    // };
    //
    // The bytebuffer needs to be the exact same length in C and Java

    int byteBufferLength = (Constants.MAX_TOPIC_LEN * Constants.CHAR_SIZE) +
            Constants.UNSIGNED_INT_SIZE +
            Constants.CHAR_SIZE +
            Constants.CHAR_SIZE +
            Constants.MAX_CLASS_LEN;

    byte[] buff = new byte[byteBufferLength];

    byte[] topicBytes = this.topic.getBytes();
    if (this.topic.length() >= Constants.MAX_TOPIC_LEN){
      System.arraycopy(topicBytes,
              0,
              buff,
              0,
              Constants.MAX_TOPIC_LEN - 2);

      buff[Constants.MAX_TOPIC_LEN - 1] = '\0'; // terminate topic
    } else {
      System.arraycopy(topicBytes,
              0,
              buff,
              0,
              this.topic.getBytes().length);
    }

    buff[Constants.MAX_TOPIC_LEN]     = (byte) (msgTypeId >> 24);
    buff[Constants.MAX_TOPIC_LEN + 1] = (byte) (msgTypeId >> 16);
    buff[Constants.MAX_TOPIC_LEN + 2] = (byte) (msgTypeId >> 8);
    buff[Constants.MAX_TOPIC_LEN + 3] = (byte) (msgTypeId);

    buff[Constants.MAX_TOPIC_LEN + 4] = '1';
    buff[Constants.MAX_TOPIC_LEN + 5] = '0';

    int startPositionCN = Constants.MAX_TOPIC_LEN + 6;
    int endPositionCN = byteBufferLength - 1;

    byte[] classNameBytes = msgTypeClassName.getBytes();

    if (msgTypeClassName.length() >= Constants.MAX_CLASS_LEN){
      // class name is to long and must be truncated
      System.arraycopy(classNameBytes, 0, buff, startPositionCN, endPositionCN - 1);
      buff[endPositionCN] = '\0'; // terminate topic

    } else {
      System.arraycopy(classNameBytes, 0, buff, startPositionCN, classNameBytes.length);
    }

    buff[endPositionCN] = '\0';

    return buff;
  }

  private void delay_first_send_for_late_joiners() {
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
