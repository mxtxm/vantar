package com.vantar.admin.model.database.dbms.synch;

import com.vantar.admin.model.index.Admin;
import com.vantar.common.Settings;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminSynch {

    public static void synch(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, params, response, true);
        if (!(DtoDictionary.Dbms.MONGO.equals(dbms) ? MongoConnection.isUp()
            : (DtoDictionary.Dbms.SQL.equals(dbms) ? SqlConnection.isUp() : ElasticConnection.isUp()))) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addEmptyLine(2).write();

        if (!params.isChecked("f") || !params.isChecked("confirm")) {
            ui  .beginFormPost()
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addSubmit(VantarKey.ADMIN_DATABASE_SYNCH)
                .finish();
            return;
        }

        if (DtoDictionary.Dbms.SQL.equals(dbms)) {
            synchSql(ui);
        } else if (DtoDictionary.Dbms.ELASTIC.equals(dbms)) {
            synchElastic(ui);
        }

        ui.finish();
    }

    public static void synchSql(WebUi ui) {
        if (!SqlConnection.isUp()) {
            return;
        }

        SqlArtaSynch synch = new SqlArtaSynch(Settings.sql());
        try {
            synch.cleanup();
            synch.createFiles();
            ui.addBlock("pre", synch.build()).write();
        } catch (DatabaseException e) {
            ui.addErrorMessage(e).write();
        }
    }

    public static void synchElastic(WebUi ui) {
        if (!ElasticConnection.isUp()) {
            return;
        }

        try {
            ElasticIndexes.create();
            ui.addMessage(VantarKey.ADMIN_FINISHED);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }
    }
}
