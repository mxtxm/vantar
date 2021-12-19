package com.vantar.util.number;

import com.vantar.common.VantarParam;
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
}
