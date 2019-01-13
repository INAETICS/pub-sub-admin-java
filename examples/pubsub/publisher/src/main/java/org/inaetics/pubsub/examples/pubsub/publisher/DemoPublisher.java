package org.inaetics.pubsub.examples.pubsub.publisher;

import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.examples.pubsub.common.Location;

public class DemoPublisher {

    private volatile Publisher publisher;

    private Location loc1;

    Thread thread = thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                sendLocationUpdate();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    });

    public DemoPublisher() {
        loc1 = new Location();
        loc1.setName("loc1");
    }

    private void sendLocationUpdate() {
        loc1.setLat(loc1.getLat() + 5.0);
        loc1.setLon(loc1.getLon() + 3.0);
        System.out.printf("Sending loc %s [%s,%s]|", loc1.getName(), loc1.getLat(), loc1.getLon());
        System.out.println();
        publisher.send(loc1);
    }

    public void start(){
        thread.start();
    }

    public void stop(){
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
