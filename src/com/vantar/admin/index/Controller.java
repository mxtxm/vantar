package com.vantar.admin.index;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin",
    "/admin/",
    "/admin/index",
})
public class Controller extends RouteToMethod {

    public void index(Params params, HttpServletResponse response) throws FinishException {
        Admin.index(params, response);
    }
}