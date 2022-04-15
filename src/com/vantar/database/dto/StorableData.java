package com.vantar.database.dto;

import com.vantar.util.object.ObjectUtil;
import java.lang.annotation.Annotation;
import java.util.*;


public class StorableData {

    public String name;
    public Object value;
    public boolean isNull;
    public Class<?> type;
    public Annotation[] annotations;


    public StorableData(Class<?> type, Object value, boolean isNull) {
        this.type = type;
        this.value = value;
        this.isNull = isNull;
    }

    public StorableData(String name, Class<?> type, Object value, boolean isNull, Annotation[] annotations) {
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

    public static Map<String, Object> toMap(List<StorableData> infos) {
        Map<String, Object> x = new HashMap<>();
        for (StorableData info : infos) {
            x.put(info.name, info.value);
        }
        return x;
    }

    @Override
    public String toString() {
        return ObjectUtil.toString(this);
    }
}
