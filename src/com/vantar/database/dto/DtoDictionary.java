package com.vantar.database.dto;

import com.vantar.common.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class DtoDictionary {

    private static final Logger log = LoggerFactory.getLogger(DtoDictionary.class);


    public enum Dbms {
        MONGO,
        SQL,
        ELASTIC,
        NOSTORE,
    }

    // <group, <dtoName, info>>
    private static final Map<String, Map<String, Info>> index = new LinkedHashMap<>();
    private static String tempCategory;


    public static void setCategory(String category) {
        tempCategory = category;
    }

    public static void add(String title, Class<? extends Dto> dtoClass, Integer onUpdateBroadcastMessage, QueryBuilder queryCache) {

        Map<String, Info> catInfo = index.get(tempCategory);
        if (catInfo == null) {
            catInfo = new LinkedHashMap<>();
        }

        Info info = new Info();
        info.queryCache = queryCache;
        info.category = tempCategory;
        info.title = title;
        info.dtoClass = dtoClass;
        info.broadcastMessage = onUpdateBroadcastMessage;

        if (dtoClass.isAnnotationPresent(Mongo.class)) {
            info.dbms = Dbms.MONGO;
        } else if (dtoClass.isAnnotationPresent(Sql.class)) {
            info.dbms = Dbms.SQL;
        } else if (dtoClass.isAnnotationPresent(Elastic.class)) {
            info.dbms = Dbms.ELASTIC;
        } else {
            info.dbms = Dbms.NOSTORE;
        }

        catInfo.put(dtoClass.getSimpleName(), info);
        index.put(tempCategory, catInfo);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, Integer onUpdateBroadcastMessage) {
        add(title, dtoClass, onUpdateBroadcastMessage, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass) {
        add(title, dtoClass, null, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, QueryBuilder queryCache) {
        add(title, dtoClass, null, queryCache);
    }

    public static Map<String, Map<String, Info>> getStructure() {
        return index;
    }

    public static List<Info> getAll(Dbms... dbmses) {
        List<Info> info = new ArrayList<>(100);
        for (Map<String, Info> bucket : index.values()) {
            for (Info item : bucket.values()) {
                if (dbmses.length == 0) {
                    info.add(item);
                    continue;
                }
                for (Dbms dbms : dbmses) {
                    if (dbms.equals(item.dbms)) {
                        info.add(item);
                        break;
                    }
                }
            }
        }
        return info;
    }

    public static List<Info> getAll() {
        List<Info> info = new ArrayList<>(100);
        for (Map<String, Info> bucket : index.values()) {
            info.addAll(bucket.values());
        }
        return info;
    }

    public static String[] getNames(Dbms dbms) {
        List<Info> info = getAll(dbms);
        String[] names = new String[info.size()];
        for (int i = 0; i < info.size(); ++i) {
            names[i] = info.get(i).dtoClass.getSimpleName();
        }
        return names;
    }

    /**
     * Get by class name or storage name
     */
    public static Info get(String name) {
        if (name == null) {
            return null;
        }
        for (Map<String, Info> bucket : index.values()) {
            Info i = bucket.get(name);
            if (i != null) {
                return i;
            }
            for (Info info : bucket.values()) {
                if (info.getDtoInstance().getStorage().equals(name)) {
                    return info;
                }
            }
        }
        return null;
    }

    public static Info get(Class<?> type) {
        return get(type.getSimpleName());
    }

    public static Dto getInstance(String className) {
        if (className == null) {
            return null;
        }
        for (Map<String, Info> bucket : index.values()) {
            Info i = bucket.get(className);
            if (i != null) {
                return i.getDtoInstance();
            }
        }
        return null;
    }

    public static <T extends Dto> T getInstance(Class<T> classType) {
        if (classType == null) {
            return null;
        }
        return ClassUtil.getInstance(classType);
    }


    public static class Info {

        public Dbms dbms;
        public String category;
        public String title;
        public Class<? extends Dto> dtoClass;
        public Integer broadcastMessage;
        public QueryBuilder queryCache;


        public Dto getDtoInstance() {
            try {
                return dtoClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("! failed to create dto instance ({})", dtoClass, e);
                return null;
            }
        }

        public String getDtoClassName() {
            return dtoClass.getSimpleName();
        }

        public String getImportData() {
            String data;
            if (Settings.isLocal()) {
                data = FileUtil.getFileContentFromClassPath("/data/import/" + StringUtil.toKababCase(getDtoClassName()) + "-local");
                if (StringUtil.isNotEmpty(data)) {
                    return data;
                }
            }
            return FileUtil.getFileContentFromClassPath("/data/import/" + StringUtil.toKababCase(getDtoClassName()));
        }
    }
}