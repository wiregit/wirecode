package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloaderType;

public class LibTorrentBTDownloadMementoImpl implements LibTorrentBTDownloadMemento, Serializable {

    private static final long serialVersionUID = 1160492348504657012L;

    private final Map<String, Object> serialObjects = new HashMap<String, Object>();

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
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    public File getSaveFile() {
        return (File) serialObjects.get("saveFile");
    }

    @Override
    public String getName() {
        return (String) serialObjects.get("name");
    }

    @Override
    public long getContentLength() {
        Long l = (Long) serialObjects.get("contentLength");
        if (l == null)
            return -1;
        else
            return l;
    }

    @Override
    public URN getSha1Urn() {
        URN sha1URN = null;
        String sha1URNString = (String) serialObjects.get("sha1Urn");
        if (sha1URNString != null) {
            try {
                sha1URN = URN.createSHA1Urn(sha1URNString);
            } catch (IOException e) {
                // invalid urn
            }
        }
        return sha1URN;
    }

    @Override
    public String getTrackerURL() {
        return (String) serialObjects.get("trackerURL");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getPaths() {
        return (List<String>) serialObjects.get("paths");
    }

    @Override
    public String getFastResumePath() {
        return (String) serialObjects.get("fastResumePath");
    }

    @Override
    public String getTorrentPath() {
        return (String) serialObjects.get("torrentPath");
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
    public void setSaveFile(File saveFile) {
        serialObjects.put("saveFile", saveFile);
    }

    @Override
    public void setName(String name) {
        serialObjects.put("name", name);
    }

    @Override
    public void setContentLength(long contentLength) {
        serialObjects.put("contentLength", contentLength);
    }

    @Override
    public void setSha1Urn(URN sha1Urn) {
        String sha1URNString = sha1Urn != null ? sha1Urn.toString() : null;
        serialObjects.put("sha1Urn", sha1URNString);
    }

    @Override
    public void setPaths(List<String> paths) {
        serialObjects.put("paths", paths);
    }

    @Override
    public void setTrackerURL(String url) {
        serialObjects.put("trackerURL", url);
    }

    @Override
    public void setFastResumePath(String data) {
        serialObjects.put("fastResumePath", data);
    }

    @Override
    public void setTorrentPath(String torrentPath) {
        serialObjects.put("torrentPath", torrentPath);
    }

    @Override
    public void setDownloadType(DownloaderType downloaderType) {
        // not needed getDownloadType is overridden to always return
        // BTDownloader.
    }
}
