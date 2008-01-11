package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;

public class DownloadUpgradeTask {
    
    private static final Log LOG = LogFactory.getLog(DownloadUpgradeTask.class);
    
    private final OldDownloadConverter oldDownloadConverter;
    private final DownloadSerializeSettings oldDownloadSettings;
    private final DownloadSerializeSettings newSettings;
    
    @Inject
    public DownloadUpgradeTask(OldDownloadConverter oldDownloadConverter,
                               @Named("oldDownloadSettings") DownloadSerializeSettings oldDownloadSettings,
                               DownloadSerializeSettings newSettings) {
        this.oldDownloadConverter = oldDownloadConverter;
        this.oldDownloadSettings = oldDownloadSettings;
        this.newSettings = newSettings;
    }
    
    public void upgrade() {
        File newSaveBackup = newSettings.getBackupFile();
        File newSave = newSettings.getSaveFile();
        if(!newSaveBackup.exists() && !newSave.exists()) {
            try {
                List<DownloadMemento> mementos = readAndConvertOldFormat();
                FileUtils.writeObject(newSaveBackup, mementos);
                if(!newSaveBackup.renameTo(newSave)) {
                    LOG.warn("Unable to rename backup to valid!");
                } else {
                    // Success! Now delete the old files.
                    oldDownloadSettings.getSaveFile().delete();
                    oldDownloadSettings.getBackupFile().delete();
                }   
            } catch(IOException iox) {
                LOG.warn("Unable to read old file or write to backup!", iox);
            }
        }
    }


    /** Converts the old serialized format to new mementos. */
    private List<DownloadMemento> readAndConvertOldFormat() throws IOException {
        try {
            return oldDownloadConverter.readAndConvertOldDownloads(oldDownloadSettings.getSaveFile());
        } catch(Throwable ignored) {
            LOG.warn("Error trying to convert old normal file.", ignored);
        }
        
        try {
            return oldDownloadConverter.readAndConvertOldDownloads(oldDownloadSettings.getBackupFile());
        } catch(Throwable ignored) {
            LOG.warn("Error trying to convert old normal file.", ignored);
        }
        
        throw new IOException("Unable to read old files!");
    }
    
}
