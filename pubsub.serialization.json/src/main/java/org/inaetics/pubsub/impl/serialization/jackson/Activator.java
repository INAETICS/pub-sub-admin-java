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
package org.inaetics.pubsub.impl.serialization.jackson;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        String[] objectClass = new String[]{Serializer.class.getName()};
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Serializer.SERIALIZER_NAME_KEY, JacksonSerializer.SERIALIZER_JACKSON);

        manager.add(
                manager.createComponent()
                        .setInterface(objectClass, properties)
                        .setImplementation(JacksonSerializer.class)
                        .add(createServiceDependency()
                                .setService(LogService.class)
                                .setRequired(false))
        );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // TODO Auto-generated method stub

    }

}
