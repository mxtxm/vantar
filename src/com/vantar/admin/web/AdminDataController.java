package com.vantar.admin.web;

import com.vantar.admin.model.database.AdminData;
import com.vantar.admin.model.heath.AdminActionLog;
import com.vantar.admin.model.index.AdminIndexData;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data",

    "/admin/data/fields",
    "/admin/data/list",
    "/admin/data/update",
    //"/admin/data/delete",
    "/admin/data/delete/many",
    "/admin/data/purge",
    "/admin/data/insert",
    "/admin/data/import",
    "/admin/data/export",
    "/admin/data/log",
    "/admin/data/log/rows",
    "/admin/data/log/object",

    "/admin/data/mongo/status",
    "/admin/data/sql/status",
    "/admin/data/elastic/status",

    "/admin/data/archive/switch",
})
@MultipartConfig(
    location="/tmp",
    fileSizeThreshold=100*1024*1024,
    maxFileSize=100*1024*1024,
    maxRequestSize=100*1024*1024
)
public class AdminDataController extends RouteToMethod {

    public void data(Params params, HttpServletResponse response) throws FinishException {
        AdminIndexData.index(params, response);
    }

    public void dataFields(Params params, HttpServletResponse response) throws FinishException {
        AdminData.fields(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataList(Params params, HttpServletResponse response) throws FinishException {
        AdminData.list(params, response, DtoDictionary.get(params.getString("dto")));
    }

//    public void dataDelete(Params params, HttpServletResponse response) throws FinishException {
//        AdminData.delete(params, response, DtoDictionary.get(params.getString("dto")));
//    }

    public void dataDeleteMany(Params params, HttpServletResponse response) throws FinishException {
        AdminData.deleteMany(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminData.purge(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUpdate(Params params, HttpServletResponse response) throws FinishException {
        AdminData.update(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataInsert(Params params, HttpServletResponse response) throws FinishException {
        AdminData.insert(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataImport(Params params, HttpServletResponse response) throws FinishException {
        AdminData.importData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataExport(Params params, HttpServletResponse response) throws VantarException {
        AdminData.exportData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLog(Params params, HttpServletResponse response) throws FinishException {
        AdminActionLog.loadLogPage(params, response);
    }

    public void dataLogRows(Params params, HttpServletResponse response) throws FinishException {
        Response.writeString(response, AdminActionLog.getRows(params));
    }

    public void dataLogObject(Params params, HttpServletResponse response) {
        Response.writeString(response, AdminActionLog.getObject(params));
    }

    public void dataSqlStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminData.statusSql(params, response);
    }

    public void dataMongoStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminData.statusMongo(params, response);
    }

    public void dataElasticStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminData.statusElastic(params, response);
    }

    public void dataArchiveSwitch(Params params, HttpServletResponse response) throws FinishException {
        AdminData.archiveSwitch(params, response, DtoDictionary.get(params.getString("dto")));
    }
}