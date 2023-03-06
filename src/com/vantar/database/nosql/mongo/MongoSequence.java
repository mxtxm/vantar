package com.vantar.database.nosql.mongo;

import com.vantar.database.dto.*;

@com.vantar.database.dto.Mongo
@Storage(Mongo.Sequence.COLLECTION)
public class MongoSequence extends DtoBase {

    public Long id;

    public String n;

    public Long c;

}