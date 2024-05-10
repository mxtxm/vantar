package com.vantar.admin.documentation;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.database.query.PageData;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
public class JsonBlock {

    // "list" / "object"
    public String format;
    // enum class
    public String enumClass;
    // search dto class
    public String searchParams;
    // search dto class
    public String searchResult;
    // dto class
    public String dto;
    // delete
    public String action;
    // include or exclude fields, include has priority
    public String[] include;
    public String[] exclude;
    // additional custom params
    public Map<String, String> other;

    private Map<String, Class<?>> enums;


    public void setEnums(Map<String, Class<?>> enums) {
        this.enums = enums;
    }

    public Object getData() {
        // > > > ENUM
        if (enumClass != null) {
            Class<?> c = enums.get(enumClass);
            if (c == null || c.getEnumConstants() == null) {
                return "!!!ERROR IN DOCUMENT!!!";
            }
            if (format.equalsIgnoreCase("list")) {
                Object[] enumsConstants = c.getEnumConstants();
                List<String> items = new ArrayList<>(enumsConstants.length);
                for (Object item : enumsConstants) {
                    items.add(item.toString());
                }
                return items;
            }
            StringBuilder sb = new StringBuilder(1000);
            for (Object item : c.getEnumConstants()) {
                sb.append("\"").append(item).append("\" or ");
            }
            if (sb.length() > 5) {
                sb.setLength(sb.length() - 4);
            }
            return sb.toString();
        }

        // > > > SEARCH PARAMS
        if (searchParams != null) {
            Map<String, Object> p = new HashMap<>(30, 1);
            p.put("page", 1);
            p.put("length", 11);
            p.put("pagination", true);
            List<String> sort = new ArrayList<>(2);
            sort.add("id:asc");
            p.put("sort", sort);
            Map<String, Object> condition = new HashMap<>(5, 1);
            condition.put("operator", "AND");
            List<Map<String, Object>> items = new ArrayList<>(2);
            Map<String, Object> conditionItem = new HashMap<>(2, 1);
            conditionItem.put("col", "id");
            conditionItem.put("type", "EQUAL");
            conditionItem.put("value", 787);
            items.add(conditionItem);
            condition.put("items", items);
            p.put("condition", condition);

            return p;
        }
        // > > > SEARCH RESULT
        Dto obj;
        if (searchResult != null) {
            PageData pageData = new PageData();
            pageData.page = 2;
            pageData.length = 10;
            pageData.recordCount = 9;
            pageData.total = 19;
            pageData.errors = "ERROR MESSAGES";
            return pageData;
        }

        // > > > DTO
        DtoDictionary.Info info = DtoDictionary.get(dto);
        if (info == null) {
            return "!!!DTO NOT FOUND " + dto + "!!!";
        }
        obj = info.getDtoInstance();

        // > > > delete / byid
        if ("delete".equalsIgnoreCase(action) || "byid".equalsIgnoreCase(action)) {
            Map<String, Object> p = new HashMap<>(2, 1);
            p.put("id", 7);
            return p;
        }

        // > > > DTO fields
        Map<String, Object> map = DummyValue.getDummyDto(obj);
        map.entrySet().removeIf(e -> {
            String k = e.getKey();
            if (CollectionUtil.contains(exclude, k)) {
                return true;
            }
            return include != null && !CollectionUtil.contains(include, k);
        });
        if (other != null) {
            other.forEach((k, v) -> {
                String[] typeName = StringUtil.split(StringUtil.remove(k, '*', ':').trim(), " ");
                map.put(typeName[1].trim(), DummyValue.getDummyObjectValue(typeName[0].trim()));
            });
        }
        if ("list".equalsIgnoreCase(format)) {
            List<Map<String, Object>> x = new ArrayList<>(1);
            x.add(map);
            return x;
        }
        return map;
    }

    public String get() {
        // > > > ENUM
        if (enumClass != null) {
            return getEnumDox();
        }
        // > > > SEARCH PARAMS
        if (searchParams != null) {
            return getSearchParams();
        }
        // > > > SEARCH RESULT
        Dto obj;
        if (searchResult != null) {
            return getSearchResult();
        }

        // > > > DTO
        DtoDictionary.Info info = DtoDictionary.get(dto);
        if (info == null) {
            return "!!!ERROR IN DOCUMENT " + dto + "!!!\n";
        }
        obj = info.getDtoInstance();

        // > > > delete
        if ("delete".equalsIgnoreCase(action)) {
            return "* <strong class='key'>id:</strong> " + obj.getClass().getSimpleName() + ".id (primary key)\n";
        }

        // > > > by id
        if ("byid".equalsIgnoreCase(action)) {
            return "* **id:** " + obj.getClass().getSimpleName() + ".id (primary key)\n";
        }

        // > > > DTO fields
        StringBuilder sb = new StringBuilder(1000);

        if (StringUtil.isNotEmpty(format)) {
            NameHash nameHash = getNameHash(dto);
            if (format.equalsIgnoreCase("list")) {
                sb.append("<a target='_blank' href='/admin/documentation/show/dtos#").append(nameHash.hash).append("'>")
                    .append("list [ {").append(nameHash.name).append("} ]").append("</a> :\n");
            } else if (format.equalsIgnoreCase("object")) {
                sb.append("<a target='_blank' href='/admin/documentation/show/dtos#").append(nameHash.hash).append("'>")
                    .append("object {").append(nameHash.name).append("}").append("</a> :\n");
            } else {
                sb.append(format).append("\n");
            }
        }

        Field idF = obj.getField("id");
        if (idF != null && ((include == null && exclude == null) || CollectionUtil.contains(include, "id")
            || !CollectionUtil.contains(exclude, "id"))) {
            sb.append("* <strong class='key'>id</strong>: ").append(obj.getClass().getSimpleName()).append(".id (primary key)\n");
        }

        for (String prop : include != null ? obj.getPropertiesInc(exclude)
            : (exclude != null ? obj.getPropertiesEx(exclude) : obj.getProperties())) {
            if ("id".equals(prop)) {
                continue;
            }

            Class<?> propType = obj.getPropertyType(prop);
            boolean isRequired = obj.hasAnnotation(prop, Required.class);

            // > field
            sb.append("* *");
            if (isRequired) {
                sb.append("*");
            }
            // > type
            sb.append(propType.getSimpleName());
            // > generic type
            List<String> genericComments = null;
            if (CollectionUtil.isCollectionOrMap(propType)) {
                sb.append("&lt;");
                genericComments = new ArrayList<>(3);
                for (Class<?> genericType : obj.getPropertyGenericTypes(prop)) {
                    String[] parts = StringUtil.split(genericType.getName(), '.');
                    String className = parts[parts.length - 1];
                    String name = StringUtil.replace(className, '$', '.');
                    sb.append(name).append(", ");
                    setReference(genericComments, genericType, obj.getClass());
                }
                sb.setLength(sb.length() - 2);
                sb.append("&gt;");
            }
            // generic type <
            sb.append(" ").append(prop).append(":");
            if (isRequired) {
                sb.append("*");
            }
            sb.append("*");
            // field <

            // > > > comments
            if (obj.hasAnnotation(prop, Default.class)) {
                sb.append(" default=").append(obj.getDefaultValue(prop));
            }
            Regex rAn = obj.getAnnotation(prop, Regex.class);
            if (rAn != null) {
                sb.append(" [regex=").append(rAn.value()).append("]");
            }
            if (propType == Location.class) {
                sb.append(" ").append(DummyValue.getDummyObjectValue(Location.class));
            }
            if (obj.hasAnnotation(prop, Localized.class)) {
                sb.append(" {\"en\": \"text\", \"fa\": \"text\", ...}");
            }
            if (obj.hasAnnotation(prop, DeLocalized.class)) {
                sb.append(" value is set based on the selected locale.");
            }
            Depends dAn = obj.getAnnotation(prop, Depends.class);
            if (dAn != null) {
                for (Class<? extends Dto> c : dAn.value()) {
                    DtoDictionary.Info i = DtoDictionary.get(c);
                    if (i != null) {
                        sb.append(" reference to ");
                        setReference(sb, i.dtoClass, obj.getClass());
                    }
                }
            }
            if (genericComments != null && !genericComments.isEmpty()) {
                for (String item : genericComments) {
                    sb.append(item).append(" ");
                }
            }
            setReference(sb, propType, obj.getClass());

            sb.append("\n");
        }

        // > > > add params based on the action
        if ("update".equalsIgnoreCase(action)) {
            sb  .append("* <span class='b-field'>String __action:</span> default=UPDATE_FEW_COLS\n")
                .append("   * UPDATE_FEW_COLS: updates only the provided fields. empty/missing/null fields are ignored.")
                .append(" to set values to null use __nullProperties.\n")
                .append("   * UPDATE_ALL_COLS: updates all fields. empty/missing/null fields are set to null.\n")
                .append("* <span class='b-field'>List&lt;String&gt; __nullProperties: ")
                .append("an explicit list of fields to be set to null.</span>\n");
        } else  if ("UPDATE_ALL_COLS".equalsIgnoreCase(action)) {
            sb  .append("   * <span class='b-field'>Fixed __action: UPDATE_ALL_COLS updates all fields.")
                .append(" empty/missing/null fields are set to null.</span>\n");
        } else  if ("UPDATE_FEW_COLS".equalsIgnoreCase(action)) {
            sb  .append("   * <span class='b-field'>Fixed __action: UPDATE_FEW_COLS updates only the provided fields. ")
                .append(" empty/missing/null fields are ignored. to set values to null use __nullProperties.</span>\n")
                .append("* <span class='b-field'>List&lt;String&gt; __nullProperties: ")
                .append("an explicit list of fields to be set to null.</span>\n");
        }

        if (other != null) {
            other.forEach((k, v) -> sb.append(k).append(" ").append(v).append("\n"));
        }

        // > > > add a data sample
        sb.append("##### sample #####\n");
        Map<String, Object> map = DummyValue.getDummyDto(obj);
        map.entrySet().removeIf(e -> {
            String k = e.getKey();
            if (CollectionUtil.contains(exclude, k)) {
                return true;
            }
            return include != null && !CollectionUtil.contains(include, k);
        });
        if (other != null) {
            other.forEach((k, v) -> {
                String[] typeName = StringUtil.split(StringUtil.remove(k, '*', ':').trim(), " ");
                map.put(typeName[1].trim(), DummyValue.getDummyObjectValue(typeName[0].trim()));
            });
        }
        String json = Json.getWithNulls().toJsonPretty(map);
        if ("list".equalsIgnoreCase(format)) {
            json = '[' + json + ']';
        }
        sb.append("<pre>").append(json).append("</pre>\n");

        return sb.toString();
    }

    private String getEnumDox() {
        Class<?> c = enums.get(enumClass);
        if (c == null || c.getEnumConstants() == null) {
            return "!!!ERROR IN DOCUMENT!!!\n";
        }
        StringBuilder sb = new StringBuilder(1000);
        if (format.equalsIgnoreCase("list")) {
            sb.append("    JSON\n    [\n");
            for (Object item : c.getEnumConstants()) {
                sb.append("        \"").append(item).append("\",\n");
            }
            sb.append("    ]\n");
        } else {
            sb.append("    STRING ");
            for (Object item : c.getEnumConstants()) {
                sb.append("\"").append(item).append("\" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getSearchParams() {
        StringBuilder sb = new StringBuilder(1000);
        if (other != null) {
            other.forEach((k, v) -> sb.append(k).append(" ").append(v).append("\n"));
        }
        searchParams = StringUtil.replace(searchParams, '.', '$');
        sb  .append("* <a target='_blank' href='/admin/documentation/show/dtos#").append(StringUtil.replace(searchParams, '$', '-'))
            .append("'>searchable fields: {").append(StringUtil.replace(searchParams, '$', '.')).append("}</a>\n")
            .append("* <a target='_blank' href='/admin/documentation/show?document=document--api--vantar--search.md'>search tutorial</a>\n");
        return sb.toString();
    }

    private String getSearchResult() {
        DtoDictionary.Info info = DtoDictionary.get(searchResult);
        if (info == null) {
            return "!!!ERROR IN DOCUMENT " + searchResult + "!!!\n";
        }
        Dto obj = info.getDtoInstance();
        NameHash nameHash = getNameHash(searchResult);
        String link = "<a target='_blank' href='/admin/documentation/show/dtos#" + nameHash.hash + "'>[ {" + nameHash.name + "} ]</a>\n";
        return
            "<pre>" +
                "JSON\n" +
                "if pagination is false then output is:" + link +
                "if pagination is true  then output is: {\n" +
                "    \"data\": " + link +
                "    \"page\": page number,\n" +
                "    \"length\": records per page,\n" +
                "    \"total\": total record count\n" +
                "}</pre>\n##### sample #####\n" +
                "<pre>{\n" +
                "    \"page\": page number,\n" +
                "    \"length\": records per page,\n" +
                "    \"total\": total record count\n" +
                "    \"data\": [{\n        " +
                StringUtil.trim(
                    StringUtil.replace(Json.d.toJsonPretty(DummyValue.getDummyDto(obj)), "\n", "\n    "),
                    '}',
                    '{'
                ) +
                "\n    }]\n}</pre>\n";
    }

    @SuppressWarnings({"unchecked"})
    private void setReference(Object html, Class<?> foreignClass, Class<?> ownerClass) {
        if (!ClassUtil.implementsInterface(foreignClass, Dto.class)) {
            return;
        }

        String[] parts = StringUtil.split(foreignClass.getName(), '.');
        String className = parts[parts.length - 1];
        NameHash nameHash = getNameHash(className);

        if (foreignClass.isEnum()) {
            nameHash.hash = ownerClass.getSimpleName() + '-' + nameHash.hash;
        }

        String link = " <a target='_blank' href='/admin/documentation/show/dtos#" + nameHash.hash + "'>{" + nameHash.name + "}</a>";
        if (html instanceof StringBuilder) {
            ((StringBuilder) html).append(link);
        } else if (html instanceof ArrayList) {
            ((List<String>) html).add(link);
        }
    }

    private NameHash getNameHash(String dtoName) {
        dtoName = StringUtil.replace(dtoName, '.', '$');
        return new NameHash(StringUtil.replace(dtoName, '$', '.'), StringUtil.replace(dtoName, '$', '-'));
    }


    private static class NameHash {

        public String name;
        public String hash;

        public NameHash(String name, String hash) {
            this.name = name;
            this.hash = hash;
        }
    }
}
