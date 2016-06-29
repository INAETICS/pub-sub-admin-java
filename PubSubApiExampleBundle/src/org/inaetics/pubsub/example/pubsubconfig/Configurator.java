package org.inaetics.pubsub.example.pubsubconfig;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class Configurator {
  
  private volatile BundleContext bundleContext;
  
  private void start() {
    ServiceReference configurationAdminReference =
        bundleContext.getServiceReference(ConfigurationAdmin.class.getName());

    if (configurationAdminReference != null) {

      ConfigurationAdmin confAdmin =
          (ConfigurationAdmin) bundleContext.getService(configurationAdminReference);

      try {
        Configuration config = confAdmin
            .getConfiguration("org.inaetics.pubsub.impl.discovery.etcd.EtcdDiscoveryManager", null);
        Dictionary props = config.getProperties();

        if (props == null) {
          props = new Hashtable();
        }

        props.put("url", "http://localhost:2379/v2/keys");

        config.update(props);

        config = confAdmin
            .getConfiguration("org.inaetics.pubsub.impl.pubsubadmin.kafka.KafkaPubSubAdmin", null);
        props = config.getProperties();

        if (props == null) {
          props = new Hashtable();
        }

        props.put("sub:zookeeper.connect", "localhost:2181");
        props.put("pub:bootstrap.servers", "localhost:9092");
        config.update(props);
      } catch (Throwable e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
