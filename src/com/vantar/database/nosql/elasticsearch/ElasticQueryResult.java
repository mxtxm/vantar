package com.vantar.database.nosql.elasticsearch;

import com.vantar.database.common.KeyValueData;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryResult;
import com.vantar.database.query.QueryResultBase;
import com.vantar.exception.NoContentException;
import com.vantar.util.collection.*;
import com.vantar.util.json.*;
import com.vantar.util.string.StringUtil;
import org.elasticsearch.search.SearchHit;
import java.util.HashMap;
import java.util.Map;


public class ElasticQueryResult extends QueryResultBase implements QueryResult, AutoCloseable {

    public final SearchHit[] hits;
    private int i = 0;


    public ElasticQueryResult(SearchHit[] hits, Dto dto) {
        this.dto = dto;
        this.hits = hits;
    }

    public void close() {

    }

    public Object peek(String field) throws NoContentException {
        if (hits == null || hits.length == 0) {
            throw new NoContentException();
        }
        String json = hits[0].getSourceAsString();
        if (json == null) {
            return null;
        }
        field = StringUtil.toSnakeCase(field);
        Map<String, Object> map = Json.d.mapFromJson(json, String.class, Object.class);
        return map == null ? null : map.get(field);
    }

    public boolean next() {
        if (hits == null || hits.length == 0 || i >= hits.length) {
            return false;
        }
        mapRecordToDto(hits[i++]);
        return true;
    }

    public <T extends Dto> T first() throws NoContentException {
        if (hits == null || hits.length == 0) {
            throw new NoContentException();
        }
        mapRecordToDto(hits[0]);
        return (T) dto;
    }

    public Map<String, String> asKeyValue(String keyField, String valueField) throws NoContentException {
        Map<String, String> result = new HashMap<>();
        for (SearchHit hit : hits) {
            dto.set(hit.getSourceAsString(), Dto.Action.GET);
            Object key = dto.getPropertyValue(keyField);
            Object value = dto.getPropertyValue(valueField);
            result.put(key == null ? null : key.toString(), value == null ? null : value.toString());
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValue(KeyValueData definition) throws NoContentException {
        Map<String, String> result = new HashMap<>();
        for (SearchHit hit : hits) {
            dto.set(hit.getSourceAsString(), Dto.Action.GET);
            Object key = dto.getPropertyValue(definition.getKeyField());
            Object value = dto.getPropertyValue(definition.getValueField());

            result.put(
                definition.getKey(key == null ? null : key.toString()),
                definition.getValue(value == null ? null : value.toString())
            );
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValueLocalized(String keyField, String valueField) throws NoContentException {
        Map<String, String> result = new HashMap<>();
        for (SearchHit hit : hits) {
            dto.set(hit.getSourceAsString(), Dto.Action.GET);
            Object key = dto.getPropertyValue(keyField);
            Map<String, String> value = (Map<String, String>) dto.getPropertyValue(valueField);
            result.put(
                key == null ? null : key.toString(),
                value == null ? null : ExtraUtils.getStringFromMap(value, locales)
            );
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    private void mapRecordToDto(SearchHit document)  {
        String json = document.getSourceAsString();
        //todo support opening values that are json strings
        //todo support lang
        //todo support enums
        if (json != null) {
            dto.reset();
            dto.set(Json.d.mapFromJson(json, String.class, Object.class), Dto.Action.GET);
        }
    }
}