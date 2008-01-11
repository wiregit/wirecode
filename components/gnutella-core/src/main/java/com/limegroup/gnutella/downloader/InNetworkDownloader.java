package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.version.DownloadInformation;

public interface InNetworkDownloader extends ManagedDownloader {

    public void initDownloadInformation(DownloadInformation downloadInformation, long startTime);

    /**
     * @return how many times was this download attempted
     */
    public int getNumAttempts();

    public long getStartTime();

}