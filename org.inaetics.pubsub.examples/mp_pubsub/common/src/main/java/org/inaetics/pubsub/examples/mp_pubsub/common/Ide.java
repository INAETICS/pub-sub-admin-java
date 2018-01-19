package org.inaetics.pubsub.examples.mp_pubsub.common;

public class Ide {

    public enum Shape {
        SQUARE,
        CIRCLE,
        TRIANGLE,
        RECTANGLE,
        HEXAGON,
        LAST_SHAPE
    }

    private Shape shape;

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }
}
