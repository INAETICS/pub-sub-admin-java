package org.inaetics.pubsub.examples.mp_pubsub.common;

public class Kinematics {

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

    public double getPositionLat(){
        return this.position.getLat();
    }

    public void setPositionLon(double lon){
        this.position.setLon(lon);
    }

    public double getPositionLon(){
        return this.position.getLon();
    }

    private class Poi {
        double lat;
        double lon;

        private double getLat() {
            return lat;
        }

        private void setLat(double lat) {
            this.lat = lat;
        }

        private double getLon() {
            return lon;
        }

        private void setLon(double lon) {
            this.lon = lon;
        }
    }

}
