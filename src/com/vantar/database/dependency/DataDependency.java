package com.vantar.database.dependency;

import com.vantar.business.CommonModelMongo;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.ClassUtil;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;


public class DataDependency {

    private static final Logger log = LoggerFactory.getLogger(DataDependency.class);
    private static final int RECURSIVE_THRESHOLD = 10;

    private final Class<? extends Dto> dtoClass;
    private DtoDictionary.Dbms dbms;
    private Dto dtoToQuery;
    private Long id;
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

    public List<Dependants> getDependencies(long id) {
        this.id = id;
        return getDependencies();
    }

    public List<Dependants> getDependencies() {
        if (id == null || dbms == null) {
            return dependencies;
        }
        dtoToQuery = null;
        dependencies = new ArrayList<>();

        for (DtoDictionary.Info hostDtoInfo : DtoDictionary.getAll(dbms, DtoDictionary.Dbms.NOSTORE)) {
            dtoToQuery = hostDtoInfo.getDtoInstance();
            for (Field hostField : hostDtoInfo.dtoClass.getFields()) {
                if (!DtoBase.isDataField(hostField)) {
                    continue;
                }

                Depends depends = hostField.getAnnotation(Depends.class);
                if (depends != null && depends.value().equals(dtoClass)) {
                    putAnnotatedDependencies(hostField, "");
                    continue;
                }

                recursiveCount = 1;
                dtoClass1 = null;
                dtoClass2 = null;
                dtoClass3 = null;
                putNotAnnotatedDependencies(hostField, "");
            }
        }

        return dependencies;
    }

    private void putAnnotatedDependencies(Field hostField, String prefix) {
        Class<?> hostFieldType = hostField.getType();
        String hostFieldName = prefix + hostField.getName();

        if (CollectionUtil.isCollection(hostFieldType)) {
            /*
             * (3) field is Collection<Long> and items are fks to "target dto"
             */
            Class<?> gClass = ClassUtil.getGenericTypes(hostField)[0];
            if (gClass.equals(Long.class)) {
                Dependants d = getDependantDataIn(hostFieldName);
                if (d != null) {
                    dependencies.add(d);
                }
                return;
            }

            /*
             * (4) field is Collection<Dto> where
             *     Dto-class="target dto".class and the objects are copies of some "target dto" records
             */
            if (gClass.equals(dtoClass)) {
                Dependants d = getDependantDataIn(hostFieldName + ".id");
                if (d != null) {
                    dependencies.add(d);
                }
                return;
            }
        }

        /*
         * (1) field is Long and is a fk to "target dto"
         */
        if (hostFieldType.equals(Long.class)) {
            Dependants d = getDependantDataEquals(hostFieldName);
            if (d != null) {
                dependencies.add(d);
            }
            return;
        }

        /*
         * (2) field is Dto where
         *     Dto-class="target dto".class and the object is a copy of a "target dto" record
         */
        if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
            Dependants d = getDependantDataEquals(hostFieldName + ".id");
            if (d != null) {
                dependencies.add(d);
            }
        }
    }

    private void putNotAnnotatedDependencies(Field hostField, String prefix) {
        if (recursiveCount > RECURSIVE_THRESHOLD) {
            log.debug(" ! ended recursion at {} iterations", recursiveCount);
            log.debug("{} {}", hostField.getName(), prefix);
            return;
        }
        ++recursiveCount;

        String hostFieldName = hostField.getName();
        Class<?> hostFieldType = hostField.getType();

        if (CollectionUtil.isCollection(hostFieldType)) {
            Class<?> gClass = ClassUtil.getGenericTypes(hostField)[0];

            /*
             * (8) field is Collection<Dto>
             */
            if (ClassUtil.implementsInterface(gClass, Dto.class)) {
                if (endRecursion(gClass)) {
                    return;
                }
                putCrawlDependencies(gClass, prefix + hostFieldName + '.');
            }
            return;
        }

        /*
         * (7) field is a Dto > crawl inside it until a "target dto" dependency is found
         */
        if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
            if (!endRecursion(hostFieldType)) {
                putCrawlDependencies(hostField.getType(), prefix + hostFieldName + '.');
            }
        }
    }

    private boolean endRecursion(Class <?> hostFieldType) {
        if (dtoClass1 == null) {
            dtoClass1 = hostFieldType;
        } else if (dtoClass2 == null) {
            if (hostFieldType.equals(dtoClass1)) {
                dtoClass2 = hostFieldType;
            } else {
                dtoClass1 = hostFieldType;
            }
        } else if (dtoClass3 == null) {
            if (hostFieldType.equals(dtoClass2)) {
                dtoClass3 = hostFieldType;
            } else {
                dtoClass1 = hostFieldType;
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
            log.error("! undefined dto({})", fieldType);
            return;
        }
        Dto instance = fieldInfo.getDtoInstance();
        if (instance == null) {
            log.error("! not instance from dto({})", fieldType);
            return;
        }

        for (Field innerField : instance.getFields()) {
            Depends depends = innerField.getAnnotation(Depends.class);
            if (depends != null && depends.value().equals(dtoClass)) {
                putAnnotatedDependencies(innerField, queryFieldName);
                continue;
            }

            Class<?> innerFieldType = innerField.getType();
            if (!ClassUtil.implementsInterface(innerField.getType(), Dto.class)) {
                if (CollectionUtil.isCollection(innerFieldType)) {
                    Class<?> gClass = ClassUtil.getGenericTypes(innerField)[0];
                    if (!ClassUtil.implementsInterface(gClass, Dto.class)) {
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
        QueryBuilder q = new QueryBuilder(dtoToQuery);
        if (in) {
            q.condition().in(fieldName, id);
        } else {
            q.condition().equal(fieldName, id);
        }
        if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            try {
                List<Dto> dtos = CommonModelMongo.getData(q);
                return new Dependants(dtoToQuery.getClass().getName(), dtos);
            } catch (ServerException e) {
                log.error("! query failed {}({}, {})", dtoToQuery.getClass().getName(), fieldName, id);
            } catch (NoContentException ignore) {

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
                sb.append(dto.getId()).append(", ");
            }
            sb.setLength(sb.length() - 2);
            return name + '(' + sb.toString() + ')';
        }
    }
}