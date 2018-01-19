package org.inaetics.pubsub.examples.mp_pubsub.common;

public class Ew {

    public static final double MIN_AREA = 50.0F;
    public static final double MAX_AREA = 15000.0F;

    public enum Color {
        GREEN,
        BLUE,
        RED,
        BLACK,
        WHITE,
        LAST_COLOR
    }

    private Color color;
    private double area;

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
