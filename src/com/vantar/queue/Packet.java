package com.vantar.queue;

import com.vantar.util.json.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Packet {

    private static final Logger log = LoggerFactory.getLogger(Packet.class);

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
        data = jobType + "|" +  object.getClass().getName() + "|" +  Json.d.toJson(object);
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

    public Class<?> getClassType() {
        try {
            return Class.forName(tClass);
        } catch (ClassNotFoundException e) {
            return null;
        }
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
        return Json.d.fromJson(data, typeClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject() {
        try {
            return (T) Json.d.fromJson(data, getClassType());
        } catch (NullPointerException e) {
            log.error("! class={}", tClass, e);
            return null;
        }
    }

    public <T> List<T> getList(Class<T> typeClass) {
        return Json.d.listFromJson(data, typeClass);
    }

    public <K, V> Map<K, V> getList(Class<K> keyTypeClass, Class<V> valueTypeClass) {
        return Json.d.mapFromJson(data, keyTypeClass, valueTypeClass);
    }

    public String toString() {
        return "type:" + type + ", class:" + tClass + ", data:" + data;
    }

    @Override
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
        Packet packet = (Packet) obj;
        if (type != packet.type) {
            return false;
        }
        if ((data == null && packet.data != null) || (data != null && !data.equals(packet.data))) {
            return false;
        }
        return (tClass != null || packet.tClass == null) && (tClass == null || tClass.equals(packet.tClass));
    }
}
