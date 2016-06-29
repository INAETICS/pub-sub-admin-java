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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EtcdStarter {
public static void start(){
  Runtime rt = Runtime.getRuntime();

  try {
    Process process = rt.exec(
        "docker run -d -v /usr/share/ca-certificates/:/etc/ssl/certs -p 4001:4001 -p 2380:2380 -p 2379:2379 "
            + "--name etcd-discovery-test quay.io/coreos/etcd:v2.3.0  " + "-name etcd0  "
            + "-advertise-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001  "
            + "-listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001 "
            + "-initial-advertise-peer-urls http://0.0.0.0:2380  "
            + "-listen-peer-urls http://0.0.0.0:2380  "
            + "-initial-cluster-token etcd-cluster-1  "
            + "-initial-cluster etcd0=http://0.0.0.0:2380  " + "-initial-cluster-state new");
    process.waitFor();
    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

    // read the output from the command
    System.out.println("Here is the standard output of the command:\n");
    String s = null;
    while ((s = stdInput.readLine()) != null) {
      System.out.println(s);
    }

    // read any errors from the attempted command
    System.out.println("Here is the standard error of the command (if any):\n");
    while ((s = stdError.readLine()) != null) {
      System.out.println(s);
    }

    Thread.sleep(2000);
  } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  } catch (InterruptedException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
}

public static void stop(){
  Runtime rt = Runtime.getRuntime();
  try {
    Process process = rt.exec("docker stop etcd-discovery-test");
    process.waitFor();
    Process process2 = rt.exec("docker rm /etcd-discovery-test");
    process2.waitFor();


  } catch (IOException e) {
    e.printStackTrace();
  } catch (InterruptedException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
}
}
