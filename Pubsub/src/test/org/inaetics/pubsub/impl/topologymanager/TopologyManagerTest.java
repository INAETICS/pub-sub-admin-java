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
package test.org.inaetics.pubsub.impl.topologymanager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.api.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.api.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.api.pubsubadmin.TopicSender;
import org.inaetics.pubsub.impl.topologymanager.PubSubTopologyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class TopologyManagerTest {
  private volatile BundleContext bundleContext;
  DiscoveryManager discoveryManager = mock(DiscoveryManager.class);
  PubSubAdmin pubSubAdmin = mock(PubSubAdmin.class);
  TopicSender topicSender = mock(TopicSender.class);
  TopicReceiver topicReceiver = mock(TopicReceiver.class);

  private void setup() {
    when(pubSubAdmin.createTopicReceiver(any())).thenReturn(topicReceiver);
    when(pubSubAdmin.matchSubscriber(any())).thenReturn(100.0);
    when(pubSubAdmin.matchPublisher(any(), any())).thenReturn(100.0);
    when(pubSubAdmin.createTopicSender(any(), any())).thenReturn(topicSender);
    when(topicSender.getService(any(), any())).thenReturn(mock(Publisher.class));

    bundleContext.registerService(PubSubAdmin.class, pubSubAdmin, null);
    bundleContext.registerService(DiscoveryManager.class, discoveryManager, null);
  }

  protected final void start() throws InvalidSyntaxException {
    setup();
    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        try {
          Thread.sleep(100);

          String topic = "topic";
          ServiceTracker tracker = new ServiceTracker<>(bundleContext,
              bundleContext.createFilter("(&(objectClass=" + Publisher.class.getName() + ")" + "("
                  + Publisher.PUBSUB_TOPIC + "=" + topic + "))"),
              null);
          tracker.open();

          verify(pubSubAdmin, times(1)).matchPublisher(any(), any());
          verify(pubSubAdmin, times(1)).createTopicSender(any(), any());
          verify(topicSender, times(1)).open();
          verify(discoveryManager, times(1)).announcePublisher(any());
          
          Dictionary props = new Hashtable<>();
          props.put(Subscriber.PUBSUB_TOPIC, "topic");
          ServiceRegistration<Subscriber> registration = bundleContext.registerService(Subscriber.class, mock(Subscriber.class), props);
          
          verify(pubSubAdmin, times(1)).matchSubscriber(any());
          verify(pubSubAdmin, times(1)).createTopicReceiver(any());
          
          
          tracker.close();
          registration.unregister();
          Thread.sleep(10050);

          verify(topicSender, times(1)).close();
          verify(discoveryManager, times(1)).removePublisher(any());
          verify(topicReceiver, times(1)).close();
          verify(discoveryManager, times(1)).removeSubscriber(any());
          
          System.out.println(this.getClass().getName() + " success");
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }


      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
  }
}
