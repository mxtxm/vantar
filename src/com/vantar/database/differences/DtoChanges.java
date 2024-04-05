package com.vantar.database.differences;

import com.vantar.database.dto.Dto;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.object.*;
import java.util.*;


public class DtoChanges {

    private final List<Change> changes = new ArrayList<>(100);

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
    public List<Change> get(Dto dtoA, Dto dtoB) {
        findDtoChanges(null, dtoA, dtoB);
        return changes;
    }

    private void findDtoChanges(String path, Dto dtoBefore, Dto dtoAfter) {
        if (dtoBefore == null && dtoAfter == null) {
            return ;
        }
        Dto dto = dtoBefore == null ? dtoAfter : dtoBefore;
        Map<String, Object> propsBefore = dtoBefore == null ? new HashMap<>(1, 1) : dtoBefore.getPropertyValuesIncludeNulls();
        Map<String, Object> propsAfter = dtoAfter == null ? new HashMap<>(1, 1) : dtoAfter.getPropertyValuesIncludeNulls();

        Set<String> includes;
        Set<String> excludes;
        if (dto instanceof Setting) {
            includes = ((Setting) dto).getDtoDiffInclude();
            excludes = ((Setting) dto).getDtoDiffExclude();
        } else {
            includes = null;
            excludes = null;
        }

        for (String name : dto.getProperties()) {
            if (includes != null) {
                if (!includes.contains(name)) {
                    continue;
                }
            } else if (excludes != null) {
                if (excludes.contains(name)) {
                    continue;
                }
            }
            Class<?> type = dto.getPropertyType(name);
            String key = path == null ? name : (path + "." + name);
            Object valueBefore = propsBefore == null ? null : propsBefore.get(name);
            Object valueAfter = propsAfter == null ? null : propsAfter.get(name);
            if (ObjectUtil.isEmpty(valueBefore) && ObjectUtil.isEmpty(valueAfter)) {
                continue;
            }

            // > > > collection
            if (ClassUtil.isInstantiable(type, Collection.class)) {
                findCollectionChanges(
                    key,
                    (Collection<?>) valueBefore,
                    (Collection<?>) valueAfter,
                    dto.getPropertyGenericTypes(name)[0]
                );

            // > > > map
            } else if (ClassUtil.isInstantiable(type, Map.class)) {
                findMapChanges(
                    key,
                    (Map<?, ?>) valueBefore,
                    (Map<?, ?>) valueAfter,
                    dto.getPropertyGenericTypes(name)[0],
                    dto.getPropertyGenericTypes(name)[1]
                );

            // > > > dto
            } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                findDtoChanges(name, (Dto) valueBefore, (Dto) valueAfter);

            // > > > scalar
            } else {
                if (valueAfter == null || !valueAfter.equals(valueBefore)) {
                    changes.add(new Change(key, ObjectUtil.toStringViewable(valueBefore), ObjectUtil.toStringViewable(valueAfter)));
                }
            }
        }
    }

    private void findCollectionChanges(String path, Collection<?> cBefore, Collection<?> cAfter, Class<?> g) {
        boolean isEmptyBefore = ObjectUtil.isEmpty(cBefore);
        boolean isEmptyAfter = ObjectUtil.isEmpty(cAfter);
        if (isEmptyBefore && isEmptyAfter) {
            return;
        }

        if (isEmptyBefore) {
            changes.add(new Change(path, null, ObjectUtil.toStringViewable(cAfter)));
            return;
        }
        if (isEmptyAfter) {
            changes.add(new Change(path, ObjectUtil.toStringViewable(cBefore), null));
            return;
        }

        Collection<?> sBefore = new HashSet<>(cBefore);
        Collection<?> sAfter = new HashSet<>(cAfter);
        sBefore.removeIf(sAfter::remove);

        // > > > collection
        if (ClassUtil.isInstantiable(g, Collection.class)) {
            if (!sBefore.isEmpty() || !sAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(sBefore), ObjectUtil.toStringViewable(sAfter)));
            }

        // > > > map
        } else if (ClassUtil.isInstantiable(g, Map.class)) {
            if (!sBefore.isEmpty() || !sAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(sBefore), ObjectUtil.toStringViewable(sAfter)));
            }

        // > > > dto
        } else if (ClassUtil.isInstantiable(g, Dto.class)) {
            Iterator<?> itBefore = sBefore.iterator();
            while (itBefore.hasNext()) {
                Object objBefore = itBefore.next();
                Long idBefore = ((Dto) objBefore).getId();
                // > not in after and does not have id --> not exists in after
                if (idBefore == null) {
                    changes.add(new Change(path, ObjectUtil.toStringViewable(objBefore), null));
                    continue;
                }
                // > look in after by id
                Iterator<?> itAfter = sAfter.iterator();
                while (itAfter.hasNext()) {
                    Object objAfter = itAfter.next();
                    Long idAfter = ((Dto) objAfter).getId();
                    if (idBefore.equals(idAfter)) {
                        findDtoChanges(path, (Dto) objBefore, (Dto) objAfter);
                        itBefore.remove();
                        itAfter.remove();
                        break;
                    }
                }
            }
            if (!sBefore.isEmpty() || !sAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(sBefore), ObjectUtil.toStringViewable(sAfter)));
            }

        // > > > scalar
        } else {
            if (!sBefore.isEmpty() || !sAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(sBefore), ObjectUtil.toStringViewable(sAfter)));
            }
        }
    }

    private void findMapChanges(String path, Map<?, ?> mBefore, Map<?, ?> mAfter, Class<?> gK, Class<?> gV) {
        boolean isEmptyBefore = ObjectUtil.isEmpty(mBefore);
        boolean isEmptyAfter = ObjectUtil.isEmpty(mAfter);
        if (isEmptyBefore && isEmptyAfter) {
            return;
        }
        if (isEmptyBefore) {
            changes.add(new Change(path, null, ObjectUtil.toStringViewable(mAfter)));
            return;
        }
        if (isEmptyAfter) {
            changes.add(new Change(path, ObjectUtil.toStringViewable(mBefore), null));
            return;
        }

        Map<?, ?> mapBefore = new HashMap<>(mBefore);
        Map<?, ?> mapAfter = new HashMap<>(mAfter);
        // > remove equal objects from both
        for (Iterator<? extends Map.Entry<?, ?>> itBefore = mapBefore.entrySet().iterator(); itBefore.hasNext(); ) {
            Map.Entry<?, ?> entry = itBefore.next();
            Object k = entry.getKey();
            Object vBefore = entry.getValue();
            Object vAfter = mapAfter.get(k);
            if (vBefore.equals(vAfter)) {
                itBefore.remove();
                mapAfter.remove(k);
            }
        }

        // > > > collection
        if (ClassUtil.isInstantiable(gV, Collection.class)) {
            if (!mapBefore.isEmpty() || !mapAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(mapBefore), ObjectUtil.toStringViewable(mapAfter)));
            }

        // > > > map
        } else if (ClassUtil.isInstantiable(gV, Map.class)) {
            if (!mapBefore.isEmpty() || !mapAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(mapBefore), ObjectUtil.toStringViewable(mapAfter)));
            }

        // > > > dto
        } else if (ClassUtil.isInstantiable(gV, Dto.class)) {
            for (Iterator<? extends Map.Entry<?, ?>> itBefore = mapBefore.entrySet().iterator(); itBefore.hasNext(); ) {
                Map.Entry<?, ?> entry = itBefore.next();
                Object k = entry.getKey();
                Object vBefore = entry.getValue();
                Object vAfter = mapAfter.remove(k);
                if (vAfter != null) {
                    findDtoChanges(path + "." + k, (Dto) vBefore, (Dto) vAfter);
                    itBefore.remove();
                }
            }
            if (!mapBefore.isEmpty() || !mapAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(mapBefore), ObjectUtil.toStringViewable(mapAfter)));
            }

        // > > > scalar
        } else {
            if (!mapBefore.isEmpty() || !mapAfter.isEmpty()) {
                changes.add(new Change(path, ObjectUtil.toStringViewable(mapBefore), ObjectUtil.toStringViewable(mapAfter)));
            }
        }
    }


    public static class Change {

        public String column;
        public String before;
        public String after;

        public Change(String column, String before, String after) {
            this.column = column;
            this.before = before;
            this.after = after;
        }
    }


    public interface Setting {

        Set<String> getDtoDiffInclude();
        Set<String> getDtoDiffExclude();
    }
}
