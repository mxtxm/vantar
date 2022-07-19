package com.vantar.database.query.data;

import com.vantar.database.dto.*;
import com.vantar.exception.DateTimeException;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
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


    public Class<?> getValueDataType() {
        Class<?> classType;
        if (value != null) {
            classType = value.getClass();
        } else if (values == null || values.length == 0) {
            return String.class;
        } else  {
            classType = values[0].getClass();
        }
        return CollectionUtil.isCollectionAndMapAndArray(classType) ? String.class : classType;
    }

    public <T> Object getValue(Class<T> classType) {
        return ObjectUtil.convert(value, classType);
    }

    public String[] getValuesAsString() {
        String[] items = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            items[i] = ObjectUtil.toString(values[i]);
        }
        return items;
    }

    public Number[] getValuesAsNumber() {
        Number[] items = new Number[values.length];
        for (int i = 0; i < values.length; i++) {
            Double n = NumberUtil.toDouble(values[i].toString());
            if (n == null) {
                return null;
            }
            items[i] = n;
        }
        return items;
    }

    public DateTime[] getValuesAsDateTime() {
        DateTime[] items = new DateTime[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                items[i] = new DateTime(values[i].toString());
            } catch (DateTimeException e) {
                return null;
            }
        }
        return items;
    }

    public Character[] getValuesAsCharacter() {
        Character[] items = new Character[values.length];
        for (int i = 0; i < values.length; i++) {
            Character c = StringUtil.toCharacter(values[i].toString());
            if (c == null) {
                return null;
            }
            items[i] = c;
        }
        return items;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

    public void separateCols() {
        if (StringUtil.contains(col, ':')) {
            String[] parts = StringUtil.splitTrim(col, ':');
            col = parts[0];
            colB = parts[1];
        }
    }

    public Dto getDto() {
        DtoDictionary.Info info = DtoDictionary.get(dto);
        return info == null ? null : info.getDtoInstance();
    }
}
