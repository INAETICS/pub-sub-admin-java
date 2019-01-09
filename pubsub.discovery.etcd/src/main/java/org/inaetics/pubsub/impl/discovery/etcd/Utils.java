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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.jackson.JsonNode;

import java.util.Iterator;
import java.util.Properties;

public class Utils {

    public static String endpointToJson(Properties endpoint) {
        String result = null;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        for (String key : endpoint.stringPropertyNames()) {
            root.put(key, endpoint.getProperty(key));
        }
        result = root.toString();
        return result;
    }

    public static Properties endpointFormJson(JsonNode node) {
        Properties result = null;
        if (node != null && node.isObject()) {
            result = new Properties();
            for (Iterator<String> it = node.getFieldNames(); it.hasNext(); ) {
                String name = it.next();
                JsonNode val = node.get("name");
                if (val != null && val.isTextual()) {
                    result.put(name, val);
                }
            }
            //TODO check mandatory fields
        }
        return result;
    }
}
