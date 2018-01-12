package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.inaetics.pubsub.api.pubsub.Subscriber;

import java.util.concurrent.LinkedBlockingQueue;

public class DemoSubscriber implements Subscriber {

    public LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

    @Override
    public void receive(Object msg, MultipartCallbacks callbacks) {
        System.out.println(msg);
        queue.add((Integer) msg);
    }
}
