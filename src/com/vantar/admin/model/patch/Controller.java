package com.vantar.admin.model.patch;

import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/patch",
    "/admin/patch/run",
})
public class Controller extends RouteToMethod {

    public void patch(Params params, HttpServletResponse response) throws FinishException {
        AdminPatch.index(params, response);
    }

    public void patchRun(Params params, HttpServletResponse response) throws FinishException, VantarException {
        AdminPatch.run(params, response);
    }
}