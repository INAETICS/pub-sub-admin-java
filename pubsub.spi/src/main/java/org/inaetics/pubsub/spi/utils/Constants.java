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

public final class Constants {

    public static final String PUBSUB_PUBLISHER_ENDPOINT_TYPE = "publisher";
    public static final String PUBSUB_SUBSCRIBER_ENDPOINT_TYPE = "subscriber";

    public static final String PUBSUB_SUBSCRIBER_SYSTEM_VISIBLITY = "system";
    public static final String PUBSUB_SUBSCRIBER_LOCAL_VISIBLITY = "local";

    //mandatory endpoint properties
    public static final String PUBSUB_ENDPOINT_TOPIC_NAME = "pubsub.topic.name";
    public static final String PUBSUB_ENDPOINT_TOPIC_SCOPE = "pubsub.topic.scope";
    public static final String PUBSUB_ENDPOINT_UUID = "pubsub.endpoint.uuid";
    public static final String PUBSUB_ENDPOINT_FRAMEWORK_UUID = "pubsub.framework.uuid";
    public static final String PUBSUB_ENDPOINT_TYPE = "pubsub.endpoint.type"; //PUBSUB_PUBLISHER_ENDPOINT_TYPE or PUBSUB_SUBSCRIBER_ENDPOINT_TYPE
    public static final String PUBSUB_ENDPOINT_ADMIN_TYPE = "pubsub.config";
    public static final String PUBSUB_ENDPOINT_SERIALIZER = "pubsub.serializer";
    public static final String PUBSUB_ENDPOINT_VISBILITY = "pubsub.visibility"; //SYSTEM or LOCAL
}
