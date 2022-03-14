package com.vantar.database.common;

import com.vantar.util.string.StringUtil;
import java.util.Map;


public class DbUtil {

    public static String getKv(Object v, String lang) {
        if (v == null) {
            return null;
        }
        if (v instanceof Map) {
            if (StringUtil.isNotEmpty(lang)) {
                v = ((Map<?, ?>) v).get(lang);
                return v == null ? null : v.toString();
            }

            StringBuilder sb = new StringBuilder();
            for (Object item : ((Map<?, ?>) v).values()) {
                sb.append(item.toString()).append(" - ");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 3);
                return sb.toString();
            } else {
                return null;
            }
        }
        return v.toString();
    }

}
