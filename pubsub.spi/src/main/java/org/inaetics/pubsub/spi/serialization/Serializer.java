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
package org.inaetics.pubsub.spi.serialization;

public interface Serializer {
  
  public static final String SERIALIZER = "serializer";

  /**
   * Serialize the input. 
   * @param obj       The object to serialize
   * @return          The bytes representing the serialized object
   */
  public byte[] serialize(Object obj);

  /**
   * Serialize the input.
   * @param container The MultipartContainer to serialize
   * @return          The bytes representing the input container
   */
  public byte[] serialize(MultipartContainer container);

  /**
   * Deserialize the input.
   * @param clazz     The class name of the object which must be deserialized
   * @param bytes     The byte representing the object
   * @return          The deserialized object
   */
  public Object deserialize(String clazz, byte[] bytes);

  /**
   * Deserialize the input.
   * @param bytes     The bytes representing a MultipartContainer
   * @return          The deserialized MultipartContainer
   */
  public MultipartContainer deserialize(byte[] bytes);

}
