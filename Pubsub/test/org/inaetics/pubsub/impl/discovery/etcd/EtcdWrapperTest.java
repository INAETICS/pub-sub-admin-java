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
package org.inaetics.pubsub.impl.discovery.etcd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.inaetics.pubsub.impl.discovery.etcd.EtcdCallback;
import org.inaetics.pubsub.impl.discovery.etcd.EtcdWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;


public class EtcdWrapperTest {



  /**
   * Start a docker container with etcd.
   */
  @Before
  public void setUp() {
    EtcdStarter.start();
  }

  /**
   * Stop etcd
   */
  @After
  public void tearDown() {
    EtcdStarter.stop();
  }

  @Test
  public void testEtcdPut() {
    EtcdWrapper wrapper = new EtcdWrapper();
    String key = "/key";
    String value = "value";

    try {
      wrapper.put(key, value, -1);
      String value2 = wrapper.get(key, false, false, -1).get("node").get("value").asText();
      assertEquals(value, value2);
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }

  @Test
  public void testEtcdDelete() {
    EtcdWrapper wrapper = new EtcdWrapper();
    String key = "/key";
    String value = "value";

    try {
      wrapper.put(key, value,-1);
      wrapper.delete(key);
      wrapper.get(key, false, false, -1).get("node").get("value").asText();
    } catch (IOException e) {
      if (!(e instanceof FileNotFoundException)) {
        fail();
      }
    }
  }
  
  @Test
  public void testEtcdCreateDirectory() {
    EtcdWrapper wrapper = new EtcdWrapper();
    String key = "/key";
    

    try {
      wrapper.createDirectory(key);
      assertTrue(wrapper.get(key, false, false, -1).get("node").get("dir").asBoolean());
    } catch (IOException e) {
      e.printStackTrace();
      fail();
      
    }
  }
  
  @Test
  public void testEtcdDeleteDirectory() {
    EtcdWrapper wrapper = new EtcdWrapper();
    String key = "/key";
    String key2 = "/key/key2";

    try {
      wrapper.createDirectory(key);
      wrapper.deleteEmptyDirectory(key);
      wrapper.get(key, false, false, -1);
    } catch (IOException e) {
      if (!(e instanceof FileNotFoundException)) {
        fail();
      }
    }
    
    try {
      wrapper.put(key2, "value", -1);
      wrapper.deleteEmptyDirectory(key);
      
    } catch (IOException e) {
      if (!e.getMessage().contains("403")) { //deleting non empty is forbidden
        fail();
      }
    }
  }
  
  @Test
  public void testEtcdWaitFor() {
    EtcdWrapper wrapper = new EtcdWrapper();
    String directory = "/key";
    String value = "value";
    try {
      wrapper.createDirectory(directory);
      wrapper.waitForChange(directory, true, -1, new EtcdCallback() {
        
        @Override
        public void onResult(JsonNode result) {
          String value2 = result.get("node").get("value").asText();
          assertEquals(value, value2);
          
        }
        
        @Override
        public void onException(Exception exception) {
          fail();
          
        }
      });
      
      Thread thread = new Thread(new Runnable() {
        
        @Override
        public void run() {
          try {
            Thread.sleep(250);
            wrapper.put("/key/value", value, -1);
          } catch (Exception e) {
            e.printStackTrace();
            fail();
          }
          
        }
      });
      thread.start();
      thread.join();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    
    
  }

  


}
