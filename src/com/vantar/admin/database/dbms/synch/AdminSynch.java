package com.vantar.admin.database.dbms.synch;

import com.vantar.admin.index.Admin;
import com.vantar.common.Settings;
import com.vantar.database.common.Db;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminSynch {

    public static void synch(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, params, response, true);
        if (!(Db.Dbms.MONGO.equals(dbms) ? Services.isUp(Db.Dbms.MONGO)
            : (Db.Dbms.SQL.equals(dbms) ? Services.isUp(Db.Dbms.SQL) : Services.isUp(Db.Dbms.SQL)))) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addEmptyLine(2).write();

        if (!params.contains("f") || !params.isChecked("confirm")) {
            ui  .beginFormPost()
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addSubmit(VantarKey.ADMIN_DATABASE_SYNCH)
                .finish();
            return;
        }

        if (Db.Dbms.SQL.equals(dbms)) {
            synchSql(ui);
        } else if (Db.Dbms.ELASTIC.equals(dbms)) {
            synchElastic(ui);
        }

        ui.finish();
    }

    public static void synchSql(WebUi ui) {
        if (!Services.isUp(Db.Dbms.SQL)) {
            return;
        }

        SqlArtaSynch synch = new SqlArtaSynch(Settings.sql());
        try {
            synch.cleanup();
            synch.createFiles();
            if (ui != null) {
                ui.addBlock("pre", synch.build()).write();
            }
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
        }
    }

    public static void synchElastic(WebUi ui) {
        if (!Services.isUp(Db.Dbms.ELASTIC)) {
            return;
        }

        try {
            ElasticIndexes.create();
            if (ui != null) {
                ui.addMessage(VantarKey.ADMIN_FINISHED);
            }
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
        }
    }
}
