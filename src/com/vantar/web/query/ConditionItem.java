package com.vantar.web.query;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import java.util.Map;

/**
 * Condition item definition
 */
public class ConditionItem {

    public String col;
    public String colB;
    public String type;
    public Object value;
    public Object[] values;
    public Map<String, Object> objects;
    public Condition condition;
    public String dto;


    public Class<?> getValueDataType(Class<?> preferredDataType) {
        if (preferredDataType == null || preferredDataType.isEnum()) {
            return String.class;
        }
        if (preferredDataType == String.class
            || preferredDataType == DateTime.class
            || preferredDataType == Character.class
            || preferredDataType == Boolean.class
            || ClassUtil.isInstantiable(preferredDataType, Number.class)) {
            return preferredDataType;
        }

        Class<?> classType;
        if (value != null) {
            classType = value.getClass();
        } else if (values == null || values.length == 0) {
            return String.class;
        } else {
            classType = values[0].getClass();
        }

        return classType.isEnum() || CollectionUtil.isCollectionOrMapOrArray(classType) ? String.class : classType;
    }

    public void separateCols() {
        if (StringUtil.contains(col, VantarParam.SEPARATOR_KEY_VAL)) {
            String[] parts = StringUtil.splitTrim(col, VantarParam.SEPARATOR_KEY_VAL);
            col = parts[0];
            colB = parts[1];
        }
    }

    public Dto getDto() {
        DtoDictionary.Info info = DtoDictionary.get(dto);
        return info == null ? null : info.getDtoInstance();
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }
}
