package com.example.geo_fencing_basedemergencyadvertising;

import org.osmdroid.views.overlay.Polygon;

class CustomGeofence {
    private String titolo;
    private String allarme1;
    private String allarme2;
    private String allarme3;
    private Polygon polygon;

    public CustomGeofence(String titolo, String allarme1, String allarme2, String allarme3,Polygon polygon) {
        this.titolo = titolo;
        this.allarme1 = allarme1;
        this.allarme2 = allarme2;
        this.allarme3 = allarme3;
        this.polygon = polygon;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getAllarme1() {
        return allarme1;
    }

    public String getAllarme2() {
        return allarme2;
    }

    public String getAllarme3() {
        return allarme3;
    }



    public Polygon getPolygon() {
        return polygon;
    }
}
