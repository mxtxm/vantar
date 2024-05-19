package com.vantar.admin.database.dbms.sequence;

import com.vantar.admin.index.Admin;
import com.vantar.database.common.Db;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminSequence {

    public static void listMongoSequences(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_SEQUENCE, params, response, true);
        if (!Services.isUp(Db.Dbms.MONGO)) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Db.Dbms.MONGO)).finish();
            return;
        }

        ui  .addHeading(2, Db.Dbms.MONGO)
            .addHrefBlock(VantarKey.ADMIN_DATABASE_CREATE_SEQUENCE, "/admin/data/list?dto=MongoSequence")
            .addEmptyLine(2).write();

        try {
            Db.mongo.autoIncrementGetAll().forEach(ui::addKeyValue);
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }
}
