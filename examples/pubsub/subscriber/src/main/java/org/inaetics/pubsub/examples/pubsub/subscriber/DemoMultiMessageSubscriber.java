package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.inaetics.pubsub.api.MultiMessageSubscriber;
import org.inaetics.pubsub.examples.pubsub.common.Location;
import org.inaetics.pubsub.examples.pubsub.common.PointOfInterrest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class DemoMultiMessageSubscriber implements MultiMessageSubscriber {

    private final Collection<Class<?>> receiveClasses = Collections.unmodifiableCollection(Arrays.asList(Location.class, PointOfInterrest.class));

    @Override
    public Collection<Class<?>> receiveClasses() {
        return this.receiveClasses;
    }

    @Override
    public void init() {
        //nop
    }

    @Override
    public void receive(Object msg) {
        if (msg instanceof Location) {
            receive((Location)msg);
        } else if (msg instanceof  PointOfInterrest) {
            receive((PointOfInterrest)msg);
        }
    }

    private void receive(Location location) {
        System.out.printf("Multi Message Recv location [%s, %s]\n", location.getLat(), location.getLon());
    }

    private void receive(PointOfInterrest poi) {
        System.out.printf("Multi Message Recv poi [%s, %s, %s]\n", poi.getName(), poi.getLocation().getLat(), poi.getLocation().getLon());
    }
}