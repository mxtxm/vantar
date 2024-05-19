package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.exception.DatabaseException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.*;
import com.vantar.util.string.StringUtil;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ElasticIndexes {

    private static final String ELASTIC_SETTINGS_JSON = "/data/index/elasticsearch-settings";
    private static final Logger log = LoggerFactory.getLogger(ElasticIndexes.class);


    public static void create() throws DatabaseException {
        for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            deleteIndex(dto);
            createIndex(dto);
        }
    }

    public static void deleteIndex(Dto dto) throws DatabaseException {
        deleteIndex(dto.getStorage());
    }

    public static void deleteIndex(String collection) throws DatabaseException {
        if (!indexExists(collection)) {
            log.info("elasticsearch index not exists({})", collection);
            return;
        }

        DeleteIndexRequest request = new DeleteIndexRequest(collection);
        try {
            ElasticConnection.getClient().indices().delete(request, RequestOptions.DEFAULT);
            log.info("elasticsearch index deleted({})", collection);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    public static boolean indexExists(Dto dto) throws DatabaseException {
        return indexExists(dto.getStorage());
    }

    public static boolean indexExists(String collection) throws DatabaseException {
        GetIndexRequest request = new GetIndexRequest(collection);
        try {
            return ElasticConnection.getClient().indices().exists(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    public static void createIndex(Dto dto) throws DatabaseException {
        String collection = dto.getStorage();
        String settingsJson = FileUtil.getFileContentFromClassPath(
            ELASTIC_SETTINGS_JSON + "-" + collection + ".json",
            ELASTIC_SETTINGS_JSON + ".json"
        );

        CreateIndexRequest request = new CreateIndexRequest(collection);
        if (StringUtil.isNotEmpty(settingsJson)) {
            request.settings(settingsJson, XContentType.JSON);
        }
        request.mapping(Json.d.toJson(getDtoMappings(dto)), XContentType.JSON);

        try {
            ElasticConnection.getClient().indices().create(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }

        log.info("elasticsearch index created({})", dto.getStorage());
    }

    public static void updateIndex(Dto dto) throws DatabaseException {
        String collection = dto.getStorage();
        String settingsJson = FileUtil.getFileContentFromClassPath(
            ELASTIC_SETTINGS_JSON + "-" + collection + ".json",
            ELASTIC_SETTINGS_JSON + ".json"
        );

        try {
            ElasticConnection.getClient().indices().close(new CloseIndexRequest(collection), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }

        UpdateSettingsRequest updateRequest = new UpdateSettingsRequest(collection);
        if (StringUtil.isNotEmpty(settingsJson)) {
            updateRequest.settings(settingsJson, XContentType.JSON);
            updateRequest.setPreserveExisting(false);
            try {
                ElasticConnection.getClient().indices().putSettings(updateRequest, RequestOptions.DEFAULT);
            } catch (ElasticsearchStatusException | IOException e) {
                throw new DatabaseException(e);
            }
        }

        PutMappingRequest mappingRequest = new PutMappingRequest(collection);
        mappingRequest.source(Json.d.toJson(getDtoMappings(dto)), XContentType.JSON);
        try {
            ElasticConnection.getClient().indices().putMapping(mappingRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }

        try {
            ElasticConnection.getClient().indices().open(new OpenIndexRequest(collection), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }

        log.info("elasticsearch index updated({})", dto.getStorage());
    }

    public static Map<String, Object> getMapping(Dto dto) throws DatabaseException {
        String collection = dto.getStorage();
        GetMappingsRequest request = new GetMappingsRequest();
        request.indices(collection);
        try {
            GetMappingsResponse response = ElasticConnection.getClient().indices().getMapping(request, RequestOptions.DEFAULT);
            return response.mappings().get(collection).sourceAsMap();
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    public static void clone(Dto dto, String cloneCollection) throws DatabaseException {
        ResizeRequest request = new ResizeRequest(dto.getStorage(), cloneCollection);
        try {
            ElasticConnection.getClient().indices().clone(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    public static void shrink(Dto dto) throws DatabaseException {
        String collection = dto.getStorage();
        String collectionTemp = collection + "shrink";
        shrink(collection, collectionTemp);
        shrink(collectionTemp, collection);
    }

    public static void refresh(Dto dto) throws DatabaseException {
        RefreshRequest request = new RefreshRequest(dto.getStorage());
        try {
            ElasticConnection.getClient().indices().refresh(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    public static void flush(Dto dto) throws DatabaseException {
        FlushRequest request = new FlushRequest(dto.getStorage());
        try {
            ElasticConnection.getClient().indices().flush(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    private static void shrink(String target, String source) throws DatabaseException {
        ResizeRequest request = new ResizeRequest(target, source);
        try {
            ElasticConnection.getClient().indices().shrink(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Annotations:
     * NoStoreValueStoreIndex
     * DataType("text,keyword")
     * Analyzer("analyzer:persian_without_stopwords,search_analyzer:persian_with_stopwords,search_quote_analyzer:persian_without_stopwords")
     * StopWords("fa")
     */
    private static Map<String, Map<String, Map<String, Object>>> getDtoMappings(Dto dto) {
        Map<String, Map<String, Map<String, Object>>> mappings = new HashMap<>();
        Map<String, Map<String, Object>> properties = new HashMap<>();

        dto.getPropertyTypes().forEach((name, type) -> {
            Field field = dto.getField(name);
            DataType dataType = field.getAnnotation(DataType.class);

            boolean isList = type.equals(List.class);
            if (isList && dataType == null) {
                return;
            }

            Map<String, Object> params = new HashMap<>();

            Analyzer analyzer = field.getAnnotation(Analyzer.class);
            if (analyzer != null) {
                for (String item : StringUtil.split(analyzer.value(), VantarParam.SEPARATOR_COMMON)) {
                    String[] keyVal = StringUtil.split(item, VantarParam.SEPARATOR_KEY_VAL);
                    params.put(keyVal[0], keyVal[1]);
                }
            }

            TermVector vector = field.getAnnotation(TermVector.class);
            if (vector != null) {
                params.put("term_vector", vector.value());
            }

            String typeName;
            if (dataType == null) {
                if (String.class.equals(type)) {
                    typeName = "text";
                } else if (DateTime.class.equals(type)) {
                    typeName = "long";
                } else {
                    typeName = type.getSimpleName().toLowerCase();
                }
            } else {

                if (isList) {
                    Map<String, Map<String, String>> props = new HashMap<>();
                    for (String item : StringUtil.split(StringUtil.remove(dataType.value(), "list>>"), VantarParam.SEPARATOR_COMMON)) {
                        String[] propertyType = StringUtil.split(item, VantarParam.SEPARATOR_KEY_VAL);
                        Map<String, String> attributes = new HashMap<>();
                        attributes.put("type", propertyType[1]);
                        props.put(propertyType[0], attributes);
                    }
                    params.put("properties", props);

                    typeName = null;
                } else {

                    if (!dataType.value().equals("object")) {
                        params.put("store", false);
                        params.put("index", !dto.hasAnnotation(name, NoIndex.class));
                    }

                    typeName = dataType.value();
                }
            }
            if (typeName != null) {
                params.put("type", typeName);
                if (typeName.equals("keyword")) {
                    params.put("eager_global_ordinals", true);
                }
            }

            Object defaultValue = dto.getDefaultValue(name);
            if (defaultValue != null) {
                params.put("null_value", defaultValue);
            } else if ("keyword".equals(typeName)) {
                params.put("null_value", "");
            }

            properties.put(name, params);
        });

        //todo: StoreIndexNotValue

        mappings.put("properties", properties);
        return mappings;
    }
}