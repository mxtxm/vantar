package com.vantar.admin.web;

import com.vantar.admin.model.AdminBackup;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data/backup/sql",
    "/admin/data/restore/sql",
    "/admin/data/backup/files/sql",

    "/admin/data/backup/mongo",
    "/admin/data/backup/mongo/q",
    "/admin/data/restore/mongo",
    "/admin/data/backup/files/mongo",

    "/admin/data/backup/elastic",
    "/admin/data/restore/elastic",
    "/admin/data/backup/files/elastic",

    "/admin/data/backup/download",
    "/admin/data/backup/upload",
    "/admin/data/backup/logs",
})
@MultipartConfig(
    location="/tmp",
    fileSizeThreshold=100*1024*1024,
    maxFileSize=100*1024*1024,
    maxRequestSize=100*1024*1024*5
)
public class AdminBackupController extends RouteToMethod {

    // > > > SQL

    public void dataBackupSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.SQL);
    }

    public void dataRestoreSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.SQL);
    }

    public void dataBackupFilesSql(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.SQL);
    }

    // > > > MONGO

    public void dataBackupMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void dataBackupMongoQ(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupQuery(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void dataRestoreMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void dataBackupFilesMongo(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.MONGO);
    }

    // > > > ELASTIC

    public void dataBackupElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    public void dataRestoreElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    public void dataBackupFilesElastic(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    // > > > all

    public void dataBackupDownload(Params params, HttpServletResponse response) {
        String filename = params.getString("file");
        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            return;
        }
        Response.download(response, backup.getPath() + filename, filename + ".zip");
    }

    public void dataBackupUpload(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.upload(params, response);
    }

    public void dataBackupLogs(Params params, HttpServletResponse response) throws FinishException {
        AdminBackup.logs(params, response);
    }
}