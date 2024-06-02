package com.vantar.database.nosql.mongo;

import com.vantar.database.common.Db;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.service.log.ServiceLog;
import org.bson.Document;
import java.util.*;


public class MongoQuery {

    public Document matches;
    public Document sort;
    public Integer skip;
    public Integer limit;
    public List<String> union;
    public String[] columns;

    protected final DbMongo db;
    protected List<Document> groups;
    protected final Dto dto;
    protected final String storage;


    public MongoQuery(Dto dto) {
        this(dto, Db.mongo);
    }

    public MongoQuery(QueryBuilder q) {
        this(q, Db.mongo);
    }

    public MongoQuery(Dto dto, DbMongo db) {
        storage = dto.getStorage();
        columns = dto.getFieldNamesForQuery();
        this.dto = dto;
        this.db = db;
        if (db ==null) {
            ServiceLog.log.error(">>>>>>fuck");
        }
    }

    public MongoQuery(QueryBuilder q, DbMongo db) {
        dto = q.getDto();
        storage = dto.getStorage();
        columns = dto.getFieldNamesForQuery();
        this.db = db;
        MongoMapping.queryBuilderToMongoQuery(q, this);
    }

    public void setGroup(Document group) {
        if (groups == null) {
             groups = new ArrayList<>();
        }
        groups.add(group);
    }

    public void setAverage(String field) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        Document group = new Document(DbMongo.ID, null);
        group.put("average", new Document("$avg", "$" + field));
        groups.add(group);
    }
}
