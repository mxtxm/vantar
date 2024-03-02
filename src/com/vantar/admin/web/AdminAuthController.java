package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.common.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet({
    "/admin/signin",
    "/admin/signout",
    "/admin/users/online",
    "/admin/users/signin/fails/reset",
})
public class AdminAuthController extends RouteToMethod {

    public void signin(Params params, HttpServletResponse response) throws ServerException {
        WebUi ui = new WebUi(params, response);
        ui  .addHeading(1, Settings.config.getProperty("title") + " : " +  VantarKey.ADMIN_SIGN_IN, "signin")
            .addEmptyLine(5);

        if (params.getString("username") == null) {
            ui  .beginFormPost()
                .addEmptyLine()
                .addInput(VantarKey.USERNAME, VantarParam.USER_NAME)
                .addPassword(VantarKey.PASSWORD, VantarParam.PASSWORD)
                .addSubmit(VantarKey.SIGN_IN)
                .blockEnd();

        } else {
            ServiceAuth auth = Services.get(ServiceAuth.class);
            if (auth == null) {
                ui.addErrorMessage("AUTH SERVICE IS DISABLED").finish();
                return;
            }
            try {
                CommonUser user = auth.signin(params);
                ui.setAuthToken(user.getToken());
                ServiceLog.log.info(" > admin dashboard signed in: {}'", user.getUsername());
                ui.redirect("/admin/index");
                return;
            } catch (AuthException e) {
                ServiceLog.log.info(" ! admin dashboard rejected: {}", e.getMessage());
                ui.addErrorMessage(e);
            }
        }

        ui.finish();
    }

    public void signout(Params params, HttpServletResponse response) {
        try {
            Services.getService(ServiceAuth.class).signout(params);
        } catch (ServiceException ignore) {

        }
        try {
            response.sendRedirect("/admin/signin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void usersOnline(Params params, HttpServletResponse response) throws FinishException {
        AdminAuth.onlineUsers(params, response);
    }

    public void usersSigninFailsReset(Params params, HttpServletResponse response) throws ServiceException {
        Services.getService(ServiceAuth.class).resetSigninFails();
    }
}