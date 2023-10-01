package com.example.geo_fencing_basedemergencyadvertising;

public class UserData {

    private String username;
    private double latitude;
    private double longitude;

    //only for test
    private int i;

    private String recognizedActivity;

    public UserData() {
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setData(String username,double latitude, double longitude, int recognizedActivity){
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.i = 0;
        if(recognizedActivity==1)
            this.recognizedActivity = "WALKING";
        else
            this.recognizedActivity = "CAR";
    }

    public void increaseCont(){
        this.username+= Integer.toString(i);
        i+=1;
    }


}
