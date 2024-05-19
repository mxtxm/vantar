package com.vantar.database.dependency;

import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
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
 *              *k         v
 * @Depends(Role.class, Dto.class)
 * public Map<Long/Role, ?> roleIds;
 *              k         *v
 * @Depends(Dto.class, Role.class) // NOT SUPPORTED
 * public Map< ?, Long/Role> roleIds;
 *              *k        *v
 * @Depends(Role.class, Feature.class) // NOT SUPPORTED
 * public Map<Long/Role, Long/Role> roleIds;
 */
public class DataDependencyMongo {

    private final Dto dto;
    private final Map<Class<? extends Dto>, Set<ReverseRelationMap.Relation>> allRelations;
    private Event event;
    private int limit = 7;
    private String filterDto;
    private int recursion = 0;


    public void setLimit(int l) {
        limit = l;
    }

    public void setFilterDto(String filterDto) {
        this.filterDto = filterDto;
    }

    public DataDependencyMongo(Dto dto) {
        this.dto = dto;
        ReverseRelationMap map = new ReverseRelationMap();
        allRelations = map.getRelations();
    }

    public void throwOnFirstDependency() throws VantarException {
        Set<ReverseRelationMap.Relation> relations = allRelations.get(dto.getClass());
        if (ObjectUtil.isEmpty(relations)) {
            return;
        }
        for (ReverseRelationMap.Relation relation : relations) {
            StringBuilder colBuff = new StringBuilder(30);
            for (ReverseRelationMap.RelationRoute route : relation.fieldRoute) {
                colBuff.append(route.fieldName).append('.');
            }
            ReverseRelationMap.RelationRoute lastField = relation.fieldRoute.get(relation.fieldRoute.size() - 1);
            QueryBuilder q = new QueryBuilder(DtoDictionary.getInstance(relation.fkClass));

            String col;
            if (lastField.isPkFk || lastField.isCollection) {
                // > > > fk or collection
                if (lastField.isDto) {
                    colBuff.append("id");
                } else {
                    colBuff.setLength(colBuff.length() - 1);
                }
                col = colBuff.toString();
                if (lastField.isCollection) {
                    q.condition().in(col, dto.getId());
                } else {
                    q.condition().equal(col, dto.getId());
                }
                // fk or collection < < <
            } else if (lastField.isMapKey) {
                // > > > map
                colBuff.setLength(colBuff.length() - 1);
                col = colBuff.toString();
                q.condition().mapKeyExists(col, dto.getId());
                // map < < <
            } else {
                continue;
            }

            if (Db.modelMongo.exists(q)) {
                throw new InputException(VantarKey.DELETE_DEPENDANT_DATA_ERROR, relation.fkClass.getSimpleName(), col);
            }
        }
    }

    public void delete(Event e) throws VantarException {
        throwOnFirstDependency();
        e.delete(dto);
    }

    public void deleteCascade(Event e) throws VantarException {
        event = e;
        deleteCascadeRec(dto);
    }

    private void deleteCascadeRec(Dto dto) throws VantarException {
        Set<ReverseRelationMap.Relation> relations = allRelations.get(dto.getClass());
        if (ObjectUtil.isEmpty(relations)) {
            return;
        }
        for (ReverseRelationMap.Relation relation : relations) {
            StringBuilder colBuff = new StringBuilder(30);
            for (ReverseRelationMap.RelationRoute route : relation.fieldRoute) {
                colBuff.append(route.fieldName).append('.');
            }
            ReverseRelationMap.RelationRoute lastField = relation.fieldRoute.get(relation.fieldRoute.size() - 1);
            QueryBuilder q = new QueryBuilder(DtoDictionary.getInstance(relation.fkClass));

            String col;
            if (lastField.isPkFk || lastField.isCollection) {
                // > > > fk or collection
                if (lastField.isDto) {
                    colBuff.append("id");
                } else {
                    colBuff.setLength(colBuff.length() - 1);
                }
                col = colBuff.toString();
                if (lastField.isCollection) {
                    q.condition().in(col, dto.getId());
                } else {
                    q.condition().equal(col, dto.getId());
                }
                // fk or collection < < <
            } else if (lastField.isMapKey) {
                // > > > map
                colBuff.setLength(colBuff.length() - 1);
                col = colBuff.toString();
                q.condition().mapKeyExists(col, dto.getId());
                // map < < <
            } else {
                continue;
            }

            try {
                for (Dto dtoDep : Db.modelMongo.getData(q)) {
                    deleteCascadeRec(dtoDep);
                }
                event.delete(dto);
            } catch (NoContentException e) {
                event.delete(dto);
            }
        }
    }

    public List<Dependency> getDependencies() {
        return getDependencies(dto);
    }

    private List<Dependency> getDependencies(Dto dto) {
        ++recursion;
        Set<ReverseRelationMap.Relation> relations = allRelations.get(dto.getClass());
        if (ObjectUtil.isEmpty(relations)) {
            return new ArrayList<>(1);
        }
        List<Dependency> dependencies = new ArrayList<>(100);

        for (ReverseRelationMap.Relation relation : relations) {
            if (recursion == 1 && filterDto != null && !relation.fkClass.getSimpleName().equalsIgnoreCase(filterDto)) {
                continue;
            }

            StringBuilder colBuff = new StringBuilder(30);
            for (ReverseRelationMap.RelationRoute route : relation.fieldRoute) {
                colBuff.append(route.fieldName).append('.');
            }
            ReverseRelationMap.RelationRoute lastField = relation.fieldRoute.get(relation.fieldRoute.size() - 1);
            QueryBuilder q = new QueryBuilder(DtoDictionary.getInstance(relation.fkClass));
            q.limit(limit);

            String col;
            if (lastField.isPkFk || lastField.isCollection) {
                // > > > fk or collection
                if (lastField.isDto) {
                    colBuff.append("id");
                } else {
                    colBuff.setLength(colBuff.length() - 1);
                }
                col = colBuff.toString();
                if (lastField.isCollection) {
                    q.condition().in(col, dto.getId());
                } else {
                    q.condition().equal(col, dto.getId());
                }
                // fk or collection < < <
            } else if (lastField.isMapKey) {
                // > > > map
                colBuff.setLength(colBuff.length() - 1);
                col = colBuff.toString();
                q.condition().mapKeyExists(col, dto.getId());
                // map < < <
            } else {
                continue;
            }

            try {
                List<Dto> data = Db.modelMongo.getData(q);
                Dependency d = new Dependency(dto, data.get(0));
                dependencies.add(d);
                d.items = new LinkedHashMap<>(data.size(), 1);
                for (Dto dtoDep : data) {
                    d.items.put(
                        dtoDep.getId() + ": " + dtoDep.getPresentationValue(", "),
                        getDependencies(dtoDep)
                    );
                }
            } catch (NoContentException ignore) {

            } catch (VantarException e) {
                ServiceLog.log.error("! {}", dto.getClass().getSimpleName(), e);
            }
        }
        return dependencies;
    }


    public interface Event {

        void delete(Dto dto) throws VantarException;
    }


    public static class Dependency {

        public String baseClass;
        public String blockerClass;
        // <id (title), RECURSIVE>
        public Map<String, List<Dependency>> items;


        public Dependency (Dto baseDto, Dto blockerDto) {
            baseClass = baseDto.getClass().getSimpleName();
            blockerClass = blockerDto.getClass().getSimpleName();
        }

        @Override
        public String toString() {
            return Json.d.toJsonPretty(this);
        }
    }
}
