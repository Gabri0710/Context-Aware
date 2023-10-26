package com.example.geo_fencing_basedemergencyadvertising;

public class UserData {

    private String username;
    private double latitude;
    private double longitude;

    private String recognizedActivity;

    public UserData() {
    }

    public void setData(String username,double latitude, double longitude, int recognizedActivity){
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        if(recognizedActivity==1)
            this.recognizedActivity = "WALKING";
        else
            this.recognizedActivity = "CAR";
    }



}
