package com.vantar.admin.database.data.index;

import com.vantar.admin.advanced.AdminAdvanced;
import com.vantar.admin.database.dbms.status.AdminStatus;
import com.vantar.admin.index.Admin;
import com.vantar.database.common.Db;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataManifest {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_DATA, params, response, true);

        List<DtoDictionary.Info> noStores = new ArrayList<>(10);
        Map<String, Map<String, Long>> countDbmses = AdminStatus.getDatabaseRecordCount();

        DtoDictionary.getManifest().forEach((groupName, groupDtos) -> {
            ui.beginBox(groupName);
            groupDtos.forEach((dtoName, info) -> {
                if (info.dbms == null) {
                    ServiceLog.log.warn("! {} missing dbms", info.getDtoClassName());
                    return;
                }
                if (info.dbms.equals(Db.Dbms.NOSTORE)) {
                    noStores.add(info);
                    return;
                }
                Map<String, Long> countDbms = countDbmses.get(info.dbms.name());
                Long c = countDbms == null ? null : countDbms.get(dtoName);

                ui  .beginFloatBoxLink(
                        "db-box",
                        info.dbms.toString(),
                        "/admin/data/list?dto=" + dtoName,
                        info.getDtoClassName(),
                        info.title + (c == null ? "" : "|" + c)
                    )
                    .addHref(
                        VantarKey.ADMIN_INSERT,
                        "/admin/data/insert?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        VantarKey.ADMIN_IMPORT,
                        "/admin/data/import?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        VantarKey.ADMIN_EXPORT,
                        "/admin/data/export?dto=" + dtoName,
                        true,
                        false,
                        "dto-action-link"
                    )
                    .addHref(
                        VantarKey.ADMIN_DATA_PURGE,
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
        AdminAdvanced.addDatabaseBoxes(ui);

        ui.finish();
    }
}