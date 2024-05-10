package com.vantar.database.nosql.mongo;

import org.aeonbits.owner.Config;


public interface MongoConfig {

    @Config.Key("mongo.connection.string")
    String getMongoConnectionString();

    @Config.Key("mongo.hosts")
    String getMongoHosts();

    @Config.Key("mongo.database")
    String getMongoDatabase();

    @Config.Key("mongo.user")
    String getMongoUser();

    @Config.Key("mongo.password")
    String getMongoPassword();

    @Config.DefaultValue("60000")
    @Config.Key("mongo.connect.timeout")
    int getMongoConnectTimeout();

    @Config.DefaultValue("60000")
    @Config.Key("mongo.socket.timeout")
    int getMongoSocketTimeout();

    @Config.DefaultValue("60000")
    @Config.Key("mongo.server.selection.timeout")
    int getMongoServerSelectionTimeout();
}
