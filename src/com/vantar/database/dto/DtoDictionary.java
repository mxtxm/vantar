package com.vantar.database.dto;

import com.vantar.common.*;
import com.vantar.database.common.Db;
import com.vantar.database.query.QueryBuilder;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class DtoDictionary {

    private static List<String> indexGroup;
    private static Map<String, Info> indexDto;


    public static void init(int dtoCount) {
        indexGroup = new ArrayList<>(dtoCount);
        indexDto = new LinkedHashMap<>(dtoCount, 1);
    }

    public static void setGroup(String group) {
        indexGroup.add(group);
    }

    public static void add(int order, String title, Class<? extends Dto> dtoClass) {
        Info info = new Info();
        info.order = order;
        info.hidden = false;
        info.title = title;
        info.dtoClass = dtoClass;
        add(info);
    }

    public static void addHidden(int order, String title, Class<? extends Dto> dtoClass) {
        Info info = new Info();
        info.order = order;
        info.hidden = true;
        info.title = title;
        info.dtoClass = dtoClass;
        add(info);
    }

    @SuppressWarnings("unchecked")
    public static void add(Info info) {
        info.group = indexGroup.get(indexGroup.size() - 1);
        if (info.dtoClass.isAnnotationPresent(Mongo.class)) {
            info.dbms = Db.Dbms.MONGO;
        } else if (info.dtoClass.isAnnotationPresent(Sql.class)) {
            info.dbms = Db.Dbms.SQL;
        } else if (info.dtoClass.isAnnotationPresent(Elastic.class)) {
            info.dbms = Db.Dbms.ELASTIC;
        } else {
            info.dbms = Db.Dbms.NOSTORE;
        }
        indexDto.put(info.dtoClass.getSimpleName(), info);
        for (Class<?> innerClass : info.dtoClass.getDeclaredClasses()) {
            if (!ClassUtil.implementsInterface(innerClass, Dto.class)) {
                continue;
            }
            Info innerInfo = new Info(info, (Class<? extends Dto>) innerClass);
            innerInfo.hidden = true;
            indexDto.put(info.dtoClass.getSimpleName() + "." + innerClass.getSimpleName(), innerInfo);
        }
    }

    /**
     * <group, <dto, info>>
     * hidden not included
     */
    public static Map<String, Map<String, Info>> getManifest() {
        Map<String, Map<String, Info>> manifest = new LinkedHashMap<>(indexGroup.size(), 1);
        for (String g : indexGroup) {
            manifest.put(g, new LinkedHashMap<>(20, 1));
        }
        for (Map.Entry<String, Info> d : indexDto.entrySet()) {
            Info i = d.getValue();
            if (i.hidden) {
                continue;
            }
            manifest.get(i.group).put(d.getKey(), i);
        }
        return manifest;
    }

    /**
     * hidden not included
     */
    public static List<Info> getAll(Db.Dbms... dbmses) {
        List<Info> info = new ArrayList<>(indexDto.size());
        for (Info i : indexDto.values()) {
            if (i.hidden) {
                continue;
            }
            if (dbmses.length > 0) {
                for (Db.Dbms dbms : dbmses) {
                    if (dbms.equals(i.dbms)) {
                        info.add(i);
                        break;
                    }
                }
            } else {
                info.add(i);
            }
        }
        return info;
    }

    /**
     * hidden not included
     */
    public static List<String> getDtoClassNames(Db.Dbms dbms) {
        List<String> names = new ArrayList<>(indexDto.size());
        for (Info i : getAll(dbms)) {
            if (i.hidden) {
                continue;
            }
            names.add(i.dtoClass.getSimpleName());
        }
        return names;
    }

    public static List<Info> getSubClasses(String className) {
        className = className + ".";
        List<Info> info = new ArrayList<>(indexDto.size());
        for (Map.Entry<String, Info> e : indexDto.entrySet()) {
            Info i = e.getValue();
            if (!i.hidden) {
                continue;
            }
            if (e.getKey().startsWith(className)) {
                info.add(i);
            }
        }
        return info;
    }

    public static Info get(Class<?> type) {
        return get(type.getSimpleName());
    }

    /**
     * Get by class name or storage name
     */
    public static Info get(String name) {
        if (name == null) {
            return null;
        }
        if (name.contains("$") || name.contains(".")) {
            name = StringUtil.replace(name, '$', '.');
        }
        Info i = indexDto.get(name);
        if (i != null) {
            return i;
        }
        for (Info j : indexDto.values()) {
            if (name.equals(j.getStorage())) {
                return i;
            }
        }
        return null;
    }

    public static Dto getInstance(String className) {
        Info i = indexDto.get(className);
        return i == null ? null : i.getDtoInstance();
    }

    public static <T extends Dto> T getInstance(Class<T> classType) {
        if (classType == null) {
            return null;
        }
        return ClassUtil.getInstance(classType);
    }


    public static class Info {

        public int order;
        public boolean hidden;
        public Db.Dbms dbms;
        public String group;
        public String title;
        public Class<? extends Dto> dtoClass;
        public Integer broadcastMessage;
        public QueryBuilder queryCache;

        public Info() {

        }

        public Info(Info i, Class<? extends  Dto> dtoClass) {
            dbms = i.dbms;
            group = i.group;
            title = i.title;
            this.dtoClass = dtoClass;
        }

        public Dto getDtoInstance() {
            try {
                return dtoClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                ServiceLog.log.error("! failed to create dto instance ({})", dtoClass, e);
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

        public String getStorage() {
            return DtoBase.getStorage(dtoClass);
        }

        @Override
        public String toString() {
            return dtoClass.getSimpleName();
        }
    }
}