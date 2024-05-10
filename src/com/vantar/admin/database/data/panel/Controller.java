package com.vantar.admin.database.data.panel;

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
    "/admin/data/undelete/search",
    "/admin/data/undelete",
    "/admin/data/undelete/many",

    "/admin/data/insert",
    "/admin/data/update",
    "/admin/data/update/property",

    "/admin/data/import",
    "/admin/data/export",

    "/admin/data/dependencies",
    "/admin/data/dependencies/dto",

    "/admin/data/log/action/search",
    "/admin/data/log/action/revert",
    "/admin/data/log/action/differences",
    "/admin/data/log/web/get",
    "/admin/data/log/web/search",

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

    public void dataDelete(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.deleteOne(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDeleteMany(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.deleteMany(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDelete.purge(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUndeleteSearch(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUnDelete.search(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUndeleteMany(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUnDelete.undeleteMany(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUndelete(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUnDelete.undeleteOne(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataInsert(Params params, HttpServletResponse response) throws FinishException {
        AdminDataInsert.insert(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUpdate(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUpdate.update(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataUpdateProperty(Params params, HttpServletResponse response) throws FinishException {
        AdminDataUpdateProperty.update(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataImport(Params params, HttpServletResponse response) throws FinishException {
        AdminDataImportExport.importData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataExport(Params params, HttpServletResponse response) throws VantarException {
        AdminDataImportExport.exportData(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDependencies(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDependency.getDtoItem(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataDependenciesDto(Params params, HttpServletResponse response) throws FinishException {
        AdminDataDependency.getDto(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLogActionSearch(Params params, HttpServletResponse response) throws FinishException {
        AdminLogAction.search(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLogActionRevert(Params params, HttpServletResponse response) throws FinishException {
        AdminLogAction.revert(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLogActionDifferences(Params params, HttpServletResponse response) throws FinishException {
        AdminLogActionDiff.view(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataLogWebGet(Params params, HttpServletResponse response) throws VantarException {
        Response.writeJson(response, AdminLogWeb.getData(params));
    }

    public void dataLogWebSearch(Params params, HttpServletResponse response) throws FinishException {
        AdminLogWeb.search(params, response, DtoDictionary.get(params.getString("dto")));
    }

    public void dataArchiveSwitch(Params params, HttpServletResponse response) throws FinishException {
        AdminDataArchive.archiveSwitch(params, response, DtoDictionary.get(params.getString("dto")));
    }
}