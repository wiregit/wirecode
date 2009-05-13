package com.limegroup.gnutella.downloader.serial;

import java.util.List;

import com.limegroup.gnutella.URN;

public interface LibTorrentBTDownloadMemento extends DownloadMemento {

    public String getName();

    public long getContentLength();

    public URN getSha1Urn();

    public String getTrackerURL();

    public List<String> getPaths();

    public String getFastResumePath();

    public void setName(String name);

    public void setContentLength(long contentLength);

    public void setSha1Urn(URN sha1Urn);

    public void setPaths(List<String> paths);

    public void setTrackerURL(String url);

    public void setFastResumePath(String data);

}