package com.vantar.admin.test;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/test/index",
    "/admin/test/web",

    "/admin/test/web/unit/create",
    "/admin/test/web/unit/list",
    "/admin/test/web/unit/run",
})
public class Controller extends RouteToMethod {

    public void testIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminTestIndex.index(params, response);
    }

    public void testWeb(Params params, HttpServletResponse response) throws FinishException {
        WebTest.show(params, response);
    }

    public void testWebUnitCreate(Params params, HttpServletResponse response) throws FinishException {
        WebUnitTestCreate.create(params, response);
    }

    public void testWebUnitList(Params params, HttpServletResponse response) throws FinishException {
        WebUnitTestList.list(params, response);
    }

    public void testWebUnitRun(Params params, HttpServletResponse response) throws FinishException {
        WebUnitTestRun.run(params, response);
    }
}