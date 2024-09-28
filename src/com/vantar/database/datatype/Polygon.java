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

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return locations.hashCode();
    }

}
