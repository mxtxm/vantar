package com.vantar.util.object;

import com.vantar.util.string.StringUtil;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;


public class ClassUtil {

    public static Class<?> typeToClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type typeX = ((ParameterizedType) type).getRawType();
            return typeX instanceof Class<?> ? (Class<?>) typeX : typeToClass(typeX);
        }
        if (type instanceof GenericArrayType) {
            Type typeX = ((GenericArrayType) type).getGenericComponentType();
            return typeX instanceof Class<?> ? (Class<?>) typeX : typeToClass(typeX);
        }
        if (type instanceof TypeVariable<?>) {
            return ((TypeVariable<?>) type).getGenericDeclaration().getClass();
        }

        String typeName = type.getTypeName();
        if (StringUtil.contains(typeName, "Map")) {
            return Map.class;
        }
        if (StringUtil.contains(typeName, "List")) {
            return List.class;
        }
        if (StringUtil.contains(typeName, "Set")) {
            return Set.class;
        }

        Object instance = getInstance(typeName);
        return instance == null ? null : instance.getClass();
    }

    public static boolean equals(Type type1, Type type2) {
        return typeToClass(type1) == typeToClass(type2);
    }

    public static Class<?>[] getGenericTypes(Field field) {
        if (field == null) {
            return null;
        }

        Type t = field.getGenericType();
        if (!(t instanceof ParameterizedType)) {
            ObjectUtil.log.warn("! field({}) does not have generics.", field.getName());
            return null;
        }

        Type[] types = ((ParameterizedType) t).getActualTypeArguments();
        Class<?>[] classes = new Class[types.length];
        for (int i = 0, typesLength = types.length; i < typesLength; i++) {
            classes[i] = typeToClass(types[i]);
        }
        return classes;
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String className) {
        try {
            return (T) Class.forName(className).getConstructor().newInstance(new Object[] {});
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
            | InstantiationException e) {

            return null;
        }
    }

    public static <T> T getInstance(Class<T> tClass) {
        try {
            return (T) tClass.getConstructor().newInstance(new Object[] {});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return null;
        }
    }

    public static boolean extendsClass(Class<?> theClass, Class<?> superClass) {
        if (theClass == superClass) {
            return true;
        }
        return superClass.isAssignableFrom(theClass);
    }

    public static boolean implementsInterface(Class<?> type, Class<?> i) {
        if (type == i) {
            return true;
        }

        for (Class<?> c : type.getInterfaces()) {
            if (c == i) {
                return true;
            }
        }

        Class<?> c = type.getSuperclass();
        while (c != null) {
            if (c == i) {
                return true;
            }
            for (Class<?> t : c.getInterfaces()) {
                if (t == i) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }

        return false;
    }

    public static List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            return new ArrayList<>();
        }

        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(packageName.replace('.', '/'));
        } catch (IOException e) {
            ObjectUtil.log.error("! package({})", packageName, e);
            return new ArrayList<>();
        }

        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            dirs.add(new File(resources.nextElement().getFile()));
        }

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().contains(".")) {
                    continue;
                }
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                } catch (ClassNotFoundException e) {
                    ObjectUtil.log.error("! package({}) class not found", packageName, e);
                }
            }
        }

        return classes;
    }
}
