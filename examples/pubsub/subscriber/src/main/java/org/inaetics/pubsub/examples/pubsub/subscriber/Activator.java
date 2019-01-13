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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.api.Subscriber;
import org.osgi.framework.BundleContext;

import java.util.Properties;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bundleContext, DependencyManager manager) {
        String[] services = new String[] {Subscriber.class.getName()};
        Properties props = new Properties();
        props.put(Subscriber.PUBSUB_TOPIC, "poi");

        manager.add(
                manager.createComponent()
                        .setInterface(services, props)
                        .setImplementation(DemoSubscriber.class)
        );
    }

}
