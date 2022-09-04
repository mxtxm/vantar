package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class QueryMatchItem {

    public int id;
    
    public String fieldName;
    public QueryOperator type;

    public QueryCondition queryValue;

    public Boolean booleanValue;

    public Character characterValue;
    public Character[] characterValues;

    public String stringValue;
    public String[] stringValues;

    public Number numberValue;
    public Number[] numberValues;

    public Object[] objectValues;

    public DateTime dateTimeValue;
    public DateTime[] dateTimeValues;

    public List<?> itemList;

    boolean returnDatetimeAsString = false;

    public Dto dto;

    // QUERY > > >

    public QueryMatchItem(QueryOperator type, QueryCondition value) {
        this.type = type;
        queryValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, QueryCondition value) {
        this.type = type;
        this.fieldName = fieldName;
        queryValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, QueryCondition value, Dto dto) {
        this.type = type;
        this.fieldName = fieldName;
        queryValue = value;
        this.dto = dto;
    }


    // BOOLEAN > > >

    public QueryMatchItem(QueryOperator type, String fieldName, Boolean value) {
        this.type = type;
        this.fieldName = fieldName;
        booleanValue = value;
    }


    // CHARACTER > > >

    public QueryMatchItem(QueryOperator type, String fieldName, Character value) {
        this.type = type;
        this.fieldName = fieldName;
        characterValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, Character[] values) {
        this.type = type;
        this.fieldName = fieldName;
        characterValues = values;
    }


    // STRING > > >

    public QueryMatchItem(QueryOperator type, String fieldName, String value) {
        this.type = type;
        this.fieldName = fieldName;
        stringValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, String[] values) {
        this.type = type;
        this.fieldName = fieldName;
        stringValues = values;
    }


    // NUMBER > > >

    public QueryMatchItem(QueryOperator type, String fieldName, Number value) {
        this.type = type;
        this.fieldName = fieldName;
        numberValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, Number[] values) {
        this.type = type;
        this.fieldName = fieldName;
        numberValues = values;
    }


    // OBJECT > > >

    public QueryMatchItem(QueryOperator type, String fieldName, Object[] values) {
        this.type = type;
        this.fieldName = fieldName;
        objectValues = values;
    }


    // DATETIME > > >

    public QueryMatchItem(QueryOperator type, String fieldName, DateTime value) {
        this.type = type;
        this.fieldName = fieldName;
        dateTimeValue = value;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, DateTime[] values) {
        this.type = type;
        this.fieldName = fieldName;
        dateTimeValues = values;
    }

    public QueryMatchItem(QueryOperator type, String fieldName) {
        this.type = type;
        this.fieldName = fieldName;
    }

    public QueryMatchItem(QueryOperator type, String fieldName, List<?> itemList) {
        this.type = type;
        this.fieldName = fieldName;
        this.itemList = itemList;
    }

    public Object getValue() {
        if (stringValue != null) {
            return stringValue;
        }
        if (numberValue != null) {
            return numberValue;
        }
        if (dateTimeValue != null) {
            return returnDatetimeAsString ? dateTimeValue.toString() : dateTimeValue.getAsTimestamp();
        }
        if (booleanValue != null) {
            return booleanValue;
        }
        if (characterValue != null) {
            return characterValue;
        }
        return null;
    }

    public Object[] getValues() {
        if (stringValues != null) {
            return stringValues;
        }
        if (numberValues != null) {
            return numberValues;
        }
        if (objectValues != null) {
            return objectValues;
        }
        if (dateTimeValues != null) {
            int l = dateTimeValues.length;
            if (returnDatetimeAsString) {
                String[] times = new String[l];
                for (int i = 0; i < l; i++) {
                    DateTime dt = dateTimeValues[i];
                    times[i] = dt.toString();
                }
                return times;
            } else {
                Long[] timeStamps = new Long[l];
                for (int i = 0; i < l; i++) {
                    DateTime dt = dateTimeValues[i];
                    timeStamps[i] = dt.getAsTimestamp();
                }
                return timeStamps;
            }
        }
        if (characterValues != null) {
            return characterValues;
        }
        return null;
    }

    public void setDatetimeAsString() {
        returnDatetimeAsString = true;
    }

    public void setDatetimeAsTimestamp() {
        returnDatetimeAsString = false;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }

    public boolean equals(QueryMatchItem obj) {
        if ((fieldName == null && obj.fieldName != null) || (fieldName != null && !fieldName.equals(obj.fieldName))) {
            return false;
        }
        if ((type == null && obj.type != null) || (type != null && !type.equals(obj.type))) {
            return false;
        }
        if ((queryValue == null && obj.queryValue != null)
            || (queryValue != null && !queryValue.equals(obj.queryValue))) {
            return false;
        }
        if ((booleanValue == null && obj.booleanValue != null)
            || (booleanValue != null && !booleanValue.equals(obj.booleanValue))) {
            return false;
        }
        if ((characterValue == null && obj.characterValue != null)
            || (characterValue != null && !characterValue.equals(obj.characterValue))) {
            return false;
        }
        if ((characterValues == null && obj.characterValues != null)
            || (characterValues != null && !Arrays.equals(characterValues, obj.characterValues))) {
            return false;
        }
        if ((stringValue == null && obj.stringValue != null)
            || (stringValue != null && !stringValue.equals(obj.stringValue))) {
            return false;
        }
        if ((stringValues == null && obj.stringValues != null)
            || (stringValues != null && !Arrays.equals(stringValues, obj.stringValues))) {
            return false;
        }
        if ((numberValue == null && obj.numberValue != null)
            || (numberValue != null && !numberValue.equals(obj.numberValue))) {
            return false;
        }
        if ((numberValues == null && obj.numberValues != null)
            || (numberValues != null && !Arrays.equals(numberValues, obj.numberValues))) {
            return false;
        }
        if ((objectValues == null && obj.objectValues != null)
            || (objectValues != null && !Arrays.equals(objectValues, obj.objectValues))) {
            return false;
        }
        if ((dateTimeValue == null && obj.dateTimeValue != null)
            || (dateTimeValue != null && !dateTimeValue.equals(obj.dateTimeValue))) {
            return false;
        }
        if ((dateTimeValues == null && obj.dateTimeValues != null)
            || (dateTimeValues != null && !Arrays.equals(dateTimeValues, obj.dateTimeValues))) {
            return false;
        }
        if ((itemList == null && obj.itemList != null)
            || (itemList != null && !itemList.equals(obj.itemList))) {
            return false;
        }
        if ((dto == null && obj.dto != null) || (dto != null && !dto.getStorage().equals(obj.dto.getStorage()))) {
            return false;
        }
        return true;
    }
}
