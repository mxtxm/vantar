package com.vantar.admin.web;

import com.vantar.admin.model.document.*;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/document/index",
    "/admin/document/show",
    "/admin/document/webservices/xlsx",
})
public class AdminDocumentController extends RouteToMethod {

    public void documentIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDocument.index(params, response);
    }

    public void documentShow(Params params, HttpServletResponse response) {
        AdminDocument.show(params, response);
    }

    public void documentWebservicesXlsx(Params params, HttpServletResponse response) throws VantarException {
        AdminWebService.webServicesXlsx(params, response);
    }

}