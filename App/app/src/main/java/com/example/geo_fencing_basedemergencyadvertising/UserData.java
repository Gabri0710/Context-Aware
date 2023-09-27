package com.example.geo_fencing_basedemergencyadvertising;

public class UserData {
    private double latitude;
    private double longitude;

    private String recognizedActivity;

    public UserData() {
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setData(double latitude, double longitude, int recognizedActivity){
        this.latitude = latitude;
        this.longitude = longitude;
        if(recognizedActivity==1)
            this.recognizedActivity = "WALKING";
        else
            this.recognizedActivity = "CAR";
    }


}
