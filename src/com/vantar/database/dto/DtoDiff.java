package com.vantar.database.dto;


import com.vantar.util.object.*;
import java.util.*;


public class DtoDiff {

    private Map<String, Difference> diff = new LinkedHashMap<>(100, 1);

    /**
     * Map<fieldName, Object>
     * Object:
     * 1- String (diff if field is scalar)
     * 2- Map<fieldName, Object> (if field if dto -> recursive)
     * 3- CollectionDiff (if field is collection)
     *      before: List<Object> (items only in dtoA)
     *      after: List<Object> (items only in dtoB)
     * 4-
     */
    public Map<String, Difference> getDiff(Dto dtoA, Dto dtoB) {
        putDiffDto(null, dtoA, dtoB);
        return diff;
    }

    private void putDiffDto(String path, Dto dtoA, Dto dtoB) {
        if (dtoA == null && dtoB == null) {
            return ;
        }
        Dto dtoX = dtoA == null ? dtoB : dtoA;
        Map<String, Object> propertiesA = dtoA == null ? new HashMap<>(1, 1) : dtoA.getPropertyValuesIncludeNulls();
        Map<String, Object> propertiesB = dtoB == null ? new HashMap<>(1, 1) : dtoB.getPropertyValuesIncludeNulls();
        Collection<String> propertyNames = propertiesA == null ? propertiesB.keySet() : propertiesA.keySet();

        Set<String> includes;
        Set<String> excludes;
        if (dtoX instanceof Setting) {
            includes = ((Setting) dtoX).getDtoDiffInclude();
            excludes = ((Setting) dtoX).getDtoDiffExclude();
            //  TestController.log.error(">>>>>>>>A>>> {} {} {} {}",path,  dtoX.getClass().getSimpleName(), includes, excludes);

        } else {
            includes = null;
            excludes = null;
        }

        for (String name : propertyNames) {
            if (includes != null) {
                if (!includes.contains(name)) {
                    continue;
                }
            } else if (excludes != null) {
                if (excludes.contains(name)) {
                    continue;
                }
            }
            String key = path == null ? name : (path + "." + name);
            Class<?> type = dtoX.getPropertyType(name);
            Object valueA = propertiesA == null ? null : propertiesA.get(name);
            Object valueB = propertiesB == null ? null : propertiesB.get(name);

            // collection
            if (ClassUtil.isInstantiable(type, Collection.class)) {
                Class<?>[] g = dtoX.getPropertyGenericTypes(name);
                putDiffCollection(key, (Collection<?>) valueA, (Collection<?>) valueB, g[0]);

                // map
            } else if (ClassUtil.isInstantiable(type, Map.class)) {
                Class<?>[] g = dtoX.getPropertyGenericTypes(name);
                MapDiff d = getDiffMap((Map<?, ?>) valueA, (Map<?, ?>) valueB, g[1]);
                if (d != null && !d.isEmpty()) {
                    //     diff.put(name, d);
                }

                // dto
            } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                putDiffDto(name, (Dto) valueA, (Dto) valueB);

                // scalar field
            } else {
                if ((valueA != null && !valueA.equals(valueB)) || (valueB != null && !valueB.equals(valueA))) {
                    if (!(ObjectUtil.isEmpty(valueA) && ObjectUtil.isEmpty(valueB))) {
                        diff.put(key, new Difference(valueA, valueB));
                    }
                }
            }
        }
    }

    /**
     * Get items that are in collectionA but not in collectionB
     */
    @SuppressWarnings("unchecked")
    private void putDiffCollection(String path, Collection<?> collectionA, Collection<?> collectionB, Class<?> g) {
        boolean isEmptyA = ObjectUtil.isEmpty(collectionA);
        boolean isEmptyB = ObjectUtil.isEmpty(collectionB);
        if (isEmptyA && isEmptyB) {
            return;
        }
        if (isEmptyA) {
            diff.put(path, Difference.onlyAfter(collectionB));
            return;
        }
        if (isEmptyB) {
            diff.put(path, Difference.onlyBefore(collectionA));
            return;
        }

        List<Object> aSubB = subtractCollection(collectionA, collectionB);
        List<Object> bSubA = subtractCollection(collectionB, collectionA);
        List<Map<String, Difference>> change;
        boolean aSubBisNotEmpty = ObjectUtil.isNotEmpty(aSubB);
        boolean bSubAisNotEmpty = ObjectUtil.isNotEmpty(bSubA);

        if (ClassUtil.implementsInterface(g, Dto.class)) {
            //  TestController.log.error(">>>>>>>>T>>>{} {} {}", g.getSimpleName(), aSubBisNotEmpty, bSubAisNotEmpty);

            Set<String> includes = null;
            Set<String> excludes = null;
            if (aSubBisNotEmpty) {
                Object x = aSubB.get(0);
                if (x instanceof Setting) {
                    includes = ((Setting) x).getDtoDiffInclude();
                    excludes = ((Setting) x).getDtoDiffExclude();
                }
            } else if (bSubAisNotEmpty) {
                Object x = bSubA.get(0);
                if (x instanceof Setting) {
                    includes = ((Setting) x).getDtoDiffInclude();
                    excludes = ((Setting) x).getDtoDiffExclude();
                }
            }
            if (includes != null || excludes != null) {
                if (aSubBisNotEmpty) {
                    for (Object item : aSubB) {
                        Dto x = (Dto) item;
                        //           TestController.log.error(">>>>>>>>T>>>{} {} {}", x.getClass().getSimpleName(), includes, excludes);
                        if (includes != null) {
                            for (String name : x.getPropertiesEx(includes.toArray(new String[0]))) {
                                x.setPropertyValue(name, null);
                            }
                        } else if (excludes != null) {
                            for (String name : excludes) {
                                x.setPropertyValue(name, null);
                            }
                        }
                    }
                }
            }






            change = putDiffCollectionDto(path, (List<? extends Dto>) collectionA, (List<? extends Dto>) collectionB);
        } else {
            change = null;
        }

        if (aSubBisNotEmpty || bSubAisNotEmpty || ObjectUtil.isNotEmpty(change)) {
            diff.put(path, new Difference(aSubB, bSubA, change));
        }
    }

    private List<Map<String, Difference>> putDiffCollectionDto(
        String path,
        Collection<? extends Dto> collectionA,
        Collection<? extends Dto> collectionB
    ) {

        Map<String, Difference> diffTemp = diff;
        List<Map<String, Difference>> changes = new ArrayList<>(100);

        for (Dto dtoA : collectionA) {
            if (dtoA.getId() == null) {
                // ?
                continue;
            }
            for (Dto dtoB : collectionB) {
                if (dtoA.getId().equals(dtoB.getId())) {
                    diff = new LinkedHashMap<>(100, 1);
                    putDiffDto(path, dtoA, dtoB);
                    if (ObjectUtil.isNotEmpty(diff)) {
                        changes.add(diff);
//                        TestController.log.error(">>>>>>>P>>{}", path);
                    }
                    break;
                }
            }
        }
        diff = diffTemp;
        return changes;
    }

    public static List<Object> subtractCollection(Collection<?> collectionA, Collection<?> collectionB) {
        List<Object> diff = new ArrayList<>(collectionA.size());
        for (Object oA : collectionA) {
            boolean found = false;
            for (Object oB : collectionB) {
                if (oA instanceof Collection && oB instanceof Collection) {
                    if (equalsCollection((Collection<?>) oA, (Collection<?>) oB)) {
                        found = true;
                        break;
                    }
                } else if (oA instanceof Map && oB instanceof Map) {
                    if (equalsMap((Map<?, ?>) oA, (Map<?, ?>) oB)) {
                        found = true;
                        break;
                    }
                } else if (oA.equals(oB)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                diff.add(oA);
            }
        }
        return diff;
    }







    /**
     * Get items that are in mapA but not in mapB
     * @param mapA
     * @param mapB
     * @return
     */
    public static MapDiff getDiffMap(Map<?, ?> mapA, Map<?, ?> mapB, Class<?> g) {
        boolean isEmptyA = ObjectUtil.isEmpty(mapA);
        boolean isEmptyB = ObjectUtil.isEmpty(mapB);
        if (isEmptyA && isEmptyB) {
            return null;
        }
        MapDiff diff = new MapDiff();
        if (isEmptyA) {
            diff.after = new HashMap<>(mapB);
            return diff;
        }
        if (isEmptyB) {
            diff.before = new HashMap<>(mapA);
            return diff;
        }
        if (ClassUtil.implementsInterface(g, Dto.class)) {
            diff.change = dtoChangeMap((Map<?, ? extends Dto>) mapA, (Map<?, ? extends Dto>) mapB);
        }
        diff.before = subtractMap(mapA, mapB);
        diff.after = subtractMap(mapB, mapA);
        return diff;
    }

    public static Map<Object, Object> dtoChangeMap(Map<?, ? extends Dto> mapA, Map<?, ? extends Dto> mapB) {
        Map<Object, Object> diff = new HashMap<>(Math.max(mapA.size(), mapB.size()), 1);
        for (Map.Entry<?, ? extends Dto> eA : mapA.entrySet()) {
            Object k = eA.getKey();
            Dto vA = eA.getValue();
            Dto vB = mapB.get(k);

            if (vB != null) {
//                Map<String, Object> d = getDiff(vA, vB);
//                if (!d.isEmpty()) {
//                    diff.put(k, d);
//                }
            }
        }
        return diff;
    }

    public static Map<Object, Object> subtractMap(Map<?, ?> mapA, Map<?, ?> mapB) {
        Map<Object, Object> diff = new HashMap<>(mapA.size(), 1);
        for (Map.Entry<?, ?> eA : mapA.entrySet()) {
            Object k = eA.getKey();
            Object vA = eA.getValue();
            Object vB = mapB.get(k);
            if (vB == null) {
                diff.put(k, vA);
            } else if (vA instanceof Collection && vB instanceof Collection) {
                if (!equalsCollection((Collection<?>) vA, (Collection<?>) vB)) {
                    diff.put(k, vA);
                }
            } else if (vA instanceof Map && vB instanceof Map) {
                if (!equalsMap((Map<?, ?>) vA, (Map<?, ?>) vB)) {
                    diff.put(k, vA);
                }
            } else if (!vA.equals(vB)) {
                if (!(vA instanceof Dto)) {
                    diff.put(k, vA);
                }
            }
        }
        return diff;
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






    public static class Difference {

        public List<Map<String, Difference>> change;
        public String before;
        public String after;


        public Difference() {

        }

        public Difference(Object before, Object after) {
            this.before = ObjectUtil.toString(before);
            this.after = ObjectUtil.toString(after);
        }

        public Difference(Object before, Object after, List<Map<String, Difference>> change) {
            if (ObjectUtil.isNotEmpty(before)) {
                this.before = ObjectUtil.toString(before);
            }
            if (ObjectUtil.isNotEmpty(after)) {
                this.after = ObjectUtil.toString(after);
            }
            if (ObjectUtil.isNotEmpty(change)) {
                this.change = change;
            }
        }

        public String toString() {
            return ObjectUtil.toString(this);
        }

        public boolean isEmpty() {
            return (before == null || before.isEmpty())
                && (after == null || after.isEmpty())
                && (change == null || change.isEmpty());
        }

        public static Difference onlyAfter(Object obj) {
            Difference difference = new Difference();
            difference.after = ObjectUtil.toString(obj);
            return difference;
        }

        public static Difference onlyBefore(Object obj) {
            Difference difference = new Difference();
            difference.before = ObjectUtil.toString(obj);
            return difference;
        }
    }






    public static class CollectionDiff {

        public List<Object> change;
        public List<Object> before;
        public List<Object> after;

        public String toString() {
            return ObjectUtil.toString(this);
        }

        public boolean isEmpty() {
            return (before == null || before.isEmpty()) && (after == null || after.isEmpty()) && (change == null || change.isEmpty());
        }
    }

    public static class MapDiff {

        public Map<Object, Object> change;
        public Map<Object, Object> before;
        public Map<Object, Object> after;

        public String toString() {
            return ObjectUtil.toString(this);
        }

        public boolean isEmpty() {
            return (before == null || before.isEmpty()) && (after == null || after.isEmpty()) && (change == null || change.isEmpty());
        }
    }


    public interface Setting {

        Set<String> getDtoDiffInclude();
        Set<String> getDtoDiffExclude();

    }
}
