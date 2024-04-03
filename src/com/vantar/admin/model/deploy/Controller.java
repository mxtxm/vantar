package com.vantar.admin.model.deploy;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/deploy/upload",
    "/admin/deploy/run",
    "/admin/deploy/shell",
})
@MultipartConfig(
    location="/tmp",
    fileSizeThreshold=200*1024*1024,
    maxFileSize=200*1024*1024,
    maxRequestSize=200*1024*1024*5
)
/**
 * EXPERIMENTAL
 */
public class Controller extends RouteToMethod {

    public void deployUpload(Params params, HttpServletResponse response) throws FinishException {
        AdminDeploy.upload(params, response);
    }

    public void deployRun(Params params, HttpServletResponse response) throws FinishException {
        AdminDeploy.deploy(params, response);
    }

    public void deployShell(Params params, HttpServletResponse response) throws FinishException {
        AdminDeploy.shell(params, response);
    }
}