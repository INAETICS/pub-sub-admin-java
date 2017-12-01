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
package org.inaetics.pubsub.spi.pubsubadmin;

import java.util.Map;

public abstract class TopicHandler {
  
  public static final String PUBSUB_ADMIN_TYPE = "pubsub.admin.type";

  private static int idCounter = 0;
  private int id = idCounter++;
  
  /**
   * Returns the topic of this TopicHandler
   */
  public abstract String getTopic();
  
  /**
   * Returns the properties that should be published by a DiscoveryManager
   */
  public abstract Map<String, String> getEndpointProperties();

  /**
   * Open the connection and optional endpoint. Only after opening is the provided endpoint valid.
   * When opening a TopicSender should register its Publish (factory) interface and a TopicReceiver should track matching 
   * Subscriber interfaces.
   */
  public abstract void open();

  /**
   * Closes the Handler. This should only be done if the endpoint info is removed from discovery.
   * When closing a TopicSender should unregister its Publish (factory) interface and a TopicReceiver should
   * stop tracking Subcriber interfaces.
   */
  public abstract void close();
  
  /**
   * Add a subscriber endpoint. If needed the Sender can connect to the endpoint.
   */
  public abstract void addSubcriberEndpoint(Map<String, String> endpoint);
  
  /**
   * Removes a subscriber endpoint. If needed the Sender can disconnect from the endpoint
   */
  public abstract void removeSubscriberEndpoint(Map<String, String> endpoint);
  
  /**
   * Add a publisher endpoint. If needed the Receiver can connect to the endpoint.
   */
  public abstract void addPublisherEndpoint(Map<String, String> endpoint);
  
  /**
   * Removes a publisher endpoint. If needed the Receiver can disconnect from the endpoint
   */
  public abstract void removePublisherEndpoint(Map<String, String> endpoint);
  
  /**
   * Checks if the TopicHandler is active. If it's not the TopologyManager will remove it.
   */
  public abstract boolean isActive();

  /**
   * Returns the ID.
   */
  public int getId() {
    return id;
  }
}
