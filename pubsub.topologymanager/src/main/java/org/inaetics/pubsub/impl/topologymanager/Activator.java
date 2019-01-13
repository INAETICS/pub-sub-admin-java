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
package org.inaetics.pubsub.impl.topologymanager;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.api.Subscriber;
import org.inaetics.pubsub.spi.discovery.AnnounceEndpointListener;
import org.inaetics.pubsub.spi.discovery.DiscoveredEndpointListener;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) {

        //TODO gogo shell

        String[] interfaces = new String[]{
                DiscoveredEndpointListener.class.getName(), ManagedService.class.getName(), ListenerHook.class.getName()
        };

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, PubSubTopologyManager.SERVICE_PID);
        properties.put("osgi.command.scope", "pubsub");
        properties.put("osgi.command.function", new String[]{"pstm"});

        manager.add(
                manager.createComponent()
                        .setInterface(interfaces, properties)
                        .setImplementation(PubSubTopologyManager.class)
                        .setCallbacks(null, "start", "stop", null)
                        .add(createServiceDependency()
                                .setService(Subscriber.class)
                                .setRequired(false)
                                .setCallbacks("subscriberAdded", "subscriberRemoved"))
                        .add(createServiceDependency()
                                .setService(PubSubAdmin.class)
                                .setRequired(false)
                                .setCallbacks("adminAdded", "adminRemoved"))
                        .add(createServiceDependency()
                                .setService(AnnounceEndpointListener.class)
                                .setRequired(false)
                                .setCallbacks("announceEndointListenerAdded", "announceEndointListenerRemoved"))
                        .add(createServiceDependency()
                                .setService(LogService.class)
                                .setRequired(false))
        );

    }

}
