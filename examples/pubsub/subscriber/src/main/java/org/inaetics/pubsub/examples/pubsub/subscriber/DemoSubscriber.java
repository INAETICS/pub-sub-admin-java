package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.inaetics.pubsub.api.Subscriber;
import org.inaetics.pubsub.examples.pubsub.common.Location;

import java.util.concurrent.LinkedBlockingQueue;

public class DemoSubscriber implements Subscriber<Location> {

    @Override
    public Class<Location> receiveClass() {
        return Location.class;
    }

    @Override
    public void init() {
        //nop
    }

    @Override
    public void receive(Location location) {
        System.out.printf("Recv (%s): [%s, %s]", location.getName(), location.getLat(), location.getLon());
        System.out.println();
    }
}