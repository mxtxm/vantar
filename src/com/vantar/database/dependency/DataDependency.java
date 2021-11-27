package com.vantar.database.dependency;

import com.vantar.business.CommonRepoMongo;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.util.object.ObjectUtil;
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

        for (DtoDictionary.Info hostDtoInfo : DtoDictionary.getAll(dbms)) {
            for (Field hostField : hostDtoInfo.dtoClass.getFields()) {
                if (!DtoBase.isDataField(hostField)) {
                    continue;
                }

                if (hostField.isAnnotationPresent(Depends.class)
                    && hostField.getAnnotation(Depends.class).value().equals(dto.getClass())) {
                    putAnnotatedDependencies(
                        dbms,
                        dto, id,
                        hostDtoInfo, hostField,
                        dependencies,
                        ""
                    );
                    continue;
                }

                if (!ObjectUtil.implementsInterface(hostField.getType(), Dto.class)) {
                    continue;
                }

                putNotAnnotatedDependencies(
                    dbms,
                    dto, id,
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
     * with @depends annotation: can only be used on Long or Collection<Long> (fkId fields)
     */
    private static void putAnnotatedDependencies(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String prefix) {

        /*
         * (3) field is Collection<Long> and items are fks to dto
         */
        if (ObjectUtil.implementsInterface(hostField.getType(), Collection.class)) {
            Class<?> gClass = ObjectUtil.getFieldGenericTypes(hostField)[0];
            if (gClass.equals(Long.class)) {
                Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), prefix + hostField.getName(), + id);
                if (d != null) {
                    dependencies.add(d);
                }
            }
            return;
        }

        /*
         * (1) field is Long and is a fk to dto
         */
        Dependants d = getDependantDataEquals(dbms, hostDtoInfo.getDtoInstance(), prefix + hostField.getName(), id);
        if (d != null) {
            dependencies.add(d);
        }
    }

    private static void putNotAnnotatedDependencies(
        DtoDictionary.Dbms dbms,
        Dto dto, Long id,
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String prefix,
        Set<Class<?>> breadCrumb) {

        Class<?> hostFieldType = hostField.getType();
        String hostFieldName = hostField.getName();

        if (ObjectUtil.implementsInterface(hostFieldType, Collection.class)) {
            Class<?> gClass = ObjectUtil.getFieldGenericTypes(hostField)[0];

            if (ObjectUtil.implementsInterface(gClass, Dto.class)) {

                /*
                 * (5) field is Collection<Dto> and is the same generic type as the dto
                 */
                if (hostFieldType.equals(dto.getClass())) {
                    Dependants d = getDependantDataIn(dbms, hostDtoInfo.getDtoInstance(), prefix + hostFieldName + "._id", id);
                    if (d != null) {
                        dependencies.add(d);
                    }
                    return;
                }

                /*
                 * (6) field is Collection<Dto>
                 */
                if (ObjectUtil.implementsInterface(hostFieldType, Dto.class)) {
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
         * (2) field is a Dto and is the same type as the dto
         */
        if (hostFieldType.equals(dto.getClass())) {
            // do nothing because the object is embedded
        }

        /*
         * (4) field is a Dto
         */
        if (ObjectUtil.implementsInterface(hostFieldType, Dto.class)) {
            putCrawlDependencies(
                dbms,
                dto, id,
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
        DtoDictionary.Info hostDtoInfo, Field hostField,
        List<Dependants> dependencies,
        String queryFieldName,
        Set<Class<?>> breadCrumb) {

        if (breadCrumb.contains(hostField.getType())) {
            return;
        }
        breadCrumb.add(hostField.getType());

        for (Field innerField : hostDtoInfo.getDtoInstance().getFields()) {

            if (innerField.isAnnotationPresent(Depends.class)
                && innerField.getAnnotation(Depends.class).value().equals(dto.getClass())) {

                DtoDictionary.Info innerDtoInfo = DtoDictionary.get(innerField.getAnnotation(Depends.class).value());
                log.error(">>>>>>>>>>{}", innerDtoInfo);

                log.error(">>>>{} {}\n {}\n{}\n{}\n{}", innerField.getAnnotation(Depends.class).value(), dto.getClass(),
                    innerDtoInfo.title, innerField,
                    dependencies,
                    queryFieldName
                );

                putAnnotatedDependencies(
                    dbms,
                    dto, id,
                    innerDtoInfo, innerField,
                    dependencies,
                    queryFieldName
                );
                continue;
            }

            if (!ObjectUtil.implementsInterface(hostField.getType(), Dto.class)) {
                continue;
            }

            DtoDictionary.Info innerDtoInfo = DtoDictionary.get(hostField.getType());
log.error(">>>>{}\n{}\n{}\n{}\n{}\n{}\n{}\n{}  a",

    dbms,
    dto, id,
    innerDtoInfo, innerField,
    dependencies,
    queryFieldName,
    breadCrumb

);
            putNotAnnotatedDependencies(
                dbms,
                dto, id,
                innerDtoInfo, innerField,
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