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
package test.org.inaetics.impl.pubsubadmin.kafka;

import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.Hashtable;

import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.api.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.api.pubsubadmin.TopicReceiver;
import org.inaetics.pubsub.api.pubsubadmin.TopicSender;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.oracle.net.Sdp;

public class PubSubAdminTest {
  private volatile PubSubAdmin admin;
  private volatile BundleContext bundleContext;
  
  void start() {
    System.out.println("STARTED" + this.getClass().getName());
    try {
    Dictionary props = new Hashtable<>();
    props.put(Subscriber.PUBSUB_TOPIC, "topic");
    ServiceRegistration<Subscriber> registration = bundleContext.registerService(Subscriber.class, mock(Subscriber.class), props);
    ServiceReference<Subscriber> reference = registration.getReference();
    
    Assert.assertTrue(admin.matchSubscriber(reference) > 0);
    TopicReceiver receiver = admin.createTopicReceiver(reference);
    Assert.assertTrue(receiver != null);
    
        

    Assert.assertTrue(admin.matchPublisher(bundleContext.getBundle(), bundleContext.createFilter("(" + Subscriber.PUBSUB_TOPIC + "=topic)")) > 0);
    
    
    TopicSender sender = admin.createTopicSender(bundleContext.getBundle(), bundleContext.createFilter("(" + Subscriber.PUBSUB_TOPIC + "=topic)"));
    
    sender.open();
   
    Publisher publisher = bundleContext.getService(bundleContext.getServiceReference(Publisher.class));
    Assert.assertTrue(publisher != null);
    
    System.out.println(this.getClass().getName() + " succes");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
