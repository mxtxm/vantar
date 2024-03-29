package com.vantar.admin.model;

import com.vantar.admin.model.index.Admin;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAuth {

    public static boolean isRoot(WebUi ui) throws FinishException {
        try {
            return Services.getService(ServiceAuth.class).isRoot(ui.params);
        } catch (ServiceException | AuthException e) {
            ui.addErrorMessage(e).finish();
            throw new FinishException();
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

        ui.beginBox(VantarKey.ADMIN_ONLINE_USERS);
        ui.addText("Token expire time: " + auth.tokenExpireMin + "mins");
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
