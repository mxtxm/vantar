package com.vantar.util.object;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Get dummy value
 */
public class DummyValue {

    public static Map<String, Object> getDummyDto(Dto dto) {
        return getDummyDto(dto, null);
    }

    private static Map<String, Object> getDummyDto(Dto dto, List<Class<?>> traversed) {
        if (traversed == null) {
            traversed = new ArrayList<>(20);
        }
        if (dto == null) {
            return new LinkedHashMap<>();
        }
        traversed.add(dto.getClass());
        Field[] fields = dto.getFields();
        Map<String, Object> dummy = new LinkedHashMap<>(fields.length, 1);
        for (Field field : fields) {
            if (field.isAnnotationPresent(NoStore.class)) {
                continue;
            }
            Class<?> type = field.getType();
            String col = field.getName();
            if (field.isAnnotationPresent(Localized.class)) {
                Map<String, String> v = new HashMap<>(2, 1);
                v.put("en", "some text");
                v.put("fa", "some text");
                dummy.put(col, v);
                continue;
            }
            dummy.put(
                col,
                getDummyObjectValue(
                    type,
                    traversed,
                    CollectionUtil.isCollectionOrMap(type) ? dto.getPropertyGenericTypes(field) : null
                )
            );
        }
        return dummy;
    }

    public static Object getDummyObjectValue(String type) {
        type = type.toLowerCase();
        Class<?> t = getType(type);
        if (t != null) {
            return getDummyObjectValue(t, null, null);
        }
        if ("file".equals(type)) {
            return "FILE/UPLOAD";
        }
        if (type.startsWith("list")) {
            Class<?> g = getType(StringUtil.remove(type, "list", "<", ">").trim());
            return g == null ? "[ ]" : getDummyObjectValue(List.class, null, new Class[] {g});
        }
        if (type.startsWith("set")) {
            Class<?> g = getType(StringUtil.remove(type, "set", "<", ">").trim());
            return g == null ? "[ ]" : getDummyObjectValue(Set.class, null, new Class[] {g});
        }
        if (type.startsWith("map")) {
            String[] parts = StringUtil.splitTrim(StringUtil.remove(type, "map", "<", ">"), ",");
            if (parts == null || parts.length != 2) {
                return "{ }";
            }
            Class<?> g1 = getType(parts[0].trim());
            Class<?> g2 = getType(parts[1].trim());
            return g1 == null || g2 == null ? "{ }" : getDummyObjectValue(Map.class, null, new Class<?>[] {g1, g2});
        }
        return null;
    }

    public static Object getDummyObjectValue(Class<?> type) {
        return getDummyObjectValue(type, null, null);
    }

    public static Object getDummyObjectValue(Class<?> type, Class<?>... g) {
        if (List.class.equals(type)) {
            return getDummyObjectValue(List.class, null, g);
        }
        if (Set.class.equals(type)) {
            return getDummyObjectValue(Set.class, null, g);
        }
        if (Map.class.equals(type)) {
            return getDummyObjectValue(Map.class, null, g);
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private static Object getDummyObjectValue(Class<?> type, List<Class<?>> traversed, Class<?>[] g) {
        if (type == String.class) {
            return "some text";
        }
        if (ClassUtil.isInstantiable(type, Number.class)) {
            return NumberUtil.toNumber(7, type);
        }
        if (type.isEnum()) {
            StringBuilder sb = new StringBuilder(200);
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
            for (Enum<?> x : enumType.getEnumConstants()) {
                sb.append(x.name()).append(" | ");
            }
            sb.setLength(sb.length() - 3);
            return sb.toString();
        }
        if (type == Boolean.class) {
            return true;
        }
        if (type == Character.class) {
            return "'C'";
        }
        if (type == Location.class) {
            return new Location(35.80252, 51.39939, 1740.0, "IR");
        }
        if (type == DateTime.class) {
            return new DateTime();
        }
        if (type == DateTimeRange.class) {
            return new DateTimeRange(new DateTime(), new DateTime().addDays(7));
        }
        if (ClassUtil.isInstantiable(type, Dto.class)) {
            if (traversed != null) {
                for (Class<?> c : traversed) {
                    if (c == type) {
                        return "{RECURSIVE " + type.getSimpleName() + "}";
                    }
                }
            }
            return getDummyDto((Dto) ClassUtil.getInstance(type), traversed);
        }
        if (ClassUtil.isInstantiable(type, Collection.class)) {
            List<Object> list = new ArrayList<>(1);
            if (g != null && g.length != 0) {
                list.add(getDummyObjectValue(g[0], traversed, null));
            }
            return list;
        }
        if (ClassUtil.isInstantiable(type, Map.class)) {
            Map<Object, Object> map = new LinkedHashMap<>(1, 1);
            if (g != null && g.length > 1) {
                map.put(
                    getDummyObjectValue(g[0], traversed, null),
                    getDummyObjectValue(g[1], traversed, null)
                );
            }
            return map;
        }
        return "{" + type.getSimpleName() + "}";
    }


    private static Class<?> getType(String type) {
        type = type.toLowerCase();
        switch (type) {
            case "long":
                return Long.class;
            case "integer":
                return Integer.class;
            case "double":
                return Double.class;
            case "float":
                return Float.class;
            case "number":
                return Number.class;
            case "string":
                return String.class;
            case "datetime":
                return DateTime.class;
            case "location":
                return Location.class;
            case "boolean":
                return BoolUtil.class;
            case "character":
                return Character.class;
            case "datetimerange":
                return DateTimeRange.class;
        }
        return null;
    }
}
