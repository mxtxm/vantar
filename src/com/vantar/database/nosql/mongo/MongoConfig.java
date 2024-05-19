package com.vantar.database.nosql.mongo;

import org.aeonbits.owner.Config;


public interface MongoConfig {

    @Config.Key("mongo.connection.string")
    String getMongoConnectionString();

    @Config.Key("mongo.database")
    String getMongoDatabase();

    @Config.Key("mongo.database.test")
    String getMongoDatabaseTest();
}
