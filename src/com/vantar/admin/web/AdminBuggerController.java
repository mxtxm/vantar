package com.vantar.admin.web;

import com.vantar.admin.Dto.Bugger;
import com.vantar.admin.model.index.Admin;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/bugger/index",
})
public class AdminBuggerController extends RouteToMethod {

    public void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Bug report", params, response, false);
        ui.addDtoAddForm(new Bugger());
        ui.finish();
    }
}