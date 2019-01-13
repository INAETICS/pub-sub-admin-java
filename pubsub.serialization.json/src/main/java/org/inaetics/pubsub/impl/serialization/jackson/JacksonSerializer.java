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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.service.log.LogService;

import java.io.IOException;

public class JacksonSerializer implements Serializer {

    private ObjectMapper mapper = new ObjectMapper();
    private volatile LogService logService;
    public static final String SERIALIZER_JACKSON = "json";

    @Override
    public byte[] serialize(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);

        } catch (JsonProcessingException e) {
            logService.log(LogService.LOG_ERROR, "Exception during JSON serialize", e);
        }

        return null;
    }

    @Override
    public Object deserialize(String clazz, byte[] bytes) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            String json = new String(bytes);

            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            JsonNode object = mapper.readTree(json);

            final Class<?> cls = Class.forName(clazz);

            Object obj = mapper.convertValue(object, cls);

            return obj;
        } catch (IOException e) {
            logService.log(LogService.LOG_ERROR, "Exception during JSON deserialize", e);
        } catch (ClassNotFoundException e) {
            logService.log(LogService.LOG_ERROR, "Exception during JSON deserialize", e);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
        return null;
    }
}
