package com.vantar.admin.auth;

import com.vantar.admin.index.Admin;
import com.vantar.common.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class AdminAuth {

    public static void signin(Params params, HttpServletResponse response) throws ServerException {
        WebUi ui = new WebUi(params, response);
        ui  .addHeading(1, Settings.config.getProperty("title") + " : " +  Locale.getString(VantarKey.ADMIN_SIGN_IN), "signin")
            .addEmptyLine(5);

        if (params.getString(VantarParam.USERNAME) == null) {
            ui  .beginFormPost()
                .addEmptyLine()
                .addInput(VantarKey.USERNAME, VantarParam.USERNAME)
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

    @SuppressWarnings("ConstantConditions")
    public static void signout(Params params, HttpServletResponse response) {
        Services.get(ServiceAuth.class).signout(params);
        try {
            response.sendRedirect("/admin/signin");
        } catch (IOException ignore) {

        }
    }

    public static void onlineUsers(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_ONLINE_USERS, params, response, true);
        ServiceAuth auth;
        try {
            auth = Services.getService(ServiceAuth.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        String codeToDelete = params.getString("delete");
        if (codeToDelete != null) {
            auth.removeToken(codeToDelete);
        }

        ui  .beginBox(VantarKey.ADMIN_RESET_SIGNIN_FAILS)
            .addHref(VantarKey.ADMIN_RESET_SIGNIN_FAILS, "/admin/users/signin/fails/reset")
            .blockEnd();

        ui  .beginBox(VantarKey.ADMIN_ONLINE_USERS)
            .addText("Token expire time: " + auth.tokenExpireMin + "mins");
        auth.getOnlineUsers().forEach((code, info) -> {
            long idle = -info.lastInteraction.secondsFromNow() / 60;
            long remaining = (auth.tokenExpireMin) - idle;
            ui.addKeyValue(
                info.user.getUsername() + " (" + info.user.getId() + ")\n",
                ui.getLines(
                    code,
                    info.lastInteraction.toString(),
                    "idle: " + idle + "mins",
                    "remaining: " + remaining + "mins",
                    ui.getHref(VantarKey.ADMIN_DELETE, "/admin/users/online?delete=" + code, false, false, null)
                ),
                null,
                false,
                Json.d.toJsonPretty(info)
            );
        });
        ui.blockEnd();

        ui  .beginBox(VantarKey.ADMIN_DELETE_TOKEN)
            .beginFormPost()
            .addInput(VantarKey.ADMIN_AUTH_TOKEN, "delete")
            .addSubmit(VantarKey.ADMIN_DELETE_DO)
            .blockEnd()
            .blockEnd();

        ui.beginBox(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP);
        auth.getSignupVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getUsername() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
            info.user.getToken() + "  --  " + info.lastInteraction.toString()
        ));
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP);
        auth.getOneTimeTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getUsername() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
            info.user.getToken() + "  --  " + info.lastInteraction.toString()
        ));
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_RECOVER_TOKEN_TEMP);
        auth.getVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getUsername() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
            info.user.getToken() + "  --  " + info.lastInteraction.toString()
        ));

        ui.finish();
    }

    public static boolean isRoot(WebUi ui) throws FinishException {
        try {
            ServiceAuth s = Services.getService(ServiceAuth.class);
            return Services.getService(ServiceAuth.class).isRoot(ui.params);
        } catch (ServiceException | AuthException e) {
            ui.addErrorMessage(e).finish();
            throw new FinishException();
        }
    }

    private static String getRoleDescription(CommonUser user) {
        if (user.getRole() != null) {
            return user.getRole().getName() + (user.getRole().isRoot() ? " (ROOT)" : "");
        }
        if (user.getRoles() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(1000);
        for (CommonUserRole role : user.getRoles()) {
            sb.append(role.getName()).append(role.isRoot() ? " *root*" : "").append(", ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
