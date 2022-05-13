package com.vantar.util.collection;

import com.vantar.util.string.StringUtil;
import java.util.Map;


public class ExtraUtils {

    public static String join(Object[] array, char glue, int limit) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 20);
        for (Object item : array) {
            sb.append(item).append(glue);
            if (--limit == 0) {
                break;
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String getStringFromMap(Map<String, String> data, String... locales) {
        if (data == null || locales == null) {
            return "";
        }
        for (String locale : locales) {
            if (StringUtil.isEmpty(locale)) {
                continue;
            }
            String value = data.get(locale);
            if (StringUtil.isNotEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
