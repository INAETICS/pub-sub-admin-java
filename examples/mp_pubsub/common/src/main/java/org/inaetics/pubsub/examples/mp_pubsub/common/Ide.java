package org.inaetics.pubsub.examples.mp_pubsub.common;

public class Ide {

    public static final String MSG_IDE_NAME = "ide"; //Has to match the message name in the msg descriptor in the C bundle!

    public enum Shape {
        SQUARE,
        CIRCLE,
        TRIANGLE,
        RECTANGLE,
        HEXAGON
    }

    private Shape shape;

    public int getShape() {
        return Shape.valueOf(shape.toString()).ordinal();
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }
}
