package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data",

    "/admin/data/fields",
    "/admin/data/list",
    "/admin/data/update",
    "/admin/data/delete",
    "/admin/data/delete/many",
    "/admin/data/purge",
    "/admin/data/insert",
    "/admin/data/import",
    "/admin/data/export",
    "/admin/data/log",

    "/admin/data/sql/status",
    "/admin/data/mongo/status",
    "/admin/data/elastic/status",
})
public class AdminDataController extends RouteToMethod {

    public void data(Params params, HttpServletResponse response) throws FinishException {
        AdminData.index(params, response);
    }

    public void dataFields(Params params, HttpServletResponse response) throws FinishException {
        AdminData.fields(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataList(Params params, HttpServletResponse response) throws FinishException {
        AdminData.list(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDelete(Params params, HttpServletResponse response) throws FinishException {
        AdminData.delete(params, response, DtoDictionary.get(params.getString("dto")));
    }

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
        AdminActionLog.record(params, response);
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
}