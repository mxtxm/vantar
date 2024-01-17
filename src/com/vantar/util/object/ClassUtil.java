package com.vantar.util.object;

import com.vantar.database.dto.Generics;
import com.vantar.util.collection.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import org.slf4j.*;

/**
 * Class utilities
 */
public class ClassUtil {

    /**
     * Convert type to class
     * @param type to convert
     * @return converted
     */
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

    /**
     * Check type equality
     * @param type1 type to check
     * @param type2 type to check
     * @return true if types are the same
     */
    public static boolean equals(Type type1, Type type2) {
        return typeToClass(type1) == typeToClass(type2);
    }

    /**
     * Get generic types of a generic field
     * @param field field to get generics from (if is a generic type)
     * @return array of generic types (null if field == null or field is not generic)
     */
    public static Class<?>[] getGenericTypes(Field field) {
        if (field == null) {
            return null;
        }

        Generics generics = field.getAnnotation(Generics.class);
        if (generics != null) {
            return generics.value();
        }

        Type t = field.getGenericType();
        if (!(t instanceof ParameterizedType)) {
            ObjectUtil.log.warn(" ! field({}, {}) does not have generics.", field.getName(), field.getType());
            return null;
        }

        Type[] types = ((ParameterizedType) t).getActualTypeArguments();
        Class<?>[] classes = new Class[types.length];
        for (int i = 0, typesLength = types.length; i < typesLength; i++) {
            classes[i] = typeToClass(types[i]);
        }
        return classes;
    }

    /**
     * Get class from class name
     * @param className package.className
     * @return class type
     */
    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get instance from class name
     * @param className package.className
     * @return instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String className) {
        try {
            return (T) Class.forName(className).getConstructor().newInstance(new Object[] {});
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
            | InstantiationException e) {
            return null;
        }
    }

    /**
     * Get instance from class
     * @param classType the class
     * @return instance
     */
    public static <T> T getInstance(Class<T> classType) {
        try {
            return (T) classType.getConstructor().newInstance(new Object[] {});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return null;
        }
    }

    /**
     * Check if a class can be morphed by a class or interface
     * @param theClass base class
     * @param superClass super class/interface to check against
     * @return
     * true if theClass == superClass
     * true if theClass extends superClass (recursive)
     * true if theClass implements superClass (recursive)
     */
    public static boolean isInstantiable(Class<?> theClass, Class<?> superClass) {
        if (theClass == superClass) {
            return true;
        }
        return superClass.isAssignableFrom(theClass);
    }

    /**
     * Check if a class extends a class
     * @param theClass base class
     * @param superClass super class to check against
     * @return
     * true if theClass == superClass
     * true if theClass extends superClass (recursive)
     * false if theClass implements superClass (recursive)
     */
    public static boolean extendsClass(Class<?> theClass, Class<?> superClass) {
        if (theClass == superClass) {
            return true;
        }
        Class<?> s = theClass.getSuperclass();
        if (s == null) {
            return false;
        }
        if (s.equals(superClass)) {
            return true;
        }
        return extendsClass(s, superClass);
    }

    /**
     * Check if a class implements an interface
     * @param theClass base class
     * @param theInterface super class to check against
     * @return
     * true if theClass == theInterface
     * false if theClass extends theInterface (recursive)
     * true if any superClass of theClass implements theInterface (recursive)
     * true if theClass implements theInterface (recursive)
     */
    public static boolean implementsInterface(Class<?> theClass, Class<?> theInterface) {
        if (theClass == theInterface) {
            return true;
        }

        for (Class<?> i : theClass.getInterfaces()) {
            if (i.equals(theInterface)) {
                return true;
            }
            boolean impls = implementsInterface(i, theInterface);
            if (impls) {
                return true;
            }
        }

        Class<?> c = theClass.getSuperclass();
        if (c != null) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.equals(theInterface)) {
                    return true;
                }
                boolean impls = implementsInterface(i, theInterface);
                if (impls) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get a list of classes that are in a package
     * @param packageName package name
     * @return list of classes
     */
    public static List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            return new ArrayList<>(1);
        }

        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(packageName.replace('.', '/'));
        } catch (IOException e) {
            ObjectUtil.log.error(" !! package({})\n", packageName, e);
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

    public static List<Class<?>> getClasses(String packageName, Class<? extends Annotation> annotation) {
        List<Class<?>> classes = getClasses(packageName);
        classes.removeIf(c -> !c.isAnnotationPresent(annotation));
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
                    ObjectUtil.log.error(" !! package({}) class not found\n", packageName, e);
                }
            }
        }

        return classes;
    }

    /**
     * Get memory size of a class static data
     * @param theClass the class
     * @return bytes
     */
    public static int sizeOfStatic(Class<?> theClass) {
        int size = 0;
        for (Field field : theClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                size += com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(field.get(null));
            } catch (IllegalAccessException ignore) {

            }
        }
        return size;
    }

    /**
     * Get class by class name (search in all packages)
     * @param className the class name
     * @return the class or null if not found
     */
    public static Class<?> getClassFromPackage(String className) {
        return getClassFromPackage(className, null);
    }

    /**
     * Get class by class name and part of package name
     * @param className the class name
     * @param packageNameStartsWith part of package name
     * @return the class or null if not found
     */
    public static Class<?> getClassFromPackage(String className, String packageNameStartsWith) {
        Package[] ps = Package.getPackages();
        for (Package p : ps) {
            String pName = p.getName();
            if (packageNameStartsWith != null && !pName.startsWith(packageNameStartsWith)) {
                continue;
            }
            Class<?> c = ClassUtil.getClass(pName + "." + className);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    /**
     * Call static method on a class - a param can not be null
     * @param packageClassMethod package.name.ClassName.methodName
     * @param params method params
     * @return method return value
     * @throws Throwable
     */
    public static Object callStaticMethod(String packageClassMethod, Object... params) throws Throwable {
        String[] parts = StringUtil.split(packageClassMethod, '.');
        Class<?>[] types = new Class[params.length];
        for (int i = 0, l = params.length; i < l; i++) {
            types[i] = params[i].getClass();
            if (ClassUtil.implementsInterface(types[i], Map.class)) {
                types[i] = Map.class;
            } else if (ClassUtil.implementsInterface(types[i], List.class)) {
                types[i] = List.class;
            }
        }
        try {
            Class<?> tClass = Class.forName(ExtraUtils.join(parts, '.', parts.length - 1));
            Method method = tClass.getMethod(parts[parts.length - 1], types);
            return method.invoke(null, params);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            ObjectUtil.log.error(" !! {}\n", packageClassMethod, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Call static method on a class - a param can be be null, params must be even
     * @param packageClassMethod package.name.ClassName.methodName
     * @param typesAndParams method params: type1, value1, type2, value2,... type must be the Class type of object
     * @return method return value
     * @throws Throwable
     */
    public static Object callStaticMethodWithTypes(String packageClassMethod, Object... typesAndParams) throws Throwable {
        String[] parts = StringUtil.split(packageClassMethod, '.');
        int s = typesAndParams.length / 2;
        Class<?>[] types = new Class[s];
        Object[] params = new Object[s];
        for (int i = 0, j = 0, k = 0, l = typesAndParams.length; i < l; i++) {
            if (i % 2 == 0) {
                types[j++] = (Class<?>) typesAndParams[i];
            } else {
                params[k++] = typesAndParams[i];
            }
        }
        try {
            Class<?> tClass = Class.forName(ExtraUtils.join(parts, '.', parts.length - 1));
            Method method = tClass.getMethod(parts[parts.length - 1], types);
            return method.invoke(null, params);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            ObjectUtil.log.error(" !! {}\n", packageClassMethod, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Call static method on a class - a param can not be null
     * @param theClass the class
     * @param params method params
     * @return method return value
     * @throws Throwable
     */
    public static Object callStaticMethod(Class<?> theClass, String methodName, Object... params) throws Throwable {
        Class<?>[] types = new Class[params.length];
        for (int i = 0, l = params.length; i < l; i++) {
            types[i] = params[i].getClass();
            if (ClassUtil.implementsInterface(types[i], Map.class)) {
                types[i] = Map.class;
            } else if (ClassUtil.implementsInterface(types[i], List.class)) {
                types[i] = List.class;
            }
        }
        try {
            Method method = theClass.getMethod(methodName, types);
            return method.invoke(null, params);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            ObjectUtil.log.error(" !! {}.{}\n", theClass.getSimpleName(), methodName, e);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Check if a method belongs to the class and is not inherited
     * @param theClass class
     * @param method method name
     * @return trye if method is defined in the class
     */
    public static boolean isClassMethod(Class<?> theClass, String method, Class<?>... params) {
        try {
            return theClass.equals(theClass.getMethod(method, params).getDeclaringClass());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static void checkControllers(Logger log) {
        log.info(" > checking controller classes...");
        int c = 0;
        int e = 0;
        for (Class<?> controllerClass : ClassUtil.getClasses("com.proda.web.api")) {
            ++c;
            WebServlet webServlet = controllerClass.getAnnotation(WebServlet.class);
            if (webServlet == null) {
                log.error(" ! 'WebServlet' annotation missing from controller {}", controllerClass.getSimpleName());
                continue;
            }

            for (String path : webServlet.value()) {
                if (ClassUtil.extendsClass(controllerClass, RouteToMethod.class)) {

                    String[] parts = StringUtil.split(path, '/');
                    StringBuilder methodName = new StringBuilder(path.length()).append(parts.length < 3 ? "index" : parts[2]);
                    for (int i = 3, l = parts.length; i < l; ++i) {
                        String part = parts[i];
                        methodName.append(Character.toUpperCase(part.charAt(0)));
                        methodName.append(part.substring(1).toLowerCase());
                    }
                    String controllerMethod = methodName.toString();
                    if (!ClassUtil.isClassMethod(controllerClass, controllerMethod, Params.class, HttpServletResponse.class)) {
                        log.error(" ! missing link --> {}.{}", controllerClass.getName(), controllerMethod);
                        ++e;
                    }

                } else if (ClassUtil.extendsClass(controllerClass, RouteToMethodParam.class)) {
                    String[] parts = StringUtil.split(path, '/');
                    StringBuilder methodName = new StringBuilder(path.length()).append(StringUtil.isEmpty(parts[1])
                        ? "index" : parts[1]);

                    for (int i = 2, l = parts.length; i < l; ++i) {
                        String part = parts[i];
                        methodName.append(Character.toUpperCase(part.charAt(0)));
                        methodName.append(part.substring(1).toLowerCase());
                    }
                    String controllerMethod = methodName.toString();
                    if (!ClassUtil.isClassMethod(controllerClass, controllerMethod, Params.class, HttpServletResponse.class)) {
                        log.error(" ! missing link --> {}.{}", controllerClass.getName(), controllerMethod);
                        ++e;
                    }
                }
            }
        }
        log.info(" < checked {} controller classes. found {} missing links.", c, e);
    }
}
