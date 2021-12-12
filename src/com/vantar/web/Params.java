package com.vantar.web;

import com.google.gson.JsonSyntaxException;
import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.query.QueryData;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.file.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;


public class Params {

    private static final Logger log = LoggerFactory.getLogger(Params.class);

    public final HttpServletRequest request;
    public final Type type;


    public enum Type {
        FORM_DATA,
        JSON,
        MULTI_PART,
        MAP,
    }


    private Map<String, Object> map;
    private boolean typeMisMatch;
    private String json;


    public Params(HttpServletRequest request) {
       this.request = request;

        String contentType = getHeader("content-type");
        if (contentType == null) {
            type = Type.FORM_DATA;
        } else if (StringUtil.contains(contentType, "json")) {
            type = Type.JSON;
        } else if (StringUtil.contains(contentType, "multipart")) {
            type = Type.MULTI_PART;
        } else {
            type = Type.FORM_DATA;
        }
    }

    public Params(Map<String, Object> map) {
        request = null;
        this.map = map;
        type = Type.MAP;
    }

    public void set(String key, Object value) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
    }

    public String getMethod() {
        return request == null ? null : request.getMethod();
    }

    public String getIp() {
        if (request == null) {
            return null;
        }
        String ip = request.getRemoteAddr();
        if (ip.equalsIgnoreCase("0:0:0:0:0:0:0:1")) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.error("getIp failed", e);
            }
        }
        return ip;
    }

    public String getBaseUrl() {
        return request.getScheme() + "://" + request.getServerName();
    }

    public String getCurrentUrl() {
        return request.getScheme() + "://" + request.getServerName() +
            ("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(request.getScheme())
                && request.getServerPort() == 443 ? "" : ":" + request.getServerPort() ) +
            request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
    }

    public String getHeader(String key) {
        return request == null ? null : request.getHeader(key);
    }

    public String getHeader(String key, String defaultValue) {
        String header = getHeader(key);
        return StringUtil.isEmpty(header) ? defaultValue : header;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    public String getLang() {
        return getHeader(VantarParam.HEADER_LANG, getString(VantarParam.LANG, Locale.getDefaultLocale()));
    }

    public String getLangNoDefault() {
        return getHeader(VantarParam.HEADER_LANG, getString(VantarParam.LANG));
    }

    @Override
    public String toString() {
        switch (type) {
            case MAP:
                return "MAP: " + Json.toJsonPretty(map);
            case FORM_DATA:
                return "FORM-DATA: " + Json.toJsonPretty(request.getParameterMap());
            case JSON:
                return "JSON: " + getJson();

            case MULTI_PART:
                return "MULTI_PART: " + Json.toJsonPretty(request.getParameterMap());
        }
        return type + ": N/A";
    }

    public boolean contains(String key) {
        String v = request == null ? null : request.getParameter(key);
        if (v != null) {
            return true;
        }
        return map != null && map.containsKey(key);
    }

    public boolean isChecked(String key) {
        return request != null && request.getParameter(key) != null;
    }

    public Map<String, Object> getAll() {
        Map<String, Object> params = map == null ? new HashMap<>() : map;
        if (request == null) {
            return params;
        }
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            String[] values = request.getParameterValues(key);
            params.putIfAbsent(key, values != null && values.length > 1 ? values : request.getParameter(key));
        }
        return params;
    }

    public boolean getTypeMissMatch() {
        return typeMisMatch;
    }



    // > > > GET



    public String getParameter(String key) {
        String v = request == null ? null : request.getParameter(key);
        if (v != null) {
            return StringUtil.remove(v, '\r');
        }
        Object obj = map == null ? null : map.get(key);
        return obj == null ? null : obj.toString();
    }

    public Object getObject(String key, Class<?> typeClass) {
        if (typeClass.equals(String.class)) {
            return getString(key);
        }
        if (typeClass.equals(Long.class)) {
            return getLong(key);
        }
        if (typeClass.equals(Integer.class)) {
            return getInteger(key);
        }
        if (typeClass.equals(Double.class)) {
            return getDouble(key);
        }
        if (typeClass.equals(Float.class)) {
            return getFloat(key);
        }
        if (typeClass.equals(Boolean.class)) {
            return getBoolean(key);
        }
        if (typeClass.equals(Character.class)) {
            return getCharacter(key);
        }
        if (typeClass.equals(DateTime.class)) {
            return getDateTime(key);
        }
        if (typeClass.isEnum()) {
            return EnumUtil.getEnumValue(typeClass, getString(key));
        }
        return null;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        typeMisMatch = false;
        String value = getParameter(key);
        return value == null || value.isEmpty() ? defaultValue : value.trim();
    }

    public Character getCharacter(String key) {
        return getCharacter(key, null);
    }

    public Character getCharacter(String key, Character defaultValue) {
        String value = getParameter(key);
        typeMisMatch = StringUtil.isNotEmpty(value) && value.length() > 1;
        Character c = StringUtil.toCharacter(value);
        return c == null ? defaultValue : c;
    }

    public Integer getInteger(String key) {
        return getInteger(key, null);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        String value = getParameter(key);
        Integer number = StringUtil.toInteger(value);
        boolean isNull = number == null;
        typeMisMatch = isNull && StringUtil.isNotEmpty(value);
        return isNull ? defaultValue : number;
    }

    public Long getLong(String key) {
        return getLong(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        String value = getParameter(key);
        Long number = StringUtil.toLong(value);
        boolean isNull = number == null;
        typeMisMatch = isNull && StringUtil.isNotEmpty(value);
        return isNull ? defaultValue : number;
    }

    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    public Double getDouble(String key, Double defaultValue) {
        String value = getParameter(key);
        Double number = StringUtil.toDouble(value);
        boolean isNull = number == null;
        typeMisMatch = isNull && StringUtil.isNotEmpty(value);
        return isNull ? defaultValue : number;
    }

    public Float getFloat(String key) {
        return getFloat(key, null);
    }

    public Float getFloat(String key, Float defaultValue) {
        String value = getParameter(key);
        Float number = StringUtil.toFloat(value);
        boolean isNull = number == null;
        typeMisMatch = isNull && StringUtil.isNotEmpty(value);
        return isNull ? defaultValue : number;
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getParameter(key);
        Boolean b = StringUtil.toBoolean(value);
        boolean isNull = b == null;
        typeMisMatch = isNull && StringUtil.isNotEmpty(value);
        return isNull ? defaultValue : b;
    }

    public DateTime getDateTime(String key) {
        typeMisMatch = false;
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new DateTime(value);
        } catch (DateTimeException e) {
            typeMisMatch = true;
            return null;
        }
    }

    public DateTime getDateTime(String key, String defaultValue) {
        typeMisMatch = false;
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }
        try {
            return new DateTime(value);
        } catch (DateTimeException e) {
            typeMisMatch = true;
            return null;
        }
    }


    // > > > GET COLLECTION



    public <T> List<T> getList(String key, Class<T> type) {
        Object object = request == null ? null : request.getParameterValues(key);
        boolean isEmpty = object == null || ((String[]) object).length <= 1;
        if (isEmpty) {
            object = getParameter(key);
            isEmpty = StringUtil.isEmpty((String) object);
        }
        List<T> list = ObjectUtil.getList(object, type);
        typeMisMatch = (list == null || list.isEmpty()) && isEmpty;
        return list;
    }

    public <T> Set<T> getSet(String key, Class<T> typeClass) {
        typeMisMatch = false;
        String value = getParameter(key);
        if (value == null || value.isEmpty() ) {
            return new HashSet<>();
        }
        return new HashSet<>(getListFromJson(value, typeClass));
    }

    private <T> List<T> getListFromJson(String value, Class<T> typeClass) {
        try {
            return Json.listFromJson(value, typeClass);
        } catch (JsonSyntaxException e) {
            log.error("!", e);
            typeMisMatch = true;
            return new ArrayList<>();
        }
    }

    public List<Integer> getIntegerList(String key) {
        return getList(key, Integer.class);
    }

    public Set<Integer> getIntegerSet(String key) {
        List<Integer> value = getIntegerList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Long> getLongList(String key) {
        return getList(key, Long.class);
    }

    public Set<Long> getLongSet(String key) {
        List<Long> value = getLongList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Double> getDoubleList(String key) {
        return getList(key, Double.class);
    }

    public Set<Double> getDoubleSet(String key) {
        List<Double> value = getDoubleList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Float> getFloatList(String key) {
        return getList(key, Float.class);
    }

    public Set<Float> getFloatSet(String key) {
        List<Float> value = getFloatList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<String> getStringList(String key) {
        return getList(key, String.class);
    }

    public Set<String> getStringSet(String key) {
        List<String> value = getStringList(key);
        return value == null ? null : new HashSet<>(value);
    }



    // > > > GET JSON



    public String getJson() {
        if (json != null) {
            return json;
        }
        if (request == null) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            log.error("!", e);
            return "";
        }

        json = buffer.toString();
        return json;
    }

    public <T> T getJson(Class<T> typeClass) {
        return Json.fromJson(getJson(), typeClass);
    }

    public <T> List<T> getJsonList(Class<T> typeClass) {
        if (request == null) {
            return null;
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            log.error("!", e);
            return null;
        }

        return Json.listFromJson(buffer.toString(), typeClass);
    }

    public <K, V> Map<K,V> getJsonMap(Class<K> typeClassKey, Class<V> typeClassValue) {
        if (request == null) {
            return null;
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            log.error("!", e);
            return null;
        }

        return Json.mapFromJson(buffer.toString(), typeClassKey, typeClassValue);
    }



    // > > > GET DATABASE QUERY



    public QueryData getQueryData() {
        return normalizeQueryData(getJson(QueryData.class));
    }

    public QueryData getQueryData(String key) {
        String json = getString(key);
        return json == null ? null : normalizeQueryData(Json.fromJson(json, QueryData.class));
    }

    private QueryData normalizeQueryData(QueryData q) {
        if (q == null) {
            return null;
        }
        if (q.lang != null) {
            set(VantarParam.LANG, q.lang);
        }
        return q;
    }



    // > > > QUERY REQUEST



    public Map<String, Object> queryParams(QueryParams q) {
        Map<String, Object> params = map == null ? new HashMap<>() : map;
        if (request == null) {
            return params;
        }
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            if (!CollectionUtil.contains(q.include, key)) {
                if (   (CollectionUtil.contains(q.exclude, key))
                    || (q.startsWith != null && !key.startsWith(q.startsWith))
                    || (q.endsWith != null && !key.endsWith(q.endsWith))
                    || (q.contains != null && !key.contains(q.contains))) {

                    continue;
                }
            }
            params.put(key, request.getParameter(key));
        }
        return params;
    }

    public static class QueryParams {

        public String contains;
        public String startsWith;
        public String endsWith;
        public String[] exclude;
        public String[] include;

    }



    // > > > LOCATION



    public Location getLocation(String key) {
        Location location = new Location(getDouble(key + "_latitude"), getDouble(key + "_longitude"));
        return location.isEmpty() ? null : location;
    }



    // > > > DATETIME RANGE



    public DateTimeRange getDateTimeRangeDefaultNow(String dateMin, String dateMax) {
        DateTimeRange range = getDateTimeRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange();
            range.adjustDateTimeRange();
        }
        return range;
    }

    public DateTimeRange getDateTimeRange(String dateMin, String dateMax, int defaultRangeDays) {
        DateTimeRange range = getDateRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange(new DateTime().addDays(-defaultRangeDays), new DateTime());
            range.adjustDateTimeRange();
        }
        return range;
    }

    public DateTimeRange getDateTimeRange(String dateMin, String dateMax) {
        DateTimeRange range = new DateTimeRange(getDateTime(dateMin), getDateTime(dateMax));
        range.adjustDateTimeRange();
        return range;
    }

    public DateTimeRange getDateRangeDefaultNow(String dateMin, String dateMax) {
        DateTimeRange range = getDateRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange();
            range.adjustDateRange();
        }
        return range;
    }

    public DateTimeRange getDateRange(String dateMin, String dateMax, int defaultRangeDays) {
        DateTimeRange range = getDateRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange(new DateTime().addDays(-defaultRangeDays), new DateTime());
            range.adjustDateRange();
        }
        return range;
    }

    public DateTimeRange getDateRange(String dateMin, String dateMax) {
        DateTimeRange range = new DateTimeRange(getDateTime(dateMin), getDateTime(dateMax));
        range.adjustDateRange();
        return range;
    }


    // > > > UPLOAD



    public Uploaded upload(String name) {
        if (request == null) {
            return new Uploaded(VantarKey.REQUIRED);
        }

        try {
            Part filePart = request.getPart(name);
            if (filePart == null || filePart.getSize() == 0) {
                return new Uploaded(VantarKey.REQUIRED);
            }

            return new Uploaded(filePart);
        } catch (IOException | ServletException e) {
            log.error("!", e);
            return new Uploaded(VantarKey.IO_ERROR);
        }
    }

    public static class Uploaded {

        private VantarKey error;
        private Part filePart;
        private FileType fileType;


        public Uploaded(VantarKey error) {
            this.error = error;
        }

        public Uploaded(Part filePart) {
            this.filePart = filePart;
        }

        public VantarKey getError() {
            return error;
        }

        public boolean isIoError() {
            return VantarKey.IO_ERROR == error;
        }

        public boolean isUploaded() {
            return VantarKey.REQUIRED != error && filePart != null && filePart.getContentType() != null
                && filePart.getSubmittedFileName() != null;
        }

        public long getSize() {
            return filePart.getSize();
        }

        public String getType() {
            setFileType();
            return fileType == null ? null : fileType.getType();
        }

        public String getSubType() {
            setFileType();
            return fileType == null ? null : fileType.getSubType();
        }

        public boolean isType(String... type) {
            setFileType();
            return fileType != null && fileType.isType(type);
        }

        private void setFileType() {
            if (fileType == null) {
                try {
                    fileType = new FileType(filePart.getInputStream());
                } catch (IOException ignore) {

                }
            }
        }

        public String getOriginalFilename() {
            return filePart.getSubmittedFileName();
        }

        public boolean moveTo(String path) {
            if (path.charAt(path.length() - 1) == '/') {
                return moveTo(path, null);
            }
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];
            parts[parts.length - 1] = "";
            return moveTo(CollectionUtil.join(parts, '/'), filename);
        }

        public boolean moveTo(String path, String filename) {
            path = StringUtil.rtrim(path, '/') + '/';
            FileUtil.makeDirectory(path);
            path += filename == null ? (Paths.get(filePart.getSubmittedFileName()).getFileName().toString()) : filename;

            Path p = Paths.get(path);

            try {
                Files.delete(p);
            } catch (Exception ignore) {

            }
            try {
                Files.copy(filePart.getInputStream(), p);
                return true;
            } catch (IOException e) {
                log.error("!", e);
                error = VantarKey.IO_ERROR;
                return false;
            }
        }

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }
}