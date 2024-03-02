package com.vantar.admin.modelw.bugger;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.ModelMongo;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminBugger {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Bug report", params, response, false);

        Bugger bugger = new Bugger();
        if (params.isChecked("f")) {
            try {
                bugger.environment = params.getStringRequired("e");
                bugger.url = params.getStringRequired("u");
                bugger.data = params.getString("d");
                bugger.description = params.getStringRequired("m");

                ModelMongo.insert(bugger);
                ui.addMessage(VantarKey.INSERT_SUCCESS);
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
