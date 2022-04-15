package com.vantar.database.query;

import com.vantar.database.dto.Dto;
import com.vantar.util.datetime.DateTime;
import java.util.List;


public class QueryMatchItem {

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
}
