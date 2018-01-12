package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.apache.felix.dm.annotation.api.*;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

@Component
public class Demo {

    public static final String SERVICE_PID = Demo.class.getName();

    private BundleContext bundleContext = FrameworkUtil.getBundle(Demo.class).getBundleContext();
    private ServiceRegistration registration;
    private DemoSubscriber subscriber = new DemoSubscriber();
    private String topic;

    @Init
    protected final void init(){
        System.out.println("INITIALIZED " + this.getClass().getName());

        this.topic = "testTopic"; //TODO: Determine using message descriptor ??
    }

    @Start
    protected final void start(){
        System.out.println("STARTED " + this.getClass().getName());

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Subscriber.PUBSUB_TOPIC, topic);
        registration = bundleContext.registerService(Subscriber.class.getName(), subscriber, properties);
    }

    @Stop
    protected final void stop(){
        System.out.println("STOPPED " + this.getClass().getName());

        registration.unregister();
    }

    @Destroy
    protected final void destroy(){
        System.out.println("DESTROYED " + this.getClass().getName());
    }

    //TODO: Method not called yet
    public synchronized List<Integer> getBuffer(){
        List<Integer> buffer = new ArrayList<>();
        subscriber.queue.drainTo(buffer);
        return buffer;

    }

}
