package com.vantar.util.object;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.exception.DateTimeException;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.*;
import com.vantar.web.Params;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.*;
import static com.carrotsearch.sizeof.RamUsageEstimator.humanSizeOf;
import static com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll;

/**
 * Object utilities
 */
public class ObjectUtil {

    protected static final Logger log = LoggerFactory.getLogger(ObjectUtil.class);

    /**
     * Check if object is empty
     * @param obj to be checked
     * @return
     * (true if obj == null)
     * (true if obj == '' or "" or "   ")
     * (true if obj == [])
     * (true if obj == {})
     * (true if obj == null)
     * (true if obj hast isEmpty() and obj.isEmpty() == true)
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if (obj instanceof String) {
            return StringUtil.isEmpty((String) obj);
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }
        try {
            Method method = obj.getClass().getMethod("isEmpty");
            return (boolean) method.invoke(obj);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
            return false;
        }
    }

    /**
     * Check if object is not empty
     * @param obj to be checked
     * @return
     * (false if obj == null)
     * (false if obj == '' or "" or "   ")
     * (false if obj == [])
     * (false if obj == {})
     * (false if obj == null)
     * (false if obj hast isEmpty() and obj.isEmpty() == true)
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * Convert object to string for storage
     * @param object to convert
     * @return
     * (null if object == null)
     * (string value if type == string or number)
     * (exception message and stacktrace if type == throwable)
     * (json of properties for other types)
     */
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
            String s = Json.d.toJsonPretty(object);
            return s == null ? object.toString() : s;
        } catch (Exception e) {
            return object.toString();
        }
    }

    /**
     * Convert object to string for view
     * @param object to convert
     * @return
     * (null if object == "null")
     * (string value if type == string or number)
     * (exception message and stacktrace if type == throwable)
     * (json of properties for other types)
     */
    public static String toStringViewable(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Number || object instanceof Boolean || object instanceof DateTime
            || object instanceof Character) {
            return object.toString();
        } else if (object instanceof Throwable) {
            return throwableToString((Throwable) object);
        }
        try {
            String s = Json.d.toJsonPretty(object);
            return s == null ? object.toString() : s;
        } catch (Exception e) {
            return object.toString();
        }
    }

    /**
     * Convert throwable to string
     * @param throwable to convert
     * @return exception message and stacktrace if type == throwable
     */
    public static String throwableToString(Throwable throwable) {
        if (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        if (throwable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder().append(throwable.toString());
        if (throwable.getStackTrace() != null) {
            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append("\n").append(element.toString());
            }
        }
        return sb.toString();
    }

    /**
     * Get size of object
     * @param object to measure
     * @return size as human readable
     */
    public static String sizeOfReadable(Object object) {
        return humanSizeOf(object);
    }

    /**
     * Get size of object
     * @param objects to measure
     * @return bytes
     */
    public static long sizeOf(Object... objects) {
        return sizeOfAll(Arrays.asList(objects));
    }

    /**
     * Is object from java. package
     * @param object to check
     * @return true == object is from java package
     */
    public static boolean isJavaNative(Object object) {
        return isJavaNative(object.getClass());
    }

    /**
     * Is class from java. package
     * @param theClass to check
     * @return true == theClass is from java package
     */
    public static boolean isJavaNative(Class<?> theClass) {
        return StringUtil.contains(theClass.getName(), "java.");
    }

    /**
     * Call method on an object
     * @param object the object
     * @param methodName the method name
     * @param params method params
     * @return method return value
     * @throws Throwable
     */
    public static Object callInstanceMethod(Object object, String methodName, Object... params) throws Throwable {
        Class<?>[] types = new Class[params.length];
        for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
            types[i] = params[i].getClass();
        }
        try {
            Method method = object.getClass().getMethod(methodName, types);
            return method.invoke(object, params);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error(" !! {}.{}\n", object.getClass().getSimpleName(), methodName, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


    /**
     * Get a dictionary of the properties and values of an object
     * @param object the object
     * @return {propertyName: value,}
     */
    public static Map<String, Object> getPropertyValues(Object object) {
        Field[] fields = object.getClass().getFields();
        Map<String, Object> params = new HashMap<>(fields.length, 1);
        try {
            for (Field field : fields) {
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

    /**
     * Set value of an object property
     * @param object the object
     * @param propertyName object's property name
     * @param value value to be set to the property
     * @return true if value set successfully
     */
    public static boolean setPropertyValueIgnoreNull(Object object, String propertyName, Object value) {
        return setPropertyValueX(object, propertyName, value, false);
    }

    public static boolean setPropertyValue(Object object, String propertyName, Object value) {
        return setPropertyValueX(object, propertyName, value, true);
    }

    private static boolean setPropertyValueX(Object object, String propertyName, Object value, boolean setNull) {
        if (object == null) {
            return false;
        }
        Field field;
        try {
            field = object.getClass().getField(propertyName);
        } catch (NoSuchFieldException e) {
            log.error(" !! {}: {}={}\n", object.getClass().getSimpleName(), propertyName, value, e);
            return false;
        }

        try {
            if (value == null) {
                if (setNull) {
                    field.set(object, null);
                }
                return true;
            }

            Class<?> type = field.getType();

            if (ClassUtil.isInstantiable(type, List.class)) {
                field.set(object, convert(value, List.class, ClassUtil.getGenericTypes(field)));
            } else if (type.equals(Set.class)) {
                field.set(object, convert(value, Set.class, ClassUtil.getGenericTypes(field)));
            } else if (type.equals(Map.class)) {
                field.set(object, convert(value, Map.class, ClassUtil.getGenericTypes(field)));
            } else {
                field.set(object, convert(value, type));
            }

            return true;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            log.error(" !! {}: {}={}\n", object.getClass().getSimpleName(), propertyName, value, e);
        }
        return false;
    }

    /**
     * Convert object to the given type
     * @param object object to be converted
     * @param classType type to convert
     * @return null if object == null or not convertible
     */
    public static <T> T convert(Object object, Class<T> classType) {
        return convert(object, classType, null);
    }

    /**
     * Convert object to the given type
     * @param object object to be converted
     * @param classType type to convert
     * @return null if object == null or not convertible
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object object, Class<T> classType, Class<?>[] generics) {
        if (object == null || object.getClass() == classType) {
            return (T) object;
        }

        if (ClassUtil.isInstantiable(classType, Number.class)) {
            return NumberUtil.toNumber(object, classType);
        }
        if (classType == String.class) {
            return (T) toString(object);
        }
        if (ClassUtil.isInstantiable(classType, Dto.class)) {
            Dto dto = DtoDictionary.get(classType).getDtoInstance();
            if (object instanceof String) {
                dto.set((String) object, Dto.Action.UPDATE_FEW_COLS);
                return (T) dto;
            }
            if (object instanceof Params) {
                dto.set((Params) object, Dto.Action.UPDATE_FEW_COLS);
                return (T) dto;
            }
            if (object instanceof Map) {
                dto.set((Map<String, Object>) object, Dto.Action.SET);
                return (T) dto;
            }
            return null;
        }
        if (ClassUtil.isInstantiable(classType, List.class)) {
            Class<?> g = generics == null || generics.length == 0 ? String.class : generics[0];
            return (T) CollectionUtil.toList(object, g);
        }
        if (ClassUtil.isInstantiable(classType, Set.class)) {
            Class<?> g = generics == null || generics.length == 0 ? String.class : generics[0];
            return (T) CollectionUtil.toSet(object, g);
        }
        if (ClassUtil.isInstantiable(classType, Map.class)) {
            Class<?> g1;
            Class<?> g2;
            if (generics == null || generics.length < 2) {
                g1 = String.class;
                g2 = String.class;
            } else {
                g1 = generics[0];
                g2 = generics[1];
            }
            return (T) CollectionUtil.toMap(object, g1, g2);
        }
        if (classType == DateTime.class) {
            try {
                return (T) new DateTime(object.toString());
            } catch (DateTimeException e) {
                return null;
            }
        }
        if (classType.isEnum()) {
            return EnumUtil.getEnumValue(object.toString(), classType);
        }
        if (classType == Location.class) {
            return (T) Location.toLocation(object);
        }
        if (classType == Boolean.class) {
            return (T) BoolUtil.toBoolean(object);
        }
        if (classType == Character.class) {
            return (T) StringUtil.toCharacter(object);
        }
        if (classType.isArray()) {
            Class<?> g = generics == null || generics.length != 1 ? String.class : generics[0];
            return (T) CollectionUtil.toArray(object, g);
        }

        return (T) object;
    }
}