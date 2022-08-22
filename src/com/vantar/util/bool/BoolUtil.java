package com.vantar.util.bool;

import com.vantar.util.string.StringUtil;


/**
 * Boolean value utilities
 */
public class BoolUtil {

    /**
     * Is nullable boolean value true
     * @param b value to check
     * @return b == null || b == false returns false
     */
    public static boolean isTrue(Boolean b) {
        return b != null && b;
    }

    /**
     * Is nullable boolean value false
     * @param b value to check
     * @return b == null || b == false returns true
     */
    public static boolean isFalse(Boolean b) {
        return b == null || !b;
    }

    /**
     * Is nullable boolean value true
     * @param b value to check
     * @return b == null || b == false returns false
     */
    public static boolean isTrue(Boolean b, boolean d) {
        return b == null ? d : b;
    }

    /**
     * Is nullable boolean value false
     * @param b value to check
     * @return b == null || b == false returns true
     */
    public static boolean isFalse(Boolean b, boolean d) {
        return !isTrue(b, d);
    }

    /**
     * Convert object to boolean
     * @param obj string or number or boolean to convert
     * @return null if obj == null or object is not convertible
     */
    public static Boolean toBoolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return StringUtil.toBoolean(obj.toString());
    }

    /**
     * Get value or default value if null
     * @param value
     * @param defaultValue
     * @return value or default value if null
     */
    public static boolean value(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }
}
