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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.dm.annotation.api.*;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.pubsubadmin.PubSubAdmin;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.inaetics.pubsub.spi.utils.Constants;
import org.inaetics.pubsub.spi.utils.Utils;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.zeromq.ZAuth;
import org.zeromq.ZContext;

import static org.osgi.framework.Constants.SERVICE_ID;

@Component
public class ZmqPubSubAdmin implements PubSubAdmin {

  public static final String SERVICE_PID = ZmqPubSubAdmin.class.getName();

  private Map<Long,Serializer> serializers = new HashMap<>();

  private final int basePort = 50000; /*TODO make configureable*/
  private final int maxPort =  55000; /*TODO make configureable*/

  private BundleContext bundleContext = FrameworkUtil.getBundle(ZmqPubSubAdmin.class).getBundleContext();

  private static final Set<String> zmqProperties = new HashSet<>(Arrays.asList(
          ZmqConstants.ZMQ_BASE_PORT,
          ZmqConstants.ZMQ_MAX_PORT,
          ZmqConstants.ZMQ_NR_OF_THREADS,
          ZmqConstants.ZMQ_SECURE
  ));
  private static final Set<String> generalProperties =
      new HashSet<>(Arrays.asList("serializer,pubsub.topic,pubsub.scope".split(",")));
  
  private volatile LogService m_LogService;

  private ZContext zmqContext;
  private ZAuth zmqAuth;


  public ZmqPubSubAdmin() { }

  @Init
  protected final void init() {
    System.out.println("INITIALIZED " + this.getClass().getName());

    String strNrOfIOThreads = bundleContext.getProperty(ZmqConstants.ZMQ_NR_OF_THREADS);
    int nrOfIOThreads = 1;
    if (strNrOfIOThreads != null){
      nrOfIOThreads = Integer.parseInt(strNrOfIOThreads.trim());
    }

    zmqContext = new ZContext(nrOfIOThreads);

    boolean secure = Boolean.parseBoolean(bundleContext.getProperty(ZmqConstants.ZMQ_SECURE));

    if (secure){
      zmqAuth = new ZAuth(zmqContext);
      zmqAuth.setVerbose(true);
      zmqAuth.configureCurve(ZAuth.CURVE_ALLOW_ANY); //TODO: Change to key path location
    }

  }

  @Start
  protected final void start() throws Exception {
    System.out.println("STARTED " + this.getClass().getName());
  }

  @Stop
  protected final void stop() throws Exception {
    System.out.println("STOPPED " + this.getClass().getName());
  }

  @Destroy
  protected final void destroy() {
    System.out.println("DESTROYED " + this.getClass().getName());

    if (zmqAuth != null){
      zmqAuth.destroy();
    }

    zmqContext.destroy();
  }

  @Override
  public MatchResult matchPublisher(long publisherBndId, final String svcFilter) {
    synchronized (this.serializers) {
      if (serializers.size() == 0) {
        return new MatchResult(PubSubAdmin.PUBSUB_ADMIN_NO_MATCH_SCORE, -1L, null);
      } else {
        //TODO forward call to Utils.matchPublisher
        long svcId = serializers.keySet().iterator().next();
        return new MatchResult(PubSubAdmin.PUBSUB_ADMIN_FULL_MATCH_SCORE, svcId, null);
      }
    }
  }

  @Override
  public MatchResult matchSubscriber(long svcProviderBndId, final Properties svcProperties) {
    synchronized (this.serializers) {
      if (serializers.size() == 0) {
        return new MatchResult(PubSubAdmin.PUBSUB_ADMIN_NO_MATCH_SCORE, -1L, null);
      } else {
        //TODO forward call to Utils.matchSubscriber
        long svcId = serializers.keySet().iterator().next();
        return new MatchResult(PubSubAdmin.PUBSUB_ADMIN_FULL_MATCH_SCORE, svcId, null);
      }
    }
  }


  @Override
  public boolean matchDiscoveredEndpoint(final Properties endpoint) {
    return ZmqPubSubAdmin.PUBSUB_ADMIN_TYPE.equals(endpoint.get(Constants.PUBSUB_ENDPOINT_ADMIN_TYPE));
  }

  @Override
  public Properties setupTopicSender(final String scope, final String topic, final Properties topicProperties, long serializerSvcId) {
    //Properties result = new Properties();
    //Try to create TopicSender and create &
    //fill props with endpoint UUID, pubsub.config, endpoint type, endpoint url, etc
    return null;
  }


  @Override
  public void teardownTopicSender(final String scope, final String topic) {
    //get TopicSender based on scope/topic and stop/destroy
  }


  @Override
  public Properties setupTopicReceiver(final String scope, final String topic, final Properties topicProperties, long serializerSvcId) {
    //Properties result = new Properties();
    //Try to create TopicReceiver and create &
    //fill props with endpoint UUID, pubsub.config, endpoint type, endpoint url, etc
    return null;
  }


  @Override
  public void teardownTopicReceiver(final String scope, final String topic) {
    //get TopicReceiver based on scope/topic and stop/destroy
  }

  @Override
  public void addDiscoveredEndpoint(final Properties endpoint) {
    //Find topic sender/receiver based on endpoint info connect
  }

  @Override
  public void removeDiscoveredEndpoint(final Properties endpoint) {
    //Find topic sender/receiver based on endpoint info and disconnect
  }

  public void addSerializer(Properties props, Serializer s) {
    String svcIdStr = props.getProperty(SERVICE_ID);
    long svcId = Long.parseLong(svcIdStr == null ? "-1L" : svcIdStr);
    synchronized (this.serializers) {
      this.serializers.put(svcId, s);
    }
  }

  public void removeSerializer(Properties props, Serializer s) {
    String svcIdStr = props.getProperty(SERVICE_ID);
    long svcId = Long.parseLong(svcIdStr == null ? "-1L" : svcIdStr);
    synchronized (this.serializers) {
      this.serializers.remove(svcId);
    }
  }
}
