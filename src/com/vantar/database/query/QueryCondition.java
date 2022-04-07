package com.vantar.database.query;

import com.vantar.database.datatype.Location;
import com.vantar.util.datetime.*;
import com.vantar.util.object.*;
import java.util.*;


public class QueryCondition {

    public List<QueryMatchItem> q = new ArrayList<>();
    public QueryOperator operator;
    public String storage;


    public QueryCondition() {
        operator = QueryOperator.AND;
    }

    public QueryCondition(QueryOperator operator) {
        this.operator = operator;
    }

    public QueryCondition(String storage) {
        this.storage = storage;
        operator = QueryOperator.AND;
    }

    public QueryCondition(String storage, QueryOperator operator) {
        this.storage = storage;
        this.operator = operator;
    }


    // EXISTS > > >

    public QueryCondition exists(QueryCondition c) {
        q.add(new QueryMatchItem(QueryOperator.EXISTS, c));
        return this;
    }


    // CONDITION > > >

    public QueryCondition addCondition(QueryCondition c) {
        q.add(new QueryMatchItem(QueryOperator.QUERY, c));
        return this;
    }


    // BOOLEAN > > >

    public QueryCondition equal(String fieldName, Boolean value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition notEqual(String fieldName, Boolean value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value));
        return this;
    }


    // OBJECT > > >

    public QueryCondition equal(String fieldName, Object value) {
        if (value == null) {
            return this;
        } else if (value instanceof Boolean) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, (Boolean) value));
        } else if (value instanceof Number) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, (Number) value));
        } else if (value instanceof Character) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, (Character) value));
        } else if (value instanceof String) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, (String) value));
        } else if (value instanceof DateTime) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, (DateTime) value));
        } else if (value.getClass().isEnum()) {
            q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value.toString()));
        }
        return this;
    }

    public QueryCondition notEqual(String fieldName, Object value) {
        if (value == null) {
            return this;
        } else if (value instanceof Boolean) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, (Boolean) value));
        } else if (value instanceof Number) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, (Number) value));
        } else if (value instanceof Character) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, (Character) value));
        } else if (value instanceof String) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, (String) value));
        } else if (value instanceof DateTime) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, (DateTime) value));
        } else if (value.getClass().isEnum()) {
            q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value.toString()));
        }
        return this;
    }


    // CHARACTER > > >

    public QueryCondition equal(String fieldName, Character value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition notEqual(String fieldName, Character value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition in(String fieldName, Character... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.IN, fieldName, values));
        return this;
    }

    public QueryCondition notIn(String fieldName, Character... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_IN, fieldName, values));
        return this;
    }

    public QueryCondition inCharacter(String fieldName, Collection<Character> values) {
        if (values == null) {
            return this;
        }
        return in(fieldName, values.toArray(new Character[0]));
    }

    public QueryCondition notInCharacter(String fieldName, Collection<Character> values) {
        if (values == null) {
            return this;
        }
        return notIn(fieldName, values.toArray(new Character[0]));
    }


    // ENUM > > >

    public QueryCondition equal(String fieldName, Enum<?> value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value.name()));
        return this;
    }

    public QueryCondition notEqual(String fieldName, Enum<?> value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value.name()));
        return this;
    }

    public QueryCondition in(String fieldName, Enum<?>... values) {
        if (values == null) {
            return this;
        }
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strValues[i] = values[i].toString();
        }
        q.add(new QueryMatchItem(QueryOperator.IN, fieldName, strValues));
        return this;
    }

    public QueryCondition notIn(String fieldName, Enum<?>... values) {
        if (values == null) {
            return this;
        }
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strValues[i] = values[i].toString();
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_IN, fieldName, strValues));
        return this;
    }

    public QueryCondition inEnum(String fieldName, Collection<Enum<?>> values) {
        if (values == null) {
            return this;
        }
        return in(fieldName, values.toArray(new Enum[0]));
    }

    public QueryCondition notInEnum(String fieldName, Collection<Enum<?>> values) {
        if (values == null) {
            return this;
        }
        return notIn(fieldName, values.toArray(new Enum[0]));
    }


    // STRING > > >

    public QueryCondition phrase(String value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.FULL_SEARCH, null, value));
        return this;
    }

    public QueryCondition equal(String fieldName, String value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition notEqual(String fieldName, String value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition like(String fieldName, String value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LIKE, fieldName, value));
        return this;
    }

    public QueryCondition notLike(String fieldName, String value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_LIKE, fieldName, value));
        return this;
    }

    public QueryCondition in(String fieldName, String... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.IN, fieldName, values));
        return this;
    }

    public QueryCondition notIn(String fieldName, String... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_IN, fieldName, values));
        return this;
    }

    public QueryCondition inString(String fieldName, Collection<String> values) {
        if (values == null) {
            return this;
        }
        return in(fieldName, values.toArray(new String[0]));
    }

    public QueryCondition notInString(String fieldName, Collection<String> values) {
        if (values == null) {
            return this;
        }
        return notIn(fieldName, values.toArray(new String[0]));
    }


    // NUMBER > > >

    public QueryCondition equal(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition notEqual(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition between(String fieldName, Number... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.BETWEEN, fieldName, values));
        return this;
    }

    public QueryCondition between(String fieldNameA, String fieldNameB, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN_EQUAL, fieldNameA, value));
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN_EQUAL, fieldNameB, value));
        return this;
    }

    public QueryCondition notBetween(String fieldName, Number... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_BETWEEN, fieldName, values));
        return this;
    }

    public QueryCondition notBetween(String fieldNameA, String fieldNameB, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN, fieldNameA, value));
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN, fieldNameB, value));
        return this;
    }

    public QueryCondition lessThan(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN, fieldName, value));
        return this;
    }

    public QueryCondition greaterThan(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN, fieldName, value));
        return this;
    }

    public QueryCondition lessThanEqual(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition greaterThanEqual(String fieldName, Number value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition in(String fieldName, Number... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.IN, fieldName, values));
        return this;
    }

    public QueryCondition notIn(String fieldName, Number... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_IN, fieldName, values));
        return this;
    }

    public QueryCondition inNumber(String fieldName, Collection<? extends Number> values) {
        return values == null ? this : in(fieldName, values.toArray(new Number[0]));
    }

    public QueryCondition notInNumber(String fieldName, Collection<? extends Number> values) {
        if (values == null) {
            return this;
        }
        return notIn(fieldName, values.toArray(new Number[0]));
    }


    // DATETIME > > >

    public QueryCondition equal(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition notEqual(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition between(String fieldName, DateTime... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.BETWEEN, fieldName, values));
        return this;
    }

    public QueryCondition between(String fieldName, DateTimeRange range) {
        if (range == null) {
            return this;
        }
        return between(fieldName, range.dateMin, range.dateMax);
    }

    public QueryCondition between(String fieldNameA, String fieldNameB, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN_EQUAL, fieldNameA, value));
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN_EQUAL, fieldNameB, value));
        return this;
    }

    public QueryCondition notBetween(String fieldName, DateTime... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_BETWEEN, fieldName, values));
        return this;
    }

    public QueryCondition notBetween(String fieldName, DateTimeRange range) {
        if (range == null) {
            return this;
        }
        return notBetween(fieldName, range.dateMin, range.dateMax);
    }

    public QueryCondition notBetween(String fieldNameA, String fieldNameB, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN, fieldNameA, value));
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN, fieldNameB, value));
        return this;
    }

    public QueryCondition lessThan(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN, fieldName, value));
        return this;
    }

    public QueryCondition greaterThan(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN, fieldName, value));
        return this;
    }

    public QueryCondition lessThanEqual(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.LESS_THAN_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition greaterThanEqual(String fieldName, DateTime value) {
        if (value == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.GREATER_THAN_EQUAL, fieldName, value));
        return this;
    }

    public QueryCondition in(String fieldName, DateTime... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.IN, fieldName, values));
        return this;
    }

    public QueryCondition notIn(String fieldName, DateTime... values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.NOT_IN, fieldName, values));
        return this;
    }

    public QueryCondition inDateTime(String fieldName, Collection<DateTime> values) {
        if (values == null) {
            return this;
        }
        return in(fieldName, values.toArray(new DateTime[0]));
    }

    public QueryCondition notInDateTime(String fieldName, Collection<DateTime> values) {
        if (values == null) {
            return this;
        }
        return notIn(fieldName, values.toArray(new DateTime[0]));
    }


    // > > > other

    public QueryCondition containsAll(String fieldName, Map<String, ?> values) {
        if (values == null) {
            return this;
        }
        QueryCondition qAnd = new QueryCondition(QueryOperator.AND);
        values.forEach((k, v) -> qAnd.equal(fieldName + "." + k, v));
        addCondition(qAnd);
        return this;
    }

    public QueryCondition containsAll(String fieldName, Set<?> values) {
        return containsAll(fieldName, new ArrayList<>(values));
    }

    public QueryCondition containsAll(String fieldName, List<?> values) {
        if (values == null) {
            return this;
        }
        q.add(new QueryMatchItem(QueryOperator.CONTAINS_ALL, fieldName, values));
        return this;
    }

    public QueryCondition isNull(String fieldName) {
        q.add(new QueryMatchItem(QueryOperator.IS_NULL, fieldName));
        return this;
    }

    public QueryCondition isNotNull(String fieldName) {
        q.add(new QueryMatchItem(QueryOperator.IS_NOT_NULL, fieldName));
        return this;
    }

    public QueryCondition isEmpty(String fieldName) {
        q.add(new QueryMatchItem(QueryOperator.IS_EMPTY, fieldName));
        return this;
    }

    public QueryCondition isNotEmpty(String fieldName) {
        q.add(new QueryMatchItem(QueryOperator.IS_NOT_EMPTY, fieldName));
        return this;
    }

    public QueryCondition near(String fieldName, Location location, Double maxDistance) {
        return near(fieldName, location, maxDistance, null);
    }

    public QueryCondition far(String fieldName, Location location, Double minDistance) {
        return near(fieldName, location, null, minDistance);
    }

    public QueryCondition near(String fieldName, Location location, Double maxDistance, Double minDistance) {
        if (location == null) {
            return this;
        }
        Number[] values = new Number[4];
        values[0] = location.latitude;
        values[1] = location.longitude;
        values[2] = maxDistance;
        values[3] = minDistance;
        q.add(new QueryMatchItem(QueryOperator.NEAR, fieldName, values));
        return this;
    }

    public QueryCondition within(String fieldName, List<Location> polygon) {
        if (polygon == null) {
            return this;
        }
        Object[] values = new Object[polygon.size()];
        int i = -1;
        for (Location point : polygon) {
            values[++i] = point;
        }
        q.add(new QueryMatchItem(QueryOperator.WITHIN, fieldName, values));
        return this;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }
}