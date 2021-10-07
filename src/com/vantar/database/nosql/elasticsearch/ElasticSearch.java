package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortOrder;
import java.util.*;


public class ElasticSearch {

    /**
     * Check by property value
     */
    public static boolean existsById(Dto dto, String property) throws DatabaseException {
        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(property, dto.getPropertyValue(property));
        return existsById(q);
    }

    /**
     * Check by id
     */
    public static boolean existsById(Dto dto) throws DatabaseException {
        GetRequest request = new GetRequest(dto.getStorage(), dto.getId().toString());
        request.fetchSourceContext(new FetchSourceContext(false));
        request.storedFields("_none_");
        try {
            return ElasticConnection.getClient().exists(request, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static boolean existsById(QueryBuilder q) throws DatabaseException {
        return count(q) > 0;
    }

    public static long count(QueryBuilder q) throws DatabaseException {
        Dto dto = q.getDto();
        CountRequest countRequest = new CountRequest(dto.getStorage());
        countRequest.query(q.conditionIsEmpty() ? new MatchAllQueryBuilder() : ElasticMapping.getElasticQuery(q.getCondition(), dto));
        try {
            CountResponse response = ElasticConnection.getClient().count(countRequest, RequestOptions.DEFAULT);
            return response.getCount();
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static long count(String collectionName) throws DatabaseException {
        CountRequest countRequest = new CountRequest(collectionName);
        countRequest.query(QueryBuilders.matchAllQuery());
        try {
            CountResponse response = ElasticConnection.getClient().count(countRequest, RequestOptions.DEFAULT);
            return response.getCount();
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static long getNextId(String collectionName) {
        SearchSourceBuilder query = new SearchSourceBuilder()
            .query(new MatchAllQueryBuilder())
            .size(1)
            .sort(VantarParam.ID, SortOrder.DESC)
            .fetchSource(new String[] {VantarParam.ID}, new String[] {});

        SearchRequest request = new SearchRequest(collectionName);
        request.source(query);
        try {
            SearchResponse searchResponse = ElasticConnection.getClient().search(request, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            if (hits.getHits().length == 0) {
                return 1;
            }
            Long v = StringUtil.toLong(hits.getHits()[0].getSourceAsMap().get(VantarParam.ID).toString());
            return v == null ? 1 : v + 1;
        } catch (Throwable e) {
            return 1;
        }
    }

    /**
     * Get by id
     */
    public static <T extends Dto> T get(T dto) throws DatabaseException, NoContentException {
        GetRequest request = new GetRequest(dto.getStorage(), dto.getId().toString());
        request.fetchSourceContext(new FetchSourceContext(true));
        request.storedFields();

        GetResponse response;
        try {
            response = ElasticConnection.getClient().get(request, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
        if (!response.isExists()) {
            throw new NoContentException();
        }

        String json = response.getSourceAsString();
        if (json == null) {
            return dto;
        }
        dto.set(json, Dto.Action.GET);
        return dto;
    }

    public static <T extends Dto> List<T> get(T dto, long... ids) throws DatabaseException, NoContentException {
        List<T> dtos = new ArrayList<>(ids.length);
        String collection = dto.getStorage();
        MultiGetRequest request = new MultiGetRequest();
        for (long id : ids) {
            request.add(new MultiGetRequest.Item(collection, Long.toString(id)));
        }

        MultiGetResponse responseAll;
        try {
            responseAll = ElasticConnection.getClient().mget(request, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }

        for (MultiGetItemResponse item : responseAll.getResponses()) {
            GetResponse response = item.getResponse();
            if (!response.isExists()) {
                continue;
            }
            String json = response.getSourceAsString();
            if (json == null) {
                continue;
            }

            T newDto = (T) ObjectUtil.getInstance(dto.getClass());
            if (newDto != null) {
                newDto.set(json, Dto.Action.GET);
                dtos.add(newDto);
            }
        }

        if (dtos.isEmpty()) {
            throw new NoContentException();
        }
        return dtos;
    }

    public static QueryResult getData(QueryBuilder q) throws DatabaseException {
        SearchSourceBuilder query = new SearchSourceBuilder();
        query.query(q.conditionIsEmpty() ? new MatchAllQueryBuilder() : ElasticMapping.getElasticQuery(q.getCondition(), q.getDto()));

        if (q.getLimit() != null) {
            query.size(q.getLimit());
        }
        if (q.getSkip() != null) {
            query.from(q.getSkip());
        }

        String[] sort = q.getSort();
        if (sort != null && sort.length > 0) {
            sort = StringUtil.splits(sort[0], ' ', VantarParam.SEPARATOR_KEY_VAL);
            if (sort.length == 2) {
                query.sort(sort[0], sort[1].equalsIgnoreCase("desc") ? SortOrder.DESC : SortOrder.ASC);
            } else if (sort.length == 1) {
                query.sort(sort[0], SortOrder.ASC);
            }
        }

        SearchRequest request = new SearchRequest(q.getDto().getStorage());
        request.source(query);
        try {
            SearchResponse searchResponse = ElasticConnection.getClient().search(request, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            return new ElasticQueryResult(hits.getHits(), q.getDtoResult());
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static QueryResult getAllData(Dto dto) throws DatabaseException {
        SearchSourceBuilder query = new SearchSourceBuilder();
        query.query(new MatchAllQueryBuilder());
        SearchRequest request = new SearchRequest(dto.getStorage());
        request.source(query);
        try {
            SearchResponse searchResponse = ElasticConnection.getClient().search(request, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            return new ElasticQueryResult(hits.getHits(), dto);
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static PageData getPage(QueryBuilder q, String... locales) throws NoContentException, DatabaseException {
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = getData(q);
        if (locales.length > 0) {
            result.setLocale(locales);
        }

        return new PageData(
            result.asList(),
            q.getPageNo(),
            q.getLimit(),
            total
        );
    }
}
