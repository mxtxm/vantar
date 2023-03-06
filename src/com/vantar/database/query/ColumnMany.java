package com.vantar.database.query;


public class ColumnMany {

    public String className;
    public String[] properties;
    public String propertyOut;


    public ColumnMany() {

    }

    public ColumnMany(String className, String[] properties, String propertyOut) {
        this.className = className;
        this.properties = properties;
        this.propertyOut = propertyOut;
    }
}
