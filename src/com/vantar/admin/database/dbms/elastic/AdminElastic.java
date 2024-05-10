package com.vantar.admin.database.dbms.elastic;

import com.vantar.admin.index.Admin;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


public class AdminElastic {

    @SuppressWarnings("unchecked")
    public static void getMappingElastic(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_ELASTIC_INDEX_DEF, params, response, true);
        if (!ElasticConnection.isUp()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.ELASTIC)).finish();
            return;
        }

        ui  .addHeading(2, DtoDictionary.Dbms.ELASTIC)
            .addEmptyLine(2).write();

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            ui.addHeading(3, info.getDtoClassName()).write();
            try {
                ElasticIndexes.getMapping(dto).forEach((k, v) -> {
                    if (v instanceof Map) {
                        ui  .beginBlock("pre")
                            .addText(k + ":\n");
                        ((Map) v).forEach((m, n) -> ui.addText("    " + m + ": " + n + "\n"));
                        ui.blockEnd();
                    } else {
                        ui.addBlock("pre", k + ": " + v).write();
                    }
                });
            } catch (DatabaseException e) {
                ui.addErrorMessage(e).write();
            }
        }

        ui.finish();
    }

    public static void actionsElastic(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_ELASTIC_SETTINGS, params, response, true);
        if (!ElasticConnection.isUp()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.ELASTIC)).finish();
            return;
        }

        ui  .addHeading(2, DtoDictionary.Dbms.ELASTIC)
            .addEmptyLine(2).write();

        String target = params.getString("target", "");
        String destination = params.getString("destination", "");
        boolean shrinkChecked = params.isChecked("shrink");
        boolean refreshChecked = params.isChecked("refresh");

        ui  .addMessage(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1)
            .beginFormPost()
            .addSelect(
                VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME,
                "target",
                DtoDictionary.getDtoClassNames(DtoDictionary.Dbms.ELASTIC),
                false,
                target
            )
            .addInput(
                VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION,
                "destination",
                destination,
                null,
                "ltr"
            )
            .addCheckbox("Shrink", "shrink", shrinkChecked)
            .addCheckbox("Refresh", "refresh", refreshChecked)
            .addSubmit("Clone")
            .blockEnd()
            .write();

        if (params.contains("f")) {
            if (StringUtil.isEmpty(target)) {
                ui.addErrorMessage(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1).finish();
                return;
            }
            DtoDictionary.Info info = DtoDictionary.get(target);
            if (info == null) {
                ui.finish();
                return;
            }

            if (StringUtil.isNotEmpty(destination)) {
                try {
                    ElasticIndexes.clone(info.getDtoInstance(), destination);
                    ui.addMessage(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2).write();
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e).write();
                }
            }

            if (shrinkChecked) {
                try {
                    ElasticIndexes.shrink(info.getDtoInstance());
                    ui.addMessage(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3).write();
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e).write();
                }
            }

            if (refreshChecked) {
                try {
                    ElasticIndexes.refresh(info.getDtoInstance());
                    ui.addMessage(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4).write();
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e).write();
                }
            }
        }

        ui.finish();
    }
}
