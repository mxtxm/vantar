package com.vantar.service.cache;

import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.util.object.*;
import org.slf4j.*;
import java.util.*;


public class ServiceDtoCache implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceDtoCache.class);
    private Map<Class<?>, Map<Long, Dto>> cache;

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

    public Map<Long, Dto> getMap(Class<? extends Dto> tClass) {
        return cache.get(tClass);
    }

    public <A extends Dto> Map<Long, A> getMap(Class<? extends Dto> tClass, Class<A> asClass) {
        Map<Long, Dto> originalData = cache.get(tClass);
        if (originalData == null) {
            return new HashMap<>(1);
        }
        Map<Long, A> data = new HashMap<>(originalData.size());
        for (Map.Entry<Long, Dto> e : originalData.entrySet()) {
            A a = ClassUtil.getInstance(asClass);
            if (a != null) {
                a.set(e.getValue());
                data.put(e.getKey(), a);
            }
        }
        return data;
    }

    public <T extends Dto> List<T> getList(Class<T> tClass) {
        List<T> data = new ArrayList<>();
        Map<Long, Dto> values = cache.get(tClass);
        if (values == null) {
            return data;
        }
        for (Dto item : values.values()) {
            data.add((T) item);
        }
        return data;
    }

    public <A extends Dto> List<A> getList(Class<? extends Dto> tClass, Class<A> asClass) {
        List<A> data = new ArrayList<>();
        Map<Long, Dto> values = cache.get(tClass);
        if (values == null) {
            return data;
        }
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
        Map<Long, Dto> data = cache.get(tClass);
        if (data == null) {
            return null;
        }
        return (T) data.get(id);
    }

    public <A extends Dto> A getDto(Class<? extends Dto> tClass, Class<A> asClass, Long id) {
        if (id == null) {
            return null;
        }
        Map<Long, Dto> data = cache.get(tClass);
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
                dtos = info.queryCache == null ? CommonRepoMongo.getAll(dto) : CommonRepoMongo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                log.error("! dto cache failed {}", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Sql.class)) {
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                dtos = info.queryCache == null ? repo.getAll(dto) : repo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                log.error("! dto cache failed {}", dto.getClass().getSimpleName(), e);
            }
        } else if (info.dtoClass.isAnnotationPresent(Elastic.class)) {
            try {
                dtos = info.queryCache == null ? CommonRepoElastic.getAll(dto) : CommonRepoElastic.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
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
