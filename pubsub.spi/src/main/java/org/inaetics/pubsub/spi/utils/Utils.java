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
package org.inaetics.pubsub.spi.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.inaetics.pubsub.api.pubsub.Publisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class Utils {
    public static Map<String, String> verySimpleLDAPFilterParser(Filter filter) {
        String filterString = filter.toString();
        Map<String, String> result = new HashMap<>();

        for (int i = 0; i < filterString.length(); i++) {
            if (filterString.charAt(i) == '=') {
                result.put(getLeft(filterString, i), getRight(filterString, i));
            }
        }
        return result;
    }

    private static String getLeft(String filterString, int equalsIndex) {
        StringBuilder result = new StringBuilder();
        for (int index = equalsIndex - 1; index > 0; index--) {
            if (filterString.charAt(index) != '(') {
                result.append(filterString.charAt(index));
            } else {
                break;
            }
        }
        return result.reverse().toString();
    }

    private static String getRight(String filterString, int equalsIndex) {
        StringBuilder result = new StringBuilder();
        for (int index = equalsIndex + 1; index < filterString.length(); index++) {
            if (filterString.charAt(index) != ')') {
                result.append(filterString.charAt(index));
            } else {
                break;
            }
        }
        return result.toString();
    }

    public static String getTopicFromProperties(Map<String, String> properties) {
        String topic = "";
        if (properties.get(Publisher.PUBSUB_SCOPE) != null) {
            topic += properties.get(Publisher.PUBSUB_SCOPE) + ".";
        }

        topic += properties.get(Publisher.PUBSUB_TOPIC);
        return topic;
    }

    public static Dictionary<String, String> mapToDictionary(Map<String, String> map) {
        Dictionary<String, String> dictionary = new Hashtable<>();
        for (String key : map.keySet()) {
            dictionary.put(key, map.get(key));
        }
        return dictionary;
    }

    public static Map<String, String> getPropertiesFromReference(ServiceReference reference) {
        Map<String, String> properties = new HashMap<>();
        for (String key : reference.getPropertyKeys()) {
            Object value = reference.getProperty(key);
            if (value instanceof String) {
                properties.put(key, (String) value);
            }
        }
        return properties;
    }

    public static Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> result = new HashMap<>();

        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = properties.getProperty(key);
            if (value instanceof String) {
                result.put(key, (String) value);
            }
        }

        return result;
    }

    /**
     * Return the framework UUID associated with the provided Bundle Context. If no framework UUID is
     * set it will be assigned.
     *
     * @param bundleContext the context
     * @return the UUID
     */
    public static String getFrameworkUUID(BundleContext bundleContext) {
        String uuid = bundleContext.getProperty("org.osgi.framework.uuid");
        if (uuid != null) {
            return uuid;
        }
        synchronized ("org.osgi.framework.uuid") {
            uuid = bundleContext.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    /**
     * DJB2 Hash Algorithm, the same hashing algorithm is used by Apache Celix
     *
     * @param input the input string to hash
     * @return the hashed string
     */
    public static int stringHash(String input) {

        int hc = 5381;

        for (int i = 0; i < input.length(); i++) {
            hc = (hc << 5) + hc + input.charAt(i);
        }

        return hc;

    }

}
