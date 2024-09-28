package com.vantar.admin.database.data.index;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data",
})
public class Controller extends RouteToMethod {

    public void data(Params params, HttpServletResponse response) throws FinishException {
        AdminDataManifest.index(params, response);
    }
}