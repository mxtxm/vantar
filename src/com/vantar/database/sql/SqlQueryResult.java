package com.vantar.database.sql;

import com.google.gson.reflect.TypeToken;
import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import org.slf4j.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;


public class SqlQueryResult extends QueryResultBase implements QueryResult, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SqlQueryResult.class);

    private final PreparedStatement statement;
    private Map<String, Column> columns;
    public final ResultSet resultSet;


    public SqlQueryResult(PreparedStatement statement, ResultSet resultSet, Dto dto) {
        this.dto = dto;
        this.statement = statement;
        this.resultSet = resultSet;
        if (dto != null) {
            fields = dto.getClass().getFields();
            exclude = dto.getExclude();
        }
    }

    public void close() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            log.error("! could not close statement or result-set", e);
        }
    }

    public Map<String, Column> getColumns() throws DatabaseException {
        if (columns == null) {
            setColumns();
        }
        return columns;
    }

    public Column getColumn(Field field) throws DatabaseException {
        if (columns == null) {
            setColumns();
        }
        return columns.get(field.getName());
    }

    private void setColumns() throws DatabaseException {
        columns = new HashMap<>();
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1, l = metaData.getColumnCount(); i <= l; ++i) {
                columns.put(
                    StringUtil.toCamelCase(metaData.getColumnName(i)),
                    new Column(i, metaData.getColumnType(i))
                );
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public Object peek(String field) throws NoContentException, DatabaseException {
        try {
            if (resultSet.first()) {
                return resultSet.getObject(StringUtil.toSnakeCase(field));
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            try {
                resultSet.beforeFirst();
            } catch (SQLException e) {
                log.error("! cursor not reset", e);
            }
        }
        throw new NoContentException();
    }

    public boolean next() throws DatabaseException {
        try {
            if (resultSet.next()) {
                mapRecordToDto();
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
        return false;
    }

    public <T extends Dto> T first() throws NoContentException, DatabaseException {
        try {
            if (resultSet.first()) {
                mapRecordToDto();
                return (T) dto;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            close();
        }
        throw new NoContentException();
    }

    public Map<String, String> asKeyValue(String keyField, String valueField) throws DatabaseException, NoContentException {
        Map<String, String> result = new HashMap<>();
        try {
            while (resultSet.next()) {
                result.put(
                    resultSet.getString(keyField),
                    resultSet.getString(valueField)
                );
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValue(KeyValueData definition) throws DatabaseException, NoContentException {
        Map<String, String> result = new HashMap<>();
        try {
            while (resultSet.next()) {
                result.put(
                    definition.getKey(resultSet.getString(definition.getKeyField())),
                    definition.getValue(resultSet.getString(definition.getValueField()))
                );
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValueLocalized(String keyField, String valueField) throws DatabaseException, NoContentException {
        Map<String, String> result = new HashMap<>();
        try {
            while (resultSet.next()) {
                result.put(
                    resultSet.getString(keyField),
                    CollectionUtil.getStringFromMap(Json.mapFromJson(resultSet.getString(valueField), String.class, String.class))
                );
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    private void mapRecordToDto() throws DatabaseException {
        if (fields == null) {
            return;
        }

        ColumnMany many = null;
        try {
            for (Field field : fields) {
                Column column = getColumn(field);

                if (   Modifier.isFinal(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers())
                    || field.isAnnotationPresent(NoStore.class)
                    || (exclude != null && exclude.contains(field.getName())
                    || column == null)
                ) {
                    continue;
                }

                DateTime d;
                switch (column.type) {
                    case Types.DATE:
                        try {
                            d = new DateTime(resultSet.getString(column.index));
                            d.setType(DateTime.DATE);
                        } catch (DateTimeException e) {
                            d = null;
                        }
                        field.set(dto, d);
                        break;

                    case Types.TIMESTAMP:
                    case Types.TIMESTAMP_WITH_TIMEZONE:
                        try {
                            d = new DateTime(resultSet.getString(column.index));
                            d.setType(DateTime.TIMESTAMP);
                        } catch (DateTimeException e) {
                            d = null;
                        }
                        field.set(dto, d);
                        break;

                    case Types.TIME:
                    case Types.TIME_WITH_TIMEZONE:
                        try {
                            d = new DateTime(resultSet.getString(column.index));
                            d.setType(DateTime.TIME);
                        } catch (DateTimeException e) {
                            d = null;
                        }
                        break;

                    default:
                        Class<?> type = field.getType();

                        if (type.isEnum()) {
                            EnumUtil.setEnumValue(resultSet.getString(column.index), type, dto, field);
                            continue;
                        }

                        if (field.isAnnotationPresent(StoreString.class)) {
                            String value = resultSet.getString(column.index);
                            if (value == null) {
                                field.set(dto, null);
                            } else {
                                field.set(dto, Json.fromJson(value, TypeToken.get(type).getType()));
                            }
                            continue;
                        }

                        if (field.isAnnotationPresent(DeLocalized.class)) {
                            String value = resultSet.getString(column.index);
                            if (value == null) {
                                field.set(dto, null);
                            } else {
                                field.set(dto, CollectionUtil.getStringFromMap(Json.mapFromJson(value, String.class, String.class), locales));
                            }
                            continue;
                        }

                        if (type == List.class || type == Set.class) {
                            Class<?>[] g = ClassUtil.getGenericTypes(field);
                            if (g == null || g.length != 1) {
                                log.warn("! type/value miss-match ({}.{})", dto.getClass().getName(), field.getName());
                                continue;
                            }
                            Class<?> listType = g[0];
                            List value;

                            if (field.isAnnotationPresent(ManyToManyGetData.class)) {
                                String[] parts = StringUtil.split(field.getAnnotation(ManyToManyGetData.class).value(), VantarParam.SEPARATOR_NEXT);
                                if (many == null) {
                                    many = new ColumnMany();
                                    many.properties = StringUtil.split(parts[2], VantarParam.SEPARATOR_COMMON);
                                    many.propertyOut = StringUtil.toSnakeCase(field.getName());
                                    // this is db column name
                                    many.className = StringUtil.toSnakeCase(many.propertyOut);
                                }

                                if (many.properties.length == 1) {
                                    value = StringUtil.splitToType(resultSet.getString(many.propertyOut), VantarParam.SEPARATOR_BLOCK_COMPLEX, type);
                                } else {
                                    String[] valueBundles = StringUtil.split(resultSet.getString(many.className), VantarParam.SEPARATOR_BLOCK_COMPLEX);
                                    value = new ArrayList<>(valueBundles.length);
                                    for (String valueBundle : valueBundles) {
                                        Dto dtoItem = (Dto) ClassUtil.getInstance(listType);
                                        if (dtoItem == null) {
                                            continue;
                                        }
                                        String[] bundles = StringUtil.split(valueBundle, VantarParam.SEPARATOR_COMMON_COMPLEX);
                                        for (int i = 0; i < bundles.length; i++) {
                                            dtoItem.setPropertyValue(many.properties[i], bundles[i]);
                                        }
                                        value.add(dtoItem);
                                    }
                                }
                            } else {
                                value = Json.listFromJson(resultSet.getString(column.index), listType);
                            }

                            field.set(
                                dto,
                                value != null && type == Set.class ? new HashSet<>(value) : value
                            );
                            continue;
                        }

                        if (type == Map.class) {
                            String value = resultSet.getString(column.index);
                            if (value == null) {
                                field.set(dto, null);
                            } else {
                                Class<?>[] g = ClassUtil.getGenericTypes(field);
                                if (g == null || g.length != 2) {
                                    log.warn("! type/value miss-match ({}.{})", dto.getClass().getName(), field.getName());
                                    continue;
                                }
                                field.set(dto, Json.mapFromJson(value, g[0], g[1]));
                            }
                            continue;
                        }

                        if (field.getType().equals(Character.class)) {
                            String value = resultSet.getString(column.index);
                            if (value != null && !value.isEmpty()) {
                                field.set(dto, value.charAt(0));
                            }
                            continue;
                        }

                        if (field.getType().equals(Long.class)) {
                            field.set(dto, resultSet.getLong(column.index));
                            continue;
                        }

                        if (field.getType().equals(Integer.class)) {
                            field.set(dto, resultSet.getInt(column.index));
                            continue;
                        }

                        if (field.getType().equals(Double.class)) {
                            field.set(dto, resultSet.getDouble(column.index));
                            continue;
                        }

                        if (field.getType().equals(Float.class)) {
                            field.set(dto, resultSet.getFloat(column.index));
                            continue;
                        }

                        field.set(dto, resultSet.getObject(column.index));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } catch (IllegalAccessException e) {
            log.error("! data > dto", e);
        }
    }
}