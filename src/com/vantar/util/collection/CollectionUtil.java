package com.vantar.util.collection;

import com.vantar.common.VantarParam;
import com.vantar.util.string.*;
import java.lang.reflect.Array;
import java.util.*;


public class CollectionUtil {

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map) {
        if (map == null) {
            return null;
        }

        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map, int topCount) {
        if (map == null) {
            return null;
        }
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
            if (--topCount == 0) {
                break;
            }
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, int topCount) {
        if (map == null) {
            return null;
        }
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
            if (--topCount == 0) {
                break;
            }
        }

        return result;
    }

    public static boolean contains(String[] array, String needle) {
        if (array == null || needle == null) {
            return false;
        }
        for (String item : array) {
            if (needle.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsPartially(String[] array, String needle) {
        if (array == null || needle == null) {
            return false;
        }
        needle = needle.toLowerCase();
        for (String item : array) {
            if (needle.contains(item.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsPartially(Collection<String> collection, String needle) {
        if (collection == null || needle == null) {
            return false;
        }
        needle = needle.toLowerCase();
        for (String item : collection) {
            if (needle.contains(item.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String join(Collection<?> collection, String glue) {
        if (collection == null || collection.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(collection.size() * 20);
        for (Object item : collection) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - glue.length());
        return sb.toString();
    }

    public static String join(Collection<?> collection, char glue) {
        if (collection == null || collection.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(collection.size() * 20);
        for (Object item : collection) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String join(Map<?, ?> collection, String glueEntry, String glueItem) {
        if (collection == null || collection.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(collection.size() * 20);
        for (Map.Entry<?, ?> entry : collection.entrySet()) {
            sb.append(entry.getKey()).append(glueEntry).append(entry.getValue()).append(glueItem);
        }
        sb.setLength(sb.length() - glueItem.length());
        return sb.toString();
    }

    public static String join(Map<?, ?> collection, char glueEntry, char glueItem) {
        if (collection == null || collection.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(collection.size() * 20);
        for (Map.Entry<?, ?> entry : collection.entrySet()) {
            sb.append(entry.getKey()).append(glueEntry).append(entry.getValue()).append(glueItem);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String join(Object[] array, String glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 20);
        for (Object item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - glue.length());
        return sb.toString();
    }

    public static String join(Object[] array, char glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 20);
        for (Object item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

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

    public static String join(double[] array, String glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (double item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - glue.length());
        return sb.toString();
    }

    public static String join(double[] array, char glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (double item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String join(long[] array, String glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (long item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - glue.length());
        return sb.toString();
    }

    public static String join(long[] array, char glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (long item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String join(int[] array, String glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (int item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - glue.length());
        return sb.toString();
    }

    public static String join(int[] array, char glue) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 10);
        for (int item : array) {
            sb.append(item).append(glue);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String toString(Object[] array) {
        return join(array, VantarParam.SEPARATOR_COMMON);
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

    public static boolean isMap(Class<?> typeClass) {
        return typeClass != null && Map.class.isAssignableFrom(typeClass);
    }

    public static boolean isMap(Object ob) {
        return ob instanceof Map<?, ?>;
    }

    public static boolean isCollection(Class<?> typeClass) {
        return typeClass != null && Collection.class.isAssignableFrom(typeClass);
    }

    public static boolean isCollection(Object ob) {
        return ob instanceof Collection<?>;
    }

    public static boolean isCollectionAndMap(Class<?> typeClass) {
        return typeClass != null && (Collection.class.isAssignableFrom(typeClass) || Map.class.isAssignableFrom(typeClass));
    }

    public static boolean isCollectionAndMap(Object ob) {
        return (ob instanceof Collection<?>) || (ob instanceof Map<?, ?>);
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length != 0;
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean containsAtLeastOneElement(Collection<?> collectionA, Collection<?> collectionB) {
        for (Object objectB : collectionB) {
            if (collectionA.contains(objectB)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAllElement(Collection<?> collectionA, Collection<?> collectionB) {
        for (Object objectB : collectionB) {
            if (!collectionA.contains(objectB)) {
                return false;
            }
        }
        return true;
    }

    public static Object[] toObjectArray(Object array) {
        int length = Array.getLength(array);
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Array.get(array, i);
        }
        return ret;
    }
}
