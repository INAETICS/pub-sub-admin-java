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
package org.inaetics.pubsub.examples.pubsub.publisher;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

    private DemoPublisher demoPublisher;

    @Override
    public void init(BundleContext bundleContext, DependencyManager manager) {

        demoPublisher = new DemoPublisher();

        try {

            String[] objectClass = new String[] {Object.class.getName()};
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(Constants.SERVICE_PID, DemoPublisher.SERVICE_PID);

            manager.add(
                    manager.createComponent()
                            .setInterface(objectClass, properties)
                            .setImplementation(demoPublisher)
                            .add(createServiceDependency()
                                    .setService(LogService.class)
                                    .setRequired(false)
                            )
                            .add(createConfigurationDependency()
                                    .setPid(DemoPublisher.SERVICE_PID)
                            )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        demoPublisher.init();

    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        demoPublisher.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        demoPublisher.stop();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        demoPublisher.destroy();
    }

}
