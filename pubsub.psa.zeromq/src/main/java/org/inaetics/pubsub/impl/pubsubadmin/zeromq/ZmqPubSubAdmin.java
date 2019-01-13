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
package org.inaetics.pubsub.impl.pubsubadmin.zeromq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.dm.annotation.api.*;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.zeromq.ZAuth;
import org.zeromq.ZContext;

import javax.sound.midi.Receiver;

import static org.osgi.framework.Constants.SERVICE_ID;

@Component
public class ZmqPubSubAdmin implements PubSubAdmin {

    public static final String SERVICE_PID = ZmqPubSubAdmin.class.getName();

    private final Map<Long, ServiceReference<Serializer>> serializers = new Hashtable<>();
    private final int basePort = 50000; /*TODO make configureable*/
    private final int maxPort = 55000; /*TODO make configureable*/
    private final double sampleScore = ZmqConstants.ZMQ_DEFAULT_SAMPLE_SCORE; /*TODO make configureable*/
    private final double controlScore = ZmqConstants.ZMQ_DEFAULT_CONTROL_SCORE; /*TODO make configureable*/
    private final double noQosScore = ZmqConstants.ZMQ_DEFAULT_NO_QOS_SCORE; /*TODO make configureable*/

    private final Map<String, ZmqTopicSender> senders = new Hashtable<>();
    private final Map<String, ZmqTopicReceiver> receivers = new Hashtable<>();

    private final BundleContext bundleContext = FrameworkUtil.getBundle(ZmqPubSubAdmin.class).getBundleContext();


    private volatile LogService log;
    private ZContext zmqContext;
    private ZAuth zmqAuth;


    public ZmqPubSubAdmin() {
    }

    @Init
    protected synchronized void init() {
        System.out.println("INITIALIZED " + this.getClass().getName());

        String strNrOfIOThreads = bundleContext.getProperty(ZmqConstants.ZMQ_NR_OF_THREADS);
        int nrOfIOThreads = 1;
        if (strNrOfIOThreads != null) {
            nrOfIOThreads = Integer.parseInt(strNrOfIOThreads.trim());
        }

        zmqContext = new ZContext(nrOfIOThreads);

        boolean secure = Boolean.parseBoolean(bundleContext.getProperty(ZmqConstants.ZMQ_SECURE));

        if (secure) {
            zmqAuth = new ZAuth(zmqContext);
            zmqAuth.setVerbose(true);
            zmqAuth.configureCurve(ZAuth.CURVE_ALLOW_ANY); //TODO: Change to key path location
        }

    }

    @Start
    protected synchronized void start() throws Exception {
        System.out.println("STARTED " + this.getClass().getName());
    }

    @Stop
    protected synchronized void stop() throws Exception {
        System.out.println("STOPPED " + this.getClass().getName());
    }

    @Destroy
    protected synchronized void destroy() {
        System.out.println("DESTROYED " + this.getClass().getName());

        if (zmqAuth != null) {
            zmqAuth.destroy();
        }

        zmqContext.destroy();
    }

    @Override
    public MatchResult matchPublisher(long publisherBndId, final String svcFilter) {
        return Utils.matchPublisher(
                bundleContext,
                publisherBndId,
                svcFilter,
                ZmqConstants.ZMQ_ADMIN_TYPE,
                sampleScore,
                controlScore,
                noQosScore);
    }

    @Override
    public MatchResult matchSubscriber(long svcProviderBndId, final Properties svcProperties) {
        return Utils.matchSubscriber(
                bundleContext,
                svcProviderBndId,
                svcProperties,
                ZmqConstants.ZMQ_ADMIN_TYPE,
                sampleScore,
                controlScore,
                noQosScore);
    }


    @Override
    public boolean matchDiscoveredEndpoint(final Properties endpoint) {
        return ZmqPubSubAdmin.PUBSUB_ADMIN_TYPE.equals(endpoint.get(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE));
    }

    @Override
    public synchronized Properties setupTopicSender(final String scope, final String topic, final Properties topicProperties, long serializerSvcId) {
        Properties endpoint = null;
        ServiceReference<Serializer> serRef = this.serializers.get(serializerSvcId);
        Serializer ser = bundleContext.getService(serRef);
        if (ser != null) {
            ZmqTopicSender sender = new ZmqTopicSender(bundleContext, zmqContext, topicProperties, scope, topic, ser);
            String key = scope + "::" + topic;
            senders.put(key, sender);
            sender.start();

            UUID endpointId = UUID.randomUUID();
            endpoint.put(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, ZmqConstants.ZMQ_ADMIN_TYPE);
            endpoint.put(Constants.PUBSUB_ENDPOINT_TYPE, Constants.PUBSUB_PUBLISHER_ENDPOINT_TYPE);
            endpoint.put(Constants.PUBSUB_ENDPOINT_UUID, endpointId.toString());
            endpoint.put(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, topic);
            endpoint.put(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, scope);
            endpoint.put(Constants.PUBSUB_ENDPOINT_SERIALIZER, serRef.getProperty(Serializer.SERIALIZER_NAME_KEY));
            endpoint.put(Constants.PUBSUB_ENDPOINT_VISBILITY, Constants.PUBSUB_SUBSCRIBER_SYSTEM_VISIBLITY);
            endpoint.put(ZmqConstants.ZMQ_CONNECTION_URL, sender.getConnectionUrl());
        }

        return endpoint; //NOTE can be null
    }


    @Override
    public synchronized void teardownTopicSender(final String scope, final String topic) {
        String key = scope + "::" + topic;
        ZmqTopicSender sender = senders.get(key);
        if (senders != null) {
            sender.stop();
        } else {
            log.log(LogService.LOG_WARNING, String.format("Cannot teardown topic sender for %s/%s. Does not exist!", scope, topic));
        }
    }


    @Override
    public synchronized Properties setupTopicReceiver(final String scope, final String topic, final Properties topicProperties, long serializerSvcId) {
        Properties endpoint = null;
        ServiceReference<Serializer> serRef = this.serializers.get(serializerSvcId);
        Serializer ser = bundleContext.getService(serRef);
        if (ser != null) {
            ZmqTopicReceiver receiver = new ZmqTopicReceiver(zmqContext, ser, topicProperties, scope, topic);
            String key = scope + "::" + topic;
            receivers.put(key, receiver);
            receiver.start();

            UUID endpointId = UUID.randomUUID();
            endpoint.put(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE, ZmqConstants.ZMQ_ADMIN_TYPE);
            endpoint.put(Constants.PUBSUB_ENDPOINT_TYPE, Constants.PUBSUB_SUBSCRIBER_ENDPOINT_TYPE);
            endpoint.put(Constants.PUBSUB_ENDPOINT_UUID, endpointId.toString());
            endpoint.put(Constants.PUBSUB_ENDPOINT_TOPIC_NAME, topic);
            endpoint.put(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE, scope);
            endpoint.put(Constants.PUBSUB_ENDPOINT_SERIALIZER, serRef.getProperty(Serializer.SERIALIZER_NAME_KEY));
            endpoint.put(Constants.PUBSUB_ENDPOINT_VISBILITY, Constants.PUBSUB_SUBSCRIBER_SYSTEM_VISIBLITY);
        }

        return endpoint; //NOTE can be null
    }


    @Override
    public synchronized void teardownTopicReceiver(final String scope, final String topic) {
        String key = scope + "::" + topic;
        ZmqTopicReceiver receiver = receivers.get(key);
        if (receiver != null) {
            receiver.stop();
        } else {
            log.log(LogService.LOG_WARNING, String.format("Cannot teardown topic receiver for %s/%s. Does not exist!", scope, topic));
        }
    }

    @Override
    public synchronized void addDiscoveredEndpoint(final Properties endpoint) {
        if (Constants.PUBSUB_PUBLISHER_ENDPOINT_TYPE.equals(endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TYPE))) {
            String url = endpoint.getProperty(ZmqConstants.ZMQ_CONNECTION_URL);
            String scope = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE);
            String topic = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME);
            if (url != null && scope != null && topic != null) {
                String key = scope + "::" + topic;
                ZmqTopicReceiver receiver = receivers.get(key);
                receiver.connectTo(url);
            } else {
                log.log(LogService.LOG_WARNING, String.format("Invalid endpoint. mandatory url (%s), scope (%s) or topic (%s) is missing", url, scope, topic));
            }
        }
    }

    @Override
    public synchronized void removeDiscoveredEndpoint(final Properties endpoint) {
        if (Constants.PUBSUB_PUBLISHER_ENDPOINT_TYPE.equals(endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TYPE))) {
            String url = endpoint.getProperty(ZmqConstants.ZMQ_CONNECTION_URL);
            String scope = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_SCOPE);
            String topic = endpoint.getProperty(Constants.PUBSUB_ENDPOINT_TOPIC_NAME);
            if (url != null && scope != null && topic != null) {
                String key = scope + "::" + topic;
                ZmqTopicReceiver receiver = receivers.get(key);
                receiver.disconnectFrom(url);
            } else {
                log.log(LogService.LOG_WARNING, String.format("Invalid endpoint. mandatory url (%s), scope (%s) or topic (%s) is missing", url, scope, topic));
            }
        }
    }

    public synchronized void addSerializer(ServiceReference<Serializer> serRef) {
        long svcId = (Long) serRef.getProperty(SERVICE_ID);
        this.serializers.put(svcId, serRef);
    }

    public synchronized void removeSerializer(ServiceReference<Serializer> serRef) {
        long svcId = (Long) serRef.getProperty(SERVICE_ID);
        this.serializers.remove(svcId);
    }
}
