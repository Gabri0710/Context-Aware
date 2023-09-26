package com.example.geo_fencing_basedemergencyadvertising;

public class LocationData {
    private double latitude;
    private double longitude;

    public LocationData() {
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setPosition(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }


}
