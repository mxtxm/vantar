package com.vantar.admin.web;

import com.vantar.admin.model.AdminAdvanced;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/advanced",
})
public class AdminAdvancedController extends RouteToMethod {

    public static void advanced(Params params, HttpServletResponse response) throws FinishException {
        AdminAdvanced.index(params, response);
    }
}