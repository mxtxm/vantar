package com.vantar.database.common;

import org.aeonbits.owner.Config;


public interface BackupSettings {

    @Config.DefaultValue("/opt/backup/")
    @Config.Key("data.storage.backup.dir")
    String getBackupDir();

}
