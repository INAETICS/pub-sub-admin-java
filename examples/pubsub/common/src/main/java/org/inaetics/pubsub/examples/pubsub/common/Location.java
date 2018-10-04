package org.inaetics.pubsub.examples.pubsub.common;

import java.io.Serializable;

public class Location implements Serializable {

    public static final String MSG_POI_NAME = "poi1"; //Has to match the message name in the msg descriptor in the C bundle!

    public static final double MIN_LAT = -90.0F;
    public static final double MAX_LAT = 90.0F;
    public static final double MIN_LON = -180.0F;
    public static final double MAX_LON = 180.0F;

    private String name;
    private String description;
    private String extra;
    private String data;
    private Poi position;

    public Location() {
        this.name = "";
        this.description = "";
        this.extra = "";
        this.data = "";
        this.position = new Poi();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setPositionLat(double lat){
        this.position.setLat(lat);
    }

    public void setPositionLong(double lon){
        this.position.setLong(lon);
    }

    public Poi getPosition() {
        return this.position;
    }

    public class Poi {
        double lat;
        double lon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLong() {
            return lon;
        }

        public void setLong(double lon) {
            this.lon = lon;
        }
    }

    @Override
    public String toString() {
        return String.format("[%f, %f] (%s, %s) data len = %d",
                this.getPosition().getLat(),
                this.getPosition().getLong(),
                this.getName(),
                this.getDescription(),
                this.getData().length());
    }

}
