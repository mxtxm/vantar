package com.vantar.util.json;

import java.lang.reflect.*;
import java.util.List;


public class ListOfJson<T> implements ParameterizedType {

    private final Class<?> wrapped;


    public ListOfJson(Class<T> wrapper) {
        this.wrapped = wrapper;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[]{wrapped};
    }

    @Override
    public Type getRawType() {
        return List.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
