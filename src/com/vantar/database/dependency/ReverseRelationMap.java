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
public class ReverseRelationMap {

    private Map<Class<? extends Dto>, Set<Relation>> relations;
    private Map<Class<? extends Dto>, Set<Class<? extends Dto>>> affectedClasses;


    public Map<Class<? extends Dto>, Set<Relation>> getRelations() {
        long t1 = System.currentTimeMillis();

        List<DtoDictionary.Info> dtos = DtoDictionary.getAll();
        relations = new HashMap<>(dtos.size(), 1);
        affectedClasses = new HashMap<>(dtos.size(), 1);
        for (DtoDictionary.Info dtoInfo : DtoDictionary.getAll()) {
            if (dtoInfo.hidden || Db.Dbms.NOSTORE.equals(dtoInfo.dbms)) {
                continue;
            }
            deepSeekDto(dtoInfo.dtoClass);
        }
        affectedClasses = null;

        // cleanup un-wanted items
        relations.entrySet().removeIf(entry -> {
            DtoDictionary.Info info = DtoDictionary.get(entry.getKey());
            boolean toRemove = info == null || info.hidden || Db.Dbms.NOSTORE.equals(info.dbms);
            if (!toRemove && entry.getValue() != null) {
                entry.getValue().removeIf(relation -> {
                    DtoDictionary.Info infoR = DtoDictionary.get(relation.fkClass);
                    return infoR == null || infoR.hidden || Db.Dbms.NOSTORE.equals(infoR.dbms);
                });
            }
            return toRemove;
        });

        ServiceLog.log.trace(" > reverse relation map creation: {}ms", System.currentTimeMillis() - t1);
        return relations;
    }

    @SuppressWarnings("unchecked")
    private void deepSeekDto(Class<? extends Dto> dtoClass) {
        for (Field field : dtoClass.getFields()) {
            if (!DtoBase.isDataField(field) || field.isAnnotationPresent(RedundantDependency.class)) {
                continue;
            }
            Class<?> type = field.getType();

            // > > > Depends
            Depends annotation = field.getAnnotation(Depends.class);
            if (annotation != null) {
                Class<? extends Dto>[] dependencies = annotation.value();

                /* > > >
                 * @Depends(Role.class)
                 * public Long roleId;
                 */
                if (ClassUtil.implementsInterface(type, Long.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isPkFk = true;
                    addRevRelation(dependencies[0], dtoClass, route);
                }
                // < < <

                /* > > >
                 * @Depends(Role.class)
                 * public Role role; // check role.id
                 */
                else if (ClassUtil.implementsInterface(type, Dto.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isDto = true;
                    addRevRelation(dependencies[0], dtoClass, route);
                }
                // < < <

                /* > > >
                 * @Depends(Role.class)
                 * public List<Long> roleIds;
                 * @Depends(Role.class)
                 * public List<Role> roles;  // check role[i].id
                 */
                else if (ClassUtil.implementsInterface(type, Collection.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isCollection = true;
                    route.isDto = ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[0], Dto.class);
                    addRevRelation(dependencies[0], dtoClass, route);
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
                    Class<?>[] g = ClassUtil.getGenericTypes(field);
                    boolean isMapKey = !dependencies[0].equals(Dto.class);
                    boolean isMapValue = !dependencies[1].equals(Dto.class);
                    if (isMapKey) {
                        RelationRoute route = new RelationRoute(field);
                        route.isMapKey = true;
                        addRevRelation(dependencies[0], dtoClass, route);
                    }
                    if (isMapValue) {
                        RelationRoute route = new RelationRoute(field);
                        route.isMapValue = true;
                        route.isDto = ClassUtil.implementsInterface(g[1], Dto.class);
                        addRevRelation(dependencies[1], dtoClass, route);
                    }
                }
                // < < <
            }
            // Depends < < <

            // > > > Deep search for dto
            if (ClassUtil.implementsInterface(type, Dto.class)) {
                RelationRoute route = new RelationRoute(field);
                route.isDto = true;
                seekInner((Class<? extends Dto>) type, dtoClass, route);

            } else if (ClassUtil.implementsInterface(type, Collection.class)) {
                if (ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[0], Dto.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isCollection = true;
                    route.isDto = true;
                    seekInner((Class<? extends Dto>) ClassUtil.getGenericTypes(field)[0], dtoClass, route);
                }

            } else if (ClassUtil.implementsInterface(type, Map.class)) {
                if (ClassUtil.implementsInterface(ClassUtil.getGenericTypes(field)[1], Dto.class)) {
                    RelationRoute route = new RelationRoute(field);
                    route.isMapValue = true;
                    route.isDto = true;
                    seekInner((Class<? extends Dto>) ClassUtil.getGenericTypes(field)[1], dtoClass, route);
                }
            }
            // Deep < < <
        }
    }

    /**
     * dtoK = rev dto
     * dtoV = iterating dto
     */
    private void seekInner(Class<? extends Dto> dtoClassK, Class<? extends Dto> dtoClassV, RelationRoute route) {
        if (dtoClassV.equals(dtoClassK)) {
            return;
        }

        deepSeekDto(dtoClassK);
        Set<Class<? extends Dto>> c = affectedClasses.get(dtoClassK);
        if (ObjectUtil.isEmpty(c)) {
            return;
        }

        for (Class<? extends Dto> affectedClasses : c) {
            Set<Relation> innerRelations = relations.get(affectedClasses);
            if (ObjectUtil.isEmpty(innerRelations)) {
                continue;
            }

            Set<Relation> newRelations = new HashSet<>(10, 1);
            for (Relation innerRelation : innerRelations) {
                if (!innerRelation.fkClass.equals(dtoClassK)) {
                    continue;
                }

                Relation r = new Relation(innerRelation);
                r.addFieldRoute(route);
                r.fkClass = dtoClassV;
                newRelations.add(r);

                Set<Class<? extends Dto>> x = this.affectedClasses.computeIfAbsent(dtoClassV, k -> new HashSet<>(20, 1));
                x.add(r.fkClass);
            }
            innerRelations.addAll(newRelations);
        }
    }

    /**
     * dtoK = rev dto
     * dtoV = iterating dto
     */
    private void addRevRelation(Class<? extends Dto> dtoClassK, Class<? extends Dto> dtoClassV, RelationRoute route) {
        Relation r = new Relation();
        r.addFieldRoute(route);
        r.fkClass = dtoClassV;

        Set<Relation> dtoRelations = relations.computeIfAbsent(dtoClassK, k -> new HashSet<>(10, 1));
        dtoRelations.add(r);

        Set<Class<? extends Dto>> x = affectedClasses.computeIfAbsent(dtoClassV, k -> new HashSet<>(20, 1));
        x.add(dtoClassK);
    }


    public static class Relation {

        public List<RelationRoute> fieldRoute;
        public Class<? extends Dto> fkClass;

        public Relation() {

        }

        public Relation(Relation r) {
            if (r.fieldRoute != null) {
                fieldRoute = new ArrayList<>(r.fieldRoute);
            }
            fkClass = r.fkClass;
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!this.getClass().equals(obj.getClass())) {
                return false;
            }
            return this.hashCode() == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return fkClass.hashCode();
        }
    }


    public static class RelationRoute {

        public String fieldName;

        // fk ---> pk / no combination
        public boolean isPkFk;
        // dto.fk ---> pk / can be combined with isCollection and isMapValue
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