package com.vantar.util.collection;

import java.util.LinkedHashMap;
import java.util.Map;


public class BucketMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;


    public BucketMap(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
