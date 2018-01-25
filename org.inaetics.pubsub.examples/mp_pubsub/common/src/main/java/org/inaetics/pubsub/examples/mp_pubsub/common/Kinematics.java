package org.inaetics.pubsub.examples.mp_pubsub.common;

public class Kinematics {

    public static final String MSG_KINEMATICS_NAME = "kinematics"; //Has to match the message name in the msg descriptor in the C bundle!

    public static final double MIN_LAT = -90.0F;
    public static final double MAX_LAT = 90.0F;
    public static final double MIN_LON = -180.0F;
    public static final double MAX_LON = 180.0F;

    public static final int MIN_OCCUR = 1;
    public static final int MAX_OCCUR = 5;

    private Poi position;
    private int occurrences;

    public Kinematics(){
        this.position = new Poi();
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
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

}
