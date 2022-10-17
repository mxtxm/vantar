package com.vantar.util.collection;

import java.util.ArrayList;


public class FixedArrayList<T> extends ArrayList<T> {

    private final int limit;


    public FixedArrayList(int limit){
        super(limit);
        this.limit = limit;
    }

    @Override
    public boolean add(T item){
        super.add(item);
        if (this.size() > limit) {
            remove(0);
        }
        return false;
    }
}
