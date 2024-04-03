package com.vantar.util.collection;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.Dto;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import java.lang.reflect.Array;
import java.util.*;


public class CollectionUtil {

    public static final int DEFAULT_INIT_SIZE = 21;

    /**
     * Is type a Map
     * @param typeClass the type
     * @return true it is
     */
    public static boolean isMap(Class<?> typeClass) {
        return typeClass != null && Map.class.isAssignableFrom(typeClass);
    }

    /**
     * Is object a Map
     * @param object the object
     * @return true it is
     */
    public static boolean isMap(Object object) {
        return object instanceof Map;
    }

    /**
     * Is type a Collection
     * @param typeClass the type
     * @return true it is
     */
    public static boolean isCollection(Class<?> typeClass) {
        return typeClass != null && Collection.class.isAssignableFrom(typeClass);
    }

    /**
     * Is object a Collection
     * @param object the object
     * @return true it is
     */
    public static boolean isCollection(Object object) {
        return object instanceof Collection;
    }

    /**
     * Is type an array
     * @param typeClass the type
     * @return true it is
     */
    public static boolean isArray(Class<?> typeClass) {
        return typeClass != null && typeClass.isArray();
    }

    /**
     * Is object an array
     * @param object the object
     * @return true it is
     */
    public static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

    /**
     * Is type a map or a collection
     * @param typeClass the type
     * @return true it is
     */
    public static boolean isCollectionOrMap(Class<?> typeClass) {
        return isMap(typeClass) || isCollection(typeClass);
    }

    /**
     * Is object a map or a collection
     * @param object the object
     * @return true it is
     */
    public static boolean isCollectionOrMap(Object object) {
        return isMap(object) || isCollection(object);
    }

    /**
     * Is type a map or a collection or an array
     * @param typeClass the type
     * @return true it is
     */
    public static boolean isCollectionOrMapOrArray(Class<?> typeClass) {
        return isMap(typeClass) || isCollection(typeClass) || isArray(typeClass);
    }

    /**
     * Is object an array
     * Is object a map or a collection or an array
     * @return true it is
     */
    public static boolean isCollectionOrMapOrArray(Object object) {
        return isMap(object) || isCollection(object) || isArray(object);
    }

    /**
     * Stick items together
     * @param iterable items
     * @param glue stick
     * @return glued items
     */
    public static String join(Iterable<?> iterable, String glue) {
        if (iterable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : iterable) {
            sb.append(item).append(glue);
        }
        int l = glue.length();
        if (sb.length() >= l) {
            sb.setLength(sb.length() - l);
        }
        return sb.toString();
    }

    /**
     * Stick items together
     * @param iterable items
     * @param glue stick
     * @return glued items
     */
    public static String join(Collection<?> iterable, char glue) {
        if (iterable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(iterable.size() * 20);
        for (Object item : iterable) {
            sb.append(item).append(glue);
        }
        if (sb.length() >= 1) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Stick items together
     * @param map items
     * @param glueEntry stick between key/value
     * @param glueItem stick between items
     * @return glued items
     */
    public static String join(Map<?, ?> map, String glueEntry, String glueItem) {
        if (map == null || map.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(map.size() * 20);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(glueEntry).append(entry.getValue()).append(glueItem);
        }
        int l = glueItem.length();
        if (sb.length() >= l) {
            sb.setLength(sb.length() - l);
        }
        return sb.toString();
    }

    /**
     * Stick items together
     * @param map items
     * @param glueEntry stick between key/value
     * @param glueItem stick between items
     * @return glued items
     */
    public static String join(Map<?, ?> map, char glueEntry, char glueItem) {
        if (map == null || map.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(map.size() * 20);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(glueEntry).append(entry.getValue()).append(glueItem);
        }
        if (sb.length() >= 1) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Stick items together
     * @param array array items
     * @param glue stick
     * @return glued items
     */
    public static String join(Object array, String glue) {
        if (array == null || !array.getClass().isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(200);
        for (int i = 0, length = Array.getLength(array); i < length; ++i) {
            sb.append(ObjectUtil.toString(Array.get(array, i))).append(glue);
        }
        int l = glue.length();
        if (sb.length() >= l) {
            sb.setLength(sb.length() - l);
        }
        return sb.toString();
    }

    /**
     * Stick items together
     * @param array array items
     * @param glue stick
     * @return glued items
     */
    public static String join(Object array, char glue) {
        if (array == null || !array.getClass().isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(200);
        for (int i = 0, length = Array.getLength(array); i < length; ++i) {
            sb.append(ObjectUtil.toString(Array.get(array, i))).append(glue);
        }
        if (sb.length() >= 1) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Check if a collection or map or array contains an object
     * @param array array or map or collection
     * @param needle needle to search for
     * @return true if contains
     */
    public static boolean contains(Object array, Object needle) {
        if (array == null || needle == null) {
            return false;
        }

        if (array.getClass().isArray()) {
            for (int i = 0, length = Array.getLength(array); i < length; ++i) {
                if (needle.equals(Array.get(array, i))) {
                    return true;
                }
            }
            return false;
        }

        if (array instanceof Collection) {
            return ((Collection<?>) array).contains(needle);
        }

        if (array instanceof Map) {
            return ((Map<?, ?>) array).containsKey(needle);
        }

        return false;
    }

    /**
     * Check if two collections share at least one element
     * @param collectionA collection A
     * @param collectionB collection B
     * @return true if they share element(s)
     */
    public static boolean shareItems(Collection<?> collectionA, Collection<?> collectionB) {
        for (Object objectB : collectionB) {
            if (collectionA.contains(objectB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if collection B is a subset of collection A
     * @param collectionB collection B
     * @param collectionA collection A
     * @return true collection B is a subset of collection A
     */
    public static boolean isSubset(Collection<?> collectionB, Collection<?> collectionA) {
        for (Object objectB : collectionB) {
            if (!collectionA.contains(objectB)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sort map by values
     * @param map the map
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortMap(map, false, -7);
    }

    /**
     * Sort map by values
     * @param map the map
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map) {
        return sortMap(map, true, -7);
    }

    /**
     * Sort map by values
     * @param map the map
     * @param topCount return only this number of top sorted items
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, int topCount) {
        return sortMap(map, false, topCount);
    }

    /**
     * Sort map by values
     * @param map the map
     * @param topCount return only this number of top sorted items
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map, int topCount) {
        return sortMap(map, true, topCount);
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortMap(Map<K, V> map, boolean desc, int topCount) {
        if (map == null) {
            return null;
        }
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(desc ? Map.Entry.comparingByValue(Comparator.reverseOrder()) : Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
            if (topCount == -7) {
                continue;
            }
            if (--topCount == 0) {
                break;
            }
        }
        return result;
    }

    /**
     * Convert object to an array
     * @param object the object
     * @param newType new array type
     * @return
     * (null if object == null or not convertible)
     * (empty Array if object is a map/collection and is empty or has zero convertible items)
     * (newType[] if object is a collection/Iterable/array/JSON list, converts each item to genericType)
     * (newType[] if object is a map/JSON map, converts each map value to genericType)
     * (newType[] if object is string split by "," then converts each item value to genericType)
     * (newType[] one item : convert object to genericType)
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T[] toArray(Object object, Class<T> newType) {
        if (object == null) {
            return null;
        }
        Class<?> type = object.getClass();
        if (type.isArray() && newType.equals(type.getComponentType())) {
            return (T[]) object;
        }

        List<T> list = toList(object, newType);
        if (list == null) {
            return null;
        }

        T[] items = (T[])Array.newInstance(newType, list.size());
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            items[i] = list.get(i);
        }
        return items;
    }

    /**
     * Convert object to a set
     * @param object the object
     * @param genericType generic type
     * @return
     * (null if object == null or not convertible)
     * (empty HashSet if object is a map/collection and is empty or has zero convertible items)
     * (Set<genericType> if object is a collection/Iterable/array/JSON list, converts each item to genericType)
     * (Set<genericType> if object is a map/JSON map, converts each map value to genericType)
     * (Set<genericType> if object is string split by "," then converts each item value to genericType)
     * (Set<genericType> one item : convert object to genericType)
     */
    public static <T> Set<T> toSet(Object object, Class<T> genericType) {
        return new HashSet<>(toList(object, genericType, null));
    }

    public static <T> Set<T> toSet(Object object, Class<T> genericType, Class<?> innerGenericTypes) {
        return new HashSet<>(toList(object, genericType, innerGenericTypes));
    }

    public static <T> List<T> toList(Object object, Class<T> genericType) {
        return toList(object, genericType, null);
    }

    /**
     * Convert object to a list
     * @param object the object
     * @param genericType generic type
     * @return
     * (null if object == null or not convertible)
     * (empty ArrayList if object is a map/collection and is empty or has zero convertible items)
     * (List<genericType> if object is a collection/Iterable/array/JSON list, converts each item to genericType)
     * (List<genericType> if object is a map/JSON map, converts each map value to genericType)
     * (List<genericType> if object is string split by "," then converts each item value to genericType)
     * (List<genericType> one item : convert object to genericType)
     */
    @SuppressWarnings({"unchecked"})
    public static <T> List<T> toList(Object object, Class<T> genericType, Class<?> innerGenericTypes) {
        if (object == null) {
            return null;
        }
        if (object instanceof Dto) {
            object = ((Dto) object).getPropertyValues();
        }
        if (object instanceof Map) {
            object = ((Map<?, ?>) object).values();
        }

        if (object instanceof Iterable) {
            if (ObjectUtil.isEmpty(object)) {
                return new ArrayList<>(1);
            }

            List<T> items = new ArrayList<>(object instanceof Collection<?> ? ((Collection<?>) object).size() : DEFAULT_INIT_SIZE);
            for (Object item : (Iterable<?>) object) {
                item = ObjectUtil.convert(item, genericType);
                if (item != null) {
                    items.add((T) item);
                }
            }
            return items;
        }

        Class<?> type = object.getClass();

        if (type.isArray()) {
            int length = Array.getLength(object);
            List<T> items = new ArrayList<>(length + 1);
            for (int i = 0; i < length; ++i) {
                T item = ObjectUtil.convert(Array.get(object, i), genericType);
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        }

        if (!type.equals(String.class)) {
            object = ObjectUtil.toString(object);
            if (object == null) {
                return null;
            }
        }
        String string = (String) object;
        if (ClassUtil.isInstantiable(genericType, Number.class)) {
            string = Persian.Number.toLatin(string);
        }
        string = string.trim();
        if (string.isEmpty()) {
            return new ArrayList<>(1);
        }

        if (string.startsWith("[") && string.endsWith("]")) {
            List<T> list = Json.d.listFromJson(string, genericType);
            return list == null ? new ArrayList<>(1) : list;
        }
        if (string.startsWith("{") && string.endsWith("}")) {
            Map<String, ?> map = Json.d.mapFromJson(string, String.class, genericType);
            return map == null ? new ArrayList<>(1) : toList(map.values(), genericType);
        }

        String[] strings = StringUtil.splitTrim(string, VantarParam.SEPARATOR_COMMON);
        if (strings == null) {
            return new ArrayList<>(1);
        }
        List<T> items = new ArrayList<>(strings.length + 1);
        for (String s : strings) {
            T item = ObjectUtil.convert(s.trim(), genericType);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Convert object to a map
     * @param object the object
     * @param genericTypeK key generic type
     * @param genericTypeV value generic type
     * @return
     * (null if object == null or not convertible)
     * (empty HashMap if object is a map/collection and is empty or has zero convertible items)
     * (Map<genericTypeK, genericTypeV> if object is a map/JSON map, converts each entry value to the genericTypes)
     * (Map<genericTypeK, genericTypeV> if object is a collection/Iterable/array/JSON list, converts each item to genericTypes if possible)
     * (Map<genericTypeK, genericTypeV> if object is string split by "," and ":" then converts each item key/value to genericTypes)
     * (key/value extract: ["k1:v1,k2:v2"] or [k,v])
     */
    public static <K, V> Map<K, V> toMap(Object object, Class<K> genericTypeK, Class<V> genericTypeV) {
        if (object == null) {
            return null;
        }
        if (object instanceof Dto) {
            object = ((Dto) object).getPropertyValues();
        }

        if (object instanceof Map) {
            if (ObjectUtil.isEmpty(object)) {
                return new HashMap<>(1, 1);
            }

            Map<K, V> items = new HashMap<>(((Map<?, ?>) object).size(), 1);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
                K k = ObjectUtil.convert(entry.getKey(), genericTypeK);
                V v = ObjectUtil.convert(entry.getValue(), genericTypeV);
                if (k != null) {
                    items.put(k, v);
                }
            }
            return items;
        }

        if (object instanceof Iterable) {
            if (ObjectUtil.isEmpty(object)) {
                return new HashMap<>(1, 1);
            }
            Map<K, V> items = new HashMap<>(object instanceof Collection<?> ?
                ((Collection<?>) object).size() : DEFAULT_INIT_SIZE, 1);
            for (Object o : (Iterable<?>) object) {
                putKv(o, items, genericTypeK, genericTypeV);
            }
            return items;
        }

        Class<?> type = object.getClass();

        if (type.isArray()) {
            int length = Array.getLength(object);
            Map<K, V> items = new HashMap<>(length + 1, 1);
            for (int i = 0; i < length; ++i) {
                putKv(Array.get(object, i), items, genericTypeK, genericTypeV);
            }
            return items;
        }

        if (!type.equals(String.class)) {
            object = ObjectUtil.toString(object);
            if (object == null) {
                return null;
            }
        }
        String string = (String) object;
        if (ClassUtil.isInstantiable(genericTypeK, Number.class) || ClassUtil.isInstantiable(genericTypeV, Number.class)) {
            string = Persian.Number.toLatin(string);
        }
        string = string.trim();
        if (string.isEmpty()) {
            return new HashMap<>(1, 1);
        }

        if (string.startsWith("[") && string.endsWith("]")) {
            List<String> list = Json.d.listFromJson(string, String.class);
            if (list == null) {
                return null;
            }
            Map<K, V> items = new HashMap<>(list.size());
            for (String s : list) {
                putKv(s, items, genericTypeK, genericTypeV);
            }
            return items;
        }
        if (string.startsWith("{") && string.endsWith("}")) {
            Map<K, V> map = Json.d.mapFromJson(string, genericTypeK, genericTypeV);
            return map == null ? new HashMap<>(1, 1) : map;
        }

        String[] strings = StringUtil.splitTrim(string, VantarParam.SEPARATOR_COMMON);
        if (strings == null) {
            return new HashMap<>(1, 1);
        }
        Map<K, V> items = new HashMap<>(strings.length + 1, 1);
        for (String s : strings) {
            putKv(s, items, genericTypeK, genericTypeV);
        }
        return items;
    }

    private static <K, V> void putKv(Object object, Map<K, V> map, Class<K> genericTypeK, Class<V> genericTypeV) {
        if (object instanceof String) {
            String[] kv = StringUtil.splitTrim((String) object, VantarParam.SEPARATOR_KEY_VAL);
            if (kv.length < 2) {
                return;
            }
            K k = ObjectUtil.convert(StringUtil.trim(kv[0], '"'), genericTypeK);
            V v = ObjectUtil.convert(StringUtil.trim(kv[1], '"'), genericTypeV);
            if (k != null && v != null) {
                map.put(k, v);
            }
            return;
        }

        if (object.getClass().isArray()) {
            if (Array.getLength(object) < 2) {
                return;
            }
            K k = ObjectUtil.convert(Array.get(object, 0), genericTypeK);
            V v = ObjectUtil.convert(Array.get(object, 1), genericTypeV);
            if (k != null && v != null) {
                map.put(k, v);
            }
            return;
        }

        if (object instanceof Iterable) {
            K k = null;
            V v = null;
            int i = 0;
            for (Object o : (Iterable<?>) object) {
                if (i == 0) {
                    k = ObjectUtil.convert(o, genericTypeK);
                } else if (i == 1) {
                    v = ObjectUtil.convert(o, genericTypeV);
                } else {
                    break;
                }
                ++i;
            }
            if (k != null && v != null) {
                map.put(k, v);
            }
        }
    }

    public static boolean equalsCollection(Collection<?> collectionA, Collection<?> collectionB) {
        if (collectionA.size() != collectionB.size()) {
            return false;
        }
        for (Object oA : collectionA) {
            boolean found = false;
            for (Object oB : collectionB) {
                if (oA instanceof Collection && oB instanceof Collection) {
                    if (equalsCollection((Collection<?>) oA, (Collection<?>) oB)) {
                        found = true;
                        break;
                    }
                }
                if (oA.equals(oB)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean equalsMap(Map<?, ?> mapA, Map<?, ?> mapB) {
        if (mapA.size() != mapB.size()) {
            return false;
        }
        if (!equalsCollection(mapA.keySet(), mapB.keySet())) {
            return false;
        }
        if (!equalsCollection(mapA.values(), mapB.values())) {
            return false;
        }
        return true;
    }
}
