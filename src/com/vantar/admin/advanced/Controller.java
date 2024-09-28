package com.vantar.admin.advanced;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/advanced",
})
public class Controller extends RouteToMethod {

    public static void advanced(Params params, HttpServletResponse response) throws FinishException {
        AdminAdvanced.index(params, response);
    }
}