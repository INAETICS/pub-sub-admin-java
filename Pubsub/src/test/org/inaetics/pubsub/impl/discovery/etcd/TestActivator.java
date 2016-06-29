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
package test.org.inaetics.pubsub.impl.discovery.etcd;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.api.discovery.DiscoveryManager;
import org.inaetics.pubsub.impl.topologymanager.PubSubTopologyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class TestActivator extends DependencyActivatorBase{

  @Override
  public void init(BundleContext context, DependencyManager manager) {

    String[] objectClass = new String[] {EventHandler.class.getName()};

    String[] topics = new String[] {org.inaetics.pubsub.impl.utils.Constants.DISCOVERY_EVENT};

    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(EventConstants.EVENT_TOPIC, topics);
    manager.add(
        createComponent()
        .setInterface(objectClass, properties)
        .setImplementation(TestTopologyManager.class)
            .add(createServiceDependency()
                .setService(DiscoveryManager.class)
                .setRequired(true))
        );
  }

  @Override
  public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {
    // TODO Auto-generated method stub
    
  }

}
