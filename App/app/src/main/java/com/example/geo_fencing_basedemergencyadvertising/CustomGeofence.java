package com.example.geo_fencing_basedemergencyadvertising;

import org.osmdroid.views.overlay.Polygon;

class CustomGeofence {
    private String description;
    private Polygon polygon;

    public CustomGeofence(String description, Polygon polygon) {
        this.description = description;
        this.polygon = polygon;
    }

    public String getDescription() {
        return description;
    }

    public Polygon getPolygon() {
        return polygon;
    }
}
