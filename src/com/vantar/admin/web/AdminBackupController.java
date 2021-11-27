package com.vantar.admin.web;

import com.vantar.admin.model.AdminBackup;
import com.vantar.common.Settings;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/data/backup/sql",
    "/admin/data/restore/sql",
    "/admin/data/backup/files/sql",

    "/admin/data/backup/mongo",
    "/admin/data/restore/mongo",
    "/admin/data/backup/files/mongo",

    "/admin/data/backup/elastic",
    "/admin/data/restore/elastic",
    "/admin/data/backup/files/elastic",

    "/admin/data/backup/download",
    "/admin/data/backup/delete",
})
public class AdminBackupController extends RouteToMethod {

    // > > > SQL

    public void dataBackupSql(Params params, HttpServletResponse response) {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.SQL);
    }

    public void dataRestoreSql(Params params, HttpServletResponse response) {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.SQL);
    }

    public void dataBackupFilesSql(Params params, HttpServletResponse response) {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.SQL);
    }

    // > > > MONGO

    public void dataBackupMongo(Params params, HttpServletResponse response) {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void dataRestoreMongo(Params params, HttpServletResponse response) {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void dataBackupFilesMongo(Params params, HttpServletResponse response) {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.MONGO);
    }

    // > > > ELASTIC

    public void dataBackupElastic(Params params, HttpServletResponse response) {
        AdminBackup.backup(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    public void dataRestoreElastic(Params params, HttpServletResponse response) {
        AdminBackup.restore(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    public void dataBackupFilesElastic(Params params, HttpServletResponse response) {
        AdminBackup.backupFiles(params, response, DtoDictionary.Dbms.ELASTIC);
    }

    // > > > all

    public void dataBackupDownload(Params params, HttpServletResponse response) {
        String filename = params.getString("file");
        Response.download(response, Settings.backup().getBackupDir() + filename, filename + ".zip");
    }

    public void dataBackupDelete(Params params, HttpServletResponse response) {
        AdminBackup.deleteFile(params, response);
    }
}