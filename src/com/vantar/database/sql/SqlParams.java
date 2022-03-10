package com.vantar.database.sql;

import com.vantar.database.dto.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.*;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class SqlParams {

    private static final String DEFAULT_GLUE = " AND ";
    private List<Object> values = new ArrayList<>();
    private StringBuilder templateAppendable;
    private List<String> templateList;
    private String glue;


    public SqlParams() {

    }

    public SqlParams(Dto dto) {
        this(StorableData.toMap(dto.getStorableData()));
    }

    public SqlParams(Dto dto, String glue) {
        this(StorableData.toMap(dto.getStorableData()), glue);
    }

    public SqlParams(Map<String, Object> params) {
        this(params, DEFAULT_GLUE);
    }

    public SqlParams(Map<String, Object> params, String glue) {
        templateAppendable = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            templateAppendable.append(param.getKey()).append("=?").append(glue);
            Object value = param.getValue();
            if (value instanceof DateTime) {
                value = SqlUtil.getDateTimeAsSql(value);
            }
            values.add(value);
        }

        if (templateAppendable.length() > 0) {
            templateAppendable.setLength(templateAppendable.length() - glue.length());
        }
    }

    public void addValue(Object value) {
        values.add(value);
    }

    public void addValues(List<Object> values) {
        this.values.addAll(values);
    }

    public void addValues(Object[] values) {
        this.values.addAll(Arrays.asList(values));
    }

    public void setValue(List<Object> values) {
        this.values = values;
    }

    public void addTemplate(String template) {
        if (templateList == null) {
            templateList = new ArrayList<>();
        }
        templateList.add(template);
    }

    public void appendTemplate(String template) {
        if (templateAppendable == null) {
            templateAppendable = new StringBuilder();
        }
        templateAppendable.append(template);
    }

    public Object[] getValues() {
        return getNormalizedValues();
    }

    public Object[] getValues(SqlParams... orderedParams) {
        for (SqlParams params : orderedParams) {
            values.addAll(params.values);
        }
        return getNormalizedValues();
    }

    private Object[] getNormalizedValues() {
        Object[] array = new Object[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            array[i] = value instanceof Collection || value instanceof Map ? Json.d.toJson(value) : value;
        }
        return array;
    }

    public String getTemplate() {
        return templateAppendable == null ?
            CollectionUtil.join(templateList, glue == null ? DEFAULT_GLUE : glue) :
            templateAppendable.toString();
    }

    public String getTemplate(String glue) {
        this.glue = glue;
        return getTemplate();
    }

    public String getTemplate(int backtrackLength) {
        if (templateAppendable == null) {
            return "";
        }
        templateAppendable.setLength(templateAppendable.length() - backtrackLength);
        return templateAppendable.toString();
    }

    public void setGlue(String glue) {
        this.glue = glue;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }
}