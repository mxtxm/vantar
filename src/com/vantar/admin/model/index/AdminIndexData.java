package com.vantar.admin.model.index;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.FinishException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminIndexData {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_DATA), params, response, true);

        List<DtoDictionary.Info> noStores = new ArrayList<>(10);

        DtoDictionary.getStructure().forEach((groupName, groupDtos) -> {
            ui.beginBox(groupName);
            groupDtos.forEach((dtoName, info) -> {
                if (info.dbms == null) {
                    ServiceLog.log.warn("! {} missing dbms", info.getDtoClassName());
                    return;
                }

                if (info.dbms.equals(DtoDictionary.Dbms.NOSTORE)) {
                    noStores.add(info);
                    return;
                }

                ui
                    .beginFloatBoxLink(
                        "db-box",
                        info.dbms.toString(),
                        "/admin/data/list?dto=" + dtoName,
                        info.getDtoClassName(),
                        info.title
                    )
                    .addHref(
                        Locale.getString(VantarKey.ADMIN_NEW_RECORD),
                        "/admin/data/insert?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        Locale.getString(VantarKey.ADMIN_IMPORT),
                        "/admin/data/import?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        Locale.getString(VantarKey.ADMIN_EXPORT),
                        "/admin/data/export?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        Locale.getString(VantarKey.ADMIN_DELETE_ALL),
                        "/admin/data/purge?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .blockEnd();
            });

            ui.blockEnd();
        });

        ui.beginBox("NOSTORE");
        noStores.forEach(info ->
            ui.beginFloatBoxLink(
                "db-box",
                "no-db",
                "/admin/data/fields?dto=" + info.getDtoClassName(),
                info.getDtoClassName(),
                info.title
            )
            .blockEnd()
        );
        ui.blockEnd();

        // > > >
        ui.beginBox(VantarKey.ADMIN_DATABASE_TITLE);

        ui.beginFloatBox("system-box", "MONGO")
        .addHrefBlock("Sequences", "/admin/database/mongo/sequences")
        .addHrefBlock("Indexed", "/admin/database/mongo/indexes")
        .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/data/mongo/status")
        .blockEnd();

        ui.beginFloatBox("system-box", "SQL")
        .addHrefBlock("Indexes", "/admin/database/sql/indexes")
        .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/data/sql/status")
        .blockEnd();

        ui.finish();
    }
}