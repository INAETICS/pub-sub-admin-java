package org.inaetics.pubsub.examples.pubsub.publisher;

import org.apache.felix.dm.annotation.api.*;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.framework.util.FelixConstants;
import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.examples.pubsub.common.Location;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class DemoPublisher {

    public static final String SERVICE_PID = DemoPublisher.class.getName();

    private BundleContext bundleContext = FrameworkUtil.getBundle(DemoPublisher.class).getBundleContext();
    private volatile ServiceTracker tracker;
    private volatile Publisher publisher;
    private volatile PublishThread publishThread;
    private String topic;
    private Location location;

    private static boolean firstTime = true;

    @Init
    protected final void init(){
        System.out.println("INITIALIZED " + this.getClass().getName());
        this.topic = "poi1"; //TODO: Determine using message descriptor ??

        this.location = new Location();
        this.location.setName("Bundle#" + bundleContext.getBundle().getBundleId());
        this.location.setExtra("DONT PANIC");
    }

    @Start
    protected final void start(){
        System.out.println("STARTED " + this.getClass().getName());

        if (publishThread != null) {
            publishThread.interrupt();
            tracker.close();
        }
        try {
            Filter filter = bundleContext.createFilter("(&(objectClass=" + Publisher.class.getName() + ")"
                    + "(" + Publisher.PUBSUB_TOPIC + "=" + topic + "))");
            tracker = new ServiceTracker(bundleContext, filter, null);
            tracker.open();
            publisher = (Publisher) tracker.waitForService(0);

            publishThread = new PublishThread();
            publishThread.start();
        } catch (InvalidSyntaxException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Stop
    protected final void stop(){
        System.out.println("STOPPED " + this.getClass().getName());

        publishThread.interrupt();
        publishThread = null;
        tracker.close();
        publisher = null;
    }

    @Destroy
    protected final void destroy(){
        System.out.println("DESTROYED " + this.getClass().getName());
    }

    private class PublishThread extends Thread {

        PublishThread(){
            location.setDescription("fw-" + bundleContext.getProperty(FelixConstants.FRAMEWORK_UUID)  + " [TID=" + this.getId() + "]");
        }

        @Override
        public void run() {

            while (!this.isInterrupted()) {

                if (publisher != null) {

                    location.setPositionLat(ThreadLocalRandom.current().nextDouble(Location.MIN_LAT, Location.MAX_LAT));
                    location.setPositionLong(ThreadLocalRandom.current().nextDouble(Location.MIN_LON, Location.MAX_LON));

                    int nrChar = ThreadLocalRandom.current().nextInt(5, 100000);
                    String[] dataArr = new String[nrChar];

                    for (int i = 0; i < nrChar; i++){
                        dataArr[i] = String.valueOf(i % 10);
                    }

                    String data = String.join("", dataArr);
                    location.setData(data);

                    publisher.send(location);

                    System.out.printf("Sent %s [%f, %f] (%s, %s) data len = %d\n",
                            topic,
                            location.getPosition().getLat(),
                            location.getPosition().getLong(),
                            location.getName(),
                            location.getDescription(),
                            nrChar);

                }
                try {
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

}
