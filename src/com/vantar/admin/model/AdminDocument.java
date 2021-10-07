package com.vantar.admin.model;

import com.vantar.common.Settings;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.*;
import com.vantar.web.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminDocument {

    private static final Logger log = LoggerFactory.getLogger(AdminDocument.class);


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), params, response);
        if (ui == null) {
            return;
        }

        String search = params.getString("search");

        ui  .beginFormGet()
            .addInput("Search", "search", search)
            .addSubmit()
            .containerEnd();

        drawStructure(ui, "document", "document", FileUtil.getResourceStructure("/document"), search);
        ui.finish();
    }

    @SuppressWarnings({"unchecked"})
    private static void drawStructure(WebUi ui, String path, String dir, Map<String, Object> paths, String search) {
        ui.beginTree(path);

        List<String> files = (List<String>) paths.get("/");
        if (files != null) {
            for (String file : files) {
                if (!StringUtil.isEmpty(search) && !containsSearch("/" + StringUtil.replace(dir, "--", "/") + "/" + file, search)) {
                    continue;
                }

                ui.addBlockLink(file, "/admin/document/show?document=" + dir + "--" + file);
            }
            paths.remove("/");
        }

        paths.forEach((innerPath, structure) ->
            drawStructure(ui, innerPath, dir + "--" + innerPath, (Map<String, Object>) structure, search)
        );

        if (dir.equals("document")) {
            ui.beginTree("dto objects");
            ui.addBlockLink("objects.md", "/admin/document/show/dtos");
            ui.containerEnd();
        }

        ui.containerEnd();
    }

    private static boolean containsSearch(String documentPath, String search) {
        String content = FileUtil.getFileContentFromClassPath(documentPath);
        if (StringUtil.isEmpty(content)) {
            content = FileUtil.getFileContent(documentPath);
        }
        if (StringUtil.isEmpty(content)) {
            return false;
        }

        return StringUtil.contains(content.toLowerCase(), search.toLowerCase());
    }

    public static void show(Params params, HttpServletResponse response) {
        show('/' + StringUtil.replace(params.getString("document"), "--", "/"), response, true);
    }

    public static void show(String documentPath, HttpServletResponse response, boolean fromClasspath) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(
            normalize(
                fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath)
            )
        );
        String html = renderer.render(document);

        Response.writeString(
            response,
            "<html>" +
                "<head>" +
                "<link rel='stylesheet' type='text/css' href='/css/document" +
                    (StringUtil.contains(documentPath, "-fa") ? "-fa" : "-en") + ".css'>" +
                "</head>" +
                "<body>" + html + "</body>" +
                "</html>"
        );
    }

    public static String getParsedDocument(String documentPath, boolean fromClasspath) {
        return normalize(
            fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath)
        );
    }

    @SuppressWarnings({"unchecked"})
    public static void createDtoDocument() {
        StringBuilder document = new StringBuilder();
        document.append("# Dto objects #\n\n");

        List<DtoDictionary.Info> dtos = new ArrayList<>();
        dtos.addAll(DtoDictionary.getAll());
        dtos.addAll(DtoDictionary.getNoStoreDtos().values());

        for (DtoDictionary.Info info : dtos) {
            Dto dto = info.getDtoInstance();
            StringBuilder enums = new StringBuilder();
            document.append("\n<label id='").append(info.dtoClass.getSimpleName()).append("'></label>\n## ")
                .append(info.dtoClass.getSimpleName()).append(" ##\n");

            for (Field field : dto.getFields()) {
                Class<?> type = field.getType();
                document.append("* ").append(type.getSimpleName());

                if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                    document.append("&lt;").append(dto.getPropertyGenericTypes(field.getName())[0].getSimpleName()).append("&gt;");

                } else if (type.equals(Map.class)) {
                    Class<?>[] genericTypes = dto.getPropertyGenericTypes(field.getName());
                    document
                        .append("&lt;").append(genericTypes[0].getSimpleName()).append(", ")
                        .append(genericTypes[1].getSimpleName()).append("&gt;");

                } else if (type.isEnum()) {
                    enums.append("\n<label id='").append(info.dtoClass.getSimpleName()).append('-').append(type.getSimpleName()).append("'></label>\n");
                    enums.append("##### enum: ").append(type.getSimpleName()).append(" #####\n");
                    final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                    for (Enum<?> x : enumType.getEnumConstants()) {
                        enums.append("* ").append(x.toString()).append("\n");
                    }
                    enums.append("\n\n");
                }
                document.append(" ").append(field.getName()).append("\n");
            }

            if (enums.length() > 0) {
                document.append(enums);
            }

            // > > > inner class
            for (Class<?> innerClass : dto.getClass().getDeclaredClasses()) {
                String href = info.dtoClass.getSimpleName() + '-' + innerClass.getSimpleName();
                String name = info.dtoClass.getSimpleName() + '.' + innerClass.getSimpleName();

                document.append("\n<label id='").append(href).append("'></label>\n");
                document.append("#### ").append(name).append(" ####\n");
                for (Field field : innerClass.getFields()) {
                    if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    Class<?> type = field.getType();
                    document.append("* ").append(type.getSimpleName());

                    if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                        Class<?>[] g = ObjectUtil.getFieldGenericTypes(field);
                        if (g == null || g.length != 1) {
                            log.warn("! invalid generics ({}.{})", dto.getClass().getSimpleName(), field.getName());
                            continue;
                        }
                        document.append("&lt;").append(g[0].getTypeName()).append("&gt;");

                    } else if (type.equals(Map.class)) {
                        Class<?>[] g = ObjectUtil.getFieldGenericTypes(field);
                        if (g == null || g.length != 2) {
                            log.warn("! invalid generics ({}.{})", dto.getClass().getSimpleName(), field.getName());
                            continue;
                        }
                        document.append("&lt;").append(g[0].getTypeName()).append(", ").append(g[1].getTypeName()).append("&gt;");

                    } else if (type.isEnum()) {
                        document.append("\n<label id='").append(innerClass.getSimpleName()).append('-').append(type.getSimpleName()).append("'></label>\n");
                        enums.append("##### enum: ").append(type.getSimpleName()).append(" #####\n");
                        final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                        for (Enum<?> x : enumType.getEnumConstants()) {
                            enums.append("* ").append(x.toString()).append("\n");
                        }
                        enums.append("\n\n");
                    }
                    document.append(" ").append(field.getName()).append("\n");
                }
            }
            // < < < inner class
        }

        FileUtil.makeDirectory(Settings.config.getProperty("documents.dir"));
        FileUtil.write(Settings.config.getProperty("documents.dir") + "objects.md", document.toString());
    }

    private static String normalize(String content) {
        StringBuilder sb = new StringBuilder();
        boolean catchData = false;
        for (String line : StringUtil.split(content, '\n')) {
            if (line.equals("{{")) {
                catchData = true;
            }

            if (catchData) {
                sb.append(line).append('\n');
            }

            if (line.equals("}}")) {
                catchData = false;
                String x = sb.toString();
                sb.setLength(0);
                DtoDocumentData dtoDocumentData = Json.fromJson(
                    StringUtil.replace(StringUtil.replace(x, "}}", "}"), "{{", "{"),
                    DtoDocumentData.class
                );
                if (dtoDocumentData == null) {
                    log.error("! object=null {}", StringUtil.replace(StringUtil.replace(x, "}}", "}\n"), "{{", "{"));
                } else {
                    if (dtoDocumentData.action != null && dtoDocumentData.action.equalsIgnoreCase("exception")) {
                        content = fixExceptions(content, x, dtoDocumentData);
                    } else {
                        content = StringUtil.replace(content, x, dtoDocumentData.get());
                    }
                }
            }
        }

        content = StringUtil.replace(content, "<<<access>>>", "PUBLIC", 1);
        return content;
    }

    private static String fixExceptions(String content, String from, DtoDocumentData dtoDocumentData) {
        content = setAccess(content, dtoDocumentData.controllerClass, dtoDocumentData.controllerMethod);
        StringBuilder to = new StringBuilder();
        for (Class<?> e : getExceptions(dtoDocumentData.controllerClass, dtoDocumentData.controllerMethod)) {
            if (e.getSimpleName().equals("InputException")) {
                to.append("* **400** (client side error) Invalid input params\n");
                continue;
            }
            if (e.getSimpleName().equals("AuthException")) {
                to.append("* **401** (client side error / unauthorized) user is not signed in or invalid/expired access token\n");
                to.append("* **403** (client side error / forbidden) user does not have permission\n");
                continue;
            }
            if (e.getSimpleName().equals("ServerException")) {
                to.append("* **500** (server side error) Unexpected backend server error, must be reported\n");
                continue;
            }
            if (e.getSimpleName().equals("NoContentException")) {
                to.append("* **204** (no content) There is no data available for the request\n");
            }
        }
        content = StringUtil.replace(content, from, to.toString(), 1);
        return content;
    }

    public static Set<Class<?>> getExceptions(String className, String methodName) {
        Class<?>[] parameterTypes = new Class[] { Params.class, HttpServletResponse.class };
        try {
            Class<?> c = ObjectUtil.getClass(className);
            if (c == null) {
                return new HashSet<>();
            }
            Method m = c.getDeclaredMethod(methodName, parameterTypes);

            Set<Class<?>> exceptions = new HashSet<>(Arrays.asList(m.getExceptionTypes()));
            if (m.isAnnotationPresent(Access.class)) {
                exceptions.add(com.vantar.exception.AuthException.class);
            }
            return exceptions;
        } catch (Exception e) {
            log.warn("! could not get exceptions ({}, {})", className, methodName, e);
            return new HashSet<>();
        }
    }

    public static String setAccess(String content, String className, String methodName) {
        Class<?>[] parameterTypes = new Class[] { Params.class, HttpServletResponse.class };
        try {
            Class<?> c = ObjectUtil.getClass(className);
            if (c == null) {
                return content;
            }
            Method m = c.getDeclaredMethod(methodName, parameterTypes);

            if (m.isAnnotationPresent(Access.class)) {
                StringBuilder access = new StringBuilder();
                for (String e : m.getAnnotation(Access.class).value()) {
                    access.append(" * ").append(e).append(" \n");
                }
                content = StringUtil.replace(content, "<<<access>>>", access.toString(), 1);
            } else {
                content = StringUtil.replace(content, "<<<access>>>", "PUBLIC", 1);
            }

            return content;
        } catch (Exception e) {
            log.warn("! could not get exceptions ({}, {})", className, methodName, e);
            return content;
        }
    }

    private static class DtoDocumentData {

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
        public String controllerMethod;


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
                for (String x: exclude) {
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
                    log.error("! can not create {}", dto);
                    return "!!!DOCUMENT CREATION ERROR!!!";
                }

                if (StringUtil.isNotEmpty(format)) {
                    if (format.equalsIgnoreCase("list")) {
                        if (dto != null) {
                            String name = StringUtil.replace(obj.getClass().getSimpleName(), '$', '.');
                            sb  .append("[ { <a href='/admin/document/show/dtos#").append(name).append("'>")
                                .append(name).append("</a> } ]")
                                .append(" (the JSON list may contain fields of object <a href='/admin/document/show/dtos#")
                                .append(name).append("'>").append(name).append("</a>").append(" as described bellow)\n");
                        }
                    } else if (format.equalsIgnoreCase("object")) {
                        if (dto != null) {
                            String name = StringUtil.replace(obj.getClass().getSimpleName(), '$', '.');
                            sb  .append("{ <a href='/admin/document/show/dtos#").append(name).append("'>").append(name)
                                .append("</a> }")
                                .append(" (the JSON may contain fields of object <a href='/admin/document/show/dtos#")
                                .append(name).append("'>").append(name).append("</a>").append(" as described bellow)\n");
                        }
                    } else {
                        sb.append(format);
                    }
                }

                for (String prop : obj.getProperties(exclude)) {
                    Class<?> propType = obj.getPropertyType(prop);
                    boolean isRequired = obj.hasAnnotation(prop, Required.class);
                    boolean isKey = false;
                    if (key != null) {
                        for (String k: key) {
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
                        for (Class<?> genericType: obj.getPropertyGenericTypes(prop)) {
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
                        for (String item: genericComments) {
                            sb.append(item);
                        }
                    }
                    if (!ObjectUtil.isJavaNative(propType) && propType != DateTime.class) {
                        setReference(sb, propType, obj.getClass());
                    }

                    sb.append("\n");
                }
            }
            return sb.toString();
        }
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
