package com.vantar.admin.documentation;

import com.vantar.common.Settings;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;


public class WebServiceDocument {

    private final Map<String, Class<?>> enums;
    private final Map<String, String> controllers;
    private String controller;
    private Method method;


    public WebServiceDocument() {
        List<Class<?>> enumList = EnumUtil.getClasses(Settings.config.getProperty("package.dto"));
        enums = new HashMap<>(enumList.size(), 1);
        for (Class<?> e : enumList) {
            enums.put(e.getSimpleName(), e);
        }

        List<Class<?>> classList = ClassUtil.getClasses(Settings.config.getProperty("package.web"), WebServlet.class);
        controllers = new HashMap<>(classList.size(), 1);
        for (Class<?> cls : classList) {
            for (String path : cls.getAnnotation(WebServlet.class).value()) {
                controllers.put(
                    path,
                    cls.getName() + ">" + StringUtil.toCamelCase(StringUtil.split(path, '/', 3)[2])
                );
            }
        }
    }

    /**
     * replace objects, add samples, ...
     * if docTag != null -> remove webservices that do not contain the tag
     */
    public String getViewableMd(String md, String docTag) {
        if (docTag != null) {
            docTag = "* " + docTag;
        }

        StringBuilder parsed = new StringBuilder(200000);
        StringBuilder block = new StringBuilder(100000);
        String[] lines = StringUtil.split(md, '\n');
        String prevLine = "";
        for (int i = 0, j = 0, lineLen = lines.length; j < lineLen; j++) {
            String line = lines[j];

            // > > > fetch public data
            if (prevLine.startsWith("### url")) {
                String url = StringUtil.trim(line, '"');
                controller = controllers.get(url);
                method = null;
                if (controller != null) {
                    String[] parts = StringUtil.split(controller, '>');
                    Class<?> c = ClassUtil.getClass(parts[0]);
                    if (c != null) {
                        try {
                            method = c.getMethod(parts[1], Params.class, HttpServletResponse.class);
                        } catch (Exception e) {
                            method = null;
                        }
                    }
                }
            }
            prevLine = line;
            // fetch public data < < <

            // > > > fetch blocks
            if (line.startsWith("## ") || j == lineLen - 1) {
                if (i > 0) {
                    String docBlock = block.toString();
                    if (docTag != null && !StringUtil.contains(docBlock, docTag)) {
                        block = new StringBuilder(100000);
                        block.append(line).append('\n');
                        ++i;
                        continue;
                    }
                    parsed.append(getViewableBlock(docBlock));
                    block = new StringBuilder(100000);
                } else {
                    parsed.append(getViewableBlock(block.toString()));
                    block = new StringBuilder(100000);
                }
                ++i;
            }
            block.append(line).append('\n');
            // fetch blocks < < <
        }

        return parsed.toString();
    }

    private String getViewableBlock(String md) {
        // > > > fetch JSON blocks
        StringBuilder sb = new StringBuilder(10000);
        boolean insideJson = false;
        for (String line : StringUtil.split(md, '\n')) {
            if (line.equals("{{")) {
                insideJson = true;
            }
            if (insideJson) {
                sb.append(line).append('\n');
            }
            if (line.equals("}}")) {
                insideJson = false;
                String jsonBlock = sb.toString();
                sb.setLength(0);

                JsonBlock dtoDocument = Json.d.fromJson(
                    StringUtil.replace(StringUtil.replace(jsonBlock, "}}", "}"), "{{", "{"),
                    JsonBlock.class
                );
                if (dtoDocument == null) {
                    ServiceLog.log.error("! invalid JSON block in document {}", jsonBlock);
                } else {
                    dtoDocument.setEnums(enums);
                    md = StringUtil.replace(md, jsonBlock, dtoDocument.get());
                }
            }
        }
        // fetch JSON blocks < < <

        md = setStandardOutput(md);
        md = setAccess(md);
        return md + getExceptions() + getController();
    }

    private String setStandardOutput(String md) {
        if (md.contains("<<<insert>>>")) {
            return StringUtil.replace(
                md,
                "<<<insert>>>",
                "    JSON\n" +
                "    {\n" +
                "        \"code\": (int) status code 200/4xx/5xx,\n" +
                "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
                "        \"dto\": (Object) { inserted-object },\n" +
                "        \"value\": (long) inserted-id,\n" +
                "        \"successful\": (boolean) true/false\n" +
                "    }\n"
            );
        } else if (md.contains("<<<update>>>")) {
            return StringUtil.replace(
                md,
                "<<<update>>>",
                "    JSON\n" +
                "    {\n" +
                "        \"code\": (int) status code 200/4xx/5xx,\n" +
                "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
                "        \"dto\": (Object) { updated-object },\n" +
                "        \"successful\": (boolean) true/false\n" +
                "    }\n"
            );
        } else if (md.contains("<<<delete>>>")) {
            return StringUtil.replace(
                md,
                "<<<delete>>>",
                "    JSON\n" +
                "    {\n" +
                "        \"code\": (int) status code 200/4xx/5xx,\n" +
                "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
                "        \"successful\": (boolean) true/false,\n" +
                "        // if successful:\n" +
                "        \"value\": (long) deleted record count\n" +
                "        // if failed because of dependencies:\n" +
                "        \"value\": [{\"name\":\"dto.class\", [{data}, {data},...]}, {}, ...]\n" +
                "    }\n"
            );
        }
        return md;
    }

    private String setAccess(String md) {
        if (method != null) {
            if (method.isAnnotationPresent(VerifyPermission.class)) {
                return StringUtil.replace(md, "<<<access>>>", "* defined in database", 1);
            }
            Feature f = method.getAnnotation(Feature.class);
            if (f != null) {
                return StringUtil.replace(md, "<<<access>>>", "* feature:=" + f.value(), 1);
            }
            Access a = method.getAnnotation(Access.class);
            if (a != null) {
                StringBuilder sa = new StringBuilder(300);
                for (String s : a.value()) {
                    sa.append("* role: ").append(s).append("\n");
                }
                return StringUtil.replace(md, "<<<access>>>", sa.toString(), 1);
            }
            return StringUtil.replace(md, "<<<access>>>", "* no permission required", 1);
        }
        return StringUtil.replace(md, "<<<access>>>", "!!!url/controller not found!!!", 1);
    }

    private String getExceptions() {
        if (method == null) {
            return "";
        }

        Set<Class<?>> exceptions = new HashSet<>(Arrays.asList(method.getExceptionTypes()));
        if (method.isAnnotationPresent(Access.class)) {
            exceptions.add(com.vantar.exception.AuthException.class);
        }

        StringBuilder to = new StringBuilder(1000);
        to.append("\n### exceptions ###\n");
        for (Class<?> e : exceptions) {
            if (e.getSimpleName().equals("InputException")) {
                to.append("* **400** (client side) Invalid input params\n");
                continue;
            }
            if (e.getSimpleName().equals("AuthException")) {
                to.append("* **401** (client side / unauthorized) user is not signed in or access token is invalid/expired\n");
                to.append("* **403** (client side / forbidden) user does not have the required permissions\n");
                continue;
            }
            if (e.getSimpleName().equals("ServerException")) {
                to.append("* **500** (server side) Unexpected backend server error/exception\n");
                continue;
            }
            if (e.getSimpleName().equals("NoContentException")) {
                to.append("* **204** (no content) There is no data available for the request\n");
                continue;
            }
            if (e.getSimpleName().equals("VantarException")) {
                to.append("* **204** (no content) There is no data available for the request\n");
                to.append("* **400** (client side) Invalid input params\n");
                to.append("* **401** (client side / unauthorized) user is not signed in or access token is invalid/expired\n");
                to.append("* **403** (client side / forbidden) user does not have the required permissions\n");
                to.append("* **500** (server side) Unexpected backend server error/exception\n");
                continue;
            }
            to.append("* **500** (server side) ").append(e.getSimpleName()).append("\n");
        }

        return to.toString();
    }

    private String getController() {
        if (method == null) {
            return "";
        }

        StringBuilder to = new StringBuilder(500);
        to.append("\n### controller ###\n* ").append(StringUtil.replace(controller, ">", ".*")).append("*\n");
        if (method.isAnnotationPresent(BackgroundTask.class)) {
            to.append("* background task (each call runs in a parallel thread)\n");
        }
        CallTimeLimit ctl = method.getAnnotation(CallTimeLimit.class);
        if (ctl != null) {
            to.append("* ").append(ctl).append("minutes call time limit (after each call blocks until the minutes is passed)\n");
        }
        return to.toString();
    }
}
