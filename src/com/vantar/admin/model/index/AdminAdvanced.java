package com.vantar.admin.model.index;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAdvanced {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_ADVANCED, params, response, true);

        // > > >
        ui.beginBox(VantarKey.ADMIN_BACKUP);
        ui  .beginFloatBox("system-box", "MONGO")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_CREATE, "/admin/data/backup/mongo")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_RESTORE, "/admin/data/restore/mongo")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_FILES, "/admin/data/backup/files/mongo")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_UPLOAD, "/admin/data/backup/upload")
            .blockEnd();
        ui  .beginFloatBox("system-box", "SQL")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_CREATE, "/admin/data/backup/sql")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_RESTORE, "/admin/data/restore/sql")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_FILES, "/admin/data/backup/files/sql")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_UPLOAD, "/admin/data/backup/upload")
            .blockEnd();
        ui  .beginFloatBox("system-box", "ELASTIC")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_CREATE, "/admin/data/backup/elastic")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_RESTORE, "/admin/data/restore/elastic")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_FILES, "/admin/data/backup/files/elastic")
            .addHrefBlock(VantarKey.ADMIN_BACKUP_UPLOAD, "/admin/data/backup/upload")
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
        ui.beginBox(VantarKey.ADMIN_DATABASE);
        ui  .beginFloatBox("system-box", "MONGO")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/data/mongo/status")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_CREATE_INDEX, "/admin/database/mongo/index/create")
            .addHrefBlock(VantarKey.ADMIN_IMPORT, "/admin/database/mongo/import")
            .addHrefBlock(VantarKey.ADMIN_DELETE_ALL, "/admin/database/mongo/purge")
            .blockEnd();
        ui  .beginFloatBox("system-box", "SQL")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/data/sql/status")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_CREATE_INDEX, "/admin/database/sql/index/create")
            .addHrefBlock(VantarKey.ADMIN_IMPORT, "/admin/database/sql/import")
            .addHrefBlock(VantarKey.ADMIN_DELETE_ALL, "/admin/database/sql/purge")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_SYNCH, "/admin/database/sql/synch")
            .blockEnd();
        ui  .beginFloatBox("system-box", "ELASTIC")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/data/elastic/status")
            .addHrefBlock(VantarKey.ADMIN_IMPORT, "/admin/database/elastic/import")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_INDEX_DEF, "/admin/database/elastic/mapping/get")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_INDEX_SETTINGS, "/admin/database/elastic/actions")
            .addHrefBlock(VantarKey.ADMIN_DELETE_ALL, "/admin/database/elastic/purge")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_SYNCH, "/admin/database/elastic/synch")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(VantarKey.ADMIN_QUEUE);
        ui  .beginFloatBox("system-box", "RabbitMQ")
            .addHrefBlock(VantarKey.ADMIN_DATABASE_STATUS, "/admin/queue/index")
            .addHrefBlock(VantarKey.ADMIN_DELETE_OPTIONAL, "/admin/queue/purge/selective")
            .addHrefBlock(VantarKey.ADMIN_DELETE_ALL, "/admin/queue/purge")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(VantarKey.ADMIN_SETTINGS);
        ui  .beginFloatBox("system-box", VantarKey.ADMIN_SETTINGS)
            .addHrefBlock(VantarKey.ADMIN_SETTINGS_RELOAD, "/admin/system/settings/reload")
            .addHrefBlock(VantarKey.ADMIN_SETTINGS_EDIT_CONFIG, "/admin/system/settings/edit/config")
            .addHrefBlock(VantarKey.ADMIN_SETTINGS_EDIT_TUNE, "/admin/system/settings/edit/tune")
            .blockEnd();
        ui.blockEnd();

        // > > >
        ui.beginBox(VantarKey.ADMIN_SYSTEM_AND_SERVICES);
        ui  .beginFloatBox("system-box", VantarKey.ADMIN_STARTUP)
            .addHrefBlock(VantarKey.ADMIN_SERVICE_STOP, "/admin/services/stop")
            .addHrefBlock(VantarKey.ADMIN_SERVICE_START, "/admin/services/start")
            .addHrefBlock(VantarKey.ADMIN_FACTORY_RESET, "/admin/factory/reset")
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
