package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;

import com.limegroup.gnutella.downloader.DownloaderType;

public interface DownloadMemento extends Serializable {
    
    public DownloaderType getDownloadType();
}
