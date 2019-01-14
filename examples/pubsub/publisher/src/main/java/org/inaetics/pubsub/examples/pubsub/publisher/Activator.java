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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.api.Publisher;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bundleContext, DependencyManager manager) {
        String filter = String.format("(%s=poi)", Publisher.PUBSUB_TOPIC);
        manager.add(
                manager.createComponent()
                        .setImplementation(DemoPublisher.class)
                        .setCallbacks(null, "start", "stop", null)
                        .add(createServiceDependency()
                                .setService(LogService.class)
                                .setRequired(false)
                        )
                        .add(createServiceDependency()
                                .setService(Publisher.class, filter)
                                .setRequired(true)
                        )
        );

        //TODO register poi Subscriber.
    }
}
