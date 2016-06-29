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
package test.org.inaetics.pubsub.impl.discovery.etcd;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.impl.utils.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;


public class TestTopologyManager implements EventHandler {
  public Set<Map<String, String>> discoveredPublishers = new HashSet<>();
  public Set<Map<String, String>> discoveredSubscribers = new HashSet<>();
  private volatile DiscoveryManager discoveryManager;
  private volatile BundleContext context;
  private SecureRandom random = new SecureRandom();


  /**
   * Implementation of EventHandler. Used to receive messages from DiscoveryManagers about the
   * creation/deletion of endpoints.
   */
  @Override
  public void handleEvent(Event event) {
    String eventType = (String) event.getProperty(Constants.DISCOVERY_EVENT_TYPE);
    Map<String, String> info = (Map<String, String>) event.getProperty(Constants.DISCOVERY_INFO);

    switch (eventType) {
      case Constants.DISCOVERY_EVENT_TYPE_CREATED:
        if (info.get(Constants.PUBSUB_TYPE).equals(Constants.PUBLISHER)) {
          discoveredPublishers.add(info);
        } else if (info.get(Constants.PUBSUB_TYPE).equals(Constants.SUBSCRIBER)) {
          discoveredSubscribers.add(info);
        }
        break;
      case Constants.DISCOVERY_EVENT_TYPE_DELETED:
        if (info.get(Constants.PUBSUB_TYPE).equals(Constants.PUBLISHER)) {
          discoveredPublishers.remove(info);
        } else if (info.get(Constants.PUBSUB_TYPE).equals(Constants.SUBSCRIBER)) {
          discoveredSubscribers.remove(info);
        }
        break;
      default:
        break;
    }
  }

  private Map<String, String> randomEndpoint() {
    Map<String, String> endpoint = new HashMap<>();
    endpoint.put(Publisher.PUBSUB_TOPIC, new BigInteger(130, random).toString(32));
    endpoint.put("service.id", new BigInteger(130, random).toString(32));
    endpoint.put(Constants.PUBSUB_TYPE, Constants.PUBLISHER);
    return endpoint;
  }

  protected final void start() {
    System.out.println("STARTED " + this.getClass().getName());
    new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          Set<Map<String, String>> endpoints = new HashSet<>();
          for (int i = 0; i < 10; i++) {
            Map<String, String> endpoint = randomEndpoint();
            endpoints.add(endpoint);
            discoveryManager.announcePublisher(endpoint);
          }

          Thread.sleep(2000);

          for (Map<String, String> map : endpoints) {
            if (!discoveredPublishers.contains(map)) {
              throw new Exception("ERROR in " + this.getClass().getName() + " discovered missing");
            }
            discoveryManager.removePublisher(map);
          }


          Thread.sleep(2000);


          if (!discoveredPublishers.isEmpty()) {
            throw new Exception("ERROR in " + this.getClass().getName() + " discovered not empty");
          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } finally {
          System.out.println("EtcdDiscoveryManager Test Finished");
        }
      }
    }).start();

  }


}
