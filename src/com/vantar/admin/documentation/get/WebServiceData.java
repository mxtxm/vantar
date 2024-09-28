package com.vantar.admin.documentation.get;

import com.vantar.admin.documentation.JsonBlock;
import com.vantar.common.Settings;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;


public class WebServiceData {

    private Data data = null;
    private final Map<String, String> controllers;
    private Method method;
    private final Map<String, Class<?>> enums;


    public WebServiceData() {
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

    public Data get(String md, String targetUrl) {
        String[] lines = StringUtil.splitTrim(md, '\n');
        boolean foundWebservice = false;
        for (int j = 0, lineLen = lines.length; j < lineLen; ++j) {
            String line = lines[j];

            // > > > fetch public data
            if (line.startsWith("### url")) {
                if (foundWebservice) {
                    break;
                }
                line = lines[++j];
                String url = StringUtil.trim(line, '"');
                if (!targetUrl.equals(url)) {
                    continue;
                }
                foundWebservice = true;

                data = new Data();
                data.url = url;

                String controller = controllers.get(data.url);
                method = null;
                if (controller != null) {
                    String[] parts = StringUtil.splitTrim(controller, '>');
                    Class<?> c = ClassUtil.getClass(parts[0]);
                    if (c != null) {
                        try {
                            method = c.getMethod(parts[1], Params.class, HttpServletResponse.class);
                        } catch (Exception e) {
                            method = null;
                        }
                    }
                }
                continue;
            }

            if (!foundWebservice) {
                continue;
            }

            if (line.startsWith("### method")) {
                line = lines[++j].trim();
                data.httpMethod = line;
                continue;
            }

            if (line.startsWith("### ") && line.contains("params")) {
                ++j;
                setInput(getBlock(j, lineLen, lines).trim());
                if (data.inputSample instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) data.inputSample).entrySet()) {
                        Object v = e.getValue();
                        if (!(v instanceof String)) {
                            continue;
                        }
                        if ("FILE/UPLOAD".equalsIgnoreCase((String) v)) {
                            if (data.files == null) {
                                data.files = new HashSet<>(5, 1);
                            }
                            data.files.add(e.getKey().toString());
                        }
                    }
                }
                --j;
                continue;
            }

            if (line.startsWith("### headers")) {
                ++j;
                data.headers = new HashMap<>(7, 1);
                String block = StringUtil.remove(getBlock(j, lineLen, lines), '*').trim();
                for (String item : StringUtil.splitTrim(block, '\n')) {
                    // "type name: description"
                    item = StringUtil.remove(item, '*').trim();
                    String[] parts = StringUtil.split(StringUtil.split(item, ':')[0], ' ');
                    String k = parts[1].trim();
                    Object v;
                    if (k.equalsIgnoreCase("X-Lang")) {
                        v = "en";
                    } else if (k.equalsIgnoreCase("X-Auth-Token")) {
                        v = "THE-SIGNIN-TOKEN";
                    } else {
                        v = DummyValue.getDummyObjectValue(parts[0].trim());
                    }
                    data.headers.put(k, v == null ? "" : v.toString());
                }
                --j;
                continue;
            }

            if (line.startsWith("### output")) {
                ++j;
                setOutput(getBlock(j, lineLen, lines).trim());
                --j;
            }
        }

        getExceptions();
        setAccess();
        return data;
    }

    @SuppressWarnings("unchecked")
    private void setInput(String block) {
        Map<String, Object> outputSample;
        if (block.startsWith("{{")) {
            JsonBlock jb = getJsonBlock(block);
            outputSample = jb == null ? null : (Map<String, Object>) jb.getData();

        } else if (block.startsWith("JSON")) {
            String json = StringUtil.remove(block, "JSON").trim();
            if (json.startsWith("[")) {
                data.inputSample = Json.d.listFromJson(json, Object.class);
                return;
            } else {
                outputSample = Json.d.mapFromJson(json, String.class, Object.class);
            }
        } else {
            String[] items = StringUtil.split(block, "\n");
            if (items == null) {
                outputSample = null;
            } else  {
                outputSample = new HashMap<>(30, 1);
                for (String item : items) {
                    // "type name: description"
                    item = StringUtil.remove(item, '*').trim();
                    String[] parts = StringUtil.split(StringUtil.split(item, ':')[0], ' ');
                    outputSample.put(parts[1].trim(), DummyValue.getDummyObjectValue(parts[0].trim()));
                }
            }
        }

        if (outputSample == null) {
            return;
        }
        if (data.inputSample == null) {
            data.inputSample = outputSample;
        } else if (data.inputSample instanceof Map) {
            ((Map<String, Object>) data.inputSample).putAll(outputSample);
        }
    }

    private void setOutput(String block) {
        if (block.contains("<<<insert>>>")) {
            HashMap<String, Object> outputSample = new HashMap<>(7, 1);
            outputSample.put("code", 200);
            outputSample.put("message", "INSERT RESULT MESSAGE");
            outputSample.put("dto", "THE INSERTED DTO OBJECT");
            outputSample.put("value", 1);
            outputSample.put("successful", true);
            data.outputSample = outputSample;
            return;
        }
        if (block.contains("<<<update>>>")) {
            HashMap<String, Object> outputSample = new HashMap<>(7, 1);
            outputSample.put("code", 200);
            outputSample.put("message", "UPDATE RESULT MESSAGE");
            outputSample.put("dto", "THE UPDATED DTO OBJECT");
            outputSample.put("value", 1);
            outputSample.put("successful", true);
            data.outputSample = outputSample;
            return;
        }
        if (block.contains("<<<delete>>>")) {
            HashMap<String, Object> outputSample = new HashMap<>(7, 1);
            outputSample.put("code", 200);
            outputSample.put("message", "DELETE RESULT MESSAGE");
            outputSample.put("dto", "THE DELETE DTO OBJECT");
            outputSample.put("value", 1);
            outputSample.put("successful", true);
            data.outputSample = outputSample;
            return;
        }
        if (block.startsWith("{{")) {
            JsonBlock jb = getJsonBlock(block);
            data.outputSample = jb == null ? null : jb.getData();
            return;
        }
        if (block.startsWith("JSON")) {
            data.outputSample = Json.d.mapFromJson(StringUtil.remove(block, "JSON").trim(), String.class, Object.class);
            return;
        }
        if (block.startsWith("VALUE")) {
            data.outputSample = StringUtil.remove(block, "VALUE").trim();
            return;
        }
        HashMap<String, Object> outputSample = new HashMap<>(7, 1);
        data.outputSample = outputSample;
        String[] items = StringUtil.split(block, "\n");
        if (items != null) {
            for (String item : items) {
                // "type name: description"
                item = StringUtil.remove(item, '*').trim();
                String[] parts = StringUtil.split(StringUtil.split(item, ':')[0], ' ');
                outputSample.put(parts[1].trim(), DummyValue.getDummyObjectValue(parts[0].trim()));
            }
        }
    }

    private void getExceptions() {
        data.exceptions = new HashSet<>(10);
        if (method == null) {
            return;
        }

        Set<Class<?>> exceptions = new HashSet<>(Arrays.asList(method.getExceptionTypes()));
        if (method.isAnnotationPresent(Access.class)) {
            exceptions.add(com.vantar.exception.AuthException.class);
        }

        for (Class<?> e : exceptions) {
            if (e.getSimpleName().equals("InputException")) {
                data.exceptions.add(400);
                continue;
            }
            if (e.getSimpleName().equals("AuthException")) {
                data.exceptions.add(401);
                data.exceptions.add(403);
                continue;
            }
            if (e.getSimpleName().equals("ServerException")) {
                data.exceptions.add(500);
                continue;
            }
            if (e.getSimpleName().equals("NoContentException")) {
                data.exceptions.add(204);
                continue;
            }
            if (e.getSimpleName().equals("VantarException")) {
                data.exceptions.add(204);
                data.exceptions.add(400);
                data.exceptions.add(401);
                data.exceptions.add(403);
                data.exceptions.add(500);
                continue;
            }
            data.exceptions.add(500);
        }
    }

    private void setAccess() {
        if (method == null) {
            return;
        }
        if (method.isAnnotationPresent(VerifyPermission.class)) {
            data.access = "VerifyPermission";
            return;
        }
        Feature f = method.getAnnotation(Feature.class);
        if (f != null) {
            data.access = "Feature: " + f.value();
            return;
        }
        Access a = method.getAnnotation(Access.class);
        if (a != null) {
            StringBuilder sa = new StringBuilder(300);
            for (String s : a.value()) {
                sa.append("* role: ").append(s).append("\n");
            }
            data.access = "Access: " + sa.toString();
        }
    }

    private JsonBlock getJsonBlock(String md) {
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
                String jsonBlock = sb.toString();
                sb.setLength(0);
                JsonBlock block = Json.d.fromJson(
                    StringUtil.replace(StringUtil.replace(jsonBlock, "}}", "}"), "{{", "{"),
                    JsonBlock.class
                );
                block.setEnums(enums);
                return block;
            }
        }
        return null;
    }

    private static String getBlock(int j, int lineLen, String[] lines) {
        StringBuilder block = new StringBuilder(500);
        for (; j < lineLen; ++j) {
            String lineInside = lines[j];
            if (lineInside.startsWith("##")) {
                return block.toString();
            }
            block.append(lineInside).append('\n');
        }
        return block.toString();
    }


    public static class Data {

        public String url;
        public String documentUrl;
        public String httpMethod;
        public String access;
        public Map<String, String> headers;
        public Set<String> files;
        public Object inputSample;
        public String inputSampleJson;
        public Object outputSample;
        public Set<Integer> exceptions;
    }
}
