package com.vantar.util.number;

import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;
import java.util.Random;


public class NumberUtil {

    public static String toBinary(byte data) {
        return String.format("%8s", Integer.toBinaryString(data & 0xFF)).replace(' ', '0');
    }

    public static double round(double value, int places) {
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static int random(int min, int max) {
        return new Random().nextInt((max - min) +1) + min;
    }

    public static double random(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }

    public static boolean isIdValid(Long id) {
        return id != null && id > VantarParam.INVALID_ID;
    }

    public static boolean isIdInvalid(Long id) {
        return id == null || id <= VantarParam.INVALID_ID;
    }

    @SuppressWarnings("unchecked")
    public static <N> N toNumber(Object obj, Class<N> type) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Number) {
            if (type.equals(Integer.class)) {
                return (N) new Integer(((Number) obj).intValue());
            } else if (type.equals(Long.class)) {
                return (N) new Long(((Number) obj).longValue());
            } else if (type.equals(Double.class)) {
                return (N) new Double(((Number) obj).doubleValue());
            } else if (type.equals(Float.class)) {
                return (N) new Float(((Number) obj).floatValue());
            } else if (type.equals(Byte.class)) {
                return (N) new Byte(((Number) obj).byteValue());
            } else {
                return null;
            }
        }

        if (type.equals(Integer.class)) {
            return (N) StringUtil.toInteger(obj.toString());
        } else if (type.equals(Long.class)) {
            return (N) StringUtil.toLong(obj.toString());
        } else if (type.equals(Double.class)) {
            return (N) StringUtil.toDouble(obj.toString());
        } else if (type.equals(Float.class)) {
            return (N) StringUtil.toFloat(obj.toString());
        } else if (type.equals(Byte.class)) {
            return (N) StringUtil.toByte(obj.toString());
        }
        return null;
    }

    public static Integer toInteger(Object obj) {
        return toNumber(obj, Integer.class);
    }

    public static Long toLong(Object obj) {
        return toNumber(obj, Long.class);
    }

    public static Double toDouble(Object obj) {
        return toNumber(obj, Double.class);
    }

    public static Float toFloat(Object obj) {
        return toNumber(obj, Float.class);
    }

    public static Byte toByte(Object obj) {
        return toNumber(obj, Byte.class);
    }
}
