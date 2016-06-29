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
package org.inaetics.pubsub.demo.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.fasterxml.jackson.databind.JsonNode;

import jdk.nashorn.internal.objects.annotations.Getter;

@Path("hosts")
public class Configurator {
  private static final String VAGRANT_HOST_IP = "10.10.10.1";
  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private volatile BundleContext bundleContext;
  
  @GET
  @Produces("application/json")
  public Response getHosts() throws MalformedURLException, IOException{
    EtcdWrapper etcdWrapper = new EtcdWrapper("http://" + VAGRANT_HOST_IP + ":2379/v2/keys");
    JsonNode hosts = etcdWrapper.get("/pubsubdemo/", false, true, -1);
    List<String> result = new ArrayList<>();
      hosts = hosts.get("node").get("nodes");
    
    for (int i = 0;; i++) {
      if (hosts.has(i)) {
        result.add(hosts.get(i).get("value").asText());
      } else {
        break;
      }
    }    
    
    return ResponseCreator.createResponse(result);
  }


  void start() {
    configurePubSub();
    publishToEtcd();
  }

  private void configurePubSub() {
    ServiceReference configurationAdminReference =
        bundleContext.getServiceReference(ConfigurationAdmin.class.getName());

    if (configurationAdminReference != null) {

      ConfigurationAdmin confAdmin =
          (ConfigurationAdmin) bundleContext.getService(configurationAdminReference);

      try {
        Configuration config = confAdmin
            .getConfiguration("org.inaetics.pubsub.impl.discovery.etcd.EtcdDiscoveryManager", null);
        Dictionary props = config.getProperties();

        // if null, the configuration is new
        if (props == null) {
          props = new Hashtable();
        }

        // set some properties
        props.put("url", "http://" + VAGRANT_HOST_IP + ":2379/v2/keys");

        // update the configuration

        config.update(props);

        config = confAdmin
            .getConfiguration("org.inaetics.pubsub.impl.pubsubadmin.kafka.KafkaPubSubAdmin", null);
        props = config.getProperties();

        // if null, the configuration is new
        if (props == null) {
          props = new Hashtable();
        }

        // set some properties
        props.put("sub:zookeeper.connect", VAGRANT_HOST_IP + ":2181");
        props.put("pub:bootstrap.servers", VAGRANT_HOST_IP + ":9092");
        config.update(props);
      } catch (Throwable e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void publishToEtcd() {
    String myIp = getIp();
    EtcdWrapper etcdWrapper = new EtcdWrapper("http://" + VAGRANT_HOST_IP + ":2379/v2/keys");

    try {
      try {
        etcdWrapper.get("/pubsubdemo/", false, true, -1);
      } catch (FileNotFoundException e) {
        etcdWrapper.createDirectory("/pubsubdemo/");
      }
      etcdWrapper.put("/pubsubdemo/" + myIp, myIp, 15);
    } catch (UnsupportedEncodingException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          etcdWrapper.refreshTTL("/pubsubdemo/" + myIp, 15);
          executor.schedule(this, 10, TimeUnit.SECONDS);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    };
    executor.schedule(runnable, 10, TimeUnit.SECONDS);
  }

  private String getIp() {
    try {
      Enumeration<NetworkInterface> nEnumeration = NetworkInterface.getNetworkInterfaces();
      while (nEnumeration.hasMoreElements()) {
        NetworkInterface networkInterface = (NetworkInterface) nEnumeration.nextElement();
        if (networkInterface.getName().equals("eth1")) {
          Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
          while (addresses.hasMoreElements()) {
            InetAddress inetAddress = (InetAddress) addresses.nextElement();
            if (inetAddress instanceof Inet4Address) {
              return inetAddress.getHostAddress();
            }
          }
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return "localhost";
  }
}
