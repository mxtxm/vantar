package com.vantar.admin.model.database.data.panel;

import com.vantar.admin.modelt.database.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data/list",
    "/admin/data/view",

    "/admin/data/delete",
    "/admin/data/delete/many",
    "/admin/data/purge",
    "/admin/data/undelete/index",
    "/admin/data/undelete/many",

    "/admin/data/insert",
    "/admin/data/update",

    "/admin/data/import",
    "/admin/data/export",

    "/admin/data/dependencies",
    "/admin/data/dependencies/dto",




    "/admin/data/log",
    "/admin/data/log/rows",
    "/admin/data/log/object",

    "/admin/data/archive/switch",
})
@MultipartConfig(
    location="/tmp",
    fileSizeThreshold=100*1024*1024,
    maxFileSize=100*1024*1024,
    maxRequestSize=100*1024*1024
)
public class Controller extends RouteToMethod {

    public void dataList(Params params, HttpServletResponse response) throws FinishException {
        AdminDataList.list(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataView(Params params, HttpServletResponse response) throws FinishException {
        AdminDataView.view(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void delete(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.deleteOne(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDeleteMany(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.deleteMany(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.purge(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUndeleteIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUnDelete.index(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUndeleteMany(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUnDelete.updateMany(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataInsert(Params params, HttpServletResponse response) throws FinishException {
        AdminDataInsert.insert(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUpdate(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUpdate.update(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataImport(Params params, HttpServletResponse response) throws FinishException {
        AdminDataImportExport.importData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataExport(Params params, HttpServletResponse response) throws VantarException {
        AdminDataImportExport.exportData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDependencies(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDependency.getRecord(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDependenciesDto(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDependency.getDto(params, response, DtoDictionary.get(params.getString("dto")));
    }






    public void dataArchiveSwitch(Params params, HttpServletResponse response) throws FinishException {
        AdminDataArchive.archiveSwitch(params, response, DtoDictionary.get(params.getString("dto")));
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
}