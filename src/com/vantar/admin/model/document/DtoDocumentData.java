package com.vantar.admin.model.document;

import com.vantar.admin.model.AdminDocument;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.*;
import java.util.*;


public class DtoDocumentData {

    public String dto;
    public String format;
    public String enumClass;
    public String[] exclude;
    public Set<String> includeFields;
    public String[] key;
    public String searchParams;
    public String searchResult;
    public String action;
    public String controllerClass;
    public boolean ignoreNoStore = true;


    public String get() {

        // > > >

        if (enumClass != null) {
            Class<?> c = ObjectUtil.getClass(enumClass);
            if (c == null || c.getEnumConstants() == null) {
                return "!!!DOCUMENT CREATION ERROR!!!";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("    JSON\n    {\n");
            for (Object item : c.getEnumConstants()) {
                sb.append("        \"").append(item).append("\",\n");
            }
            sb.append("    }\n");
            return sb.toString();
        }

        // > > >

        if (searchParams != null) {
            String[] parts = StringUtil.split(searchParams, '.');
            String className = parts[parts.length - 1];
            String name = StringUtil.replace(className, '$', '.');
            String hash = StringUtil.replace(className, '$', '-');
            return "* <a href='/admin/document/show/dtos#" + hash + "'>{" + name +
                "}</a> : <a href='/admin/document/show?document=document--webservice--common--search.md'>SEE \"search params\"</a>\n";
        }

        // > > >

        if (searchResult != null) {
            String[] parts = StringUtil.split(searchResult, '.');
            String className = parts[parts.length - 1];
            String name = StringUtil.replace(className, '$', '.');
            String hash = StringUtil.replace(className, '$', '-');
            return
                "<pre>" +
                    "JSON<br/>" +
                    "    if pagination = false in params then <a href='/admin/document/show/dtos#" + hash + "'>[{" + name + "}]</a></br>" +
                    "    if pagination = true in params then {<br/>" +
                    "        List&lt;" + name + "&gt; data: <a href='/admin/document/show/dtos#" + hash + "'>" + name + "</a><br/>" +
                    "        int page: page number<br/>" +
                    "        int length: records per page<br/>" +
                    "        long total: total number of records<br/>" +
                    "    }</pre><br/>\n";
        }

        // > > >

        boolean excludeAll = false;
        if (exclude != null) {
            for (String x : exclude) {
                if (x.equals("all")) {
                    excludeAll = true;
                    break;
                }
            }
        }

        if (includeFields == null) {
            includeFields = new HashSet<>();
        }

        StringBuilder sb = new StringBuilder();
        if (dto != null) {
            Dto obj = ObjectUtil.getInstance(dto);
            if (obj == null) {
                AdminDocument.log.error("! can not create {}", dto);
                return "!!!DOCUMENT CREATION ERROR!!!";
            }

            if (StringUtil.isNotEmpty(format)) {
                if (format.equalsIgnoreCase("list")) {
                    if (dto != null) {
                        String name = StringUtil.replace(obj.getClass().getSimpleName(), '$', '.');
                        sb.append("[ { <a href='/admin/document/show/dtos#").append(name).append("'>")
                            .append(name).append("</a> } ]")
                            .append(" (JSON list may contain fields of object <a href='/admin/document/show/dtos#")
                            .append(name).append("'>").append(name).append("</a>").append(" as described bellow)\n");
                    }
                } else if (format.equalsIgnoreCase("object")) {
                    if (dto != null) {
                        String name = StringUtil.replace(obj.getClass().getSimpleName(), '$', '.');
                        sb.append(" (may contain fields of object <a href='/admin/document/show/dtos#")
                            .append(name).append("'>").append(name).append("</a>").append(" as described bellow)\n");
                    }
                } else {
                    sb.append(format);
                }
            }


            for (String prop : getProperties(obj, exclude, ignoreNoStore)) {
                Class<?> propType = obj.getPropertyType(prop);
                boolean isRequired = obj.hasAnnotation(prop, Required.class);
                boolean isKey = false;
                if (key != null) {
                    for (String k : key) {
                        if (k.equals(prop)) {
                            isKey = true;
                            break;
                        }
                    }
                }

                if (!includeFields.isEmpty() && !includeFields.contains(prop) && !isKey) {
                    continue;
                }
                if (excludeAll && !isKey) {
                    continue;
                }

                sb.append("* ");
                if (isRequired) {
                    sb.append("**");
                }

                if (isKey) {
                    sb.append("<strong class='key'>");
                }

                sb.append(propType.getSimpleName());
                List<String> genericComments = null;
                if (propType == List.class || propType == Map.class || propType == Collection.class) {
                    genericComments = new ArrayList<>();
                    sb.append("&lt;");
                    for (Class<?> genericType : obj.getPropertyGenericTypes(prop)) {
                        String[] parts = StringUtil.split(genericType.getName(), '.');
                        String className = parts[parts.length - 1];
                        String name = StringUtil.replace(className, '$', '.');
                        sb.append(name).append(", ");

                        if (!ObjectUtil.isJavaNative(genericType) && propType != DateTime.class) {
                            setReference(genericComments, genericType, obj.getClass());
                        }
                    }
                    sb.setLength(sb.length() - 2);
                    sb.append("&gt;");
                }

                sb.append(" ").append(prop).append(":");

                if (isRequired) {
                    sb.append("**");
                }


                // > > > comments

                if (isKey) {
                    sb.append("</strong>");
                    if (prop.equals("id")) {
                        sb.append(" ").append(obj.getClass().getSimpleName()).append(" id (primary key) ");
                    }
                    sb.append(" [update/delete condition]");
                }
                if (propType == Location.class) {
                    sb.append(" {\"latitude\": double, \"longitude\": double}");
                }
                if (obj.hasAnnotation(prop, Unique.class)) {
                    sb.append(" [must be unique]");
                }
                if (obj.hasAnnotation(prop, Regex.class)) {
                    sb.append(" [regex=").append(obj.getAnnotation(prop, Regex.class).value()).append("]");
                }
                if (obj.hasAnnotation(prop, Default.class)) {
                    sb.append(" default=").append(obj.getDefaultValue(prop));
                }
                if (obj.hasAnnotation(prop, Localized.class)) {
                    sb.append(" {\"lang1\": \"valueForLang1\", \"lang2\": \"valueForLang2\", ....}");
                }
                if (obj.hasAnnotation(prop, DeLocalized.class)) {
                    sb.append(" value is set based on selected locale.");
                }
                if (prop.endsWith("Id")) {
                    Object foreignObj = ObjectUtil.getInstance(StringUtil.firstCharToUpperCase(StringUtil.remove(prop, "Id")));
                    if (foreignObj != null) {
                        sb.append(" id reference to ");
                        setReference(sb, foreignObj.getClass(), obj.getClass());
                    }
                }
                if (genericComments != null) {
                    for (String item : genericComments) {
                        sb.append(item);
                    }
                }
                if (!ObjectUtil.isJavaNative(propType) && propType != DateTime.class) {
                    setReference(sb, propType, obj.getClass());
                }

                sb.append("\n");
            }

            sb.append("##### sample #####\n");
            try {
                sb.append("<pre>").append(Json.makePretty(getAsJsonExampleDto(obj.getClass()))).append("</pre>\n");
            } catch (Exception e) {
                AdminDocument.log.warn("! JSON error", e);
                sb.append("<pre><br/>").append(getAsJsonExampleDto(obj.getClass())).append("</pre>\n");

            }
        }

        return sb.toString();
    }

    private String getAsJsonExampleList(Field f, Class<?> dto) {
        StringBuilder json = new StringBuilder();
        json.append('[');
        Class<?>[] g = ObjectUtil.getFieldGenericTypes(f);
        if (g.length == 1) {
            Class<?> genericType = g[0];

            if (ObjectUtil.implementsInterface(genericType, Dto.class)) {
                if (genericType.equals(dto)) {
                    json.append("\"{RECURSIVE}\"");
                } else {
                    json.append(getAsJsonExampleDto(genericType));
                }
            } else if (ObjectUtil.extendsClass(genericType, Number.class)) {
                json.append("000");
            } else if (genericType == String.class) {
                json.append("\"STRING\"");
            } else if (genericType == Boolean.class) {
                json.append("true,");
            } else if (genericType == Location.class) {
                json.append("{\"latitude\":000,\"longitude\":000}");
            } else if (genericType == List.class || genericType == Set.class || genericType == Collection.class) {
                json.append("[]");
            } else if (genericType == Map.class) {
                json.append("{}");
            } else {
                json.append(genericType.getName());
            }
        }
        json.append("]");
        return json.toString();
    }

    private String getAsJsonExampleMap(Field f) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        Class<?>[] g = ObjectUtil.getFieldGenericTypes(f);
        if (g.length == 2) {
            Class<?> genericTypeK = g[0];
            if (ObjectUtil.extendsClass(genericTypeK, Number.class)) {
                json.append("000:");
            } else if (genericTypeK == String.class) {
                json.append("\"STRING\":");
            }

            Class<?> genericType = g[1];

            if (ObjectUtil.implementsInterface(genericType, Dto.class)) {
                json.append(getAsJsonExampleDto(genericType));
            } else if (ObjectUtil.extendsClass(genericType, Number.class)) {
                json.append("000");
            } else if (genericType == String.class) {
                json.append("\"STRING\"");
            } else if (genericType == Boolean.class) {
                json.append("true,");
            } else if (genericType == Location.class) {
                json.append("{\"latitude\":000,\"longitude\":000}");
            } else if (genericType == List.class || genericType == Set.class || genericType == Collection.class) {
                json.append("[]");
            } else if (genericType == Map.class) {
                json.append("{}");
            }
        }
        json.append("}");
        return json.toString();
    }

    public String getAsJsonExampleDto(Class<?> obj) {
        boolean excludeAll = false;
        if (exclude != null) {
            for (String x : exclude) {
                if (x.equals("all")) {
                    excludeAll = true;
                    break;
                }
            }
        }

        if (includeFields == null) {
            includeFields = new HashSet<>();
        }

        StringBuilder json = new StringBuilder();
        json.append("{");

        for (Field f : getProperties(obj, exclude, ignoreNoStore)) {
            Class<?> propType = f.getType();
            String prop = f.getName();
            boolean isKey = false;
            if (key != null) {
                for (String k : key) {
                    if (k.equals(prop)) {
                        isKey = true;
                        break;
                    }
                }
            }

            if (!includeFields.isEmpty() && !includeFields.contains(prop) && !isKey) {
                continue;
            }
            if (excludeAll && !isKey) {
                continue;
            }

            json.append('"').append(prop).append("\":");

            if (propType.isEnum()) {
                json.append('"');
                for (Object x : propType.getEnumConstants()) {
                    json.append(x.toString()).append(" | ");
                }
                json.setLength(json.length() - 3);
                json.append("\",");
                continue;
            }

            if (ObjectUtil.extendsClass(propType, Number.class)) {
                json.append("000").append(',');
                continue;
            }
            if (propType == Boolean.class) {
                json.append("true,");
                continue;
            }
            if (propType == String.class) {
                json.append("\"STRING\",");
                continue;
            }
            if (propType == Location.class) {
                json.append("{\"latitude\":000,\"longitude\":000,\"countryCode\":\"en\"},");
                continue;
            }
            if (f.isAnnotationPresent(Localized.class)) {
                json.append("{\"en\":\"STRING\",\"fa\":\"STRING\"},");
                continue;
            }
            if (propType == DateTime.class) {
                json.append("\"Y-M-D h:m:s\",");
                continue;
            }
            if (propType == DateTimeRange.class) {
                json.append("{\"dateMin\":\"Y-M-D h:m:s\",\"dateMax\":\"Y-M-D h:m:s\"},");
                continue;
            }

            if (propType == List.class || propType == Set.class || propType == Collection.class) {
                json.append(getAsJsonExampleList(f, obj)).append(',');
                continue;
            }
            if (propType == Map.class) {
                json.append(getAsJsonExampleMap(f)).append(',');
                continue;
            }
            if (ObjectUtil.implementsInterface(propType, Dto.class)) {
                json.append(getAsJsonExampleDto(propType)).append(',');
                continue;
            }

            json.append("\"???\",");
        }

        json.setLength(json.length() - 1);
        json.append('}');
        return json.toString();
    }

    private static List<Field> getProperties(Class<?> cls, String[] exclude, boolean ignoreNoStore) {
        List<Field> properties = new ArrayList<>();
        for (Field field : cls.getFields()) {
            int m = field.getModifiers();
            if (Modifier.isFinal(m) || Modifier.isStatic(m)) {
                continue;
            }
            if (ignoreNoStore && field.isAnnotationPresent(NoStore.class)) {
                continue;
            }
            if (CollectionUtil.contains(exclude, field.getName())) {
                continue;
            }
            properties.add(field);
        }
        return properties;
    }

    private static List<String> getProperties(Dto dto, String[] exclude, boolean ignoreNoStore) {
        List<String> properties = new ArrayList<>();
        for (Field field : dto.getClass().getFields()) {
            int m = field.getModifiers();
            if (Modifier.isFinal(m) || Modifier.isStatic(m)) {
                continue;
            }
            if (ignoreNoStore && field.isAnnotationPresent(NoStore.class)) {
                continue;
            }
            if (CollectionUtil.contains(exclude, field.getName())) {
                continue;
            }
            properties.add(field.getName());
        }
        return properties;
    }

    @SuppressWarnings({"unchecked"})
    private static void setReference(Object obj, Class<?> classType, Class<?> containerClassType) {
        if (classType == Location.class || classType == DateTime.class) {
            return;
        }

        String[] parts = StringUtil.split(classType.getName(), '.');
        String className = parts[parts.length - 1];
        String name = StringUtil.replace(className, '$', '.');
        String hash = StringUtil.replace(className, '$', '-');

        if (obj instanceof StringBuilder) {
            ((StringBuilder) obj).append(" <a href='/admin/document/show/dtos#")
                .append(hash).append("'>{").append(name).append("} (see object reference document)</a>");
        } else if (obj instanceof ArrayList) {
            ((ArrayList<String>) obj).add(" <a href='/admin/document/show/dtos#" + hash + "'>{" + name + "} (see object reference document)</a>");
        }
    }

}
