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
package org.inaetics.pubsub.demo.config;

import javax.ws.rs.core.Response;

public class ResponseCreator {
public static Response createResponse(Object object){
  return Response
      .status(200)
      .header("Access-Control-Allow-Origin", "*")
      .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
      .header("Access-Control-Allow-Credentials", "true")
      .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
      .header("Access-Control-Max-Age", "1209600")
      .entity(object)
      .build();
}
public static Response createResponse(){
  return Response
      .status(200)
      .header("Access-Control-Allow-Origin", "*")
      .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
      .header("Access-Control-Allow-Credentials", "true")
      .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
      .header("Access-Control-Max-Age", "1209600")
      .build();
}
}
