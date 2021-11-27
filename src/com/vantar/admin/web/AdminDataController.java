package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/data",

    "/admin/data/fields",
    "/admin/data/list",
    "/admin/data/update",
    "/admin/data/delete",
    "/admin/data/purge",
    "/admin/data/insert",
    "/admin/data/import",
    "/admin/data/log",

    "/admin/data/sql/status",
    "/admin/data/mongo/status",
    "/admin/data/elastic/status",
})
public class AdminDataController extends RouteToMethod {

    public void data(Params params, HttpServletResponse response) {
        AdminData.index(params, response);
    }

    public void dataFields(Params params, HttpServletResponse response) {
        AdminData.fields(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataList(Params params, HttpServletResponse response) {
        AdminData.list(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDelete(Params params, HttpServletResponse response) {
        AdminData.delete(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataPurge(Params params, HttpServletResponse response) {
        AdminData.purge(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUpdate(Params params, HttpServletResponse response) {
        AdminData.update(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataInsert(Params params, HttpServletResponse response) {
        AdminData.insert(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataImport(Params params, HttpServletResponse response) {
        AdminData.importData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLog(Params params, HttpServletResponse response) {
        AdminActionLog.show(params, response);
    }

    public void dataSqlStatus(Params params, HttpServletResponse response) {
        AdminData.statusSql(params, response);
    }

    public void dataMongoStatus(Params params, HttpServletResponse response) {
        AdminData.statusMongo(params, response);
    }

    public void dataElasticStatus(Params params, HttpServletResponse response) {
        AdminData.statusElastic(params, response);
    }
}