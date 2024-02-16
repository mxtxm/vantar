package com.vantar.admin.model.index;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAdvanced {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), params, response, true);

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_BACKUP));
        ui  .beginFloatBox("system-box", "MONGO")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/mongo")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/mongo")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/mongo")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_UPLOAD), "/admin/data/backup/upload")
            .blockEnd();
        ui  .beginFloatBox("system-box", "SQL")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/sql")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/sql")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/sql")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_UPLOAD), "/admin/data/backup/upload")
            .blockEnd();
        ui  .beginFloatBox("system-box", "ELASTIC")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), "/admin/data/backup/elastic")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), "/admin/data/restore/elastic")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), "/admin/data/backup/files/elastic")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_BACKUP_UPLOAD), "/admin/data/backup/upload")
            .blockEnd();
        try {
            ServiceBackup serviceBackup = Services.getService(ServiceBackup.class);
            ui  .beginFloatBox("system-box", "Service")
                .addMessage("Last run: " + serviceBackup.getLastRun())
                .addMessage("Next run: " + serviceBackup.getNextRun())
                .addMessage("Interval: " + serviceBackup.intervalHour + "hours")
                .addMessage("Path: " + serviceBackup.path)
                .addHrefBlock("Logs", "/admin/data/backup/logs")
                .blockEnd();
        } catch (ServiceException ignore) {

        }
        ui.blockEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE));
        ui  .beginFloatBox("system-box", "MONGO")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/mongo/status")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_CREATE_INDEX), "/admin/database/mongo/index/create")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_IMPORT), "/admin/database/mongo/import")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/mongo/purge")
            .blockEnd();
        ui  .beginFloatBox("system-box", "SQL")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/sql/status")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_CREATE_INDEX), "/admin/database/sql/index/create")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_IMPORT), "/admin/database/sql/import")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/sql/purge")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH), "/admin/database/sql/synch")
            .blockEnd();
        ui  .beginFloatBox("system-box", "ELASTIC")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/data/elastic/status")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_IMPORT), "/admin/database/elastic/import")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_DEF), "/admin/database/elastic/mapping/get")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS), "/admin/database/elastic/actions")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/database/elastic/purge")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH), "/admin/database/elastic/synch")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_QUEUE));
        ui  .beginFloatBox("system-box", "RabbitMQ")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS), "/admin/queue/status")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_OPTIONAL), "/admin/queue/purge/selective")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/queue/purge")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_SETTINGS));
        ui  .beginFloatBox("system-box", Locale.getString(VantarKey.ADMIN_SETTINGS))
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_SETTINGS_RELOAD), "/admin/system/settings/reload")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG), "/admin/system/settings/edit/config")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_SETTINGS_EDIT_TUNE), "/admin/system/settings/edit/tune")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_SYSTEM_AND_SERVICES));
        ui  .beginFloatBox("system-box", Locale.getString(VantarKey.ADMIN_STARTUP))
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_SERVICE_STOP), "/admin/services/stop")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_SERVICE_START), "/admin/services/start")
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_FACTORY_RESET), "/admin/factory/reset")
            .addHrefBlock("GC", "/admin/system/gc")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox("Deploy");
        ui  .beginFloatBox("system-box", "Deploy")
            .addHrefBlock("Upload", "/admin/deploy/upload")
            .addHrefBlock("Run", "/admin/deploy/run")
            .addHrefBlock("Shell", "/admin/deploy/shell")
            .blockEnd();
        ui.blockEnd();

        ui.blockEnd().finish();
    }
}
