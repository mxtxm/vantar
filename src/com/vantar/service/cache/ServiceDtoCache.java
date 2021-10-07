package com.vantar.service.cache;

import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.service.Services;
import org.slf4j.*;
import java.util.*;


public class ServiceDtoCache implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceDtoCache.class);
    private Map<Class<?>, Map<Long, Dto>> cache;

    public Boolean onEndSetNull;


    public void start() {
        cache = Collections.synchronizedMap(new LinkedHashMap<>());
        for (DtoDictionary.Info info : DtoDictionary.getAll()) {
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

    public Map<Long, Dto> getMap(Class<? extends Dto> tClass) {
        return cache.get(tClass);
    }

    public <T extends Dto> List<T> getList(Class<T> tClass) {
        List<T> data = new ArrayList<>();
        for (Dto item : cache.get(tClass).values()) {
            data.add((T) item);
        }
        return data;
    }

    public <T extends Dto> T getDto(Class<T> tClass, Long id) {
        if (id == null) {
            return null;
        }
        Map<Long, Dto> data = cache.get(tClass);
        if (data == null) {
            return null;
        }
        return (T) data.get(id);
    }

    public void update(String dtoName) {
        update(DtoDictionary.get(dtoName));
    }

    private void update(DtoDictionary.Info info) {
        if (info == null || !info.dtoClass.isAnnotationPresent(Cache.class)) {
            return;
        }

        Dto dto = info.getDtoInstance();
        List<Dto> dtos = null;

        if (info.dtoClass.isAnnotationPresent(Mongo.class)) {
            try {
                dtos = info.queryCache == null ? CommonRepoMongo.getAll(dto) : CommonRepoMongo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (DatabaseException e) {
                log.error("! dto cache failed {}", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Sql.class)) {
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                dtos = info.queryCache == null ? repo.getAll(dto) : repo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (DatabaseException e) {
                log.error("! dto cache failed {}", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Elastic.class)) {
            try {
                dtos = info.queryCache == null ? CommonRepoElastic.getAll(dto) : CommonRepoElastic.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (DatabaseException e) {
                log.error("! dto cache failed {}", dto.getClass().getSimpleName(), e);
            }
        } else {
            return;
        }
        if (dtos == null) {
            dtos = new ArrayList<>();
        }

        Map<Long, Dto> data = Collections.synchronizedMap(new LinkedHashMap<>(dtos.size() + 15));

        for (Dto d : dtos) {
            data.put(d.getId(), d);
        }

        cache.put(info.dtoClass, data);
    }
}
