package org.inaetics.pubsub.examples.pubsub.subscriber;

import org.apache.felix.dm.annotation.api.*;
import org.inaetics.pubsub.api.Subscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

@Component
public class Demo {

    public static final String SERVICE_PID = Demo.class.getName();

    private BundleContext bundleContext = FrameworkUtil.getBundle(Demo.class).getBundleContext();
    private ServiceRegistration registration;
    private DemoSubscriber subscriber;
    private String topic;

    @Init
    protected final void init(){
        System.out.println("INITIALIZED " + this.getClass().getName());

        this.topic = "poi1"; //TODO: Determine using message descriptor ??

        this.subscriber = new DemoSubscriber(this.topic);
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

}
