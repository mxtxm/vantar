package com.vantar.database.dependency;

import com.vantar.business.CommonModelMongo;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.*;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;


public class DataDependency {

    private static final Logger log = LoggerFactory.getLogger(DataDependency.class);
    private static final int RECURSIVE_THRESHOLD = 10;

    private final Class<? extends Dto> dtoClass;
    private DtoDictionary.Dbms dbms;
    private Dto dependantDto;
    private final Long id;
    private List<Dependants> dependencies;
    private int recursiveCount;

    private Class<?> dtoClass1;
    private Class<?> dtoClass2;
    private Class<?> dtoClass3;


    public DataDependency(Dto dto) {
        id = dto.getId();
        dtoClass = dto.getClass();
        if (dtoClass.isAnnotationPresent(Mongo.class)) {
            dbms = DtoDictionary.Dbms.MONGO;
        }
    }

    public List<Dependants> getDependencies() {
        dependencies = new ArrayList<>();
        if (id == null || dbms == null) {
            return dependencies;
        }

        dependantDto = null;
        for (DtoDictionary.Info dtoInfo : DtoDictionary.getAll(dbms)) {
            dependantDto = dtoInfo.getDtoInstance();
            for (Field field : dtoInfo.dtoClass.getFields()) {
                if (!DtoBase.isDataField(field)) {
                    continue;
                }

                // explicit dependency (the only way to identify dependency)
                Depends depends = field.getAnnotation(Depends.class);
                if (depends != null && depends.value().equals(dtoClass)) {
                    putAnnotatedDependencies(field, "");
                    continue;
                }

                // dont crawl itself, if has self reference then @Depends would have caught it
                if (dtoInfo.dtoClass.equals(dtoClass)) {
                    continue;
                }

                // crawl to find dependency (find any reference that leads to @Depends)
                recursiveCount = 1;
                dtoClass1 = null;
                dtoClass2 = null;
                dtoClass3 = null;
                putNotAnnotatedDependencies(field, "");
            }
        }

        return dependencies;
    }

    private void putAnnotatedDependencies(Field dependentField, String prefix) {
        Class<?> dependentFieldType = dependentField.getType();
        String dependentFieldName = prefix + dependentField.getName();

        if (CollectionUtil.isCollection(dependentFieldType)) {
            /*
             * (3) field is Collection<Long> and items are fks to "target dto"
             */
            Class<?> gClass = ClassUtil.getGenericTypes(dependentField)[0];
            if (gClass.equals(Long.class)) {
                Dependants d = getDependantDataIn(dependentFieldName);
                if (!Dependants.isEmpty(d)) {
                    dependencies.add(d);
                }
                return;
            }

            /*
             * (4) field is Collection<Dto> where
             *     Dto-class="target dto".class and the objects are copies of some "target dto" records
             */
            if (gClass.equals(dtoClass)) {
                Dependants d = getDependantDataIn(dependentFieldName + ".id");
                if (!Dependants.isEmpty(d)) {
                    dependencies.add(d);
                }
                return;
            }
        }

        /*
         * (1) field is Long and is a fk to "target dto"
         */
        if (dependentFieldType.equals(Long.class)) {
            Dependants d = getDependantDataEquals(dependentFieldName);
            if (!Dependants.isEmpty(d)) {
                dependencies.add(d);
            }
            return;
        }

        /*
         * (2) field is Dto where
         *     Dto-class="target dto".class and the object is a copy of a "target dto" record
         */
        if (ClassUtil.isInstantiable(dependentFieldType, Dto.class)) {
            Dependants d = getDependantDataEquals(dependentFieldName + ".id");
            if (!Dependants.isEmpty(d)) {
                dependencies.add(d);
            }
        }
    }

    private void putNotAnnotatedDependencies(Field dependentField, String prefix) {
        if (recursiveCount > RECURSIVE_THRESHOLD) {
            return;
        }
        ++recursiveCount;

        String dependentFieldName = dependentField.getName();
        Class<?> dependentFieldType = dependentField.getType();

        if (CollectionUtil.isCollection(dependentFieldType)) {
            Class<?> gClass = ClassUtil.getGenericTypes(dependentField)[0];

            /*
             * (8) field is Collection<Dto>
             */
            if (ClassUtil.isInstantiable(gClass, Dto.class)) {
                if (endRecursion(gClass)) {
                    return;
                }
                putCrawlDependencies(gClass, prefix + dependentFieldName + '.');
            }
            return;
        }

        /*
         * (7) field is a Dto > crawl inside it until a "target dto" dependency is found
         */
        if (ClassUtil.isInstantiable(dependentFieldType, Dto.class)) {
            if (!endRecursion(dependentFieldType)) {
                putCrawlDependencies(dependentField.getType(), prefix + dependentFieldName + '.');
            }
        }
    }

    private boolean endRecursion(Class <?> dependentFieldType) {
        if (dtoClass1 == null) {
            dtoClass1 = dependentFieldType;
        } else if (dtoClass2 == null) {
            if (dependentFieldType.equals(dtoClass1)) {
                dtoClass2 = dependentFieldType;
            } else {
                dtoClass1 = dependentFieldType;
            }
        } else if (dtoClass3 == null) {
            if (dependentFieldType.equals(dtoClass2)) {
                dtoClass3 = dependentFieldType;
            } else {
                dtoClass1 = dependentFieldType;
                dtoClass2 = null;
            }
        } else {
            return true;
        }
        return false;
    }

    private void putCrawlDependencies(Class<?> fieldType, String queryFieldName) {
        DtoDictionary.Info fieldInfo = DtoDictionary.get(fieldType);
        if (fieldInfo == null) {
            if (fieldType.isAnnotationPresent(NoStore.class)) {
                log.debug("! ({}) not defined in DtoDictionary", fieldType);
            }
            return;
        }
        Dto instance = fieldInfo.getDtoInstance();
        if (instance == null) {
            return;
        }

        for (Field innerField : instance.getFields()) {
            Depends depends = innerField.getAnnotation(Depends.class);
            if (depends != null && depends.value().equals(dtoClass)) {
                putAnnotatedDependencies(innerField, queryFieldName);
                continue;
            }

            Class<?> innerFieldType = innerField.getType();
            if (!ClassUtil.isInstantiable(innerField.getType(), Dto.class)) {
                if (CollectionUtil.isCollection(innerFieldType)) {
                    Class<?> gClass = ClassUtil.getGenericTypes(innerField)[0];
                    if (!ClassUtil.isInstantiable(gClass, Dto.class)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            putNotAnnotatedDependencies(innerField, queryFieldName);
        }
    }

    private Dependants getDependantDataEquals(String fieldName) {
        return getDependantData(fieldName, false);
    }

    private Dependants getDependantDataIn(String fieldName) {
        return getDependantData(fieldName, true);
    }

    private Dependants getDependantData(String fieldName, boolean in) {
        QueryBuilder q = new QueryBuilder(dependantDto);
        if (in) {
            q.condition().in(fieldName, id);
        } else {
            q.condition().equal(fieldName, id);
        }
        if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            try {
                List<Dto> dtos = CommonModelMongo.getData(q);
                return new Dependants(dependantDto.getClass().getName(), dtos);
            } catch (NoContentException ignore) {

            } catch (VantarException e) {
                log.error("! query failed {}({}, {})", dependantDto.getClass().getName(), fieldName, id);
            }
        }
        return null;
    }


    public static class Dependants {

        public String name;
        public List<Dto> dtos;

        public Dependants(String name, List<Dto> dtos) {
            this.name = name;
            this.dtos = dtos;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Dto dto : dtos) {
                sb.append(dto.getId()).append("+ ");
            }
            if (sb.length() > 1) {
                sb.setLength(sb.length() - 2);
            }
            return name + '(' + sb.toString() + ')';
        }

        public static boolean isEmpty(Dependants dependants) {
            return dependants == null || ObjectUtil.isEmpty(dependants.dtos);
        }
    }
}