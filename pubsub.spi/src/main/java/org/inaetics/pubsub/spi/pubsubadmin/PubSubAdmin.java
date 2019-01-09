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
package org.inaetics.pubsub.spi.pubsubadmin;

import java.util.Properties;

public interface PubSubAdmin {

    public static final String PUBSUB_ADMIN_TYPE = "pubsub.admin.type";
    public static final double PUBSUB_ADMIN_FULL_MATCH_SCORE = 100.0;
    public static final double PUBSUB_ADMIN_NO_MATCH_SCORE = 0.0;

    public class MatchResult {
        public final double score;
        public final long selectedSerializerSvcId;
        public final Properties topicProperties;

        public MatchResult(double score, long serSvcId, Properties topicProperties) {
            this.score = score;
            this.selectedSerializerSvcId = serSvcId;
            this.topicProperties = topicProperties;
        }
    }


    /**
     * Inspects if there is a match with requested publisher. This will only be called if requested topic
     * does not yet have a TopicSender (responsibility of TopologyManager).
     */
    public MatchResult matchPublisher(long publisherBndId, final String svcFilter);

    /**
     * Inspect if there is a match with provided subscriber. Note this will only be called there is not yet a
     * a TopcReceiver with the topic provided by the subscriber.
     */
    public MatchResult matchSubscriber(long svcProviderBndId, final Properties svcProperties);


    /**
     * Match a discoverd endpoint. Return true if the endpoint is a match and the required serializer service is
     * available.
     */
    public boolean matchDiscoveredEndpoint(final Properties endpoint);

    /**
     * Creates a TopicSender. Info from the filter and bundle (extender pattern) can be used to configure the sender.
     * Note that this is only true for the first match. After that the TopicSender will verify if the requested
     * publisher is possible. If not this is considered an error -> configuration issue.
     */
    public Properties setupTopicSender(final String scope, final String topic, final Properties topicProperties, long serializerSvcId);


    public void teardownTopicSender(final String scope, final String topic);

    /**
     * Creates a TopicReceiver. info from the properties and bundle (extender pattern) can be used to configure the
     * receiver. Note that this is only true for the first match. After that the TopicSender will verify if the requested
     * publisher is possible. If not this is considered an error -> configuration issue.
     */
    public Properties setupTopicReceiver(final String scope, final String topic, final Properties topicProperties, long serializerSvcId);

    public void teardownTopicReceiver(final String scope, final String topic);

    public void addDiscoveredEndpoint(final Properties endpoint);

    public void removeDiscoveredEndpoint(final Properties endpoint);
}
