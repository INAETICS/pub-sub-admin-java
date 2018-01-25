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

import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.spi.serialization.MultipartContainer;
import org.inaetics.pubsub.spi.serialization.Serializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ZmqSubscriber extends Thread {

  private BundleContext bundleContext = FrameworkUtil.getBundle(ZmqSubscriber.class).getBundleContext();

  private final Map<Subscriber, BlockingQueue<MultipartContainer>> subscribers =
          Collections.synchronizedMap(new HashMap<>());
  private final Map<Subscriber, SubscriberCaller> subscriberCallers =
          Collections.synchronizedMap(new HashMap<>());

  private Map<String, String> zmqProperties;
  private String topic;
  private Serializer serializer;

  private ZMQ.Socket socket;

  public ZmqSubscriber(Map<String, String> zmqProperties, String topic, String serializer, ZMQ.Socket socket) {
    this.zmqProperties = zmqProperties;
    this.topic = topic;

    this.socket = socket;

    try {
      ServiceTracker tracker = new ServiceTracker<>(bundleContext,
              bundleContext.createFilter("(&(objectClass=" + Serializer.class.getName() + ")" + "("
                      + Serializer.SERIALIZER + "=" + serializer + "))"),
              null);
      tracker.open();
      this.serializer = (Serializer) tracker.waitForService(0);

    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void run() {
    while (!this.isInterrupted()) {

      ZFrame headerMsg = ZFrame.recvFrame(this.socket);

      if (headerMsg == null) {

        System.out.println("headerMsg is null");

      } else {

        if (headerMsg.hasMore()) {
          ZFrame payloadMsg = ZFrame.recvFrame(this.socket);

          if (payloadMsg == null) {
            System.out.println("payloadMsg is null!");
            headerMsg.destroy();
          } else {

            MultipartContainer rootContainer = serializer.deserialize(payloadMsg.getData());

            boolean more = payloadMsg.hasMore();
            while (more){
              ZFrame hMsg = ZFrame.recvFrame(this.socket);
              if (hMsg == null) {
                System.out.println("hMsg is null");
                break;
              }

              ZFrame pMsg = ZFrame.recvFrame(this.socket);
              if (pMsg == null) {
                System.out.println("pMsg is null");
                hMsg.destroy();
                break;
              }

              MultipartContainer mc = serializer.deserialize(pMsg.getData());
              List<Object> objs = mc.getObjects();
              for (Object obj : objs){
                rootContainer.addObject(obj);
              }

              more = pMsg.hasMore();

            } // while (more)

            synchronized (subscribers) {
              for (BlockingQueue<MultipartContainer> queue : subscribers.values()) {
                queue.add(rootContainer);
              }
            }

          } //payloadMsg != null

        } else {
          System.out.printf("Received message for topic '%s' without payload!\n", new String(headerMsg.getData()));
          headerMsg.destroy();
        } //!headerMsg.hasMore()

      } // headerMsg != null

    } // while (!this.isInterrupted())
  }

  private class SubscriberCaller extends Thread {

    private BlockingQueue<MultipartContainer> queue;
    private Subscriber subscriber;

    public SubscriberCaller(Subscriber subscriber, BlockingQueue<MultipartContainer> queue) {
      this.subscriber = subscriber;
      this.queue = queue;
    }

    @Override
    public void run() {
      while (!this.isInterrupted()) {
        try {
          MultipartContainer container = queue.take();
          subscriber.receive(container.getObjects().get(0), container);
        } catch (InterruptedException e) {
          //TODO: Auto-generated catch block
        }
      }
    }

  }

  public void connect(Subscriber subscriber) {
    BlockingQueue<MultipartContainer> queue = new LinkedBlockingQueue<MultipartContainer>();
    SubscriberCaller caller = new SubscriberCaller(subscriber, queue);
    this.subscribers.put(subscriber, queue);
    this.subscriberCallers.put(subscriber, caller);
    caller.start();
  }

  public void disconnect(Subscriber subscriber) {
    this.subscribers.remove(subscriber);
    this.subscriberCallers.get(subscriber).interrupt();
    this.subscriberCallers.remove(subscriber);
  }

  public boolean hasSubscribers() {
    return !subscribers.isEmpty();
  }

  public void stopZmqSubscriber() {
    this.interrupt();
    for (Thread thread : subscriberCallers.values()) {
      thread.interrupt();
    }
  }

  public String getTopic() {
    return this.topic;
  }


}
