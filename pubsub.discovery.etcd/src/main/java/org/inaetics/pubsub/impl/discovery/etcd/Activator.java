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
package org.inaetics.pubsub.impl.discovery.etcd;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.spi.discovery.AnnounceEndpointListener;
import org.inaetics.pubsub.spi.discovery.DiscoveredEndpointListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

import java.util.Properties;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bundleContext, DependencyManager manager) throws Exception {

        String[] services = new String[]{
                AnnounceEndpointListener.class.getName(), ManagedService.class.getName()
        };

        Properties properties = new Properties();
        properties.put(Constants.SERVICE_PID, EtcdDiscovery.class.getName());
        properties.put("osgi.command.scope", "pubsub");
        properties.put("osgi.command.function", new String[]{"psd"});

        EtcdDiscovery disc = new EtcdDiscovery(new EtcdWrapper(), 10);

        manager.add(
                manager.createComponent()
                        .setInterface(services, properties)
                        .setImplementation(disc)
                        .setCallbacks(null, "start", "stop", null)
                        .add(createServiceDependency()
                                .setService(LogService.class)
                                .setRequired(false))
                        .add(createServiceDependency()
                                .setService(DiscoveredEndpointListener.class)
                                .setCallbacks("discoveredEndpointListenerAdded", "discoveredEndpointListenerRemoved"))
                        .add(createConfigurationDependency()
                                .setRequired(false)
                                .setPid(EtcdDiscovery.class.getName()))
        );

    }

}
