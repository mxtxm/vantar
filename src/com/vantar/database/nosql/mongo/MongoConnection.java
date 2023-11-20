package com.vantar.database.nosql.mongo;

import com.mongodb.MongoClient;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.event.*;
import com.vantar.exception.DatabaseException;
import com.vantar.util.string.StringUtil;
import java.util.*;


public class MongoConnection {

    public static MongoConfig config;
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static boolean isUp = false;
    public static boolean isShutdown = false;


    public static boolean isEnabled() {
        return config != null;
    }

    public static boolean isUp() {
        return isUp;
    }

    public synchronized static void connect(MongoConfig config) {
        if (isShutdown) {
            return;
        }
        MongoConnection.config = config;
        MongoClientOptions.Builder o = MongoClientOptions.builder()
            .connectTimeout(config.getMongoConnectTimeout())
            .socketTimeout(config.getMongoSocketTimeout())
            .addConnectionPoolListener(new ConnectionPoolListenerAdapter() {
                @Override
                public void connectionPoolClosed(ConnectionPoolClosedEvent event) {
                    super.connectionPoolClosed(event);
                    connect(config);
                }
            })
            .cursorFinalizerEnabled(false)
            .connectionsPerHost(2000)
            .serverSelectionTimeout(config.getMongoServerSelectionTimeout());

        MongoClientOptions options = o.build();

        List<ServerAddress> servers = new ArrayList<>();
        for (String host : config.getMongoHosts().split(",")) {
            String[] parts = host.split(":");
            servers.add(new ServerAddress(parts[0], StringUtil.toInteger(parts[1])));
        }

        String user = config.getMongoUser();
        if (user == null || user.isEmpty()) {
            mongoClient = new MongoClient(servers, options);
        } else {
            MongoCredential credential = MongoCredential.createCredential(
                user,
                config.getMongoDatabase(),
                config.getMongoPassword().toCharArray()
            );

            mongoClient = new MongoClient(servers, credential, options);
        }

        try {
            database = mongoClient.getDatabase(config.getMongoDatabase());
            database.getCollection(Mongo.Sequence.COLLECTION).countDocuments();
            isUp = true;
            Mongo.log.info(" >> successfully connected to mongo");
        } catch (Exception e) {
            Mongo.log.error(" !! FAILED TO CONNECT TO MONGO (is mongo up)", e);
            mongoClient.close();
        }

//        TransactionOptions txnOptions = TransactionOptions.builder()
//            .readPreference(ReadPreference.primary())
//            .readConcern(ReadConcern.LOCAL)
//            .writeConcern(WriteConcern.MAJORITY)
//            .build();

//        https://docs.mongodb.com/manual/core/transactions/
//
//        MongoClient mongoClient = new MongoClient();
//        DB db = mongoClient.getDB("test");
//
//        char[] password = new char[] {'s', 'e', 'c', 'r', 'e', 't'};
//        boolean authenticated = db.authenticate("root", password);
//
//        if (authenticated) {
//            System.out.println("Successfully logged in to MongoDB!");
//        } else {
//            System.out.println("Invalid username/password");
//        }
    }

    public static MongoDatabase getDatabase() throws DatabaseException {
        if (isShutdown) {
            throw new DatabaseException("mongo is down");
        }
        if (!isUp || mongoClient == null || database == null) {
            connect(config);
        }
        if (!isUp) {
            throw new DatabaseException("mongo is down");
        }
        return database;
    }

    public synchronized static void shutdown() {
        isShutdown = true;
        mongoClient.close();
        isUp = false;
        Mongo.log.info(" >> mongo connection closed");
    }

    public static MongoIterable<String> getCollections() throws DatabaseException {
        return getDatabase().listCollectionNames();
    }
}
