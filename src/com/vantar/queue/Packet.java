package com.vantar.queue;

import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Packet {

    private int type;
    private String data;
    private String tClass;


    // > Q
    public Packet(Object object) {
        this(object, 0);
    }

    // > Q
    public Packet(Object object, int jobType) {
        if (object == null) {
            return;
        }
        data = jobType + "|" +  object.getClass().getName() + "|" +  Json.toJson(object);
    }

    // Q >
    public Packet(byte[] data) {
        if (data == null) {
            return;
        }
        setSerializedPacket(new String(data, StandardCharsets.UTF_8));
    }

    public boolean isValid() {
        return data != null;
    }

    public void setSerializedPacket(String serialized) {
        String[] parts = StringUtil.split(serialized, '|', 3);
        if (parts.length < 3) {
            return;
        }
        this.type = StringUtil.toInteger(parts[0]);
        this.tClass = parts[1];
        this.data = parts[2];
    }

    public int getType() {
        return type;
    }

    public String gettClass() {
        return tClass;
    }

    public String getString() {
        return data;
    }

    public Long getLong() {
        return StringUtil.toLong(data);
    }

    public Double getDouble() {
        return StringUtil.toDouble(data);
    }

    public <T> T getObject(Class<T> typeClass) {
        return Json.fromJson(data, typeClass);
    }

    public <T> T getObject() {
        try {
            return (T) Json.fromJson(data, Class.forName(tClass));
        } catch (ClassNotFoundException | NullPointerException e) {
            return null;
        }
    }

    public <T> List<T> getList(Class<T> typeClass) {
        return Json.listFromJson(data, typeClass);
    }

    public <K, V> Map<K, V> getList(Class<K> keyTypeClass, Class<V> valueTypeClass) {
        return Json.mapFromJson(data, keyTypeClass, valueTypeClass);
    }

    public String toString() {
        return "type:" + type + ", class:" + tClass + ", data:" + data;
    }
}
