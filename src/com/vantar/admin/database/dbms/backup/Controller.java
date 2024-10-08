package com.vantar.admin.database.dbms.backup;

import com.vantar.database.common.Db;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data/backup/mongo",
    "/admin/data/backup/mongo/q",
    "/admin/data/restore/mongo",
    "/admin/data/backup/files/mongo",

    "/admin/data/backup/sql",
    "/admin/data/restore/sql",
    "/admin/data/backup/files/sql",

    "/admin/data/backup/elastic",
    "/admin/data/restore/elastic",
    "/admin/data/backup/files/elastic",

    "/admin/data/backup/download",
    "/admin/data/backup/upload",
    "/admin/data/backup/logs",
})
@MultipartConfig(
    location="/tmp",
    fileSizeThreshold=1000*1024*1024,
    maxFileSize=1000*1024*1024,
    maxRequestSize=400*1024*1024*5
)
public class Controller extends RouteToMethod {

    // > > > MONGO

    public void dataBackupMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, Db.Dbms.MONGO);
    }

    public void dataBackupMongoQ(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupQuery(params, response, Db.Dbms.MONGO);
    }

    public void dataRestoreMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, Db.Dbms.MONGO);
    }

    public void dataBackupFilesMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, Db.Dbms.MONGO);
    }

    // > > > SQL

    public void dataBackupSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, Db.Dbms.SQL);
    }

    public void dataRestoreSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, Db.Dbms.SQL);
    }

    public void dataBackupFilesSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, Db.Dbms.SQL);
    }

    // > > > ELASTIC

    public void dataBackupElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, Db.Dbms.ELASTIC);
    }

    public void dataRestoreElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, Db.Dbms.ELASTIC);
    }

    public void dataBackupFilesElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, Db.Dbms.ELASTIC);
    }

    // > > > all

    public void dataBackupDownload(Params params, HttpServletResponse response) {
        String filename = params.getString("file");
        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            return;
        }
        Response.download(response, backup.getDir() + filename, filename + ".zip");
    }

    public void dataBackupUpload(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.upload(params, response);
    }

    public void dataBackupLogs(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.logs(params, response);
    }
}