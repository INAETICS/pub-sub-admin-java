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
package org.inaetics.pubsub.api.discovery;

import java.util.List;
import java.util.Map;

public interface DiscoveryManager {
  public final static String FRAMEWORK_UUID = "framework.uuid";
  public final static String SERVICE_ID = "service.id";
  public final static String PUBSUB_TYPE = "pubsub.type";

  /**
   * Announce a Publisher on the network
   * @param info
   */
  public void announcePublisher(Map<String, String> properties);
  
  /**
   * Remove a Publisher from the network
   * @param info
   */
  public void removePublisher(Map<String, String> properties);
  
  /**
   * Announce a Subscriber on the network
   * @param info
   */
  public void announceSubscriber(Map<String, String> properties);
  
  /**
   * Remove a Subscriber from the network
   * @param info
   */
  public void removeSubscriber(Map<String, String> properties);
  
  /**
   * Returns all currently active Subscribers
   * @return
   */
  public List<Map<String, String>> getCurrentSubscriberEndPoints();
  
  /**
   * Returns all currently active Publishers
   * @return
   */
  public List<Map<String, String>> getCurrentPublisherEndPoints();
}
