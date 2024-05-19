package com.vantar.database.common;

import com.vantar.business.ModelMongo;
import com.vantar.database.nosql.mongo.DbMongo;
import com.vantar.service.Services;


public class Db {

    public enum Dbms implements Services.DataSources  {
        MONGO,
        SQL,
        ELASTIC,
        NOSTORE,
    }


    public static DbMongo mongo;
    public static Object sql;
    public static Object elastic;

    public static ModelMongo modelMongo;
}
