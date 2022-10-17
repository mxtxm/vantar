package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.mongodb.client.model.Projections;
import com.vantar.admin.model.Admin;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.exception.DatabaseException;
import org.bson.Document;
import java.util.*;


class MongoQuery {

    public Document matches;
    public Document sort;
    public Integer skip;
    public Integer limit;
    public List<String> union;
    public String[] columns;
    private List<Document> groups;
    private final Dto dto;
    private final Dto dtoResult;


    public MongoQuery(Dto dto, Dto dtoResult) {
        this.dto = dto;
        this.dtoResult = dtoResult;
    }

    public MongoQuery(QueryBuilder q) {
        dto = q.getDto();
        dtoResult = q.getDtoResult();
        MongoMapping.queryBuilderToMongoQuery(q, this);
    }

    public void group(Document group) {
        if (groups == null) {
             groups = new ArrayList<>();
        }
        groups.add(group);
    }

    public void average(String field) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        Document group = new Document(Mongo.ID, null);
        group.put("average", new Document("$avg", "$" + field));
        groups.add(group);
    }

    public long count() throws DatabaseException {
        try {
            if (matches == null || matches.isEmpty()) {
                return MongoConnection.getDatabase().getCollection(dto.getStorage()).countDocuments();
            } else {
                return MongoConnection.getDatabase().getCollection(dto.getStorage()).countDocuments(matches);
            }
        } catch (Exception e) {
            Mongo.log.error(" !! count({})", dto, e);
            throw new DatabaseException(e);
        }
    }

    public QueryResult getData() throws DatabaseException {
        try {
            FindIterable<Document> find;
            if (matches == null || matches.isEmpty()) {
                find = MongoConnection.getDatabase().getCollection(dto.getStorage()).find();
            } else {
                find = MongoConnection.getDatabase().getCollection(dto.getStorage()).find(matches);
            }

            if (columns != null && columns.length > 0) {
                find.projection(Projections.fields(Projections.include(columns)));
            }

            if (sort != null) {
                find.sort(sort);
            }
            if (limit != null) {
                find.limit(limit);
            }
            if (skip != null) {
                find.skip(skip);
            }

            return new MongoQueryResult(find, dtoResult);
        } catch (Exception e) {
            Mongo.log.error(" !! query {}", dto.getStorage(), e);
            throw new DatabaseException(e);
        }
    }

    public FindIterable<Document> getResult() throws DatabaseException {
        try {
            FindIterable<Document> find;
            if (matches == null || matches.isEmpty()) {
                find = MongoConnection.getDatabase().getCollection(dto.getStorage()).find();
            } else {
                find = MongoConnection.getDatabase().getCollection(dto.getStorage()).find(matches);
            }

            if (columns != null && columns.length > 0) {
                find.projection(Projections.fields(Projections.include(columns)));
            }

            return find;
        } catch (Exception e) {
            Mongo.log.error(" !! query {}", dto.getStorage(), e);
            throw new DatabaseException(e);
        }
    }

    public QueryResult getDataX() throws DatabaseException {
        List<Document> query = new ArrayList<>();

        if (union != null) {
            for (String collection : union) {
                query.add(new Document("$unionWith", collection));
            }
        }

        if (matches != null && !matches.isEmpty()) {
            query.add(new Document("$match", matches));
        }
        if (sort != null) {
            query.add(new Document("$sort", sort));
        }
        if (skip != null) {
            query.add(new Document("$skip", skip));
        }
        if (limit != null) {
            query.add(new Document("$limit", limit));
        }

        try {
            return new MongoQueryResult(
                MongoConnection.getDatabase().getCollection(dto.getStorage()).aggregate(query),
                dtoResult
            );
        } catch (Exception e) {
            Mongo.log.error(" !! aggr {}", dto, e);
            throw new DatabaseException(e);
        }
    }

    public AggregateIterable<Document> getAggregate() throws DatabaseException {
        List<Document> query = new ArrayList<>(7);
        if (matches != null && !matches.isEmpty()) {
            query.add(new Document("$match", matches));
        }
        if (groups != null && !groups.isEmpty()) {
            for (Document group : groups) {
                query.add(new Document("$group", group));
            }
        }
        if (sort != null) {
            query.add(new Document("$sort", sort));
        }
        if (limit != null) {
            query.add(new Document("$limit", limit));
        }
        if (skip != null) {
            query.add(new Document("$skip", skip));
        }
        try {
            return MongoConnection.getDatabase().getCollection(dto.getStorage()).aggregate(query).allowDiskUse(true);
        } catch (Exception e) {
            Mongo.log.error(" !! aggr {}", dto, e);
            throw new DatabaseException(e);
        }
    }
}
