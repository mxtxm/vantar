package com.vantar.util.bool;


public class BoolUtil {

    public static boolean isTrue(Boolean b) {
        return b != null && b;
    }

    public static boolean isFalse(Boolean b) {
        return b == null || !b;
    }
}
