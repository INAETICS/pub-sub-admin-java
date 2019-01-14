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

import org.inaetics.pubsub.api.Constants;
import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
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

    private static Properties getBundleProperties(final Bundle bnd, boolean publisher, String topic) {
        Properties result = new Properties();
        String path = String.format("META-INF/topics/%s/%s.propeties", publisher ? "pub" : "sub", topic);
        URL propsUrl = bnd.getEntry(path);
        if (propsUrl != null) {
            try {
                result.load(propsUrl.openStream());
            } catch (IOException ie) {
                System.err.println("Error reading properties from url " + propsUrl.getFile());
            }
        }
        return result;
    }

    private static String getScopeFromFilter(final String filter) {
        return getAttributeValueFromFilter(filter, Publisher.PUBSUB_SCOPE);
    }

    private static String getTopicFromFilter(final String filter) {
        return getAttributeValueFromFilter(filter, Publisher.PUBSUB_TOPIC);
    }

    private static PubSubAdmin.MatchResult matchFor(
            BundleContext ctx,
            final Properties topicProperties,
            final String adminType,
            double sampleScore,
            double controlScore,
            double noQosScore) {

        double score = PubSubAdmin.PUBSUB_ADMIN_NO_MATCH_SCORE;
        long serializerSvcId = -1L;

        String requestedAdminType = null;
        String requestedQos = null;
        String requestedSerializer = null;
        if (topicProperties != null) {
            requestedAdminType = (String) topicProperties.getOrDefault(Constants.TOPIC_CONFIG_KEY, null);
            requestedQos = (String) topicProperties.getOrDefault(Constants.TOPIC_QOS_KEY, null);
            requestedSerializer = (String) topicProperties.getOrDefault(Constants.TOPIC_SERIALIZER_KEY, null);
        }

        if (requestedAdminType != null && requestedAdminType.equals(adminType)) {
            score = PubSubAdmin.PUBSUB_ADMIN_FULL_MATCH_SCORE;
        } else if (requestedQos != null && requestedQos.equals(Constants.TOPIC_CONTROL_QOS_VALUE)) {
            score = controlScore;
        } else if (requestedQos != null && requestedQos.equals(Constants.TOPIC_SAMPLE_QOS_VALUE)) {
            score = sampleScore;
        } else {
            score = noQosScore;
        }

        if (requestedSerializer != null) {
            String serFilter = String.format("(%s=%s)", Serializer.SERIALIZER_NAME_KEY, requestedSerializer);
            try {
                Collection<ServiceReference<Serializer>> refs = ctx.getServiceReferences(Serializer.class, serFilter);
                if (refs.size() == 1) {
                    String idStr = refs.iterator().next().getProperty("service.id").toString();
                    long id = Long.parseLong(idStr);
                    serializerSvcId = id;
                }
            } catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            ServiceReference<Serializer> ref = ctx.getServiceReference(Serializer.class);
            if (ref != null) {
                String idStr = ref.getProperty("service.id").toString();
                long id = Long.parseLong(idStr);
                serializerSvcId = id;
            }
        }

        return new PubSubAdmin.MatchResult(score, serializerSvcId, topicProperties);
    }

    public static PubSubAdmin.MatchResult matchSubscriber(
            BundleContext ctx,
            long requestingBundleId,
            final Properties subscriberProperties,
            final String adminType,
            double sampleScore,
            double controlScore,
            double noQosScore) {
        String topic = subscriberProperties.getProperty(Constants.TOPIC_KEY);
        Bundle bnd = ctx.getBundle(requestingBundleId);
        Properties topicProperties = null;
        if (topic != null && bnd != null) {
            topicProperties = getBundleProperties(bnd, false, topic);
        }
        return matchFor(ctx, topicProperties, adminType, sampleScore, controlScore, noQosScore);
    }

    public static PubSubAdmin.MatchResult matchPublisher(
            BundleContext ctx,
            long requestingBundleId,
            final String requestFilter,
            final String adminType,
            double sampleScore,
            double controlScore,
            double noQosScore) {

        String topic = getTopicFromFilter(requestFilter);
        Bundle bnd = ctx.getBundle(requestingBundleId);
        Properties topicProperties = null;
        if (topic != null && bnd != null) {
            topicProperties = getBundleProperties(bnd, true, topic);
        }
        return matchFor(ctx, topicProperties, adminType, sampleScore, controlScore, noQosScore);
    }



    public static String getAttributeValueFromFilter(final String filter, final String attribute) {
        String result = null;
        Pattern pattern = Pattern.compile(".*\\(" + attribute + "=([^)]*)\\).*");
        Matcher m = pattern.matcher(filter);
        if (m.matches()) {
            result = m.group(1);
        }
        return result;
    }


    public static boolean validateEndpoint(LogService log, Properties endpoint) {
        //check if the mandatory key exists
        Object uuid = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_UUID);
        Object type = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TYPE);
        Object topic = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TOPIC_NAME);
        Object scope = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE);
        Object ser = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_SERIALIZER);
        Object adminType = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_ADMIN_TYPE);
        Object fwUUID = endpoint.get(org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_FRAMEWORK_UUID);
        boolean valid = uuid != null && type != null && topic != null && scope != null && ser != null && adminType != null && fwUUID != null;
        if (!valid) {
            if (uuid == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_UUID));
            }
            if (type == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TYPE));
            }
            if (topic == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TOPIC_NAME));
            }
            if (scope == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE));
            }
            if (ser == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_SERIALIZER));
            }
            if (adminType == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_ADMIN_TYPE));
            }
            if (fwUUID == null) {
                log.log(LogService.LOG_WARNING, String.format("Missing %s entry", org.inaetics.pubsub.spi.utils.Constants.PUBSUB_ENDPOINT_FRAMEWORK_UUID));
            }
        }
        return valid;
    }
}
