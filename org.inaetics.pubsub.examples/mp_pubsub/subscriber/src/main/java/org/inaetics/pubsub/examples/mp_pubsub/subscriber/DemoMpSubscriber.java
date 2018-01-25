package org.inaetics.pubsub.examples.mp_pubsub.subscriber;

import org.inaetics.pubsub.api.pubsub.Subscriber;
import org.inaetics.pubsub.examples.mp_pubsub.common.Ew;
import org.inaetics.pubsub.examples.mp_pubsub.common.Ide;
import org.inaetics.pubsub.examples.mp_pubsub.common.Kinematics;

public class DemoMpSubscriber implements Subscriber {

    private String topic;

    public DemoMpSubscriber(String topic) {
        this.topic = topic;
    }

    @Override
    public void receive(Object msg, MultipartCallbacks callbacks) {
        Kinematics kinematics = callbacks.getMultipart(Kinematics.class);
        Ide ide = callbacks.getMultipart(Ide.class);
        Ew ew = callbacks.getMultipart(Ew.class);

        if (kinematics == null) {
            System.out.println("MP_SUBSCRIBER: Unexpected NULL data for kinematics data");
        } else {
            System.out.printf("kin_data: pos=[%f, %f] occurrences=%d\n",
                    kinematics.getPosition().getLat(),
                    kinematics.getPosition().getLong(),
                    kinematics.getOccurrences());
        }

        if (ide == null) {
            System.out.println("MP_SUBSCRIBER: Unexpected NULL data for ide data");
        } else {
            System.out.printf("ide_data: shape=%s\n",
                    Ide.Shape.values()[ide.getShape()].toString());
        }

        if (ew == null) {
            System.out.println("MP_SUBSCRIBER: Unexpected NULL data for ew data");
        } else {
            System.out.printf("ew_data: area=%f color=%s\n",
                    ew.getArea(),
                    Ew.Color.values()[ew.getColor()].toString());
        }

        System.out.print("\n");

    }
}