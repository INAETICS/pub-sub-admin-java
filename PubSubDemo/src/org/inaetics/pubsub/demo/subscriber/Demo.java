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
package org.inaetics.pubsub.demo.subscriber;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.amdatu.web.rest.doc.Description;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.demo.config.ResponseCreator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@Path("subscriber")
@Description("Subscriber rest endpoint")
public class Demo {
  private volatile BundleContext bundleContext;
  private ServiceRegistration registration;
  private DemoSubscriber subscriber = new DemoSubscriber();

  @POST
  @Path("start")
  public synchronized Response startSubscriber(@QueryParam("topic") String topic) {
    System.out.println("startSubscriber");
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(Subscriber.PUBSUB_TOPIC, topic);
    registration =
        bundleContext.registerService(Subscriber.class.getName(), subscriber, properties);
    
    return ResponseCreator.createResponse();
  }

  @POST
  @Path("stop")
  public synchronized Response stopSubscriber() {
    System.out.println("stopSubscriber");
    registration.unregister();
    
    return ResponseCreator.createResponse();
  }
  
  @GET
  @Path("getBuffer")
  @Produces("application/json")
  public synchronized Response getBuffer(){
    List<Integer> buffer = new ArrayList<>();
    subscriber.queue.drainTo(buffer);
    return ResponseCreator.createResponse(buffer);

  }
}
