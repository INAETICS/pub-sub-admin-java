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
package org.inaetics.pubsub.demo.publisher;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.amdatu.web.rest.doc.Description;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.demo.config.ResponseCreator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

@Path("publisher")
@Description("Publisher rest endpoint")
public class DemoPublisher {
  private volatile BundleContext bundleContext;
  private volatile ServiceTracker tracker;
  private volatile Publisher publisher;
  private volatile PublishThread publishThread;
  

  @POST
  @Path("publish")
  public synchronized Response publish(@QueryParam("value") int value) {
    System.out.println("publish");

    if (publisher != null) {
      publisher.send(new Integer(value));
    }
    
    return ResponseCreator.createResponse();
  }


  @POST
  @Path("start")
  public synchronized Response startPublisher(@QueryParam("topic") String topic) {
    System.out.println("startPublisher");
    if (publishThread != null) {
      publishThread.interrupt();
      tracker.close();
    }
    try {
      Filter filter = bundleContext.createFilter("(&(objectClass=" + Publisher.class.getName() + ")"
          + "(" + Publisher.PUBSUB_TOPIC + "=" + topic + "))");
      tracker = new ServiceTracker(bundleContext, filter, null);
      tracker.open();
      publisher = (Publisher) tracker.waitForService(0);

      publishThread = new PublishThread();
      publishThread.start();
    } catch (InvalidSyntaxException | InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return ResponseCreator.createResponse();
  }

  @POST
  @Path("stop")
  public synchronized Response stopPublisher() {
    System.out.println("stopPublisher");
    publishThread.interrupt();
    publishThread = null;
    tracker.close();
    publisher = null;
    
    return ResponseCreator.createResponse();
  }

  private class PublishThread extends Thread {
    @Override
    public void run() {
      while (!this.isInterrupted()) {

        if (publisher != null) {
          System.out.println("publishing");
          Integer integer = ThreadLocalRandom.current().nextInt(0, 11);
          publisher.send(integer);
        }
        try {
          Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  }

}
