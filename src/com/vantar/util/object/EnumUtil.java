package com.vantar.util.object;

import com.vantar.service.log.ServiceLog;
import org.slf4j.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

/**
 * Enum utilities
 */
public class EnumUtil {

    private static final Logger log = LoggerFactory.getLogger(EnumUtil.class);
    private static final Pattern PATTERN_ENUM_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    /**
     * Get a list of enum values as string
     * @param values enum values
     * @return string list
     */
    public static List<String> getEnumValues(Enum<?>[] values) {
        return Stream.of(values)
            .map(Enum::toString)
            .collect(Collectors.toList());
    }

    public static String[] getEnumValues(Class<? extends Enum<?>> enumType) {
        String[] values = new String[enumType.getEnumConstants().length];
        int i = 0;
        for (Enum<?> x : enumType.getEnumConstants()) {
            values[i++] = x.toString();
        }
        return values;
    }

    /**
     * Get a list of enum values as string
     * @param values enum values
     * @param exclude excluded values
     * @return string list
     */
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

    /**
     * Set enum value of a string(if it is an item of enum class) to a field of an object
     * @param value value to be set
     * @param type enum class type
     * @param object object to set it's field
     * @param field field to set it's value
     * @throws IllegalAccessException
     */
    public static void setEnumValue(String value, Class<?> type, Object object, Field field) throws IllegalAccessException {
        try {
            field.set(object, getEnumValue(value, type));
        } catch (IllegalArgumentException e) {
            log.error(" !! {} > ({}){}.{}\n", value, type.getSimpleName(), object.getClass().getSimpleName(), field.getName(), e);
        }
    }

    /**
     * Get enum value from a string(if it is an item of enum class)
     * @param value string value
     * @param type enum class type
     * @return enum value or null if string can not be mapped to enum item
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getEnumValue(String value, Class<T> type) {
        try {
            return value == null ?
                null :
                (T) Enum.valueOf((Class<? extends Enum>) type, PATTERN_ENUM_INVALID_CHARS.matcher(value).replaceAll(""));
        } catch (IllegalArgumentException e) {
            log.error(" !! {} > ({})\n", value, type.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Get enum value from a string(if it is an item of enum class)
     * @param value string value
     * @param type enum class type
     * @return enum value or null if string can not be mapped to enum item
     * @throws IllegalAccessException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getEnumValueThrow(String value, Class<T> type) throws IllegalArgumentException {
        return value == null ?
            null :
            (T) Enum.valueOf((Class<? extends Enum>) type, PATTERN_ENUM_INVALID_CHARS.matcher(value).replaceAll(""));
    }

    public static List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            return new ArrayList<>(1);
        }

        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(packageName.replace('.', '/'));
        } catch (IOException e) {
            ServiceLog.log.error(" !! package({})\n", packageName, e);
            return new ArrayList<>(1);
        }

        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            dirs.add(new File(resources.nextElement().getFile()));
        }

        List<Class<?>> classes = new ArrayList<>(40);
        for (File directory : dirs) {
            classes.addAll(ClassUtil.findClasses(directory, packageName, Enum.class));
        }
        return classes;
    }
}
