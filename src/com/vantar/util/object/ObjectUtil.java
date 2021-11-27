package com.vantar.util.object;

import com.google.gson.JsonSyntaxException;
import com.vantar.common.VantarParam;
import com.vantar.exception.DateTimeException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.string.*;
import org.slf4j.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import static com.carrotsearch.sizeof.RamUsageEstimator.humanSizeOf;
import static com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll;


public class ObjectUtil {

    private static final Logger log = LoggerFactory.getLogger(ObjectUtil.class);


    public static Map<String, Object> getPropertyValues(Object object) {
        Map<String, Object> params = new HashMap<>();
        try {
            for (Field field : object.getClass().getFields()) {
                if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                Object value = field.get(object);
                if (value != null) {
                    params.put(field.getName(), value);
                }
            }
        } catch (IllegalAccessException ignore) {

        }
        return params;
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static <T> T getInstance(String className) {
        try {
            return (T) Class.forName(className).getConstructor().newInstance(new Object[] {});
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return null;
        }
    }

    public static <T> T getInstance(Class<T> tClass) {
        try {
            return (T) tClass.getConstructor().newInstance(new Object[] {});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return null;
        }
    }

    public static List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            return new ArrayList<>();
        }

        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(packageName.replace('.', '/'));
        } catch (IOException e) {
            log.error("! package({})", packageName, e);
            return new ArrayList<>();
        }

        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            dirs.add(new File(resources.nextElement().getFile()));
        }

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().contains(".")) {
                    continue;
                }
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                } catch (ClassNotFoundException e) {
                    log.error("! package({}) class not found", packageName, e);
                }
            }
        }

        return classes;
    }

    public static String toString(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Number || object instanceof Boolean || object instanceof DateTime || object instanceof Character) {
            return object.toString();
        }
        try {
            return Json.toJsonPretty(object);
        } catch (Exception e) {
            log.error("! toJson error {}", object.getClass());
            return "{}";
        }
    }

    public static String throwableToString(Throwable e) {
        if (e.getCause() != null) {
            e = e.getCause();
        }

        if (e == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(1000).append(e.toString());
        if (e.getStackTrace() != null) {
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("\n").append(element.toString());
            }
        }
        return sb.toString();
    }

    public static boolean isIdValid(Long id) {
        return id != null && id > VantarParam.INVALID_ID;
    }

    public static boolean isIdInvalid(Long id) {
        return id == null || id <= VantarParam.INVALID_ID;
    }

    public static Object convert(Object object, Type classType) {
        if (object.getClass() == classType || !(classType instanceof Class<?>)) {
            return object;
        }
        if (classType == Long.class) {
            return toLong(object);
        }
        if (classType == Integer.class) {
            return toInteger(object);
        }
        if (classType == Double.class) {
            return toDouble(object);
        }
        if (classType == Character.class) {
            return toCharacter(object);
        }
        if (classType == Boolean.class) {
            return toBoolean(object);
        }
        if (classType == Float.class) {
            return toFloat(object);
        }
        if (classType == DateTime.class) {
            try {
                return new DateTime(object.toString());
            } catch (DateTimeException e) {
                return null;
            }
        }
        if (classType == String.class) {
            return object.toString();
        }

        return object;
    }

    public static Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return StringUtil.toInteger(obj.toString());
    }

    public static Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return StringUtil.toLong(obj.toString());
    }

    public static Double toDouble(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return StringUtil.toDouble(obj.toString());
    }

    public static Float toFloat(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        return StringUtil.toFloat(obj.toString());
    }

    public static Boolean toBoolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return StringUtil.toBoolean(obj.toString());
    }

    public static Character toCharacter(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Character) {
            return (Character) obj;
        }
        return StringUtil.toCharacter(obj.toString());
    }

    public static boolean setPropertyValue(Object obj, String name, Object value) {
        if (obj == null) {
            return false;
        }
        Field field;
        try {
            field = obj.getClass().getField(name);
        } catch (NoSuchFieldException e) {
            return false;
        }

        try {
            if (value == null) {
                field.set(obj, null);
                return true;
            }

            Class<?> type = field.getType();
            if (value instanceof Number && !value.getClass().equals(field.getType())) {
                if (type.equals(Integer.class)) {
                    field.set(obj, ((Number) value).intValue());

                } else if (type.equals(Long.class)) {
                    field.set(obj, ((Number) value).longValue());

                } else if (type.equals(Double.class)) {
                    field.set(obj, ((Number) value).doubleValue());
                }
                return true;
            }

            if (!(value instanceof String)) {
                field.set(obj, StringUtil.isEmpty(value.toString()) ? null : value);
                return true;
            }

            if (type.equals(String.class)) {
                field.set(obj, value);

            } else if (type.equals(Integer.class)) {
                field.set(obj, StringUtil.toInteger((String) value));

            } else if (type.equals(Long.class)) {
                field.set(obj, StringUtil.toLong((String) value));

            } else if (type.equals(Double.class)) {
                field.set(obj, StringUtil.toDouble((String) value));

            } else if (type.equals(Boolean.class)) {
                field.set(obj, StringUtil.toBoolean((String) value));

            } else if (type.equals(Character.class)) {
                field.set(obj, StringUtil.toCharacter((String) value));

            } else if (type.equals(DateTime.class)) {
                field.set(obj, ((String) value).equalsIgnoreCase("now") ? new DateTime() : new DateTime((String) value));

            } else if (type.equals(List.class) || type.equals(ArrayList.class)) {
                Class<?> genericClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                field.set(obj, Json.listFromJson(value.toString(), genericClass));

            } else if (type.equals(Set.class)) {
                Class<?> genericClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                List<?> list = Json.listFromJson(value.toString(), genericClass);
                field.set(obj, list == null ? null : new HashSet<>(list));

            } else if (type.equals(Map.class)) {
                Type[] types = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                field.set(obj, Json.mapFromJson(value.toString(), (Class<?>) types[0], (Class<?>) types[1]));
            }

            return true;
        } catch (IllegalAccessException | IllegalArgumentException | DateTimeException e) {
            log.error("! set({} < {})", name.trim(), value, e);
        }

        return false;
    }

    public static String sizeOfReadable(Object object) {
        return humanSizeOf(object);
    }

    public static long sizeOf(Object... objects) {
        return sizeOfAll(Arrays.asList(objects));
    }

    public static boolean implementsInterface(Class<?> type, Class<?> i) {
        if (type == i) {
            return true;
        }

        for (Class<?> c : type.getInterfaces()) {
            if (c == i) {
                return true;
            }
        }

        Class<?> c = type.getSuperclass();
        while (c != null) {
            for (Class<?> t : c.getInterfaces()) {
                if (t == i) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }

        return false;
    }

    public static boolean extendsClass(Class<?> type, Class<?> c) {
        if (type == c) {
            return true;
        }
        return c.isAssignableFrom(type);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> List<T> getList(Object object, Class<T> genericType) {
        if (object == null) {
            return null;
        }
        if (object instanceof List) {
            return (List<T>) object;
        }

        Class<?> type = object.getClass();

        String[] strings;
        if (object.getClass().isArray()) {
            strings = Json.fromJson(Json.toJson(object), String[].class);
        } else {
            if (!(object instanceof String)) {
                object = object.toString();
            }
            if (type.equals(Integer.class) || type.equals(Long.class) || type.equals(Double.class)
                || type.equals(Float.class) || type.equals(Number.class)) {

                object = Persian.Number.toLatin((String) object);
            }

            String value = (String) object;
            if (value.isEmpty()) {
                return new ArrayList<>();
            }

            if (value.startsWith("[") && value.endsWith("]")) {
                try {
                    return Json.listFromJson(value, genericType);
                } catch (JsonSyntaxException e) {
                    log.error("!", e);
                    return new ArrayList<>();
                }
            }
            strings = StringUtil.split(value, VantarParam.SEPARATOR_COMMON);
        }

        if (strings == null) {
            return null;
        }

        List<T> values = new ArrayList<>();

        if (genericType.equals(Integer.class)) {
            for (String n : strings) {
                T x = (T) ObjectUtil.toInteger(n);
                if (x == null) {
                    return null;
                }
                values.add(x);
            }

        } else if (genericType.equals(Long.class)) {
            for (String n : strings) {
                T x = (T) ObjectUtil.toLong(n);
                if (x == null) {
                    return null;
                }
                values.add(x);
            }

        } else if (genericType.equals(Double.class)) {
            for (String n : strings) {
                T x = (T) ObjectUtil.toDouble(n);
                if (x == null) {
                    return null;
                }
                values.add(x);
            }

        } else if (genericType.equals(Float.class)) {
            for (String n : strings) {
                T x = (T) ObjectUtil.toFloat(n);
                if (x == null) {
                    return null;
                }
                values.add(x);
            }

        } else if (genericType.equals(String.class)) {
            for (String s : strings) {
                if (s == null) {
                    return null;
                }
                values.add((T) s);
            }

        } else if (genericType.equals(DateTime.class)) {
            for (String s : strings) {
                if (s == null) {
                    return null;
                }
                try {
                    values.add((T) new DateTime(s));
                } catch (DateTimeException e) {
                    return null;
                }
            }

        } else if (genericType.isEnum()) {

            for (String s : strings) {
                if (s == null) {
                    return null;
                }
                try {
                    values.add(EnumUtil.getEnumValue(genericType, s));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

        } else {
            return null;
        }

        return values;
    }

    public static boolean isJavaNative(Object object) {
        return isJavaNative(object.getClass());
    }

    public static boolean isJavaNative(Class<?> classType) {
        return StringUtil.contains(classType.getName(), "java.");
    }

    public static boolean equals(Type type1, Type type2) {
        return toClass(type1) == toClass(type2);
    }

    public static Class<?> toClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type typeX = ((ParameterizedType) type).getRawType();
            return typeX instanceof Class<?> ? (Class<?>) typeX : toClass(typeX);
        }
        if (type instanceof GenericArrayType) {
            Type typeX = ((GenericArrayType) type).getGenericComponentType();
            return typeX instanceof Class<?> ? (Class<?>) typeX : toClass(typeX);
        }
        if (type instanceof TypeVariable<?>) {
            return ((TypeVariable<?>) type).getGenericDeclaration().getClass();
        }

        String typeName = type.getTypeName();
        if (StringUtil.contains(typeName, "Map")) {
            return Map.class;
        }
        if (StringUtil.contains(typeName, "List")) {
            return List.class;
        }
        if (StringUtil.contains(typeName, "Set")) {
            return Set.class;
        }
        return null;
    }

    public static Class<?>[] getFieldGenericTypes(Field field) {
        if (field == null) {
            return null;
        }
        Type t = field.getGenericType();
        if (!(t instanceof ParameterizedType)) {
            log.warn("! field({}) does not have generics.", field.getName());
            return null;
        }

        Type[] types = ((ParameterizedType) t).getActualTypeArguments();
        Class<?>[] classes = new Class[types.length];
        for (int i = 0, typesLength = types.length; i < typesLength; i++) {
            classes[i] = toClass(types[i]);
        }
        return classes;
    }
}
