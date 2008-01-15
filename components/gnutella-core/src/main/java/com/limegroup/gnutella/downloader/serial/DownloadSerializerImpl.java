package com.limegroup.gnutella.downloader.serial;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadSerializerImpl implements DownloadSerializer {
    
    private static final Log LOG = LogFactory.getLog(DownloadSerializerImpl.class);
    
    private final DownloadSerializeSettings downloadSerializeSettings;
    
    @Inject
    public DownloadSerializerImpl(DownloadSerializeSettings downloadSerializeSettings) {
        this.downloadSerializeSettings = downloadSerializeSettings;
    }
    
    public List<DownloadMemento> readFromDisk() {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getSaveFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getBackupFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        return Collections.emptyList();        
    }
    
    
    public boolean writeToDisk(List<? extends DownloadMemento> mementos) {
        File backupFile = downloadSerializeSettings.getBackupFile();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(backupFile)));
            out.writeObject(mementos);
        } catch(IOException iox) {
            LOG.warn("Unable to write to backup file!", iox);
            return false;
        } finally {
            IOUtils.close(out);
        }
        
        File saveFile = downloadSerializeSettings.getSaveFile();
        if(saveFile.equals(backupFile)) {
            LOG.debug("Save file is backup, bailing!");
            return true;
        }
        
        File saveDir = saveFile.getParentFile();
        File tmpFile = null;
        try {
            tmpFile = FileUtils.createTempFile("lwc", "tmp", saveDir);
        } catch(IOException ignored) {
            LOG.warn("Error creating temp file", ignored);
        }
        
        // If we could make a temp file, rename save to that.
        if(tmpFile != null) {
            tmpFile.delete();
            if(!saveFile.renameTo(tmpFile)) {
                LOG.debug("Unable to rename save to temp, deleting instead!");
                saveFile.delete();
            }
        } else {
            saveFile.delete();
        }
        
        boolean renamed = backupFile.renameTo(downloadSerializeSettings.getSaveFile());
        if(tmpFile != null)
            tmpFile.delete();
        return renamed;
    }
}
