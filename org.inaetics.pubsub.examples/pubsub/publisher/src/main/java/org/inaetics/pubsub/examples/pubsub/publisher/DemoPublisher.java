package org.inaetics.pubsub.examples.pubsub.publisher;

import org.apache.felix.dm.annotation.api.*;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.inaetics.pubsub.api.pubsub.Publisher;
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

    @Init
    protected final void init(){
        System.out.println("INITIALIZED " + this.getClass().getName());
        this.topic = "testTopic"; //TODO: Determine using message descriptor ??
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
            while (!this.isInterrupted()) {

                if (publisher != null) {
                    Integer integer = ThreadLocalRandom.current().nextInt(0, 11);
                    System.out.println("publishing: " + integer);
                    publisher.send(integer);
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
