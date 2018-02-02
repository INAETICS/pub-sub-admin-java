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

import java.io.IOException;
import java.util.Iterator;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.service.log.LogService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
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
  public byte[] serialize(MultipartContainer container) {
    try {
      return mapper.writeValueAsBytes(container);

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

  @Override
  public MultipartContainer deserialize(byte[] bytes) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try {
      String json = new String(bytes);
      
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      MultipartContainer container = new MultipartContainer();
      JsonNode jsonNode = mapper.readTree(json);
      Iterator<JsonNode> classes = jsonNode.get("classes").elements();
      Iterator<JsonNode> objects = jsonNode.get("objects").elements();

      while (objects.hasNext() && classes.hasNext()) {
        JsonNode object = (JsonNode) objects.next();
        JsonNode clazz = (JsonNode) classes.next();

        final Class<?> cls = Class.forName(clazz.asText());
        Object obj = mapper.convertValue(object, cls);
        container.addObject(obj);
      }

      return container;
    } catch (IOException e) {
      logService.log(LogService.LOG_ERROR, "Exception during JSON deserialize", e);
    } catch (ClassNotFoundException e) {
      logService.log(LogService.LOG_ERROR, "Exception during JSON deserialize", e);
    } finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
    return null;
  }

  @Start
  protected final void start(){
    System.out.println("STARTED " + this.getClass().getName());
  }

  @Stop
  protected final void stop(){
    System.out.println("STOPPED " + this.getClass().getName());
  }

  @Destroy
  protected final void destroy(){
    System.out.println("DESTROYED " + this.getClass().getName());
  }

}
