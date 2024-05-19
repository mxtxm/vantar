package com.vantar.admin.bugger;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.database.common.Db;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminBugger {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Bug report", params, response, false);

        Bugger bugger = new Bugger();
        if (params.contains("f")) {
            try {
                bugger.environment = params.getStringRequired("e");
                bugger.url = params.getStringRequired("u");
                bugger.data = params.getString("d");
                bugger.description = params.getStringRequired("m");
                Db.modelMongo.insert(new ModelCommon.Settings(bugger).mutex(false).logEvent(false));
                ui.addMessage(VantarKey.SUCCESS_INSERT);
            } catch (VantarException e) {
                ui.addErrorMessage(e);
            }
        } else {
            bugger.environment = "Test server";
        }

        ui  .addMessage("Report a webservice bug or problem:")
            .beginFormPost()
            .addSelect("Environment", "e", new String[] {"Test server", "Production Server"}, bugger.environment)
            .addInput("URL", "u", bugger.url)
            .addTextArea("Input data", "d", bugger.data)
            .addTextArea("Description", "m", bugger.description, "large")
            .addSubmit();

        ui.finish();
    }
}
