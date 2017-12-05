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
package org.inaetics.pubsub.impl.topologymanager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.inaetics.pubsub.spi.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.pubsubadmin.TopicHandler;
import org.inaetics.pubsub.spi.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.spi.pubsubadmin.TopicSender;
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;


public class PubSubTopologyManager
    implements ListenerHook, ServiceListener, EventHandler, ManagedService {

  public final static String SERVICE_PID = PubSubTopologyManager.class.getName();

  private final List<PubSubAdmin> pubSubAdmins = new CopyOnWriteArrayList<>();
  private final List<DiscoveryManager> discoveryManagers = new CopyOnWriteArrayList<>();

  private BundleContext bundleContext =
      FrameworkUtil.getBundle(PubSubTopologyManager.class).getBundleContext();

  private volatile LogService m_LogService;

  private final Map<String, TopicSender> senders = new HashMap<>();

  private final Map<String, TopicReceiver> receivers = new HashMap<>();

  private final Set<Map<String, String>> publisherEndpoints = new HashSet<>();
  private final Set<Map<String, String>> subscriberEndpoints = new HashSet<>();

  private final Map<Publisher, ServiceRegistration<Publisher>> publishers = new HashMap<>();

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  public static volatile int DELETE_DELAY = 10;


  /**
   * Implementation of ListenerHook. Called when a bundle requests a Service. If the Service is a
   * Publisher, Add it to a TopicSender.
   */
  @Override
  public synchronized void added(Collection<ListenerInfo> listeners) {
    for (ListenerInfo info : listeners) {
      try {
        if (info.getFilter() != null) {
          Filter filter = FrameworkUtil.createFilter(info.getFilter());
          if (isFilterForPublisher(filter)) {

            if (!info.isRemoved()) {
              Bundle user = info.getBundleContext().getBundle();
              Map<String, String> filterProperties = Utils.verySimpleLDAPFilterParser(filter);
              String topic = Utils.getTopicFromProperties(filterProperties);
              if (senders.get(topic) == null) {
                TopicSender sender = getTopicSender(user, filter);
                
                if (sender != null) {

                  for (Map<String, String> publisher : publisherEndpoints) {
                    sender.addPublisherEndpoint(publisher);
                  }

                  for (Map<String, String> subscriber : subscriberEndpoints) {
                    sender.addSubscriberEndpoint(subscriber);
                  }

                  sender.open();
                  senders.put(topic, sender);

                  Map<String, String> endpoint = sender.getEndpointProperties();
                  for (DiscoveryManager discoveryManager : discoveryManagers) {
                    discoveryManager.announcePublisher(endpoint);

                  }
                }
              }
            }
          }
        }
      } catch (InvalidSyntaxException e) {
        // Requesters should provide valid filters. Skip non-valid
        m_LogService.log(LogService.LOG_WARNING, e.getMessage(), e);
        continue;
      }
    }
  }

  /**
   * Implementation of ListenerHook. Called when a bundle stops requesting a Service. If the Service
   * is a Publisher, remove it from its TopicSender.
   */
  @Override
  public synchronized void removed(Collection<ListenerInfo> listeners) {
    for (ListenerInfo info : listeners) {

      try {
        if (info.getFilter() != null) {
          Filter filter = FrameworkUtil.createFilter(info.getFilter());

          if (isFilterForPublisher(filter)) {

            Bundle user = info.getBundleContext().getBundle();
            Map<String, String> filterProperties = Utils.verySimpleLDAPFilterParser(filter);
            String topic = Utils.getTopicFromProperties(filterProperties);
            if (senders.get(topic) != null) {
              final TopicSender sender = senders.get(topic);

              executorService.schedule(new Runnable() {
                @Override
                public void run() {
                  synchronized (PubSubTopologyManager.class) {
                    if (!sender.isActive()) {
                      sender.close();
                      senders.remove(topic);

                      for (DiscoveryManager discoveryManager : discoveryManagers) {
                        discoveryManager.removePublisher(sender.getEndpointProperties());
                      }
                    }
                  }
                }
              }, DELETE_DELAY, TimeUnit.SECONDS);
            }
          }
        }
      } catch (InvalidSyntaxException e) {
        // Requesters should provide valid filters. Skip non-valid
        m_LogService.log(LogService.LOG_WARNING, e.getMessage(), e);
        continue;
      }
    }
  }

  /**
   * Implementation of ServiceListener. Receives events when a Subscriber is (un)registered.
   */
  @Override
  public synchronized void serviceChanged(ServiceEvent event) {
    if (event.getServiceReference().getProperty("objectClass") != null) {
      String[] objectClasses = (String[]) event.getServiceReference().getProperty("objectClass");
      if (Arrays.asList(objectClasses).contains(Subscriber.class.getName())) {
        ServiceReference reference = event.getServiceReference();
        switch (event.getType()) {
          case ServiceEvent.REGISTERED:
            connectSubscriber(reference);
            break;
          case ServiceEvent.MODIFIED:
            disconnectSubscriber(reference);
            connectSubscriber(reference);
            break;
          case ServiceEvent.MODIFIED_ENDMATCH:
          case ServiceEvent.UNREGISTERING:
            disconnectSubscriber(reference);
            break;
          default:
            break;
        }
      }
    }
  }

  /**
   * Disconnect a Subscriber from a TopicReceiver.
   * 
   * @param reference
   */
  private void disconnectSubscriber(ServiceReference reference) {
    Map<String, String> properties = Utils.getPropertiesFromReference(reference);
    String topic = Utils.getTopicFromProperties(properties);

    TopicReceiver receiver = receivers.get(topic);
    if (receiver != null) {
      receiver.disconnectSubscriber(reference);
      if (!receiver.isActive()) {
        executorService.schedule(new Runnable() {
          @Override
          public void run() {
            if (!receiver.isActive()) {
              receiver.close();
              receivers.remove(topic);

              for (DiscoveryManager discoveryManager : discoveryManagers) {
                discoveryManager.removeSubscriber(receiver.getEndpointProperties());
              }
            }
          }
        }, DELETE_DELAY, TimeUnit.SECONDS);
      }
    }
  }

  /**
   * Connect a Subscriber to a TopicReceiver.
   * 
   * @param reference
   */
  private void connectSubscriber(ServiceReference reference) {
    Map<String, String> properties = Utils.getPropertiesFromReference(reference);
    String topic = Utils.getTopicFromProperties(properties);

    TopicReceiver receiver = null;
    if (receivers.get(topic) != null) {
      receiver = receivers.get(topic);
      receiver.connectSubscriber(reference);
    } else {
      receiver = getTopicReceiver(reference);

      if (receiver != null) {
        receivers.put(topic, receiver);
        receiver.connectSubscriber(reference);

        for (Map<String, String> publisher : publisherEndpoints) {
          receiver.addPublisherEndpoint(publisher);
        }

        for (Map<String, String> subscriber : subscriberEndpoints) {
          receiver.addSubscriberEndpoint(subscriber);
        }

        receiver.open();

        for (DiscoveryManager discoveryManager : discoveryManagers) {
          discoveryManager.announceSubscriber(receiver.getEndpointProperties());
        }
      }
    }
  }

  /**
   * Get a TopicSender from the PubSubAdmin with the highest score.
   * 
   * @param user
   * @param filter
   * @return
   */
  private TopicSender getTopicSender(Bundle user, Filter filter) {
    double highestScore = 0;
    PubSubAdmin bestAdmin = null;

    for (PubSubAdmin admin : pubSubAdmins) {

      double score = admin.matchPublisher(user, filter);

      if (score > highestScore) {
        highestScore = score;
        bestAdmin = admin;
      }
    }

    if (bestAdmin != null) {
      return bestAdmin.createTopicSender(user, filter);
    }
    return null;
  }

  /**
   * Get a TopicReceiver from the PubSubAdmin with the highest score.
   * 
   * @param subscriber
   * @return
   */
  private TopicReceiver getTopicReceiver(ServiceReference<Subscriber> subscriber) {
    double highestScore = -1;
    PubSubAdmin bestAdmin = null;

    for (PubSubAdmin admin : pubSubAdmins) {

      double score = admin.matchSubscriber(subscriber);

      if (score > highestScore) {
        highestScore = score;
        bestAdmin = admin;
      }
    }

    if (bestAdmin != null) {
      return bestAdmin.createTopicReceiver(subscriber);
    }

    return null;
  }

  /**
   * Implementation of ManagedService.
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

    if (properties != null) {
      if (properties.get("delete.delay") != null) {
        DELETE_DELAY = Integer.parseInt((String) properties.get("delete.delay"));
      }
    }
  }

  /**
   * Implementation of EventHandler. Used to receive messages from DiscoveryManagers about the
   * creation/deletion of endpoints.
   */
  @Override
  public synchronized void handleEvent(Event event) {
    String eventType = (String) event.getProperty(Constants.DISCOVERY_EVENT_TYPE);
    Map<String, String> info = (Map<String, String>) event.getProperty(Constants.DISCOVERY_INFO);

    switch (eventType) {
      case Constants.DISCOVERY_EVENT_TYPE_CREATED:
        if (info.get(Constants.PUBSUB_TYPE).equals(Constants.PUBLISHER)) {
          addpublisherEndpoint(info);
        } else if (info.get(Constants.PUBSUB_TYPE).equals(Constants.SUBSCRIBER)) {
          addSubscriberEndpoint(info);
        }
        break;
      case Constants.DISCOVERY_EVENT_TYPE_DELETED:
        if (info.get(Constants.PUBSUB_TYPE).equals(Constants.PUBLISHER)) {
          removePublisherEndpoint(info);
        } else if (info.get(Constants.PUBSUB_TYPE).equals(Constants.SUBSCRIBER)) {
          removeSubscriberEndpoint(info);
        }
        break;
      default:
        break;
    }
  }

  /**
   * Add a Publisher endpoint to the known endpoints.
   * 
   * @param endpoint
   */
  private void addpublisherEndpoint(Map<String, String> endpoint) {
    publisherEndpoints.add(endpoint);

    for (TopicHandler handler : senders.values()) {
      handler.addPublisherEndpoint(endpoint);
    }
    for (TopicHandler handler : receivers.values()) {
      handler.addPublisherEndpoint(endpoint);
    }
  }

  /**
   * Remove a Publisher endpoint from the known endpoints.
   * 
   * @param endpoint
   */
  private void removePublisherEndpoint(Map<String, String> endpoint) {
    publisherEndpoints.remove(endpoint);

    for (TopicHandler handler : senders.values()) {
      handler.removePublisherEndpoint(endpoint);
    }
    for (TopicHandler handler : receivers.values()) {
      handler.removePublisherEndpoint(endpoint);
    }
  }

  /**
   * Add a Subscriber endpoint to the known endpoints.
   * 
   * @param endpoint
   */
  private void addSubscriberEndpoint(Map<String, String> endpoint) {
    subscriberEndpoints.add(endpoint);

    for (TopicHandler handler : senders.values()) {
      handler.addSubscriberEndpoint(endpoint);
    }
    for (TopicHandler handler : receivers.values()) {
      handler.addSubscriberEndpoint(endpoint);
    }
  }

  /**
   * Remove a Subscriber endpoint from the known endpoints.
   * 
   * @param endpoint
   */
  private void removeSubscriberEndpoint(Map<String, String> endpoint) {
    subscriberEndpoints.remove(endpoint);

    for (TopicHandler handler : senders.values()) {
      handler.removeSubscriberEndpoint(endpoint);
    }
    for (TopicHandler handler : receivers.values()) {
      handler.removeSubscriberEndpoint(endpoint);
    }
  }

  /**
   * Inform the TopicHandler of all Publisher and Subscriber endpoints.
   * 
   * @param handler
   */
  private void informHandlerOfEndpoints(TopicHandler handler) {
    for (Map<String, String> endpoint : publisherEndpoints) {
      handler.addPublisherEndpoint(endpoint);
    }
    for (Map<String, String> endpoint : subscriberEndpoints) {
      handler.addSubscriberEndpoint(endpoint);
    }
  }

  private boolean isFilterForPublisher(Filter filter) {
    return filter.toString().contains(Publisher.class.getName());
  }

  // DependencyManager callback
  private void adminAdded(PubSubAdmin admin) {
    pubSubAdmins.add(admin);
  }

  // DependencyManager callback
  private void adminRemoved(PubSubAdmin admin) {
    pubSubAdmins.remove(admin);
  }

  // DependencyManager callback
  private synchronized void discoveryManagerAdded(DiscoveryManager manager) {
    discoveryManagers.add(manager);

    publisherEndpoints.addAll(manager.getCurrentPublisherEndPoints());
    subscriberEndpoints.addAll(manager.getCurrentSubscriberEndPoints());
  }

  // DependencyManager callback
  private void discoveryManagerRemoved(DiscoveryManager manager) {
    discoveryManagers.remove(manager);
  }

  protected final void start() throws Exception {
    System.out.println("STARTED " + this.getClass().getName());
    
    ServiceReference[] references = bundleContext.getAllServiceReferences(Subscriber.class.getName(), null);
    
    for(ServiceReference reference: references){
      connectSubscriber(reference);
    }

    bundleContext.addServiceListener(this, "(objectClass=" + Subscriber.class.getName() + ")");

  }

  protected final synchronized void stop() throws Exception {
    System.out.println("STOPPED " + this.getClass().getName());

    for (TopicSender sender : senders.values()) {
      sender.close();
      for (DiscoveryManager discoveryManager : discoveryManagers) {
        discoveryManager.removePublisher(sender.getEndpointProperties());
      }
    }
    for (TopicReceiver receiver : receivers.values()) {
      receiver.close();
      for (DiscoveryManager discoveryManager : discoveryManagers) {
        discoveryManager.removeSubscriber(receiver.getEndpointProperties());
      }
    }
  }

  protected final void destroy() {
    System.out.println("DESTROYED " + this.getClass().getName());
  }



}
