package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/document/index",
    "/admin/document/show",
})
public class AdminDocumentController extends RouteToMethod {

    public void documentIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDocument.index(params, response);
    }

    public void documentShow(Params params, HttpServletResponse response) {
        AdminDocument.show(params, response);
    }
}