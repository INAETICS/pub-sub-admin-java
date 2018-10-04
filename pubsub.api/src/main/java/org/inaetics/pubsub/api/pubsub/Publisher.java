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
package org.inaetics.pubsub.api.pubsub;

public interface Publisher {
  public static final String PUBSUB_TOPIC = "pubsub.topic";
  public static final String PUBSUB_SCOPE = "pubsub.scope";
  public static final String PUBSUB_STRATEGY = "pubsub.strategy";
  public static final String PUBSUB_CONFIG = "pubsub.config";
  public static final String PUBSUB_ENDPOINT_URL = "pubsub.endpoint";

  // flags
  public static final int PUBLISHER_FIRST_MSG = 01;
  public static final int PUBLISHER_PART_MSG = 02;
  public static final int PUBLISHER_LAST_MSG = 04;

  public void send(Object msg);

  public void send(Object msg, int msgTypeId);

  public void sendMultipart(Object msg, int flags) throws MultipartException;

  public void sendMultipart(Object msg, int msgTypeId, int flags) throws MultipartException;

  public int localMsgTypeIdForMsgType(String msgType);

}
