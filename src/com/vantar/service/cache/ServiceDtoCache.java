package com.vantar.service.cache;

import com.vantar.business.*;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.NoContentException;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.object.ClassUtil;
import java.util.*;


public class ServiceDtoCache implements Services.Service {

    private Map<Class<?>, Map<Long, ? extends Dto>> cache;
    private volatile boolean pause = false;
    private volatile boolean serviceUp = false;
    private volatile boolean lastSuccess = true;
    private List<String> logs;

    // > > > service params injected from config
    // < < <

    // > > > service methods

    @Override
    public void start() {
        List<DtoDictionary.Info> dtos = DtoDictionary.getAll();
        cache = Collections.synchronizedMap(new LinkedHashMap<>(dtos.size()));
        for (DtoDictionary.Info info : dtos) {
            update(info);
        }
        serviceUp = true;
        pause = false;
    }

    @Override
    public void stop() {
        cache = null;
        serviceUp = false;
    }

    @Override
    public void pause() {
        pause = true;
    }

    @Override
    public void resume() {
        pause = false;
    }

    @Override
    public boolean isUp() {
        return serviceUp;
    }

    @Override
    public boolean isOk() {
        return serviceUp && lastSuccess;
    }

    @Override
    public boolean isPaused() {
        return pause;
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

    private void setLog(String msg) {
        if (logs == null) {
            logs = new ArrayList<>(5);
        }
        logs.add(msg);
    }

    // service methods < < <

    public void update(String dtoName) {
        update(DtoDictionary.get(dtoName));
    }

    public void update(Class<?> dtoClass) {
        update(DtoDictionary.get(dtoClass.getSimpleName()));
    }

    private void update(DtoDictionary.Info info) {
        if (pause) {
            return;
        }
        lastSuccess = true;
        if (info == null || !info.dtoClass.isAnnotationPresent(Cache.class)) {
            return;
        }

        Dto dto = info.getDtoInstance();
        List<Dto> dtos = null;

        if (info.dtoClass.isAnnotationPresent(Mongo.class)) {
            try {
                dtos = info.queryCache == null ?
                    Db.mongo.getAllData(dto).asList() :
                    Db.mongo.getData(info.queryCache).asList();
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                ServiceLog.fatal(ServiceDtoCache.class, " !! dto cache failed ({})", dto.getClass().getSimpleName(), e);
                lastSuccess = false;
                setLog(e.getMessage());
            }
        } else if (info.dtoClass.isAnnotationPresent(Sql.class)) {
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                dtos = info.queryCache == null ? repo.getAll(dto) : repo.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                ServiceLog.fatal(ServiceDtoCache.class, " !! dto cache failed ({})", dto.getClass().getSimpleName(), e);
                lastSuccess = false;
                setLog(e.getMessage());
            }
        } else if (info.dtoClass.isAnnotationPresent(Elastic.class)) {
            try {
                dtos = info.queryCache == null ? CommonRepoElastic.getAll(dto) : CommonRepoElastic.getData(info.queryCache);
            } catch (NoContentException ignore) {

            } catch (Exception e) {
                ServiceLog.fatal(ServiceDtoCache.class, " !! dto cache failed ({})", dto.getClass().getSimpleName(), e);
                lastSuccess = false;
                setLog(e.getMessage());
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
        ServiceLog.log.info("  -> cache loaded ({})", info.dtoClass.getSimpleName());
    }

    public Set<Class<?>> getCachedClasses() {
        return cache.keySet();
    }

    @SuppressWarnings("unchecked")
    public <D extends Dto> Map<Long, D> getMap(Class<D> tClass) {
        return (Map<Long, D>) cache.get(tClass);
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public <T extends Dto> List<T> getList(Class<T> tClass) {
        Map<Long, T> values = (Map<Long, T>) cache.get(tClass);
        if (values == null) {
            return new ArrayList<>(1);
        }
        return new ArrayList<>(values.values());
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    public static <T extends Dto> List<T> asList(Class<T> tClass) {
        return Services.get(ServiceDtoCache.class).getList(tClass);
    }

    public static <A extends Dto> List<A> asList(Class<? extends Dto> tClass, Class<A> asClass) {
        return Services.get(ServiceDtoCache.class).getList(tClass, asClass);
    }

    public static <D extends Dto> Map<Long, D> asMap(Class<D> tClass) {
        return Services.get(ServiceDtoCache.class).getMap(tClass);
    }

    public static <A extends Dto> Map<Long, A> asMap(Class<? extends Dto> tClass, Class<A> asClass) {
        return Services.get(ServiceDtoCache.class).getMap(tClass, asClass);
    }

    public static <T extends Dto> T asDto(Class<T> tClass, Long id) {
        return Services.get(ServiceDtoCache.class).getDto(tClass, id);
    }

    public static <A extends Dto> A asDto(Class<? extends Dto> tClass, Class<A> asClass, Long id) {
        return Services.get(ServiceDtoCache.class).getDto(tClass, asClass, id);
    }
}