package com.vantar.admin.model.document;

import com.vantar.admin.model.AdminDocument;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;


public class WebServiceDocumentCreator {

    public static String getParsedMd(String content, String tag) {
        if (tag != null) {
            tag = "* " + tag;
        }
        StringBuilder parsed = new StringBuilder();
        StringBuilder block = new StringBuilder();

        String[] lines = StringUtil.split(content, '\n');
        for (int i = 0, j = 0, linesLength = lines.length; j < linesLength; j++) {
            String line = lines[j];
            if (line.startsWith("## ") || j == linesLength - 1) {
                if (i > 0) {
                    String docBlock = block.toString();
                    if (tag != null && !StringUtil.contains(docBlock, tag)) {
                        block = new StringBuilder();
                        block.append(line).append('\n');
                        ++i;
                        continue;
                    }
                    parsed.append(getParsedMdX(docBlock));
                    block = new StringBuilder();
                } else {
                    parsed.append(getParsedMdX(block.toString()));
                    block = new StringBuilder();
                }
                ++i;
            }
            block.append(line).append('\n');
        }

        return parsed.toString();
    }

    public static Set<String> getTags(String content) {
        Set<String> tags = new HashSet<>();

        boolean inBlock = false;
        for (String line : StringUtil.split(content, '\n')) {
            line = line.trim();
            if (line.equals("### tags ###")) {
                inBlock = true;
                continue;
            }

            if (inBlock) {
                if (line.startsWith("#")) {
                    inBlock = false;
                    continue;
                }
                tags.add(StringUtil.remove(line, '*').trim());
            }
        }

        return tags;
    }

    private static String getParsedMdX(String content) {
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
                    AdminDocument.log.error("! object=null {}", StringUtil.replace(StringUtil.replace(x, "}}", "}\n"), "{{", "{"));
                } else {
                    if (dtoDocumentData.action != null && dtoDocumentData.action.equalsIgnoreCase("exception")) {
                        content = fixExceptions(content, x, dtoDocumentData);
                    } else {
                        content = StringUtil.replace(content, x, dtoDocumentData.get());
                    }
                }
            }
        }

        content = StringUtil.replace(content, "<<<insert>>>", "    JSON\n" +
            "    {\n" +
            "        \"code\": (int) status code 200/4xx/5xx,\n" +
            "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
            "        \"dto\": (Object) { inserted-object },\n" +
            "        \"value\": (long) inserted-id,\n" +
            "        \"successful\": (boolean) true/false\n" +
            "    }\n");
        content = StringUtil.replace(content, "<<<update>>>", "    JSON\n" +
            "    {\n" +
            "        \"code\": (int) status code 200/4xx/5xx,\n" +
            "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
            "        \"dto\": (Object) { updated-object },\n" +
            "        \"successful\": (boolean) true/false\n" +
            "    }\n");
        content = StringUtil.replace(content, "<<<delete>>>", "    JSON\n" +
            "    {\n" +
            "        \"code\": (int) status code 200/4xx/5xx,\n" +
            "        \"message\": (String) \"message, i.e. success or user validation errors\",\n" +
            "        \"successful\": (boolean) true/false\n" +
            "    }\n");

        content = StringUtil.replace(content, "<<<access>>>", "PUBLIC", 1);
        return content;
    }

    private static String fixExceptions(String content, String from, DtoDocumentData dtoDocumentData) {
        String controllerMethod = StringUtil.trim(getAfterTag(from, "### url ###", content), '"');
        controllerMethod = StringUtil.split(controllerMethod, '/', 3)[2];
        controllerMethod = StringUtil.toCamelCase(controllerMethod);

        content = setAccess(content, dtoDocumentData.controllerClass, controllerMethod);

        StringBuilder to = new StringBuilder();
        for (Class<?> e : getExceptions(dtoDocumentData.controllerClass, controllerMethod)) {
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

    private static String getAfterTag(String from, String tag, String content) {
        boolean reachedTag = false;
        StringBuilder sb = new StringBuilder();

        content = StringUtil.split(content, from)[0];
        String[] lines = StringUtil.split(content, '\n');

        int m = lines.length - 1;
        for (; m > 0 ; --m) {
            String line = lines[m];
            if (line.equals(tag)) {
                break;
            }
        }


        for (int i = m ; i < lines.length ; ++i){
            String line = lines[i];
            if (line.startsWith(tag)) {
                reachedTag = true;
                continue;
            }
            if (reachedTag) {
                if (line.startsWith("###")) {
                    break;
                }
                sb.append(line).append('\n');
            }
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
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
            AdminDocument.log.warn("! could not get exceptions ({}, {})", className, methodName, e);
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
            AdminDocument.log.warn("! could not get exceptions ({}, {})", className, methodName, e);
            return content;
        }
    }
}
