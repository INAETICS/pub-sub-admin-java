package org.inaetics.pubsub.example.pubsubconfig;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {

  @Override
  public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void init(BundleContext arg0, DependencyManager manager) throws Exception {
    manager.add(createComponent().setImplementation(Configurator.class));
  }

}
