package com.vantar.admin.bugger;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/bugger/index",
})
public class Controller extends RouteToMethod {

    public void buggerIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminBugger.index(params, response);
    }
}