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
package org.inaetics.pubsub.impl.discovery.etcd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.impl.utils.Constants;
import org.inaetics.pubsub.impl.utils.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EtcdDiscoveryManager implements DiscoveryManager, ManagedService {
  public final static String SERVICE_PID = EtcdDiscoveryManager.class.getName();
  public final static String PUBSUB_ROOT_PATH = "/pubsub/";
  public final static String PUBSUB_PUBLISHER_PATH = PUBSUB_ROOT_PATH + "publisher/";
  public final static String PUBSUB_SUBSCRIBER_PATH = PUBSUB_ROOT_PATH + "subscriber/";

  private final Set<Map<String, String>> publishers = Collections.synchronizedSet(new HashSet<>());
  private final Set<Map<String, String>> subscribers = Collections.synchronizedSet(new HashSet<>());

  private volatile int TTL = 15;
  private volatile int TTLRefresh = 10;
  private volatile boolean stopped = false;
  private String etcdUrl = "http://localhost:2379/v2/keys";
  private EtcdWrapper etcdWrapper;
  private ExecutorService executor;
  private ResponseListener responseListener;
  private TTLRefresher refresher;;
  private BundleContext myBundleContext;
  private String localFrameworkUUID;
  private volatile LogService m_LogService;

  @Override
  public void announcePublisher(Map<String, String> properties) {
    publishers.add(properties);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          etcdWrapper.put(getPublisherEtcdPath(properties), mapToJSON(properties), TTL);
        } catch (IOException e) {
          m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
      }
    });
    
  }

  @Override
  public void removePublisher(Map<String, String> properties) {
    publishers.remove(properties);
    
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          etcdWrapper.delete(getPublisherEtcdPath(properties));
        } catch (IOException e) {
          m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
      }
    });
  }

  @Override
  public void announceSubscriber(Map<String, String> properties) {
    subscribers.add(properties);

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          etcdWrapper.put(getSubscriberEtcdPath(properties), mapToJSON(properties), TTL);
        } catch (IOException e) {
          m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
      }
    });
  }

  @Override
  public void removeSubscriber(Map<String, String> properties) {
    subscribers.remove(properties);

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          etcdWrapper.delete(getSubscriberEtcdPath(properties));
        } catch (IOException e) {
          m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
      }
    });
  }

  @Override
  public List<Map<String, String>> getCurrentSubscriberEndPoints() {
    List<Map<String, String>> subscribers = new ArrayList<>();
    try {
      JsonNode response = etcdWrapper.get(PUBSUB_SUBSCRIBER_PATH, false, true, -1);
      List<JsonNode> nodes = getRecursivelyFromDirectory(response.get("node"));
      for (JsonNode node : nodes) {
        subscribers.add(getValueMap(node));
      }
    } catch (IOException e) {
      m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
    }
    return subscribers;
  }

  @Override
  public List<Map<String, String>> getCurrentPublisherEndPoints() {
    List<Map<String, String>> publishers = new ArrayList<>();
    try {
      JsonNode response = etcdWrapper.get(PUBSUB_PUBLISHER_PATH, false, true, -1);
      List<JsonNode> nodes = getRecursivelyFromDirectory(response.get("node"));
      for (JsonNode node : nodes) {
        publishers.add(getValueMap(node));
      }
    } catch (IOException e) {
      m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
    }
    return publishers;
  }

  private class TTLRefresher extends Thread {
    private EtcdWrapper wrapper;

    public TTLRefresher(String etcdUrl) {
      wrapper = new EtcdWrapper(etcdUrl);
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          synchronized (EtcdDiscoveryManager.class) {
            for (Map<String, String> subscriber : subscribers) {
              wrapper.refreshTTL(getSubscriberEtcdPath(subscriber), TTL);
            }
            for (Map<String, String> publisher : publishers) {
              wrapper.refreshTTL(getPublisherEtcdPath(publisher), TTL);
            }
          }
          Thread.sleep(TTLRefresh * 1000);
        } catch (IOException | InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          return;
        }
      }
    }

  }

  public String getSubscriberEtcdPath(Map<String, String> info) {
    String serviceID = info.get(SERVICE_ID);
    String topic = info.get(Subscriber.PUBSUB_TOPIC);
    return PUBSUB_SUBSCRIBER_PATH + topic + "/" + localFrameworkUUID + "-" + serviceID;
  }

  public String getPublisherEtcdPath(Map<String, String> info) {
    String serviceID = info.get(SERVICE_ID);
    String topic = info.get(Publisher.PUBSUB_TOPIC);
    return PUBSUB_PUBLISHER_PATH + topic + "/" + localFrameworkUUID + "-" + serviceID;
  }

  protected final void start(){
    myBundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    localFrameworkUUID = Utils.getFrameworkUUID(myBundleContext);

    System.out.println("STARTED " + this.getClass().getName());
    
    etcdWrapper = new EtcdWrapper(etcdUrl);
    executor = Executors.newCachedThreadPool();
    refresher = new TTLRefresher(etcdUrl);
    refresher.start();
    responseListener = new ResponseListener();
    stopped = false;

    executor.execute(new Runnable() {
      @Override
      public void run() {
        initDiscovery();
      }
    });
  }

  
  public void stopDiscovery() {
    stopped = true;
    executor.shutdownNow();
    etcdWrapper.stop();
    refresher.interrupt();
  }



  private class ResponseListener implements EtcdCallback {

    @Override
    public void onResult(JsonNode result) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          pubSubChanged(result);
        }
      }).start();;

    }

    @Override
    public void onException(Exception exception) {
      if (stopped) {
        System.out.println("Stopped " + this.getClass().getSimpleName());
        return;
      } else {
        System.out.println("Restarting " + this.getClass().getSimpleName());
        new Thread(new Runnable() {
          @Override
          public void run() {
            initDiscovery();
          }
        }).start();
      }
    }
  }

  private void pubSubChanged(JsonNode response) {
    try {
      long index = response.get("node").get("modifiedIndex").asLong();
      if (!isEtcdDirectory(response)) {
        if (response.get("action").asText().equals("set")) {
          if (response.has("prevNode")) {
            // remove old
            JsonNode prevNode = response.get("prevNode");
            informTopologyManagerOfDelete(prevNode);
          }
          JsonNode newNode = response.get("node");
          informTopologyManagerOfDiscover(newNode);

        } else if (response.get("action").asText().equals("delete")
            || response.get("action").asText().equals("expire")) {
          // remove
          JsonNode prevNode = response.get("prevNode");
          informTopologyManagerOfDelete(prevNode);
        }
      }

      setDirectoryWatch(index + 1);
    } catch (Throwable e) {
      m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
    }

  }

  private boolean isEtcdDirectory(JsonNode node) {
    if (node.has("dir") && node.get("dir").asBoolean()) {
      return true;
    } else {
      return false;
    }

  }

  private void initDiscovery() {
    try {
      try {
        etcdWrapper.get(PUBSUB_ROOT_PATH, false, true, -1);
      } catch (FileNotFoundException e) {
        etcdWrapper.createDirectory(PUBSUB_ROOT_PATH);
      }

      JsonNode response = etcdWrapper.get(PUBSUB_ROOT_PATH, false, true, -1);
      long index = response.get("EtcdIndex").asLong();
      List<JsonNode> nodes = getRecursivelyFromDirectory(response.get("node"));
      for (JsonNode node : nodes) {
        informTopologyManagerOfDiscover(node);
      }
      setDirectoryWatch(index + 1);
    } catch (Throwable e) {
      e.printStackTrace();
      m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
    }
  }

  private List<JsonNode> getRecursivelyFromDirectory(JsonNode node) {
    JsonNode nodes = node.get("nodes");

    List<JsonNode> resultNodes = new ArrayList<>();

    if (nodes != null) {
      for (int i = 0;; i++) {
        if (nodes.has(i)) {
          JsonNode foundNode = nodes.get(i);
          if (isEtcdDirectory(foundNode)) {
            resultNodes.addAll(getRecursivelyFromDirectory(foundNode));
          } else {
            resultNodes.add(foundNode);
          }
        } else {
          break;
        }
      }
    }
    return resultNodes;
  }

  /**
   * Wait for a change in etcd with the specified index
   * 
   * @param index
   */
  private void setDirectoryWatch(long index) {
    etcdWrapper.waitForChange(PUBSUB_ROOT_PATH, true, index, responseListener);
  }

  /**
   * Get the Map from the JsonNode.
   * 
   * @param node
   * @return
   */
  private Map<String, String> getValueMap(JsonNode node) {
    if (!isEtcdDirectory(node)) {
      try {
        String json = node.get("value").asText();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map =
            mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        return map;
      } catch (Exception e) {
        m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
      }
    }
    return null;
  }

  private void informTopologyManagerOfDiscover(JsonNode node) {
    Map<String, String> info = getValueMap(node);
    if (info != null) {
      sendEvent(info, Constants.DISCOVERY_EVENT_TYPE_CREATED);
    }
  }


  private void informTopologyManagerOfDelete(JsonNode node) {
    Map<String, String> info = getValueMap(node);
    if (info != null) {
      sendEvent(info, Constants.DISCOVERY_EVENT_TYPE_DELETED);
    }
  }

  /**
   * Send an event to the TopologyManager.
   * 
   * @param info
   * @param eventType
   */
  private void sendEvent(Map<String, String> info, String eventType) {
    ServiceReference ref = myBundleContext.getServiceReference(EventAdmin.class.getName());
    if (ref != null) {
      EventAdmin eventAdmin = (EventAdmin) myBundleContext.getService(ref);

      Dictionary<String, Object> properties = new Hashtable<>();
      properties.put(Constants.DISCOVERY_EVENT_TYPE, eventType);
      properties.put(Constants.DISCOVERY_INFO, info);

      Event reportGeneratedEvent = new Event(Constants.DISCOVERY_EVENT, properties);

      eventAdmin.sendEvent(reportGeneratedEvent);

    }

  }

  protected final void stop() throws Exception {
    stopDiscovery();
    System.out.println("STOPPED " + this.getClass().getName());
  }

  void destroy() {
    System.out.println("DESTROYED " + this.getClass().getName());
  }


  /**
   * Implementation of ManagedService
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties != null) {
      if (properties.get("url") != null) {
        this.etcdUrl = (String) properties.get("url");
      } if (properties.get("TTL") != null) {
        this.TTL = (Integer) properties.get("TTL");
      }if (properties.get("TTLRefresh") != null) {
        this.TTLRefresh = (Integer) properties.get("TTLRefresh");
      }
    }

  }

  private String mapToJSON(Map<String, String> map) {
    try {
      return new ObjectMapper().writeValueAsString(map);
    } catch (JsonProcessingException e) {
      m_LogService.log(LogService.LOG_ERROR, e.getMessage(), e);
    }
    return "";
  }
}
