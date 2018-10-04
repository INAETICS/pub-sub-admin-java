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
package org.inaetics.pubsub.spi.utils;

public final class Constants {
  public static final String PUBSUB_CONFIG_PATH = "/pubsub/"; 

  public static final String DISCOVERY_EVENT = "org/ineatics/pubsub/discovery";
  public static final String DISCOVERY_EVENT_TYPE = "discovery.event.type";
  public static final String DISCOVERY_INFO = "discovery.info";
  public static final String DISCOVERY_EVENT_TYPE_CREATED = "discovery.event.type.created";
  public static final String DISCOVERY_EVENT_TYPE_DELETED = "discovery.event.type.deleted";
  
  public static final String PUBSUB_TYPE = "pubsub.type";
  public static final String PUBLISHER = "publisher";
  public static final String SUBSCRIBER = "subscriber";

  public static final int MAX_TOPIC_LEN = 1024;
  public static final int MAX_CLASS_LEN = 1024;

  public static final int UNSIGNED_INT_SIZE = 4;
  public static final int CHAR_SIZE = 1;

}
