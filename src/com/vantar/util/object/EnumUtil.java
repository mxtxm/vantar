package com.vantar.util.object;

import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;


public class EnumUtil {

    private static final Logger log = LoggerFactory.getLogger(EnumUtil.class);
    private static final Pattern PATTERN_ENUM_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9]");


    public static List<String> getEnumValues(Enum<?>[] values) {
        return Stream.of(values)
            .map(Enum::toString)
            .collect(Collectors.toList());
    }

    public static List<String> getEnumValues(Enum<?>[] values, String... exclude) {
        List<String> items = new ArrayList<>(values.length);
        for (Enum<?> e : values) {
            String v = e.toString();
            boolean include = true;
            for (String item : exclude) {
                if (item.equals(v)) {
                    include = false;
                    break;
                }
            }
            if (include) {
                items.add(v);
            }
        }
        return items;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setEnumValue(Object object, Class<?> type, Field field, String value) throws IllegalAccessException {
        try {
            field.set(object, value == null ?
                null :
                Enum.valueOf((Class<? extends Enum>) type, PATTERN_ENUM_INVALID_CHARS.matcher(value).replaceAll("")));
        } catch (IllegalArgumentException e) {
            log.error("! {} > ({}){}.{}", value, type.getSimpleName(), object.getClass().getSimpleName(), field.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getEnumValue(Class<T> type, String value) {
        try {
            return value == null ?
                null :
                (T) Enum.valueOf((Class<? extends Enum>) type, PATTERN_ENUM_INVALID_CHARS.matcher(value).replaceAll(""));
        } catch (IllegalArgumentException e) {
            log.error("! {} > ({})", value, type.getSimpleName(), e);
            return null;
        }
    }
}
