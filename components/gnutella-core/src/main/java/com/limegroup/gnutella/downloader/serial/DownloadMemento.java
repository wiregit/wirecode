package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.DownloaderType;

public interface DownloadMemento extends Serializable {
    
    public DownloaderType getDownloadType();
    
    public List<Range> getRanges();
    
    public File getIncompleteFile();
}
