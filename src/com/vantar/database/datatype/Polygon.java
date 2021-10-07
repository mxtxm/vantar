package com.vantar.database.datatype;

import java.util.*;


public class Polygon {

    public final List<Location> locations;

    public Polygon(List<Location> locations) {
        this.locations = locations;
    }

    public Polygon() {
        this.locations = new ArrayList<>();
    }

    public void addLocation(double latitude, double longitude) {
        locations.add(new Location(latitude, longitude));
    }
}
