package com.vantar.util.number;

import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;
import java.util.Random;

/**
 * Number value utilities
 */
public class NumberUtil {

    /**
     * To binary string
     * @param data data to convert
     * @return binary string
     */
    public static String toBinary(byte data) {
        return String.format("%8s", Integer.toBinaryString(data & 0xFF)).replace(' ', '0');
    }

    /**
     * Round a number
     * @param value the value to be rounded
     * @param places decimal places to be rounded to
     * @return rounded value
     */
    public static double round(double value, int places) {
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Get a random number
     * @param min min value
     * @param max max value
     * @return a number between min and max
     */
    public static int random(int min, int max) {
        return new Random().nextInt((max - min) +1) + min;
    }

    /**
     * Get a random number
     * @param min min value
     * @param max max value
     * @return a number between min and max
     */
    public static double random(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }

    /**
     * Check if value is a valid number for database id
     * @param id to check
     * @return false if id == null or id < 1
     */
    public static boolean isIdValid(Long id) {
        return id != null && id > VantarParam.INVALID_ID;
    }

    /**
     * Check if value is not a valid number for database id
     * @param id to check
     * @return true if id == null or id < 1
     */
    public static boolean isIdInvalid(Long id) {
        return id == null || id <= VantarParam.INVALID_ID;
    }

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @param type a number type class to be converted to
     * @return null if obj == null or object is not convertible to a number
     */
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

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @return null if obj == null or object is not convertible to a number
     */
    public static Integer toInteger(Object obj) {
        return toNumber(obj, Integer.class);
    }

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @return null if obj == null or object is not convertible to a number
     */
    public static Long toLong(Object obj) {
        return toNumber(obj, Long.class);
    }

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @return null if obj == null or object is not convertible to a number
     */
    public static Double toDouble(Object obj) {
        return toNumber(obj, Double.class);
    }

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @return null if obj == null or object is not convertible to a number
     */
    public static Float toFloat(Object obj) {
        return toNumber(obj, Float.class);
    }

    /**
     * Convert object to number
     * @param obj string or number to convert
     * @return null if obj == null or object is not convertible to a number
     */
    public static Byte toByte(Object obj) {
        return toNumber(obj, Byte.class);
    }
}
