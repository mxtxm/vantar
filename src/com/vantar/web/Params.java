package com.vantar.web;

import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.web.query.QueryData;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.auth.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.file.*;
import com.vantar.util.json.*;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Params {

    private static final Logger log = LoggerFactory.getLogger(Params.class);
    public static final Map<Long, Params> threadParams = new ConcurrentHashMap<>(100, 1);


    public synchronized static void setThreadParams(Params params) {
        threadParams.put(Thread.currentThread().getId(), params);
    }

    public synchronized static void removeThreadParams() {
        threadParams.remove(Thread.currentThread().getId());
    }

    public synchronized static Params getThreadParams() {
        return threadParams.get(Thread.currentThread().getId());
    }



    public HttpServletRequest request;
    public Type type;


    public enum Type {
        FORM_DATA,
        JSON,
        MULTI_PART,
        MAP,
    }


    private Map<String, Object> map;
    private boolean typeMisMatch;
    private String json;
    private Set<String> ignoreParams;


    public Params() {

    }

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

    public Params(Params params) {
        request = null;
        type = Type.MAP;
        map = new HashMap<>(20, 1);

        Enumeration<String> parameterNames = params.request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String k = parameterNames.nextElement();
            map.put(k, params.request.getParameter(k));
        }
    }

    public void removeParams(String... params) {
        ignoreParams = new HashSet<>(Arrays.asList(params));
    }

    public synchronized void set(String key, Object value) {
        if (map == null) {
            map = new ConcurrentHashMap<>(10, 1);
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
                log.error(" !! getIp failed\n", e);
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

    public Map<String, String> getHeaders(String... exclude) {
        Map<String, String> headers = new HashMap<>(15, 1);
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!CollectionUtil.contains(exclude, name)) {
                headers.put(name, request.getHeader(name));
            }
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
                return "MAP: " + Json.d.toJsonPretty(map);
            case FORM_DATA:
                return "FORM-DATA: " + Json.d.toJsonPretty(getRequestParams());
            case JSON:
                return "JSON: " + getJson();
            case MULTI_PART:
                return "MULTI_PART: " + Json.d.toJsonPretty(getRequestParams());
        }
        return type + ": N/A";
    }

    public String toJsonString() {
        switch (type) {
            case MAP:
                return Json.d.toJson(map);
            case FORM_DATA:
            case MULTI_PART:
                return Json.d.toJson(getRequestParams());
            case JSON:
                return getJson();
        }
        return "{}";
    }

    public Map<String, Object> toMap() {
        switch (type) {
            case MAP:
                return map;
            case FORM_DATA:
            case MULTI_PART:
                return getRequestParams();
            case JSON:
                return Json.d.mapFromJson(getJson(), String.class, Object.class);
        }
        return null;
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
        Map<String, Object> params = map == null ? new HashMap<>(50, 1) : map;
        if (request == null) {
            return params;
        }
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();

            if (ignoreParams != null && ignoreParams.contains(key)) {
                continue;
            }

            String[] values = request.getParameterValues(key);
            params.putIfAbsent(key, values != null && values.length > 1 ? values : request.getParameter(key));
        }
        return params;
    }

    public Map<String, Object> getRequestParams() {
        Map<String, Object> params = new HashMap<>(50, 1);
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



    @SuppressWarnings("unchecked")
    public <T> T getX(String key) {
        return map == null ? null : (T) map.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getX(String key, T defaultValue) {
        T obj = map == null ? defaultValue : (T) map.get(key);
        return obj == null ? defaultValue : obj;
    }

    public String getParameter(String key) {
        if (ignoreParams != null && ignoreParams.contains(key)) {
            return null;
        }

        if (map != null) {
            Object obj = map.get(key);
            if (obj != null) {
                return obj.toString();
            }
        }

        String v = request == null ? null : request.getParameter(key);
        return v == null ? null : StringUtil.remove(v, '\r');
    }

    public Object getObjectRequired(String key, Class<?> typeClass) throws InputException {
        Object v = getObject(key, typeClass);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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
            return EnumUtil.getEnumValue(getString(key), typeClass);
        }
        return null;
    }

    public String getStringRequired(String key) throws InputException {
        String v = getString(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        typeMisMatch = false;
        String value = getParameter(key);
        return value == null || value.isEmpty() ? defaultValue : value.trim();
    }

    public char getCharacterRequired(String key) throws InputException {
        Character v = getCharacter(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public int getIntegerRequired(String key) throws InputException {
        Integer v = getInteger(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public long getLongRequired(String key) throws InputException {
        Long v = getLong(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public double getDoubleRequired(String key) throws InputException {
        Double v = getDouble(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public float getFloatRequired(String key) throws InputException {
        Float v = getFloat(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public boolean getBooleanRequired(String key) throws InputException {
        Boolean v = getBoolean(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public DateTime getDateTimeRequired(String key) throws InputException {
        DateTime v = getDateTime(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
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

    public DateTime getDateTime(String key, DateTime defaultValue) {
        typeMisMatch = false;
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
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



    public <T> List<T> getListRequired(String key, Class<T> type) throws InputException {
        List<T> v = getList(key, type);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public <T> List<T> getList(String key, Class<T> type) {
        if (ignoreParams != null && ignoreParams.contains(key)) {
            return null;
        }

        Object object = request == null ? null : request.getParameterValues(key);
        boolean isEmpty = object == null || ((String[]) object).length <= 1;
        if (isEmpty) {
            object = getParameter(key);
            isEmpty = StringUtil.isEmpty((String) object);
        }
        List<T> list = CollectionUtil.toList(object, type);
        typeMisMatch = (list == null || list.isEmpty()) && isEmpty;
        return list;
    }

    public <T> Set<T> getSetRequired(String key, Class<T> type) throws InputException {
        Set<T> v = getSet(key, type);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public <T> Set<T> getSet(String key, Class<T> typeClass) {
        typeMisMatch = false;
        String value = getParameter(key);
        if (value == null || value.isEmpty() ) {
            return new HashSet<>(1, 1);
        }
        return new HashSet<>(getListFromJson(value, typeClass));
    }

    private <T> List<T> getListFromJson(String value, Class<T> typeClass) {
        try {
            return Json.d.listFromJson(value, typeClass);
        } catch (Exception e) {
            typeMisMatch = true;
            return new ArrayList<>(1);
        }
    }

    public List<Integer> getIntegerListRequired(String key) throws InputException {
        List<Integer> v = getIntegerList(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public List<Integer> getIntegerList(String key) {
        return getList(key, Integer.class);
    }

    public Set<Integer> getIntegerSetRequired(String key) throws InputException {
        Set<Integer> v = getIntegerSet(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Set<Integer> getIntegerSet(String key) {
        List<Integer> value = getIntegerList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Long> getLongListRequired(String key) throws InputException {
        List<Long> v = getLongList(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public List<Long> getLongList(String key) {
        return getList(key, Long.class);
    }

    public Set<Long> getLongSetRequired(String key) throws InputException {
        Set<Long> v = getLongSet(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Set<Long> getLongSet(String key) {
        List<Long> value = getLongList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Double> getDoubleListRequired(String key) throws InputException {
        List<Double> v = getDoubleList(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public List<Double> getDoubleList(String key) {
        return getList(key, Double.class);
    }

    public Set<Double> getDoubleSetRequired(String key) throws InputException {
        Set<Double> v = getDoubleSet(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Set<Double> getDoubleSet(String key) {
        List<Double> value = getDoubleList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<Float> getFloatListRequired(String key) throws InputException {
        List<Float> v = getFloatList(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public List<Float> getFloatList(String key) {
        return getList(key, Float.class);
    }

    public Set<Float> getFloatSetRequired(String key) throws InputException {
        Set<Float> v = getFloatSet(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Set<Float> getFloatSet(String key) {
        List<Float> value = getFloatList(key);
        return value == null ? null : new HashSet<>(value);
    }

    public List<String> getStringListRequired(String key) throws InputException {
        List<String> v = getStringList(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public List<String> getStringList(String key) {
        return getList(key, String.class);
    }

    public Set<String> getStringSetRequired(String key) throws InputException {
        Set<String> v = getStringSet(key);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Set<String> getStringSet(String key) {
        List<String> value = getStringList(key);
        return value == null ? null : new HashSet<>(value);
    }



    // > > > GET ENUM



    public <E extends Enum<?>> E getEnumRequired(String key, Class<E> type) throws InputException {
        E v = getEnum(key, type);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public <E extends Enum<?>> E getEnum(String key, Class<E> type) {
        return getEnum(key, type, null);
    }

    public <E extends Enum<?>> E getEnum(String key, Class<E> type, E defaultValue) {
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return EnumUtil.getEnumValue(value, type);
    }



    // > > > GET JSON



    public String getJson() {
        if (fileUploaded) {
            return "";
        }
        if (json != null) {
            return json;
        }
        if (request == null) {
            return "";
        }
        json = (String) request.getAttribute("__json__");
        if (json != null) {
            return json;
        }


        StringBuilder buffer = new StringBuilder(1000);
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IllegalStateException ignore) {
            return "";
        } catch (Exception e) {
            log.error(" !! JSON input\n", e);
            return "";
        }

        json = buffer.toString();
        request.setAttribute("__json__", json);
        return json;
    }

    public <T> T getJson(Class<T> typeClass) {
        return Json.d.fromJson(getJson(), typeClass);
    }

    public <T> List<T> getJsonList(Class<T> typeClass) {
        if (request == null) {
            return null;
        }
        return Json.d.listFromJson(getJson(), typeClass);
    }

    public <T> List<T> getJsonListRequired(Class<T> typeClass) throws InputException {
        if (request == null) {
            throw new InputException(VantarKey.REQUIRED, "-");
        }
        List<T> v = Json.d.listFromJson(getJson(), typeClass);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, "-");
        }
        return v;
    }

    public <K, V> Map<K,V> getJsonMap(Class<K> typeClassKey, Class<V> typeClassValue) {
        if (request == null) {
            return null;
        }
        return Json.d.mapFromJson(getJson(), typeClassKey, typeClassValue);
    }

    public <K, V> Map<K,V> getJsonMapRequired(Class<K> typeClassKey, Class<V> typeClassValue) throws InputException {
        if (request == null) {
            throw new InputException(VantarKey.REQUIRED, "-");
        }
        Map<K, V> v = Json.d.mapFromJson(getJson(), typeClassKey, typeClassValue);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, "-");
        }
        return v;
    }

    public <T> T extractFromJson(String key, Class<T> type) {
        return Json.d.extract(getJson(), key, type);
    }

    public <T> T extractFromJsonRequired(String key, Class<T> type) throws InputException {
        T v = Json.d.extract(getJson(), key, type);
        if (v == null) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public <T> T extractFromJson(String key, Class<T> type, Object defaultValue) {
        T value = Json.d.extract(getJson(), key, type);
        return value == null ? (T) defaultValue : value;
    }



    // > > > GET DATABASE QUERY



    public QueryBuilder getQueryBuilder() {
        return getQueryBuilder((Dto) null);
    }

    public QueryBuilder getQueryBuilder(Dto dto) {
        QueryData q = getJson(QueryData.class);
        if (q == null) {
            return null;
        }
        if (q.lang != null) {
            set(VantarParam.LANG, q.lang);
        }
        return q.getQueryBuilder(dto);
    }

    public QueryBuilder getQueryBuilder(String key) {
        return getQueryBuilder(key, null);
    }

    public QueryBuilder getQueryBuilder(String key, Dto dto) {
        String json = getString(key);
        if (json == null) {
            return null;
        }
        QueryData q = Json.d.fromJson(json, QueryData.class);
        if (q == null) {
            return null;
        }
        if (q.lang != null) {
            set(VantarParam.LANG, q.lang);
        }
        return q.getQueryBuilder(dto);
    }



    // > > > QUERY REQUEST



    public Map<String, Object> queryParams(QueryParams q) {
        Map<String, Object> params = map == null ? new HashMap<>(20, 1) : map;
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



    public Location getLocationRequired(String key) throws InputException {
        Location v = getLocation(key);
        if (v == null || v.isEmpty() || !v.isValid()) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public Location getLocation(String key) {
        String locationString = getString(key);
        Location location = locationString != null ?
            new Location(locationString) :
            new Location(
                getDouble(key + "_latitude"),
                getDouble(key + "_longitude"),
                getDouble(key + "_altitude"),
                getString(key + "_countryCode")
            );
        return location.isEmpty() ? null : location;
    }



    // > > > DATETIME RANGE



    public DateTimeRange getDateTimeRangeDefaultNow(String dateMin, String dateMax) {
        DateTimeRange range = getDateTimeRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange().setDefaultRange();
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

    public DateTimeRange getDateTimeRangeRequired(String dateMin, String dateMax) throws InputException {
        DateTimeRange v = getDateTimeRange(dateMin, dateMax);
        if (v == null || v.isEmpty() || !v.isValid()) {
            throw new InputException(VantarKey.REQUIRED, dateMin + "," + dateMax);
        }
        return v;
    }

    public DateTimeRange getDateTimeRange(String dateMin, String dateMax) {
        DateTimeRange range = new DateTimeRange(getDateTime(dateMin), getDateTime(dateMax));
        range.adjustDateTimeRange();
        return range;
    }

    public DateTimeRange getDateTimeRangeRequired(String key) throws InputException {
        DateTimeRange v = getDateTimeRange(key);
        if (v == null || v.isEmpty() || !v.isValid()) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public DateTimeRange getDateTimeRange(String key) {
        try {
            DateTimeRange range = new DateTimeRange(getString(key));
            range.adjustDateTimeRange();
            return range;
        } catch (DateTimeException e) {
            return null;
        }
    }

    public DateTimeRange getDateRangeDefaultNow(String dateMin, String dateMax) {
        DateTimeRange range = getDateRange(dateMin, dateMax);
        if (!range.isValid()) {
            range = new DateTimeRange().setDefaultRange();
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

    public DateTimeRange getDateRangeRequired(String dateMin, String dateMax) throws InputException {
        DateTimeRange v = getDateRange(dateMin, dateMax);
        if (v == null || v.isEmpty() || !v.isValid()) {
            throw new InputException(VantarKey.REQUIRED, dateMin + "," + dateMax);
        }
        return v;
    }

    public DateTimeRange getDateRange(String dateMin, String dateMax) {
        DateTimeRange range = new DateTimeRange(getDateTime(dateMin), getDateTime(dateMax));
        range.adjustDateRange();
        return range;
    }

    public DateTimeRange getDateRangeRequired(String key) throws InputException {
        DateTimeRange v = getDateRange(key);
        if (v == null || v.isEmpty() || !v.isValid()) {
            throw new InputException(VantarKey.REQUIRED, key);
        }
        return v;
    }

    public DateTimeRange getDateRange(String key) {
        try {
            DateTimeRange range = new DateTimeRange(getString(key));
            range.adjustDateRange();
            return range;
        } catch (DateTimeException e) {
            return null;
        }
    }



    // > > > UPLOAD



    private List<String> uploadFiles;
    private boolean fileUploaded = false;

    public List<String> getUploadFiles() {
        return uploadFiles;
    }

    public Uploaded upload(String name) {
        if (request == null) {
            log.error("! bad upload request request=null");
            return new Uploaded(VantarKey.REQUIRED);
        }
        String contentType = request.getContentType();
        if (!"post".equalsIgnoreCase(request.getMethod())) {
            log.error("! bad upload request method={} content-type={}", request.getMethod(), contentType);
            return new Uploaded(VantarKey.MUST_BE_POST_MULTIPART);
        }
        if (contentType == null || !contentType.toLowerCase().contains("multipart")) {
            log.error("! bad upload request method={} content-type={}", request.getMethod(), contentType);
            return new Uploaded(VantarKey.MUST_BE_POST_MULTIPART);
        }

        fileUploaded = true;
        try {
            Part filePart = request.getPart(name);
            if (filePart == null || filePart.getSize() == 0) {
                return new Uploaded(VantarKey.REQUIRED);
            }
            Uploaded upload = new Uploaded(filePart);
            if (uploadFiles == null) {
                uploadFiles = new ArrayList<>(10);
            }
            uploadFiles.add(upload.getOriginalFilename());
            return upload;
        } catch (IOException | ServletException e) {
            log.error("! bad upload request key={} method={} content-type={}", name, request.getMethod(), contentType, e);
            return new Uploaded(VantarKey.IO_ERROR);
        }
    }


    public static class Uploaded implements Closeable {

        private VantarKey error;
        private Part filePart;
        private FileType fileType;


        public Uploaded(VantarKey error) {
            if (error != null) {
                this.error = error;
                return;
            }
            if (!isUploaded()) {
                this.error = VantarKey.REQUIRED;
            }
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
            return filePart != null && filePart.getContentType() != null
                && filePart.getSubmittedFileName() != null;
        }

        public boolean isUploadedOk() {
            return error == null && filePart != null && filePart.getContentType() != null
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

        public String getMimeType() {
            setFileType();
            return fileType == null ? "" : fileType.getMimeType();
        }

        private void setFileType() {
            if (fileType == null) {
                try {
                    fileType = new FileType(filePart.getInputStream(), filePart.getSubmittedFileName());
                } catch (IOException ignore) {

                }
            }
        }

        public String getOriginalFilename() {
            return filePart.getSubmittedFileName();
        }

        public String getOriginalExtension() {
            String[] parts = StringUtil.split(filePart.getSubmittedFileName(), '.');
            return parts.length < 2 ? "" : parts[parts.length - 1];
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
            DirUtil.makeDirectory(path);
            path += filename == null ? (Paths.get(filePart.getSubmittedFileName()).getFileName().toString()) : filename;

            Path p = Paths.get(path);

            try {
                Files.delete(p);
            } catch (Exception ignore) {

            }
            try {
                Files.copy(filePart.getInputStream(), p);
                return true;
            } catch (Exception e) {
                log.error(" !! upload inputStream > ({}/{})\n", path, filename, e);
                error = VantarKey.IO_ERROR;
                return false;
            }
        }

        public String toString() {
            return ObjectUtil.toStringViewable(this);
        }

        public void close() {
            try {
                filePart.getInputStream().close();
            } catch (Exception ignore) {

            }
        }
    }

    public CommonUser getCurrentUser() throws AuthException {
        CommonUser user = ServiceAuth.getCurrentSignedInUser(this);
        if (user == null) {
            throw new AuthException(VantarKey.EXPIRED_AUTH_TOKEN);
        }
        return user;
    }

    public CommonUser getCurrentUserIfExists() {
        return ServiceAuth.getCurrentSignedInUser(this);
    }
}