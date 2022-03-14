package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/sql/synch",
    "/admin/database/sql/index/create",
    "/admin/database/sql/indexes",
    "/admin/database/sql/purge",
    "/admin/database/sql/import",

    "/admin/database/mongo/index/create",
    "/admin/database/mongo/indexes",
    "/admin/database/mongo/sequences",
    "/admin/database/mongo/purge",
    "/admin/database/mongo/import",

    "/admin/database/elastic/synch",
    "/admin/database/elastic/purge",
    "/admin/database/elastic/import",
    "/admin/database/elastic/mapping/get",
    "/admin/database/elastic/actions",
})
public class AdminDatabaseController extends RouteToMethod {

    // > > > SQL

    public void databaseSqlSynch(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.synchSql(params, response);
    }

    public void databaseSqlIndexCreate(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.createSqlIndex(params, response);
    }

    public void databaseSqlIndexes(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.listSqlIndexes(params, response);
    }

    public void databaseSqlPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.purgeSql(params, response);
    }

    public void databaseSqlImport(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.importSql(params, response);
    }

    // > > > mongo

    public void databaseMongoIndexCreate(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.createMongoIndex(params, response);
    }

    public void databaseMongoIndexes(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.listMongoIndexes(params, response);
    }

    public void databaseMongoPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.purgeMongo(params, response);
    }

    public void databaseMongoSequences(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.listMongoSequences(params, response);
    }

    public void databaseMongoImport(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.importMongo(params, response);
    }

    // > > > ELASTIC

    public void databaseElasticSynch(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.synchElastic(params, response);
    }

    public void databaseElasticPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.purgeElastic(params, response);
    }

    public void databaseElasticImport(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.importElastic(params, response);
    }

    public void databaseElasticMappingGet(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.getMappingElastic(params, response);
    }

    public void databaseElasticActions(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabase.actionsElastic(params, response);
    }
}