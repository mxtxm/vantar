package com.vantar.admin.web;

import com.vantar.admin.model.AdminAdvanced;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/advanced",
})
public class AdminAdvancedController extends RouteToMethod {

    public static void advanced(Params params, HttpServletResponse response) {
        AdminAdvanced.index(params, response);
    }
}