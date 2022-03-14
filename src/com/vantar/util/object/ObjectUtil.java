package com.vantar.util.object;

import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.*;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.*;
import static com.carrotsearch.sizeof.RamUsageEstimator.humanSizeOf;
import static com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll;


public class ObjectUtil {

    protected static final Logger log = LoggerFactory.getLogger(ObjectUtil.class);


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

    public static String toString(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Number || object instanceof Boolean || object instanceof DateTime
            || object instanceof Character) {
            return object.toString();
        } else if (object instanceof Throwable) {
            return throwableToString((Throwable) object);
        }
        try {
            return Json.d.toJsonPretty(object);
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
        StringBuilder sb = new StringBuilder().append(e.toString());
        if (e.getStackTrace() != null) {
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("\n").append(element.toString());
            }
        }
        return sb.toString();
    }

    public static String sizeOfReadable(Object object) {
        return humanSizeOf(object);
    }

    public static long sizeOf(Object... objects) {
        return sizeOfAll(Arrays.asList(objects));
    }

    public static boolean isJavaNative(Object object) {
        return isJavaNative(object.getClass());
    }

    public static boolean isJavaNative(Class<?> classType) {
        return StringUtil.contains(classType.getName(), "java.");
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

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object object, Class<T> classType) {
        if (object == null || object.getClass() == classType) {
            return (T) object;
        }

        if (ClassUtil.extendsClass(classType, Number.class)) {
            return NumberUtil.toNumber(object, classType);
        }
        if (classType == Character.class) {
            return (T) toCharacter(object);
        }
        if (classType == Boolean.class) {
            return (T) toBoolean(object);
        }
        if (classType == DateTime.class) {
            try {
                return (T) new DateTime(object.toString());
            } catch (DateTimeException e) {
                return null;
            }
        }
        if (classType == Location.class) {
            return (T) new Location(object.toString());
        }
        if (classType == String.class) {
            return (T) object.toString();
        }

        return (T) object;
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

            if (type.equals(String.class)) {
                String s = value.toString();
                field.set(obj, StringUtil.isEmpty(s) ? null : s);

            } else if (ClassUtil.extendsClass(type, Number.class)) {
                field.set(obj, NumberUtil.toNumber(value, type));

            } else if (type.equals(Boolean.class)) {
                field.set(obj, ObjectUtil.toBoolean(value));

            } else if (type.equals(Character.class)) {
                field.set(obj, ObjectUtil.toCharacter(value));

            } else if (type.equals(DateTime.class)) {
                field.set(obj, new DateTime(value.toString()));

            } else if (type.equals(Location.class)) {
                field.set(obj, new Location(value.toString()));

            } else if (type.equals(List.class) || type.equals(ArrayList.class)) {
                field.set(obj, Json.d.listFromJson(value.toString(), ClassUtil.getGenericTypes(field)[0]));

            } else if (type.equals(Set.class)) {
                List<?> list = Json.d.listFromJson(value.toString(), ClassUtil.getGenericTypes(field)[0]);
                field.set(obj, list == null ? null : new HashSet<>(list));

            } else if (type.equals(Map.class)) {
                Class<?>[] types = ClassUtil.getGenericTypes(field);
                field.set(obj, Json.d.mapFromJson(value.toString(), types[0], types[1]));
            } else if (type.isEnum()) {
                field.set(obj, EnumUtil.getEnumValue(value.toString(), type));
            }
            return true;
        } catch (IllegalAccessException | IllegalArgumentException | DateTimeException e) {
            log.error("! set({} < {})", name.trim(), value, e);
        }

        return false;
    }

    @SuppressWarnings({"unchecked"})
    public static <T> List<T> toList(Object object, Class<T> genericType) {
        if (object == null) {
            return null;
        }
        if (object instanceof List) {
            return (List<T>) object;
        }

        Class<?> type = object.getClass();

        String[] strings;
        if (type.isArray()) {
            strings = Json.d.fromJson(Json.d.toJson(object), String[].class);
        } else {
            if (!type.equals(String.class)) {
                object = object.toString();
            }
            String value = ((String) object).trim();
            if (ClassUtil.extendsClass(type, Number.class)) {
                value = Persian.Number.toLatin(value);
            }
            if (value.isEmpty()) {
                return new ArrayList<>();
            }

            if (value.startsWith("[") && value.endsWith("]")) {
                List<T> list = Json.d.listFromJson(value, genericType);
                return list == null ? new ArrayList<>() : list;
            }
            strings = StringUtil.split(value, VantarParam.SEPARATOR_COMMON);
        }
        if (strings == null) {
            return new ArrayList<>();
        }

        List<T> values = new ArrayList<>();

        if (ClassUtil.extendsClass(genericType, Number.class)) {
            for (String n : strings) {
                T x = NumberUtil.toNumber(n, genericType);
                if (x != null) {
                    values.add(x);
                }
            }
        } else if (genericType.equals(String.class)) {
            for (String s : strings) {
                if (s != null) {
                    values.add((T) s);
                }
            }
        } else if (genericType.equals(DateTime.class)) {
            for (String s : strings) {
                if (s != null) {
                    try {
                        values.add((T) new DateTime(s));
                    } catch (DateTimeException ignore) {

                    }
                }
            }
        } else if (genericType.equals(Location.class)) {
            for (String s : strings) {
                if (s != null) {
                    Location l = new Location(s);
                    if (l.isValid()) {
                        values.add((T) l);
                    }
                }
            }
        } else if (genericType.isEnum()) {
            for (String s : strings) {
                if (s != null) {
                    try {
                        T e = EnumUtil.getEnumValue(s, genericType);
                        if (e != null) {
                            values.add(e);
                        }
                    } catch (IllegalArgumentException ignore) {

                    }
                }
            }
        } else {
            return new ArrayList<>();
        }

        return values;
    }

    // todo complete this
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Object object, Class<K> k, Class<V> v) {
        if (object instanceof String) {
            return Json.d.mapFromJson((String) object, k, v);
        } else if (object instanceof Map) {
            return (Map<K, V>) object;
        }
        return null;
    }

    public static Object callStaticMethod(String packageClassMethod, Object... params) throws Throwable {
        String[] parts = StringUtil.split(packageClassMethod, '.');

        Class<?>[] types = new Class[params.length];
        for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
            types[i] = params[i].getClass();
        }
        try {
            Class<?> tClass = Class.forName(CollectionUtil.join(parts, '.', parts.length - 1));
            Method method = tClass.getMethod(parts[parts.length - 1], types);
            return method.invoke(null, params);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            log.error("! {}", packageClassMethod, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public static Object callStaticMethod(Class<?> tClass, String methodName, Object... params) throws Throwable {
        Class<?>[] types = new Class[params.length];
        for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
            types[i] = params[i].getClass();
        }
        try {
            Method method = tClass.getMethod(methodName, types);
            return method.invoke(null, params);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("! {}.{}", tClass.getSimpleName(), methodName, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
