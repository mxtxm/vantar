package com.vantar.database.sql;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.DatabaseException;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.string.*;
import org.slf4j.*;
import java.sql.*;
import java.util.*;


public class SqlExecute extends SqlQueryHelper {

    private static final Logger log = LoggerFactory.getLogger(SqlExecute.class);
    protected SqlConnection connection;


    public SqlExecute() {

    }

    public SqlExecute(SqlConnection connection) {
        this.connection = connection;
    }

    public void close(PreparedStatement statement) {
        close(statement, null);
    }

    public void close(PreparedStatement statement, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            log.error("! failed to close result set", e);
        }

        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            log.error("! failed to close statement ", e);
        }
    }

    public void execute(String sql, Dto data) throws DatabaseException {
        execute(sql, data.getStorableData());
    }

    public void execute(String sql, Object... params) throws DatabaseException {
        try (PreparedStatement statement = connection.getDatabase().prepareStatement(sql)) {
            statement.setEscapeProcessing(true);

            int i = 1;
            for (Object param : params) {
                statement.setObject(i++, param);
            }

            statement.executeUpdate();
        } catch (Throwable e) {
            log.error("! failed sql execute({}, {})", sql, params, e);
            throw new DatabaseException(e);
        }
    }

    public long insert(Dto dto) throws DatabaseException {
        dto.setCreateTime(true);
        long id = insert(dto.getStorage(), StorableData.toMap(dto.getStorableData()));

        if (!dto.beforeInsert()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }
        List<ManyToManyDefinition> many = dto.getManyToManyFieldValues(id);
        if (many != null) {
            for (ManyToManyDefinition definition : many) {
                for (Long fkRight : definition.fkRightValue) {
                    execute(
                        "INSERT INTO " + definition.storage + " (" + definition.fkLeft + "," + definition.fkRight +
                            ") VALUES (" + definition.fkLeftValue + "," + fkRight + ");"
                    );
                }
            }
        }

        return id;
    }

    public long insert(String table, Map<String, Object> params) throws DatabaseException {
        StringBuilder valueTemplates = new StringBuilder(100);
        Object[] values = new Object[params.size()];
        StringBuilder sql = new StringBuilder(500).append("INSERT INTO ").append(table).append(" (");

        int i = 0,
            escapeCount = params.size() + 1;

        for (Map.Entry<String, Object> param : params.entrySet()) {
            Object value = param.getValue();

            if (value != null) {
                if (value instanceof DateTime) {
                    value = SqlUtil.getDateTimeAsSql(value);
                } else if (value instanceof Collection || value instanceof Map) {
                    value = Json.toJson(value);
                }
            }

            sql.append(param.getKey()).append(',');
            valueTemplates.append("?,");
            values[i++] = value;
        }

        if (sql.length() > 0) {
            sql.setLength(sql.length() - 1);
            valueTemplates.setLength(valueTemplates.length() - 1);
        }

        sql.append(") VALUES (").append(valueTemplates).append(");");

        try (PreparedStatement statement=connection.getDatabase().prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            statement.setEscapeProcessing(true);

            i = 1;
            for (Object value : values) {
                statement.setObject(i++, value);
                if (i == escapeCount) {
                    break;
                }
            }

            statement.executeUpdate();
            ResultSet tableKeys = statement.getGeneratedKeys();

            return tableKeys.next() ? tableKeys.getLong(1) : 0;
        } catch (SQLException e) {
            log.error("! failed to insert ({}, {})", sql, values, e);
            throw new DatabaseException(e);
        }
    }

    public void insert(List<? extends Dto> data) throws DatabaseException {
        StringBuilder sql = new StringBuilder(data.size() * 100);
        StringBuilder valueTemplates = new StringBuilder(100);
        Object[] values = new Object[0];
        boolean firstDto = true;
        int i = 0;

        for (Dto dto : data) {
            Map<String, Object> params = StorableData.toMap(dto.getStorableData());

            if (firstDto) {
                sql.append("INSERT INTO ").append(dto.getStorage()).append(" (");
                valueTemplates.append('(');
                int paramCount = 0;

                for (Map.Entry<String, Object> param : params.entrySet()) {
                    sql.append(param.getKey()).append(',');
                    valueTemplates.append('?').append(',');
                    paramCount++;
                }
                if (sql.length() > 0) {
                    sql.setLength(sql.length() - 1);
                    valueTemplates.setLength(valueTemplates.length() - 1);
                }

                valueTemplates.append(')');
                sql.append(") VALUES ").append(valueTemplates);
                values = new Object[data.size() * paramCount];
                firstDto = false;
            } else {
                sql.append(',').append(valueTemplates);
            }

            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                if (value instanceof DateTime) {
                    value = SqlUtil.getDateTimeAsSql(value);
                } else if (value instanceof Collection || value instanceof Map) {
                    value = Json.toJson(value);
                }
                values[i++] = value;
            }

            if (!dto.beforeInsert()) {
                throw new DatabaseException(VantarKey.EVENT_REJECT);
            }
        }
        sql.append(';');

        try (PreparedStatement statement = connection.getDatabase().prepareStatement(sql.toString())) {
            statement.setEscapeProcessing(true);

            i = 1;
            for (Object value : values) {
                statement.setObject(i++, value);
            }

            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("! insert failed ({}, {})", sql, values, e);
            throw new DatabaseException(e);
        }
    }

    public void update(Dto dto) throws DatabaseException {
        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }

        SqlParams sqlParams = new SqlParams(dto, ",");
        execute(
            "UPDATE " + dto.getStorage() + " SET " + sqlParams.getTemplate() + " WHERE id=" + dto.getId() + ";",
            sqlParams.getValues()
        );
    }

    public void update(Dto dto, String fieldName) throws DatabaseException {
        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }

        SqlParams sqlParams = new SqlParams(dto, ",");
        sqlParams.addValue(dto.getPropertyValue(fieldName));
        execute(
            "UPDATE " + dto.getStorage() + " SET " + sqlParams.getTemplate() + " WHERE " + fieldName + "=?;",
            sqlParams.getValues()
        );
    }

    public void update(QueryBuilder q) throws DatabaseException {
        if (!q.getDto().beforeUpdate()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }

        SqlParams set = new SqlParams(q.getDto(), ",");
        SqlParams condition = SqlMapping.getSqlMatches(q.condition(), q.getDto());
        execute(
            "UPDATE " + q.getDto().getStorage() +
            " SET " + set.getTemplate() +
            " WHERE " + condition.getTemplate() + ";", set.getValues(condition)
        );
    }

    public void updateBySql(Dto data, String condition) throws DatabaseException {
        updateBySql(data.getStorage(), condition, StorableData.toMap(data.getStorableData()));
    }

    public void updateBySql(String table, String condition, Map<String, Object> params) throws DatabaseException {
        SqlParams sqlParams = new SqlParams(params, ",");
        execute("UPDATE " + table + " SET " + sqlParams.getTemplate() + " WHERE " + condition + ";", sqlParams.getValues());
    }

    public void delete(Dto dto) throws DatabaseException {
        Long id = dto.getId();
        if (id == null) {
            delete(dto.getStorage(), StorableData.toMap(dto.getStorableData()));
            return;
        }
        execute("DELETE FROM " + dto.getStorage() + " WHERE id=" + id + ";");
    }

    public void delete(QueryBuilder q) throws DatabaseException {
        SqlParams condition = SqlMapping.getSqlMatches(q.condition(), q.getDto());
        execute("DELETE FROM " + q.getDto().getStorage() + " WHERE " + condition.getTemplate() + ";", condition.getValues());
    }

    public void delete(String table, Map<String, Object> params) throws DatabaseException {
        SqlParams sqlParams = new SqlParams(params);
        execute("DELETE FROM " + table + " WHERE " + sqlParams.getTemplate() + ";", sqlParams.getValues());
    }

    public void delete(Dto data, List<Long> ids) throws DatabaseException {
        if (!ids.isEmpty()) {
            execute("DELETE FROM " + data.getStorage() + " WHERE id IN(" + CollectionUtil.join(ids, ',') + ");");
        }
    }

    private void execute(String sql) throws DatabaseException {
        try (PreparedStatement statement = connection.getDatabase().prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            log.error("! failed sql execute({})", sql, e);
            throw new DatabaseException(e);
        }
    }

    // orderId:1;orderId;userId:user_idx;userId,commentId
    public void createIndex(Dto dto, boolean deleteIfExists) throws DatabaseException {
        for (String item : dto.getIndexes()) {
            String[] parts = StringUtil.split(item, VantarParam.SEPARATOR_KEY_VAL);
            String cols = parts[0];
            String idxName;
            if (parts.length == 1 || StringUtil.toInteger(parts[1]) != null) {
                idxName = dto.getStorage() + '_' + StringUtil.replace(StringUtil.remove(cols, ' '), ',', '_') + 'x';
            } else {
                idxName = StringUtil.toSnakeCase(parts[1]);
            }

            if (deleteIfExists) {
                execute("DROP INDEX IF EXISTS " + idxName + " CASCADE;");
                execute("CREATE INDEX " + idxName + " ON " + dto.getStorage() + "(" + cols + ");");
            } else {
                execute("CREATE INDEX IF NOT EXISTS " + idxName + " ON " + dto.getStorage() + "(" + cols + ");");
            }
        }
    }

    public void removeIndex(Dto dto) throws DatabaseException {
        for (String item : dto.getIndexes()) {
            String[] parts = StringUtil.split(item, VantarParam.SEPARATOR_KEY_VAL);
            String cols = parts[0];
            String idxName;
            if (parts.length == 1) {
                idxName = dto.getStorage() + '_' + StringUtil.replace(StringUtil.remove(cols, ' '), ',', '_') + 'x';
            } else {
                idxName = parts[1];
            }

            execute("DROP INDEX IF EXISTS " + idxName + " CASCADE;");
        }
    }

    public List<String> getIndexes(Dto dto) throws DatabaseException {
        List<String> indexes = new ArrayList<>();
        String sql = "SELECT indexname, indexdef FROM pg_indexes WHERE tablename='" + dto.getStorage() + "';";
        PreparedStatement statement = null;
        try {
            ResultSet rs = connection.getDatabase().prepareStatement(sql).executeQuery();
            while (rs.next()) {
                indexes.add(rs.getString("indexname") + ": " + rs.getString("indexdef"));
            }

        } catch (SQLException e) {
            log.error("! failed to get indexes ({})", sql, e);
            close(statement);
        }

        return indexes;
    }
}
