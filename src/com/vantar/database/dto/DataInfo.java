package com.vantar.database.dto;

import java.lang.annotation.Annotation;
import java.util.*;


public class DataInfo {

    public String name;
    public Object value;
    public boolean isNull;
    public Class<?> type;
    public Annotation[] annotations;


    public DataInfo(Class<?> type, Object value, boolean isNull) {
        this.type = type;
        this.value = value;
        this.isNull = isNull;
    }

    public DataInfo(String name, Class<?> type, Object value, boolean isNull, Annotation[] annotations) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isNull = isNull;
        this.annotations = annotations;
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        if (annotations == null) {
            return false;
        }
        for (Annotation a : annotations) {
            if (a.getClass().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotation) {
        if (annotations == null) {
            return null;
        }
        for (Annotation a : annotations) {
            if (a.getClass().equals(annotation)) {
                return a;
            }
        }
        return null;
    }

    public static Map<String, Object> toMap(List<DataInfo> infos) {
        Map<String, Object> x = new HashMap<>();
        for (DataInfo info : infos) {
            x.put(info.name, info.value);
        }
        return x;
    }

    @Override
    public String toString() {
        return "type:" + type.getSimpleName() + " name:" + name + " value:" + value;
    }
}
