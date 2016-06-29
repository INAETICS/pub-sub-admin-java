package org.inaetics.pubsub.example.dependencymanager;

import java.util.Dictionary;
import java.util.Hashtable;

import org.inaetics.pubsub.api.pubsub.Publisher;
import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import DataObjects.Person;

public class PublisherExample {
  private volatile Publisher publisher;
  private Thread thread;

  protected final void start() throws Exception {
    System.out.println("STARTED " + this.getClass().getName());
    
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        int i = 0;
        while (!Thread.interrupted()) {
          System.out.println("sending...");
          publisher.send(new Person("Jeroen", i++));
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            return;
          }
        }
      }
    };

    thread = new Thread(runnable);
    thread.start();

  }

  protected final void stop() throws Exception {
    System.out.println("STOPPED " + this.getClass().getName());
    thread.interrupt();
  }

  void destroy() {
    System.out.println("DESTROYED " + this.getClass().getName());
  }
}
