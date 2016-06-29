package org.inaetics.pubsub.example.dependencymanager;

import org.inaetics.pubsub.api.pubsub.Subscriber;

import DataObjects.Person;

public class SubscriberExample implements Subscriber {

  @Override
  public void receive(Object msg, MultipartCallbacks callbacks) {
    System.out.println("Received message: " + (Person) msg);
  }
}
