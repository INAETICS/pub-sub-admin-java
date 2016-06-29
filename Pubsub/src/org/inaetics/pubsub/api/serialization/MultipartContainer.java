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
package org.inaetics.pubsub.api.serialization;

import java.util.ArrayList;
import java.util.List;

import org.inaetics.pubsub.api.pubsub.Subscriber.MultipartCallbacks;

public class MultipartContainer implements MultipartCallbacks{
  private List<String> classes = new ArrayList<>();
  private List<Object> objects = new ArrayList<>();
  
  public void addObject(Object object){
    classes.add(object.getClass().getName());
    objects.add(object);
  }
  
  public List<String> getClasses() {
    return classes;
  }
  public List<Object> getObjects() {
    return objects;
  }

  @Override
  public <T> T getMultipart(Class<T> clazz) {
    for (int i = 0; i<classes.size(); i++) {
      String cls = classes.get(i);
      if(cls.equals(clazz.getName())){
        return (T) objects.get(i);
      }
    }
    return null;
  }
}
