package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.DownloaderType;

public class BTDownloadMemento implements DownloadMemento {

    private final Map<String, Serializable> propertiesMap;

    public BTDownloadMemento(Map<String, Serializable> propertiesMap) {
        this.propertiesMap = propertiesMap;
    }

    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }
    
    public Map<String, Serializable> getPropertiesMap() {
        return propertiesMap;
    }

    public List<Range> getRanges() {
        return Collections.emptyList();
    }

    public File getIncompleteFile() {
        return null;
    }

}
