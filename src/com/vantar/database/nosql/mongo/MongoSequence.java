package com.vantar.database.nosql.mongo;

import com.vantar.database.dto.*;

@Mongo
@Storage(DbMongo.AUTO_INCREMENT_COLLECTION)
public class MongoSequence extends DtoBase {

    public Long id;
    public String n;
    public Long c;
}