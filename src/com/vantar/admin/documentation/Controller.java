package com.vantar.admin.documentation;

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
}