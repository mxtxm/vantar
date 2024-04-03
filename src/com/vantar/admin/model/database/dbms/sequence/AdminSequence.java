package com.vantar.admin.model.database.dbms.sequence;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.mongo.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminSequence {

    public static void listMongoSequences(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_SEQUENCE, params, response, true);
        if (!MongoConnection.isUp()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.MONGO)).finish();
            return;
        }

        ui  .addHeading(2, DtoDictionary.Dbms.MONGO)
            .addHrefBlock(VantarKey.ADMIN_DATABASE_CREATE_SEQUENCE, "/admin/data/list?dto=MongoSequence")
            .addEmptyLine(2).write();

        try {
            Mongo.Sequence.getAll().forEach(ui::addKeyValue);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }
}
