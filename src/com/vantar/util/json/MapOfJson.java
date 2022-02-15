package com.vantar.util.json;

import java.lang.reflect.*;
import java.util.Map;


class MapOfJson<K, V> implements ParameterizedType {

    private final Class<?> wrappedK;
    private final Class<?> wrappedV;


    public MapOfJson(Class<K> wrapperK, Class<V> wrapperV) {
        this.wrappedK = wrapperK;
        this.wrappedV = wrapperV;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] { wrappedK, wrappedV };
    }

    @Override
    public Type getRawType() {
        return Map.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
