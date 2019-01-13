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

public class ZmqConstants {
    public static final String ZMQ_ADMIN_TYPE = "zmq";

    public static final String ZMQ_BASE_PORT = "org.inaetics.pubsub.psa.zeromq.bindport.min";
    public static final int ZMQ_BASE_PORT_DEFAULT = 5501;

    public static final String ZMQ_MAX_PORT = "org.inaetics.pubsub.psa.zeromq.bindport.max";
    public static final int ZMQ_MAX_PORT_DEFAULT = 6000;

    public static final String ZMQ_NR_OF_THREADS = "org.inaetics.pubsub.psa.zeromq.threads";
    public static final String ZMQ_SECURE = "org.inaetics.pubsub.psa.zeromq.secure";

    public final static String ZMQ_CONNECTION_URL = "zmq.url";

    public final static double ZMQ_DEFAULT_SAMPLE_SCORE = 70.0;
    public final static double ZMQ_DEFAULT_CONTROL_SCORE = 80.0;
    public final static double ZMQ_DEFAULT_NO_QOS_SCORE = 80.0;

}
