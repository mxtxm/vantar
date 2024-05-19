package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import java.lang.reflect.Field;
import java.util.*;


public class ModelMongoChecking extends ModelCommon {

    public static void throwUniqueViolation(Dto dto, DbMongo db) throws VantarException {
        for (Field field : dto.getClass().getFields()) {
            if (field.isAnnotationPresent(Unique.class)) {
                try {
                    if (!db.isUnique(dto, field.getName())) {
                        throw new InputException(VantarKey.UNIQUE, field.getName());
                    }
                } catch (VantarException e) {
                    ServiceLog.error(ModelMongoChecking.class, " !! unique dto={} field={}", dto, field.getName(), e);
                    throw e;
                }
                continue;
            }

            if (field.isAnnotationPresent(UniqueCi.class)) {
                try {
                    if (!db.isUnique(dto, field.getName())) {
                        throw new InputException(VantarKey.UNIQUE, field.getName());
                    }
                } catch (VantarException e) {
                    ServiceLog.error(ModelMongoChecking.class, " !! unique dto={} field={}", dto, field.getName(), e);
                    throw e;
                }
            }
        }

        if (dto.getClass().isAnnotationPresent(UniqueGroup.class)) {
            for (String group : dto.getClass().getAnnotation(UniqueGroup.class).value()) {
                try {
                    if (!db.isUnique(dto, StringUtil.split(group, VantarParam.SEPARATOR_COMMON))) {
                        throw new InputException(VantarKey.UNIQUE, "(" + group + ")");
                    }
                } catch (VantarException e) {
                    ServiceLog.error(ModelMongoChecking.class, " !! unique dto={} field={}", dto, "(" + group + ")", e);
                    throw e;
                }
            }
        }
    }

    /**
     * relation: fk does not have reference ---> throw error
     * parent/child relation: parent.id=child.id ---> throw error
     */
    public static void throwDependencyViolations(Dto dto, DbMongo db) throws VantarException {
        Long id = dto.getId();
        for (Field field : dto.getClass().getFields()) {
            Object value;
            try {
                value = field.get(dto);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (value == null) {
                continue;
            }

            Depends annotationDepends = field.getAnnotation(Depends.class);
            if (annotationDepends != null) {
                throwRelationViolations(dto, annotationDepends, field, value, id, db);
            }

            DependsValue annotationDependsValue = field.getAnnotation(DependsValue.class);
            if (annotationDependsValue != null) {
                throwValueDependencyViolations(annotationDependsValue, field, value, db);
            }
        }
    }

    private static void throwRelationViolations(Dto dto, Depends annotation, Field field, Object value, Long id, DbMongo db)
        throws VantarException {

        Class<? extends Dto>[] dependencies = annotation.value();

        /* > > >
         * @Depends(Role.class)
         * public Long roleId;
         * @Depends(Role.class)
         * public Role role; // check role.id
         */
        if (value instanceof Long || value instanceof Dto) {
            // parent child dependency
            if (id != null && dependencies[0].equals(dto.getClass())) {
                throwParentChildViolation(id, value, field.getName());
            }
            // fk dependency
            throwRelationViolation(dto, dependencies[0], value, field.getName(), db);
            return;
        }
        // < < <

        /* > > >
         * @Depends(Role.class)
         * public List<Long> roleIds;
         * @Depends(Role.class)
         * public List<Role> roles;  // check role[i].id
         */
        if (value instanceof Collection) {
            // parent child dependency
            if (id != null && dependencies[0].equals(dto.getClass())) {
                for (Object item : (Collection<?>) value) {
                    throwParentChildViolation(id, item, field.getName());
                }
            }
            // fk dependency
            for (Object item : (Collection<?>) value) {
                throwRelationViolation(dto, dependencies[0], item, field.getName(), db);
            }
            return;
        }
        // < < <

        /* > > >
         *              k         *v
         * @Depends(Dto.class, Role.class)
         * public Map<?, Long/Role> roleIds;
         *              *k         v
         * @Depends(Role.class, Dto.class)
         * public Map<Long/Role, ?> roleIds;
         *              *k        *v
         * @Depends(Role.class, Feature.class)
         * public Map<Long/Role, Long/Role> roleIds;
         */
        if (value instanceof Map) {
            // parent child dependency
            if (id != null) {
                if (dependencies[0].equals(dto.getClass())) {
                    for (Object item : ((Map<?, ?>) value).keySet()) {
                        throwParentChildViolation(id, item, field.getName());
                    }
                }
                if (dependencies[1].equals(dto.getClass())) {
                    for (Object item : ((Map<?, ?>) value).values()) {
                        throwParentChildViolation(id, item, field.getName());
                    }
                }
            }
            // fk dependency
            if (!dependencies[0].equals(Dto.class)) {
                for (Object item : ((Map<?, ?>) value).keySet()) {
                    throwRelationViolation(dto, dependencies[0], item, field.getName(), db);
                }
            }
            if (!dependencies[1].equals(Dto.class)) {
                for (Object item : ((Map<?, ?>) value).values()) {
                    throwRelationViolation(dto, dependencies[1], item, field.getName(), db);
                }
            }
        }
        // < < <
    }

    private static void throwRelationViolation(Dto dto, Class<? extends Dto> fkClass, Object value, String name, DbMongo db)
        throws VantarException {

        if (value == null) {
            return;
        }
        Long fkValue = value instanceof Long ? (Long) value : ((Dto) value).getId();
        if (fkValue == null) {
            return;
        }
        try {
            if (!db.exists(DtoBase.getStorage(fkClass), new Document(DbMongo.ID, fkValue))) {
                throw new InputException(VantarKey.MISSING_REFERENCE, name, value);
            }
        } catch (VantarException e) {
            ServiceLog.error(ModelMongoChecking.class, " !! could not check reference dto={} field={} value={}"
                , dto, name, fkValue, e);
            throw e;
        }
    }

    private static void throwParentChildViolation(Long id, Object value, String name) throws InputException {
        if ((value instanceof Long && id.equals(value)) || (value instanceof Dto && id.equals(((Dto) value).getId()))) {
            throw new InputException(VantarKey.MISSING_REFERENCE, name);
        }
    }

    private static void throwValueDependencyViolations(DependsValue annotation, Field field, Object value, DbMongo db)
        throws VantarException {

        /* > > >
         * @DependsValue(dto = Frequency.class, field = "name")
         * public Number/String frequency;
         */
        if (value instanceof String || value instanceof Number) {
            throwValueRelation(annotation.dto(), annotation.field(), value, field.getName(), db);
            return;
        }
        // < < <

        /* > > >
         * @DependsValue(dto = Frequency.class, field = "name")
         * public List<Number/String> frequencies;
         */
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                throwValueRelation(annotation.dto(), annotation.field(), item, field.getName(), db);
            }
            return;
        }
        // < < <

        /* > > >
         *              k         *v
         * @DependsValue(dto = Frequency.class, field = "name", isValue = true)
         * public Map<?, Long/String> frequencies;
         *              *k         v
         * @DependsValue(dto = Frequency.class, field = "name", isKey = true)
         * public Map<Long/String, ?> frequencies;
         *              *k        *v
         * @DependsValue(dto = Frequency.class, field = "name", isKey = true, isValue = true)
         * public Map<Long/String, Long/String> frequencies;
         */
        if (value instanceof Map) {
            // fk dependency
            if (!annotation.isKey()) {
                for (Object item : ((Map<?, ?>) value).keySet()) {
                    throwValueRelation(annotation.dto(), annotation.field(), item, field.getName(), db);
                }
            }
            if (!annotation.isValue()) {
                for (Object item : ((Map<?, ?>) value).values()) {
                    throwValueRelation(annotation.dto(), annotation.field(), item, field.getName(), db);
                }
            }
        }
        // < < <
    }

    private static void throwValueRelation(Class<? extends Dto> fClass, String fField, Object value, String name, DbMongo db)
        throws VantarException {

        try {
            if (!db.exists(DtoBase.getStorage(fClass), new Document(fField, value))) {
                throw new InputException(VantarKey.MISSING_REFERENCE, name, value);
            }
        } catch (VantarException e) {
            ServiceLog.error(ModelMongoChecking.class, " !! could not check reference dto={} field={} value={}"
                , fClass, name, value, e);
            throw e;
        }
    }
}