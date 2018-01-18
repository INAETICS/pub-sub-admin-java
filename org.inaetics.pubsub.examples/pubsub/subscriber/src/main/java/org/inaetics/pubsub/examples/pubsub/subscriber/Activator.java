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
package org.inaetics.pubsub.examples.pubsub.subscriber;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bundleContext, DependencyManager manager) {

        try {

            String[] objectClass = new String[] {Object.class.getName()};
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(Constants.SERVICE_PID, Demo.SERVICE_PID);

            manager.add(
                    manager.createComponent()
                            .setInterface(objectClass, properties)
                            .setImplementation(Demo.class)
                            .add(createServiceDependency()
                                    .setService(LogService.class)
                                    .setRequired(false))
                            .add(createConfigurationDependency()
                                    .setRequired(false)
                                    .setPid(Demo.SERVICE_PID))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
