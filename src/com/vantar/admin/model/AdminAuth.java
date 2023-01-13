package com.vantar.admin.model;

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
            return Services.get(ServiceAuth.class).isRoot(ui.params);
        } catch (ServiceException | AuthException e) {
            ui.addErrorMessage(e).finish();
            throw new FinishException();
        }
    }

    public static void onlineUsers(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ONLINE_USERS), params, response, true);

        ServiceAuth auth;
        try {
            auth = Services.get(ServiceAuth.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        String tk = params.getString("tk");
        if (tk != null) {
            auth.removeToken(tk);
        }

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_RESET_SIGNIN_FAILS))
            .addLink(Locale.getString(VantarKey.ADMIN_RESET_SIGNIN_FAILS), "/admin/users/signin/fails/reset")
            .containerEnd()
            .beginBox(Locale.getString(VantarKey.ADMIN_DELETE_TOKEN))
            .addText(Locale.getString(VantarKey.ADMIN_DELETE_TOKEN_DESCRIPTION))
            .beginFormPost()
            .addInput(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN), "tk")
            .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE_DO))
            .containerEnd()
            .containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_ONLINE_USERS));
        ui.addText("Token expire time: " + auth.tokenExpireMin + "mins");

        auth.getOnlineUsers().forEach((code, info) -> {
            long idle = -info.lastInteraction.secondsFromNow() / 60;
            long remaining = (auth.tokenExpireMin) - idle;
            ui.addKeyValueExtendable(
                info.user.getUsername() + " (" + info.user.getId() + ")\n",
                code
                    + "  --  " +  info.lastInteraction.toString() + " --> "
                    + " (idle=" + idle + "mins, remaining=" + remaining + "mins)",
                "<label>" + Json.d.toJsonPretty(info) + "</label>"
            );
        });

        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP));
        auth.getSignupVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getUsername() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
            info.user.getToken() + "  --  " + info.lastInteraction.toString()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP));
        auth.getOneTimeTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getUsername() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
            info.user.getToken() + "  --  " + info.lastInteraction.toString()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECOVER_TOKEN_TEMP));
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
        StringBuilder sb = new StringBuilder();
        for (CommonUserRole role : user.getRoles()) {
            sb.append(role.getName()).append(role.isRoot() ? " *root*" : "").append(", ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
