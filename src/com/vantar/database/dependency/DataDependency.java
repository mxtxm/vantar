package com.vantar.database.dependency;

import com.vantar.business.CommonRepoMongo;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.util.object.*;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;


public class DataDependency {

    private static final Logger log = LoggerFactory.getLogger(DataDependency.class);

    /**
     * get dependencies on (dto, id)
     */
    public static List<Dependants> getDependencies(Dto dto, long id) {
        List<Dependants> dependencies = new ArrayList<>();

        DtoDictionary.Dbms dbms;
        if (dto.getClass().isAnnotationPresent(Mongo.class)) {
            dbms = DtoDictionary.Dbms.MONGO;
        } else {
            return dependencies;
        }

        for (DtoDictionary.Info hostDtoInfo : DtoDictionary.getAll(dbms, DtoDictionary.Dbms.NOSTORE)) {
            for (Field hostField : hostDtoInfo.dtoClass.getFields()) {
                if (!DtoBase.isDataField(hostField)) {
                    continue;
                }

                Depends depends = hostField.getAnnotation(Depends.class);
                if (depends != null && depends.value().equals(dto.getClass())) {
                    putAnnotatedDependencies(
                        dbms,
                        dto, id,
                        hostDtoInfo, hostField,
                        dependencies,
                        ""
                    );
                    continue;
                }

                // todo: recursive fields?
                //if (!ObjectUtil.implementsInterface(hostField.getType(), Dto.class)) {
                  //  continue;
                //}

                putNotAnnotatedDependencies(
                    dbms,
                    dto, id,
                    hostDtoInfo,
                    hostDtoInfo, hostField,
                    dependencies,
                    "",
                    new HashSet<>()
                );
            }
        }

        return dependencies;
    }

    /**
     * with @depends annotation:
     *      Long or Collection<Long> (fkId fields)
     *      Dto or Collection<Dto>   (fkId fields based on Dto.id)
     */
    private static void putAnnotatedDependencies(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String prefix) {

        Class<?> hostFieldType = hostField.getType();
        String hostFieldName = prefix + hostField.getName();

        if (ClassUtil.implementsInterface(hostFieldType, Collection.class)) {

            /*
             * (3) field is Collection<Long> and items are fks to "target dto"
             */
            Class<?> gClass = ClassUtil.getGenericTypes(hostField)[0];
            if (gClass.equals(Long.class)) {
                Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), hostFieldName, id);
                if (d != null) {
                    dependencies.add(d);
                }
                return;
            }

            /*
             * (8) field is Collection<Dto> where
             *     Dto-class="target dto".class and the objects are copies of some "target dto" records
             */
            if (gClass.equals(dto.getClass())) {
                Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), hostFieldName + id, id);
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
            Dependants d = getDependantDataEquals(dbms, hostDtoInfo.getDtoInstance(), hostFieldName, id);
            if (d != null) {
                dependencies.add(d);
            }
            return;
        }
        /*
         * (8) field is Dto where
         *     Dto-class="target dto".class and the object is a copy of a "target dto" record
         */
        if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
            Dependants d = getDependantDataEquals(dbms, hostDtoInfo.getDtoInstance(), hostFieldName + ".id", id);
            if (d != null) {
                dependencies.add(d);
            }
        }
    }

    /**
     * with @depends annotation:
     *      Long or Collection<Long> (fkId fields)
     *      Dto or Collection<Dto>   (fkId fields based on Dto.id)
     */
    private static void putAnnotatedDependenciesX(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String prefix) {

        Class<?> hostFieldType = hostField.getType();
        String hostFieldName = prefix + hostField.getName();

        if (ClassUtil.implementsInterface(hostFieldType, Collection.class)) {

            /*
             * (3) field is Collection<Long> and items are fks to "target dto"
             */
            Class<?> gClass = ClassUtil.getGenericTypes(hostField)[0];
            if (gClass.equals(Long.class)) {
                Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), hostFieldName, id);
                if (d != null) {
                    dependencies.add(d);
                }
                return;
            }

            /*
             * (8) field is Collection<Dto> where
             *     Dto-class="target dto".class and the objects are copies of some "target dto" records
             */
            if (gClass.equals(dto.getClass())) {
                Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), hostFieldName + id, id);
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
            Dependants d = getDependantDataEquals(dbms, hostDtoInfo.getDtoInstance(), hostFieldName, id);
            if (d != null) {
                dependencies.add(d);
            }
            return;
        }
        /*
         * (8) field is Dto where
         *     Dto-class="target dto".class and the object is a copy of a "target dto" record
         */
        if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
            Dependants d = getDependantDataEquals(dbms, hostDtoInfo.getDtoInstance(), hostFieldName + ".id", id);
            if (d != null) {
                dependencies.add(d);
            }
        }
    }

    private static void putNotAnnotatedDependencies(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info firstHostDtoInfo,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String prefix,
        Set<Class<?>> breadCrumb) {

        Class<?> hostFieldType = hostField.getType();
        String hostFieldName = hostField.getName();

        if (ClassUtil.implementsInterface(hostFieldType, Collection.class)) {
            Class<?> gClass = ClassUtil.getGenericTypes(hostField)[0];

            if (ClassUtil.implementsInterface(gClass, Dto.class)) {

                /*
                 * (5) field is Collection<Dto> where generic type is "target dto".class
                 */
                if (hostFieldType.equals(dto.getClass())) {
                    // do nothing because the object is embedded (require explicit @Dependency)
                    return;
                }

                /*
                 * (6) field is Collection<Dto>
                 */
                if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
//                    Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), prefix + hostFieldName + "._id", id);
//                    if (d != null) {
//                        dependencies.add(d);
//                    }
//                    return;

//                  for (host)
//                    putCrawlDependencies(
//                        dbms,
//                        dto, id,
//                        hostDtoInfo, hostField,
//                        dependencies,
//                        new HashSet<>(),
//                        hostFieldName + '.'
//                    );
                }

                return;
            }
        }

        /*
         * (2) field is a Dto and is the same type as the "target dto".class
         */
        if (hostFieldType.equals(dto.getClass())) {
            // do nothing because the object is embedded (require explicit @Dependency)
            return;
        }

        /*
         * (4) field is a Dto > crawl inside it until a "target dto" dependency or object is found
         */
        if (ClassUtil.implementsInterface(hostFieldType, Dto.class)) {
            putCrawlDependencies(
                dbms,
                dto, id,
                firstHostDtoInfo,
                hostDtoInfo, hostField,
                dependencies,
                prefix + hostFieldName + '.',
                breadCrumb
            );
        }
    }

    private static void putCrawlDependencies(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info firstHostDtoInfo,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String queryFieldName,
        Set<Class<?>> breadCrumb) {

        if (breadCrumb.contains(hostField.getType())) {
            return;
        }
        breadCrumb.add(hostField.getType());

        DtoDictionary.Info info = DtoDictionary.get(hostField.getType());
        if (info == null) {
            log.error("! undefined dto({})", hostField.getType());
            return;
        }
        Dto instance = info.getDtoInstance();
        if (instance == null) {
            log.error("! not instance from dto({})", hostField.getType());
            return;
        }

        for (Field innerField : instance.getFields()) {

            Depends depends = innerField.getAnnotation(Depends.class);
            if (depends != null && depends.value().equals(dto.getClass())) {
                putAnnotatedDependencies(
                    dbms,
                    dto, id,
                    firstHostDtoInfo, innerField,
                    dependencies,
                    queryFieldName
                );
                continue;
            }

            if (!ClassUtil.implementsInterface(innerField.getType(), Dto.class)) {
                continue;
            }

            putNotAnnotatedDependencies(
                dbms,
                dto, id,
                firstHostDtoInfo,
                hostDtoInfo, innerField,
                dependencies,
                queryFieldName,
                breadCrumb
            );
        }
    }

    private static Dependants getDependantDataEquals(DtoDictionary.Dbms dbms, Dto dto, String fieldName, long fk) {
        return getDependantData(dbms, dto, fieldName, fk, false);
    }

    private static Dependants getDependantDataIn(DtoDictionary.Dbms dbms, Dto dto, String fieldName, long fk) {
        return getDependantData(dbms, dto, fieldName, fk, true);
    }

    // todo: if cached maybe use cache instead of db
    private static Dependants getDependantData(DtoDictionary.Dbms dbms, Dto dto, String fieldName, long fk, boolean in) {
log.error("\n\nQUEYYY\n\n{} {} {} {}", dto.getClass().getSimpleName(), fieldName, fk, in);
        QueryBuilder q = new QueryBuilder(dto);
        if (in) {
            q.condition().in(fieldName, fk);
        } else {
            q.condition().equal(fieldName, fk);
        }

        if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            try {
                List<Dto> dtos = CommonRepoMongo.getData(q);
                return new Dependants(dto.getClass().getName(), dtos);
            } catch (DatabaseException e) {
                log.error("! query failed {}({}, {})", dto.getClass().getName(), fieldName, fk);
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