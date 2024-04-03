package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.*;
import java.io.Closeable;
import java.util.Map;


public class ElasticWrite implements Closeable {

    private BulkRequest bulk;


    public static long insertOne(Dto dto) throws DatabaseException, VantarException {
        dto.setCreateTime(true);
        dto.setUpdateTime(true);
        if (dto.getId() == null) {
            dto.setId(ElasticSearch.getNextId(dto.getStorage()));
        }
        writeIndexOne(dto, DocWriteRequest.OpType.CREATE);
        return dto.getId();
    }

    public long insert(Dto dto) throws DatabaseException {
        dto.setCreateTime(true);
        dto.setUpdateTime(true);
        if (dto.getId() == null) {
            dto.setId(ElasticSearch.getNextId(dto.getStorage()));
        }

        if (!dto.beforeInsert()) {
            throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        writeIndex(dto, DocWriteRequest.OpType.CREATE);
        return dto.getId();
    }

    public static void upsertOne(Dto dto) throws DatabaseException, VantarException {
        dto.setCreateTime(true);
        dto.setUpdateTime(true);

        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        writeIndexOne(dto, null);
    }

    public void upsert(Dto dto) throws DatabaseException {
        dto.setCreateTime(true);
        dto.setUpdateTime(true);

        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        writeIndex(dto, null);
    }

    public static void updateOne(Dto dto) throws DatabaseException, VantarException {
        dto.setCreateTime(false);
        dto.setUpdateTime(true);
        Long id = dto.getId();

        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        if (id != null) {
            UpdateRequest request = new UpdateRequest(dto.getStorage(), id.toString()).doc(StorableData.toMap(dto.getStorableData()));
            try {
                checkFailure(ElasticConnection.getClient().update(request, RequestOptions.DEFAULT));
                return;
            } catch (Throwable e) {
                throw new DatabaseException(e);
            }
        }

        UpdateByQueryRequest request = new UpdateByQueryRequest(dto.getStorage());
        request.setQuery(new TermQueryBuilder("user", "kimchy"));
        try {
            checkBulkFailure(ElasticConnection.getClient().updateByQuery(request, RequestOptions.DEFAULT));
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public void update(Dto dto) throws DatabaseException {
        dto.setCreateTime(false);
        dto.setUpdateTime(true);

        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        bulk.add(new UpdateRequest(dto.getStorage(), dto.getId().toString()).doc(StorableData.toMap(dto.getStorableData())));
    }

    public static void deleteOne(Dto dto) throws DatabaseException, VantarException {
        DeleteRequest request = new DeleteRequest(dto.getStorage(), dto.getId().toString());
        try {
            checkFailure(ElasticConnection.getClient().delete(request, RequestOptions.DEFAULT));
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static void purgeData(String collection) throws DatabaseException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(collection);
        request.setQuery(QueryBuilders.matchAllQuery());
        try {
            checkBulkFailure(ElasticConnection.getClient().deleteByQuery(request, RequestOptions.DEFAULT));
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public static void delete(com.vantar.database.query.QueryBuilder q) throws DatabaseException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(q.getDto().getStorage());
        request.setQuery(ElasticMapping.getElasticQuery(q.getCondition(), q.getDto()));
        try {
            checkBulkFailure(ElasticConnection.getClient().deleteByQuery(request, RequestOptions.DEFAULT));
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    public void delete(Dto dto) {
        bulk.add(new DeleteRequest(dto.getStorage(), dto.getId().toString()));
    }

    public void startTransaction() {
        bulk = new BulkRequest();
    }

    public void rollback() {
        bulk = null;
    }

    public void close() {
        bulk = null;
    }

    public void commit() throws DatabaseException {
        if (bulk == null || bulk.numberOfActions() == 0) {
            return;
        }
        try {
            BulkResponse response = ElasticConnection.getClient().bulk(bulk, RequestOptions.DEFAULT);

            if (response.hasFailures()) {
                StringBuilder sb = new StringBuilder();
                for (BulkItemResponse item : response) {
                    if (item.isFailed()) {
                        sb.append(" >").append(item.getFailure().getMessage()).append("< ");
                    }
                }
                throw new DatabaseException("bulk failed." + sb.toString());
            }
            bulk = null;
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    private static void writeIndexOne(Dto dto, DocWriteRequest.OpType action) throws DatabaseException {
        Map<String, Object> data = ElasticMapping.getDataForWrite(dto);
        IndexRequest request = new IndexRequest(dto.getStorage())
            .id(data.get(VantarParam.ID).toString())
            .source(data);

        if (action != null) {
            request.opType(action);
        }
        try {
            checkFailure(ElasticConnection.getClient().index(request, RequestOptions.DEFAULT));
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    private void writeIndex(Dto dto, DocWriteRequest.OpType action) {
        Map<String, Object> data = ElasticMapping.getDataForWrite(dto);

        IndexRequest request = new IndexRequest(dto.getStorage())
            .id(data.get(VantarParam.ID).toString())
            .source(data);

        if (action != null) {
            request.opType(action);
        }
        bulk.add(request);
    }

    private static void checkFailure(ReplicationResponse response) throws DatabaseException {
        ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
        int failedShards = shardInfo.getFailed();
        if (failedShards > 0) {
            StringBuilder sb = new StringBuilder();
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                sb.append(" >").append(failure.reason()).append("< ");
            }
            throw new DatabaseException(failedShards + " shards failed." + sb.toString());
        }
    }

    private static void checkBulkFailure(BulkByScrollResponse response) throws DatabaseException {
        try {
            if (!response.getBulkFailures().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (BulkItemResponse.Failure item : response.getBulkFailures()) {
                    sb.append(" >").append(item.getMessage()).append("< ");
                }
                throw new DatabaseException("bulk failed." + sb.toString());
            }
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }
}
