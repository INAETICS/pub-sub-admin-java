package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.examples.pubsub.common.Location;

import java.util.concurrent.LinkedBlockingQueue;

public class DemoSubscriber implements Subscriber {

    public LinkedBlockingQueue<Location> queue = new LinkedBlockingQueue<>();
    private String topic;

    public DemoSubscriber(String topic) {
        this.topic = topic;
    }

    @Override
    public void receive(Object msg, MultipartCallbacks callbacks) {
        Location location = (Location) msg;

        int nrDataChars = 25;
        System.out.printf("Recv (%s): [%f, %f] (%s, %s) data_len = %d data = %s\n",
                this.topic,
                location.getPosition().getLat(),
                location.getPosition().getLong(),
                location.getName(),
                location.getDescription(),
                location.getData().length(),
                location.getData().substring(0, nrDataChars));

        queue.add(location);
    }
}