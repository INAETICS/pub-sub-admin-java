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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.inaetics.pubsub.api.serialization.MultipartContainer;
import org.junit.Before;
import org.junit.Test;

public class JacksonSerializerTest {
  private JacksonSerializer serializer;

  @Before
  public void setup() {
    serializer = new JacksonSerializer();
  }

  @Test
  public void serializeTest() throws IOException {
    Object testClassObject = new TestClass("Jeroen", 21);
    MultipartContainer container = new MultipartContainer();
    container.addObject(testClassObject);

    byte[] bytes = serializer.serialize(container);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.write(
        "{\"classes\":[\"org.inaetics.pubsub.impl.serialization.jackson.TestClass\"],\"objects\":[{\"name\":\"Jeroen\",\"age\":21}]}"
            .getBytes());

    byte[] result = outputStream.toByteArray();

    assertTrue(Arrays.equals(result, bytes));
  }

  @Test
  public void deserializeTest() {
    Object testClassObject = new TestClass("Jeroen", 21);
    MultipartContainer container = new MultipartContainer();
    container.addObject(testClassObject);
    byte[] bytes = serializer.serialize(container);
    MultipartContainer resultContainer = serializer.deserialize(bytes);
//    TestClass  = (TestClass) 
//    assertTrue(result.equals(testClassObject));
  }

}
