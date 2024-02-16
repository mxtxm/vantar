package com.vantar.admin.web;

import com.vantar.admin.model.index.AdminPatch;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/patch",
    "/admin/patch/run",
})
public class AdminPatchController extends RouteToMethod {

    public void patch(Params params, HttpServletResponse response) throws FinishException {
        AdminPatch.index(params, response);
    }

    public void patchRun(Params params, HttpServletResponse response) throws FinishException {
        AdminPatch.run(params, response);
    }
}