package com.vantar.database.sql;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.util.collection.CollectionUtil;
import org.slf4j.*;
import java.sql.*;
import java.util.List;


public class SqlSearch extends SqlQueryHelper {

    private static final Logger log = LoggerFactory.getLogger(SqlSearch.class);
    protected SqlConnection connection;


    public SqlSearch(SqlConnection connection) {
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

    public boolean exists(Dto dto, String property) throws DatabaseException {
        Long id = dto.getId();
        String sql = id == null ?
            "SELECT 1 FROM " + dto.getStorage() + " WHERE " + property + "=?;" :
            "SELECT 1 FROM " + dto.getStorage() + " WHERE " + property + "=? AND id<>?;";

        try (SqlQueryResult queryResult = (SqlQueryResult) (id == null ?
                getData(dto, sql, dto.getPropertyValue(property)) :
                getData(dto, sql, dto.getPropertyValue(property), id))) {

            return queryResult.resultSet.next();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean existsById(Dto dto) throws DatabaseException {
        return exists(dto, "id");
    }

    public boolean existsByDto(Dto dto) throws DatabaseException {
        SqlParams sqlParams = new SqlParams(DataInfo.toMap(dto.getFieldValues()));
        String sql = "SELECT 1 FROM " + dto.getStorage() + " WHERE " + sqlParams.getTemplate() + ';';

        try (SqlQueryResult queryResult = (SqlQueryResult) getData(dto, sql, sqlParams.getValues())) {
            return queryResult.resultSet.next();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean exists(QueryBuilder q) throws DatabaseException {
        SqlParams sqlParams = SqlMapping.getSqlMatches(q.condition(), q.getDto());
        String sql = "SELECT 1 FROM " + q.getDto().getStorage() + " WHERE " + sqlParams.getTemplate() + ';';
        try (SqlQueryResult queryResult = (SqlQueryResult) getData(q.getDto(), sql, sqlParams.getValues())) {
            return queryResult.resultSet.next();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean exists(Dto dto, String condition, Object... params) throws DatabaseException {
        String sql = "SELECT 1 FROM " + dto.getStorage() + " WHERE " + condition + ';';
        try (SqlQueryResult queryResult = (SqlQueryResult) getData(dto, sql, params)) {
            log.debug("{} ({})", sql, params);
            return queryResult.resultSet.next();
        } catch (SQLException e) {
            log.error("failed. {} ({}) e={}", sql, params, e);
            throw new DatabaseException(e);
        }
    }

    public long count(QueryBuilder q) throws DatabaseException {
        SqlParams sqlParams = SqlMapping.getSqlMatches(q.condition(), q.getDto());
        String sql = "SELECT count(*) FROM " + q.getDto().getStorage() + " WHERE " + sqlParams.getTemplate() + ';';
        try (SqlQueryResult queryResult = (SqlQueryResult) getData(q.getDto(), sql, sqlParams.getValues())) {
            return queryResult.resultSet.next() ? queryResult.resultSet.getLong(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public long count(String collectionName) throws DatabaseException {
        String sql = "SELECT count(*) FROM " + collectionName+ ';';
        try (SqlQueryResult queryResult = (SqlQueryResult) getData(null, sql)) {
            return queryResult.resultSet.next() ? queryResult.resultSet.getLong(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public QueryResult getData(QueryBuilder q) throws DatabaseException {
        SqlParams sqlParams = SqlMapping.queryBuilderToSql(q, SqlMapping.CountMethod.NONE);
        return getData(q.getDtoResult(), sqlParams.getTemplate(), sqlParams.getValues());
    }

    public QueryResult getData(Dto dto, String sql, List<Object> params) throws DatabaseException {
        return getData(dto, sql, params.toArray());
    }

    public QueryResult getData(Dto dto, String sql, Object... params) throws DatabaseException {
        PreparedStatement statement = null;
        try {
            statement = connection.getDatabase().prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            if (params != null && params.length > 0) {
                statement.setEscapeProcessing(true);
                int i = 1;
                for (Object param : params) {
                    statement.setObject(i++, param);
                }
            }

            return new SqlQueryResult(statement, statement.executeQuery(), dto);
        } catch (SQLException e) {
            log.error("! failed to get data ({}, {})", sql, CollectionUtil.toString(params), e);
            close(statement);
            throw new DatabaseException(e);
        }
    }

    public QueryResult getAllData(Dto dto, String... sort) throws DatabaseException {
        return getData(dto, "SELECT * FROM " + dto.getStorage() + (sort.length == 0 ? "" : " ORDER BY " + CollectionUtil.join(sort, VantarParam.SEPARATOR_COMMON)) + ";");
    }

    public PageData getPage(QueryBuilder q, String... locales) throws NoContentException, DatabaseException {
        long total = q.getTotal();
        boolean getFullCount = total == 0;

        QueryResult result;
        if (SqlConnection.getDbms().equals(SqlDbms.POSTGRESQL)) {
            SqlParams sqlParams = SqlMapping.queryBuilderToSql(
                q,
                getFullCount ? SqlMapping.CountMethod.INCLUDE_TOTAL_COUNT : SqlMapping.CountMethod.NONE
            );
            result = getData(q.getDtoResult(), sqlParams.getTemplate(), sqlParams.getValues());
            if (locales.length > 0) {
                result.setLocale(locales);
            }

            if (getFullCount) {
                Object fullCount = result.peek(VantarParam.TOTAL_COUNT);
                if (fullCount != null) {
                    total = (long) fullCount;
                }
            }

        } else {
            SqlParams sqlParams = SqlMapping.queryBuilderToSql(q, SqlMapping.CountMethod.NONE);
            result = getData(q.getDtoResult(), sqlParams.getTemplate(), sqlParams.getValues());
            if (locales.length > 0) {
                result.setLocale(locales);
            }

            if (getFullCount) {
                sqlParams = SqlMapping.queryBuilderToSql(q, SqlMapping.CountMethod.COUNT_QUERY);
                try (SqlQueryResult queryResult = (SqlQueryResult) getData(q.getDto(), sqlParams.getTemplate(), sqlParams.getValues())) {
                    total = queryResult.resultSet.next() ? queryResult.resultSet.getLong(1) : 0;
                } catch (SQLException e) {
                    throw new DatabaseException(e);
                }
            }
        }

        return new PageData(
            result.asList(),
            q.getPageNo(),
            q.getLimit(),
            total
        );
    }
}
