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

import org.osgi.framework.ServiceReference;

public abstract class TopicReceiver extends TopicHandler{
  
  /**
   * Connect a Subscriber to the Receiver
   * @param reference
   */
  public abstract void connectSubscriber(ServiceReference reference);
  
  /**
   * Disconnect a Subscriber from the Receiver
   * @param reference
   */
  public abstract void disconnectSubscriber(ServiceReference reference);
}
