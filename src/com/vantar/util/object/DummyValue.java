package com.vantar.util.object;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.number.NumberUtil;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Set dummy value
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
        Class<?> t;
        type = type.toLowerCase();
        switch (type) {
            case "long":
                t = Long.class;
                break;
            case "integer":
                t = Integer.class;
                break;
            case "double":
                t = Double.class;
                break;
            case "float":
                t = Float.class;
                break;
            case "number":
                t = Number.class;
                break;
            case "string":
                t = String.class;
                break;
            case "datetime":
                t = DateTime.class;
                break;
            case "location":
                t = Location.class;
                break;
            case "boolean":
                t = BoolUtil.class;
                break;
            case "character":
                t = Character.class;
                break;
            case "datetimerange":
                t = DateTimeRange.class;
                break;
            default:
                if (type.startsWith("list")) {
                    return "[ ]";
                }
                if (type.startsWith("set")) {
                    return "[ ]";
                }
                return "{ }";
        }
        return getDummyObjectValue(t, null, null);
    }

    public static Object getDummyObjectValue(Class<?> type) {
        return getDummyObjectValue(type, null, null);
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
}
