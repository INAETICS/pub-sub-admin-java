package org.inaetics.pubsub.example.dependencymanager;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.impl.discovery.etcd.EtcdDiscoveryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class Activator extends DependencyActivatorBase {
  
  private final static String TOPIC = "example-topic";

  @Override
  public void init(BundleContext context, DependencyManager manager) throws Exception {
    
    manager.add(createComponent()
        .setImplementation(PublisherExample.class)
        .add(createServiceDependency()
            .setService(Publisher.class, "(" + Publisher.PUBSUB_TOPIC +"=" + TOPIC + ")")
            .setRequired(true))
    );
    
    String[] objectClass = new String[] {Subscriber.class.getName()};
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Subscriber.PUBSUB_TOPIC, TOPIC);
    
    manager.add(createComponent()
        .setInterface(objectClass, properties)
        .setImplementation(SubscriberExample.class));
  }

  @Override
  public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {}

}
