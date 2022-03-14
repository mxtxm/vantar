package com.vantar.database.dto;

import com.vantar.common.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.util.file.FileUtil;
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

    private static final Map<String, Map<String, Info>> index = new LinkedHashMap<>();
    private static String tempCategory;


    public static void setCategory(String category) {
        tempCategory = category;
    }

    /**
     * @param command: "present:name,id;insert-exclude:id;update-exclude:id;insert-include:id;update-include:id"
     */
    public static void add(String title, Class<? extends Dto> dtoClass, Integer onUpdateBroadcastMessage
        , String command, QueryBuilder queryCache) {

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

        info.insertExclude = new ArrayList<>();
        info.insertExclude.add("id");
        info.insertExclude.add("create_t");
        info.insertExclude.add("update_t");
        info.updateExclude = new ArrayList<>();
        info.insertExclude.add("id");
        info.insertExclude.add("create_t");
        info.insertExclude.add("update_t");
        info.present = new ArrayList<>();
        try {
            dtoClass.getField("name");
            info.present.add("name");
        } catch (NoSuchFieldException e) {
            info.present.add("id");
        }

        if (StringUtil.isNotEmpty(command)) {
            for (String c : StringUtil.split(command, VantarParam.SEPARATOR_BLOCK)) {
                String[] commandProperties = StringUtil.split(c, VantarParam.SEPARATOR_KEY_VAL);
                String[] properties = StringUtil.split(commandProperties[1], VantarParam.SEPARATOR_COMMON);
                switch (commandProperties[0]) {
                    case "present":
                        info.present = new ArrayList<>();
                        for (String property : properties) {
                            info.present.add(StringUtil.toSnakeCase(property));
                        }
                        break;

                    case "insert-exclude":
                        for (String property : properties) {
                            info.insertExclude.add(StringUtil.toSnakeCase(property));
                        }
                        break;

                    case "insert-include":
                        for (String property : properties) {
                            info.insertExclude.remove(StringUtil.toSnakeCase(property));
                        }
                        break;

                    case "update-exclude":
                        for (String property : properties) {
                            info.updateExclude.add(StringUtil.toSnakeCase(property));
                        }
                        break;

                    case "update-include":
                        for (String property : properties) {
                            info.updateExclude.remove(StringUtil.toSnakeCase(property));
                        }
                        break;
                }
            }
        }

        catInfo.put(dtoClass.getSimpleName(), info);
        index.put(tempCategory, catInfo);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, Integer onUpdateBroadcastMessage, String command) {
        add(title, dtoClass, onUpdateBroadcastMessage, command, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, Integer onUpdateBroadcastMessage) {
        add(title, dtoClass, onUpdateBroadcastMessage, null, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, String command) {
        add(title, dtoClass, null, command, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass) {
        add(title, dtoClass, null, null, null);
    }

    public static void add(String title, Class<? extends Dto> dtoClass, QueryBuilder queryCache) {
        add(title, dtoClass, null, null, queryCache);
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
                for (Dbms dbms : dbmses){
                    if (item.dbms.equals(dbms)) {
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


    public static class Info {

        public Dbms dbms;
        public String category;
        public String title;
        public Class<? extends Dto> dtoClass;
        public List<String> insertExclude;
        public List<String> updateExclude;
        public List<String> present;
        public Integer broadcastMessage;
        public QueryBuilder queryCache;


        public String[] getInsertExclude() {
            return insertExclude.toArray(new String[0]);
        }

        public String[] getUpdateExclude() {
            return updateExclude.toArray(new String[0]);
        }

        public Dto getDtoInstance() {
            try {
                return dtoClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("! failed to create dto instance ({})", dtoClass, e);
            }
            return null;
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