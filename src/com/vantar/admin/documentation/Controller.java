package com.vantar.admin.documentation;

import com.vantar.admin.documentation.get.WebServiceGet;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/documentation/index",
    "/admin/documentation/show",
    "/admin/documentation/show/dtos",
    "/admin/documentation/show/dtos/json",
    "/admin/documentation/webservices/xlsx",

    "/admin/documentation/webservice/get",
    "/admin/documentation/webservice/get/json",
})
public class Controller extends RouteToMethod {

    public void documentationIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDocumentation.index(params, response);
    }

    public void documentationShow(Params params, HttpServletResponse response) {
        AdminDocumentation.show(params, response);
    }

    public void documentationShowDtos(Params params, HttpServletResponse response) {
        AdminDocumentation.showDtoObjectDocument(response);
    }

    public void documentationShowDtosJson(Params params, HttpServletResponse response) {
        AdminDocumentation.showDtoObjectDocumentJson(response);
    }

    public void documentationWebservicesXlsx(Params params, HttpServletResponse response) throws VantarException {
        WebServiceManifest.downloadXlsx(response);
    }

    public void documentationWebserviceGet(Params params, HttpServletResponse response) throws VantarException {
        Response.writeJson(response, WebServiceGet.get(params));
    }

    public void documentationWebserviceGetJson(Params params, HttpServletResponse response) throws VantarException {
        Response.writeJson(response, WebServiceGet.getJson(params));
    }
}