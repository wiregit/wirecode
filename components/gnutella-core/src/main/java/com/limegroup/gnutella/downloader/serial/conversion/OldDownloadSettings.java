package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import com.google.inject.Singleton;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.settings.SharingSettings;

@Singleton
public class OldDownloadSettings implements DownloadSerializeSettings {
    
    public File getBackupFile() {
        return SharingSettings.OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
    }

    public File getSaveFile() {
        return SharingSettings.OLD_DOWNLOAD_SNAPSHOT_FILE.getValue();
    }

}
