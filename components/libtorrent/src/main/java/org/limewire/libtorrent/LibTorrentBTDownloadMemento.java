package org.limewire.libtorrent;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

public class LibTorrentBTDownloadMemento implements DownloadMemento, Serializable {

    private static final long serialVersionUID = 1160492348504657012L;
    
    private Map<String, Object> serialObjects = new HashMap<String, Object>();
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributes() {
        return (Map<String, Object>) serialObjects.get("attributes");
    }

    @Override
    public String getDefaultFileName() {
        return (String) serialObjects.get("defaultFileName");
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return (DownloaderType) serialObjects.get("downloadType");
    }
    
    @Override
    public File getSaveFile() {
        return (File) serialObjects.get("saveFile");
    }

    public String getName() {
        return (String) serialObjects.get("name");
    }
    
    public long getContentLength() {
        Long l = (Long) serialObjects.get("contentLength");
        if(l == null)
            return -1;
        else
            return l;
    }

    public URN getSha1Urn() {
        return (URN) serialObjects.get("sha1Urn");
    }    
    
    public String getTrackerURL() {
        return (String) serialObjects.get("trackerURL");
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPaths() {
        return (List<String>) serialObjects.get("paths");
    }

    public char[] getFastResumeData() {
        return (char[]) serialObjects.get("fastResumeData");
    }
    
    @Override
    public void setAttributes(Map<String, Object> attributes) {
        serialObjects.put("attributes", attributes);
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

    public void setName(String name) {
        serialObjects.put("name", name);
    }
    
    public void setContentLength(long contentLength) {
        serialObjects.put("contentLength", contentLength);
    }
    
    public void setSha1Urn(URN sha1Urn) {
        serialObjects.put("sha1Urn", sha1Urn);
    }

    public void setPaths(List<String> paths) {
        serialObjects.put("paths", paths);
    }
    
    public void setTrackerURL(String url) {
        serialObjects.put("trackerURL", url);
    }
    
    public void setFastResumeData(char[] data) {
        serialObjects.put("fastResumeData", data);
    }
    
}
