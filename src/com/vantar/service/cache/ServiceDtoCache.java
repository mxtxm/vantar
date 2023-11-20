package com.vantar.service.cache;

import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.MongoQuery;
import com.vantar.database.query.QueryResult;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.util.object.*;
import org.slf4j.*;
import java.util.*;


public class ServiceDtoCache implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceDtoCache.class);
    private Map<Class<?>, Map<Long, ? extends Dto>> cache;

    public Boolean onEndSetNull;


    public void start() {
        List<DtoDictionary.Info> dtos = DtoDictionary.getAll();
        cache = Collections.synchronizedMap(new LinkedHashMap<>(dtos.size()));
        for (DtoDictionary.Info info : dtos) {
            update(info);
        }
    }

    public void stop() {
        cache = null;
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    public Set<Class<?>> getCachedClasses() {
        return cache.keySet();
    }

    public <D extends Dto> Map<Long, D> getMap(Class<D> tClass) {
        return (Map<Long, D>) cache.get(tClass);
    }

    public <D extends Dto, A extends Dto> Map<Long, A> getMap(Class<D> tClass, Class<A> asClass) {
        Map<Long, D> originalData = (Map<Long, D>) cache.get(tClass);
        if (originalData == null) {
            return new HashMap<>(1, 1);
        }
        Map<Long, A> data = new HashMap<>(originalData.size(), 1);
        for (Map.Entry<Long, D> e : originalData.entrySet()) {
            A a = ClassUtil.getInstance(asClass);
            if (a != null) {
                a.set(e.getValue());
                data.put(e.getKey(), a);
            }
        }
        return data;
    }

    public <T extends Dto> List<T> getList(Class<T> tClass) {
        Map<Long, T> values = (Map<Long, T>) cache.get(tClass);
        if (values == null) {
            return new ArrayList<>(1);
        }
        return new ArrayList<>(values.values());
    }

    public <D extends Dto, A extends Dto> List<A> getList(Class<D> tClass, Class<A> asClass) {
        Map<Long, D> values = (Map<Long, D>) cache.get(tClass);
        if (values == null) {
            return new ArrayList<>(1);
        }
        List<A> data = new ArrayList<>(values.size());
        for (Dto item : values.values()) {
            A a = ClassUtil.getInstance(asClass);
            if (a != null) {
                a.set(item);
                data.add(a);
            }
        }
        return data;
    }

    public <T extends Dto> T getDto(Class<T> tClass, Long id) {
        if (id == null) {
            return null;
        }
        Map<Long, T> data = (Map<Long, T>) cache.get(tClass);
        if (data == null) {
            return null;
        }
        return data.get(id);
    }

    public <D extends Dto, A extends Dto> A getDto(Class<D> tClass, Class<A> asClass, Long id) {
        if (id == null) {
            return null;
        }
        Map<Long, D> data = (Map<Long, D>) cache.get(tClass);
        if (data == null) {
            return null;
        }
        A a = ClassUtil.getInstance(asClass);
        if (a == null) {
            return null;
        }
        a.set(data.get(id));
        return a;
    }

    public void update(String dtoName) {
        update(DtoDictionary.get(dtoName));
    }

    public void update(Class<?> dtoClass) {
        update(DtoDictionary.get(dtoClass.getSimpleName()));
    }

    private void update(DtoDictionary.Info info) {
        if (info == null || !info.dtoClass.isAnnotationPresent(Cache.class)) {
            return;
        }

        Dto dto = info.getDtoInstance();
        List<Dto> dtos = null;

        if (info.dtoClass.isAnnotationPresent(Mongo.class)) {
            try {
                dtos = info.queryCache == null ?
                    MongoQuery.getAllData(dto).asList() :
                    MongoQuery.getData(info.queryCache).asList();
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                log.error(" !! dto cache failed ({})\n", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Sql.class)) {
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                dtos = info.queryCache == null ? repo.getAll(dto) : repo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                log.error(" !! dto cache failed ({})\n", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Elastic.class)) {
            try {
                dtos = info.queryCache == null ? CommonRepoElastic.getAll(dto) : CommonRepoElastic.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                log.error(" !! dto cache failed ({})\n", dto.getClass().getSimpleName(), e);
            }
        } else {
            return;
        }
        if (dtos == null) {
            dtos = new ArrayList<>(1);
        }

        Map<Long, Dto> data = Collections.synchronizedMap(new LinkedHashMap<>(dtos.size() + 15));

        for (Dto d : dtos) {
            data.put(d.getId(), d);
        }

        cache.put(info.dtoClass, data);
        log.info("   >> cache loaded ({})", info.dtoClass.getSimpleName());
    }

    public static <T extends Dto> List<T> asList(Class<T> tClass) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getList(tClass);
    }

    public static <A extends Dto> List<A> asList(Class<? extends Dto> tClass, Class<A> asClass) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getList(tClass, asClass);
    }

    public static <D extends Dto> Map<Long, D> asMap(Class<D> tClass) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getMap(tClass);
    }

    public static <A extends Dto> Map<Long, A> asMap(Class<? extends Dto> tClass, Class<A> asClass) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getMap(tClass, asClass);
    }

    public static <T extends Dto> T asDto(Class<T> tClass, Long id) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getDto(tClass, id);
    }

    public static <A extends Dto> A asDto(Class<? extends Dto> tClass, Class<A> asClass, Long id) throws ServiceException {
        return Services.get(ServiceDtoCache.class).getDto(tClass, asClass, id);
    }

    public boolean isOk() {
        return true;
    }
}