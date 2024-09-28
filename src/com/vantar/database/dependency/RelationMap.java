package com.vantar.database.dependency;

import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Dependencies
 *
 * @Depends(Role.class)
 * public Long roleId;
 * @Depends(Role.class)
 * public Role role; // check role.id
 *
 * @Depends(Role.class)
 * public List<Long> roleIds;
 * @Depends(Role.class)
 * public List<Role> roles;  // check role[i].id
 *              k         *v
 * @Depends(Dto.class, Role.class)
 * public Map< ?, Long/Role> roleIds;
 *              *k         v
 * @Depends(Role.class, Dto.class)
 * public Map<Long, ?> roleIds;
 *              *k        *v
 * @Depends(Role.class, Feature.class)
 * public Map<Long, Long/Role> roleIds;
 */
public class RelationMap {

    private Map<Class<? extends Dto>, List<Relation>> relations;


    public Map<Class<? extends Dto>, List<Relation>> getRelations() {
        long t1 = System.currentTimeMillis();

        List<DtoDictionary.Info> dtos = DtoDictionary.getAll();
        relations = new HashMap<>(dtos.size(), 1);
        for (DtoDictionary.Info dtoInfo : DtoDictionary.getAll()) {
            if (dtoInfo.hidden || Db.Dbms.NOSTORE.equals(dtoInfo.dbms)) {
                continue;
            }
            deepSeekDto(dtoInfo.dtoClass);
        }

        // cleanup un-wanted items
        relations.entrySet().removeIf(entry -> {
            DtoDictionary.Info info = DtoDictionary.get(entry.getKey());
            return info == null || info.hidden || Db.Dbms.NOSTORE.equals(info.dbms);
        });

        ServiceLog.log.trace(" > relation map creation: {}ms", System.currentTimeMillis() - t1);
        return relations;
    }

    @SuppressWarnings("unchecked")
    private void deepSeekDto(Class<? extends Dto> dtoClass) {
        if (relations.containsKey(dtoClass)) {
            return;
        }

        List<Relation> dtoRelations = new ArrayList<>(14);
        for (Field field : dtoClass.getFields()) {
            if (!DtoBase.isDataField(field)) {
                continue;
            }
            Class<?> type = field.getType();

            // > > > Depends
            Depends annotation = field.getAnnotation(Depends.class);
            if (annotation != null) {
                Class<? extends Dto>[] dependencies = annotation.value();

                Relation r = new Relation();
                RelationRoute route = new RelationRoute(field);
                r.addFieldRoute(route);
                r.fkClasses = dependencies;
                dtoRelations.add(r);

                /* > > >
                 * @Depends(Role.class)
                 * public Long roleId;
                 */
                if (ClassUtil.implementsInterface(type, Long.class)) {
                    route.isPkFk = true;
                }
                // < < <

                /* > > >
                 * @Depends(Role.class)
                 * public Role role; // check role.id
                 */
                else if (ClassUtil.implementsInterface(type, Dto.class)) {
                    route.isDto = true;
                }
                // < < <

                /* > > >
                 * @Depends(Role.class)
                 * public List<Long> roleIds;
                 * @Depends(Role.class)
                 * public List<Role> roles;  // check role[i].id
                 */
                else if (ClassUtil.implementsInterface(type, Collection.class)) {
                    route.isCollection = true;
                    route.isDto = ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[0], Dto.class);
                }
                // < < <

                /* > > >
                 *              k         *v
                 * @Depends(Dto.class, Role.class)
                 * public Map<?, Long/Role> roleIds;
                 *              *k         v
                 * @Depends(Role.class, Dto.class)
                 * public Map<Long, ?> roleIds;
                 *              *k        *v
                 * @Depends(Role.class, Feature.class)
                 * public Map<Long, Long/Role> roleIds;
                 */
                else if (ClassUtil.implementsInterface(type, Map.class)) {
                    route.isMapKey = !dependencies[0].equals(Dto.class);
                    route.isMapValue = !dependencies[1].equals(Dto.class);
                    Class<?>[] g = ClassUtil.getGenericTypes(field);
                    route.isDto = route.isMapValue && ClassUtil.implementsInterface(g[1], Dto.class);
                }
                // < < <
            }
            // Depends < < <

            // > > > Deep search for dto
            if (ClassUtil.implementsInterface(type, Dto.class)) {
                RelationRoute route = new RelationRoute(field);
                route.isDto = true;
                seekInner((Class<? extends Dto>) type, dtoClass, dtoRelations, route);

            } else if (ClassUtil.implementsInterface(type, Collection.class)) {
                if (ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[0], Dto.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isCollection = true;
                    route.isDto = true;
                    seekInner((Class<? extends Dto>) ClassUtil.getGenericTypes(field)[0], dtoClass, dtoRelations, route);
                }

            } else if (ClassUtil.implementsInterface(type, Map.class)) {
                if (ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[1], Dto.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isMapValue = true;
                    route.isDto = true;
                    seekInner((Class<? extends Dto>) ClassUtil.getGenericTypes(field)[1], dtoClass, dtoRelations, route);
                }

            }
            // Deep < < <
        }
        relations.put(dtoClass, dtoRelations);
    }

    private void seekInner(
        Class<? extends Dto> dtoClassInner,
        Class<? extends Dto> dtoClass,
        List<Relation> dtoRelations,
        RelationRoute route
    ) {

        if (dtoClass.equals(dtoClassInner)) {
            return;
        }
        deepSeekDto(dtoClassInner);
        List<Relation> innerRelations = relations.get(dtoClassInner);
        if (ObjectUtil.isEmpty(innerRelations)) {
            return;
        }
        for (Relation innerRelation : innerRelations) {
            Relation r = new Relation(innerRelation);
            r.addFieldRoute(route);
            dtoRelations.add(r);
        }
    }


    public static class Relation {

        public List<RelationRoute> fieldRoute;
        public Class<? extends Dto>[] fkClasses;

        public Relation() {

        }

        public Relation(Relation r) {
            if (r.fieldRoute != null) {
                fieldRoute = new ArrayList<>(r.fieldRoute);
            }
            fkClasses = r.fkClasses;
        }

        public void addFieldRoute(RelationRoute f) {
            if (fieldRoute == null) {
                fieldRoute = new ArrayList<>(10);
            }
            fieldRoute.add(0, f);
        }

        @Override
        public String toString() {
            return Json.d.toJsonPretty(this);
        }
    }


    public static class RelationRoute {

        public String fieldName;

        // fk ---> pk / no combination
        public boolean isPkFk;
        // dto.fk ---> pk / can be combined with isCollection
        public boolean isDto;
        // [fk] ---> pk / can be combined with isDto
        public boolean isCollection;

        // map combinations
        public boolean isMapKey;
        public boolean isMapValue;

        public RelationRoute(Field field) {
            fieldName = field.getName();
        }
    }
}