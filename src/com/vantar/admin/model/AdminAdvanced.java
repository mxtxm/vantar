package com.vantar.admin.model;

import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAdvanced {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), params, response, true);

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_BACKUP));
        ui  .beginFloatBox("system-box", "SQL")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/sql")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/sql")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/sql")
            .containerEnd();

        ui  .beginFloatBox("system-box", "MONGO")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/mongo")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/mongo")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/mongo")
            .containerEnd();

        ui  .beginFloatBox("system-box", "ELASTIC")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/elastic")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/elastic")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/elastic")
            .containerEnd();

        ui.containerEnd();


        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE));
        ui  .beginFloatBox("system-box", "SQL")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/sql/status")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_CREATE_INDEX), "/admin/database/sql/index/create")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_IMPORT), "/admin/database/sql/import")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/sql/purge")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH), "/admin/database/sql/synch")
            .containerEnd();

        ui  .beginFloatBox("system-box", "MONGO")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/mongo/status")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_CREATE_INDEX), "/admin/database/mongo/index/create")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_IMPORT), "/admin/database/mongo/import")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/mongo/purge")
            .containerEnd();

        ui  .beginFloatBox("system-box", "ELASTIC")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/elastic/status")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_IMPORT), "/admin/database/elastic/import")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_DEF), "/admin/database/elastic/mapping/get")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS), "/admin/database/elastic/actions")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/elastic/purge")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH), "/admin/database/elastic/synch")
            .containerEnd();

        ui.containerEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_QUEUE));
        ui  .beginFloatBox("system-box", "RabbitMQ")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/queue/status")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_OPTIONAL), "/admin/queue/purge/selective")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/queue/purge")
            .containerEnd();

        ui.containerEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_SYSTEM_AND_SERVICES));
        ui  .beginFloatBox("system-box", Locale.getString(VantarKey.ADMIN_STARTUP))
            .addBlockLink(Locale.getString(VantarKey.ADMIN_SERVICE_STOP), "/admin/system/services/stop")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_SERVICE_START), "/admin/system/services/start")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_FACTORY_RESET), "/admin/system/factory/reset")
            .addBlockLink("GC", "/admin/system/gc")
            .containerEnd();

        ui.containerEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_SETTINGS));
        ui  .beginFloatBox("system-box", Locale.getString(VantarKey.ADMIN_SETTINGS))
            .addBlockLink(Locale.getString(VantarKey.ADMIN_SETTINGS_RELOAD), "/admin/system/settings/reload")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG), "/admin/system/settings/edit/config")
            .addBlockLink(Locale.getString(VantarKey.ADMIN_SETTINGS_EDIT_TUNE), "/admin/system/settings/edit/tune")
            .containerEnd();

        ui.containerEnd();

        ui.containerEnd().finish();
    }
}
