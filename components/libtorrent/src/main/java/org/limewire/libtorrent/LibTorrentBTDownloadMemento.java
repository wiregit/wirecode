package org.limewire.libtorrent;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

public class LibTorrentBTDownloadMemento implements DownloadMemento, Serializable {

    private static final long serialVersionUID = 1160492348504657012L;
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributes() {
        return (Map<String, Object>)serialObjects.get("attributes");
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        serialObjects.put("attributes", attributes);
    }
    
    @Override
    public String getDefaultFileName() {
        return (String)serialObjects.get("defaultFileName");
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return (DownloaderType)serialObjects.get("downloadType");
    }
    
    @Override
    public File getSaveFile() {
        return (File)serialObjects.get("saveFile");
    }

    @Override
    public void setDefaultFileName(String defaultFileName) {
        serialObjects.put("defaultFileName", defaultFileName);
    }
    
    @Override
    public void setDownloadType(DownloaderType downloaderType) {
        serialObjects.put("downloadType", downloaderType);
    }
    
    @Override
    public void setSaveFile(File saveFile) {
        serialObjects.put("saveFile", saveFile);
    }

}
