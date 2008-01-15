package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

public interface CoreDownloader extends Downloader {

    public static final String ATTRIBUTES = "attributes";
    public static final String DEFAULT_FILENAME = "defaultFileName";
    public static final String FILE_SIZE = "fileSize";
    
    /**
     * The key under which the saveFile File is stored in the attribute map
     * used in serializing and deserializing ManagedDownloaders. 
     */
    public static final String SAVE_FILE = "saveFile";

    /**
     * Sets the inactive priority of this download.
     */
    public void setInactivePriority(int priority);
    
    public GUID getQueryGUID();

    /**
     * Starts the download.
     */
    public void startDownload();

    /**
     * @return whether the download is still alive and cannot be
     * restarted.
     */
    public boolean isAlive();

    /**
     * @return whether it makes sense to restart this download.
     */
    public boolean shouldBeRestarted();

    /**
     * @return whether the download should be removed from 
     * the waiting list.
     */
    public boolean shouldBeRemoved();

    /**
     * Handles state changes and other operations while
     * inactive.
     */
    public void handleInactivity();

    public boolean isQueuable();

    /**
     * Cleans up any resources before this downloader 
     * is completely disposed.
     */
    public void finish();

    public boolean conflicts(URN urn, long fileSize, File... files);

    public boolean conflictsWithIncompleteFile(File incomplete);

    public boolean conflictsSaveFile(File saveFile);

    public void initialize();

    /**
     * Returns the type of download
     */
    public DownloaderType getDownloadType();
    
    /** Adds all new properties & attributes, not overwriting old ones. */
    public void addNewProperties(Map<String, Serializable> newProperties);
    
    /** Constructs a memento that will be used for serialization. */
    DownloadMemento toMemento();

}