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
package org.inaetics.pubsub.api.pubsubadmin;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public interface PubSubAdmin {

  public static final String PUBSUB_ADMIN_CONFIG = "pubsub.admin.config";
  public static final String PUBSUB_INTENT_RELIABLE = "pubsub.intent.reliable";
  public static final String PUBSUB_INTENT_LATE_JOINER_SUPPORT = "pubsub.intent.latejoiner";
  
  /**
   * Inspects if there is a match with requested publisher. This will only be called if requested topic
   * does not yet have a TopicSender (responsibility of TopologyManager).
   */
  public double matchPublisher(Bundle requester, Filter filter); 
  
  /**
   * Creates a TopicSender. Info from the filter and bundle (extender pattern) can be used to configure the sender.
   * Note that this is only true for the first match. After that the TopicSender will verify if the requested 
   * publisher is possible. If not this is considered an error -> configuration issue. 
   */
  public TopicSender createTopicSender(Bundle requester, Filter filter);
  
  /**
   * Inspect if there is a match with provided subscriber. Note this will only be called there is not yet a
   * a TopcReceiver with the topic provided by the subscriber. 
   */
  public double matchSubscriber(ServiceReference ref);
  
  /**
   * Creates a TopicReceiver. info from the properties and bundle (extender pattern) can be used to configure the
   * receiver. Note that this is only true for the first match. After that the TopicSender will verify if the requested 
   * publisher is possible. If not this is considered an error -> configuration issue. 
   */
  public TopicReceiver createTopicReceiver(ServiceReference ref);
}
