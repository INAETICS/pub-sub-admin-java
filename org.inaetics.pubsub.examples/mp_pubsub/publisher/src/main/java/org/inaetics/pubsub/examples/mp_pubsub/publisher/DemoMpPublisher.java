package org.inaetics.pubsub.examples.mp_pubsub.publisher;

import org.apache.felix.dm.annotation.api.*;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.inaetics.pubsub.api.pubsub.MultipartException;
import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.examples.mp_pubsub.common.Ew;
import org.inaetics.pubsub.examples.mp_pubsub.common.Ide;
import org.inaetics.pubsub.examples.mp_pubsub.common.Kinematics;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class DemoMpPublisher {

    public static final String SERVICE_PID = DemoMpPublisher.class.getName();

    private BundleContext bundleContext = FrameworkUtil.getBundle(DemoMpPublisher.class).getBundleContext();
    private volatile ServiceTracker tracker;
    private volatile Publisher publisher;
    private volatile PublishThread publishThread;
    private String topic;

    private Kinematics kinematics;
    private Ew ew;
    private Ide ide;

    @Init
    protected final void init(){
        System.out.println("INITIALIZED " + this.getClass().getName());
        this.topic = "testMpTopic"; //TODO: Determine using message descriptor ??

        this.kinematics = new Kinematics();
        this.ew = new Ew();
        this.ide = new Ide();
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

        @Override
        public void run() {
            int counter = 1;

            while (!this.isInterrupted()) {

                if (publisher != null) {

                    kinematics.setPositionLat(ThreadLocalRandom.current().nextDouble(Kinematics.MIN_LAT, Kinematics.MAX_LAT));
                    kinematics.setPositionLon(ThreadLocalRandom.current().nextDouble(Kinematics.MIN_LON, Kinematics.MAX_LON));
                    kinematics.setOccurrences(ThreadLocalRandom.current().nextInt(Kinematics.MIN_OCCUR, Kinematics.MAX_OCCUR));
                    try {
                        System.out.printf("Track#%d kin_data: pos=[%f, %f] occurrences=%d\n",
                                counter,
                                kinematics.getPositionLat(),
                                kinematics.getPositionLon(),
                                kinematics.getOccurrences());

                        publisher.sendMultipart(kinematics, Publisher.PUBLISHER_FIRST_MSG);
                    } catch (MultipartException e) {
                        System.out.println("Error with first message: " + e.getMessage());
                    }

                    ide.setShape(Ide.Shape.values()[ThreadLocalRandom.current().nextInt(0, Ide.Shape.values().length)]);
                    try {
                        System.out.printf("Track#%d ide_data: shape=%s\n",
                                counter,
                                ide.getShape().toString());
                        publisher.sendMultipart(ide, Publisher.PUBLISHER_PART_MSG);
                    } catch (MultipartException e) {
                        System.out.println("Error with part message: " + e.getMessage());
                    }

                    ew.setArea(ThreadLocalRandom.current().nextDouble(Ew.MIN_AREA, Ew.MAX_AREA));
                    ew.setColor(Ew.Color.values()[ThreadLocalRandom.current().nextInt(0, Ew.Color.values().length)]);
                    try {
                        System.out.printf("Track#%d ew_data: area=%f color=%s\n",
                                counter,
                                ew.getArea(),
                                ew.getColor().toString());
                        publisher.sendMultipart(ew, Publisher.PUBLISHER_LAST_MSG);
                    } catch (MultipartException e) {
                        System.out.println("Error with last message: " + e.getMessage());
                    }

                    System.out.print("\n");
                    counter++;

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
