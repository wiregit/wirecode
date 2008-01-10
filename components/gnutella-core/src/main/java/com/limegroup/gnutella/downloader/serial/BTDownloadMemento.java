package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;
import java.util.Map;

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

}
