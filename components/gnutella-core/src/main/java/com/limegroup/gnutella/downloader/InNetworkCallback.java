package com.limegroup.gnutella.downloader;

import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Downloader.DownloadStatus;

/**
 * Once an in-network download finishes, the UpdateHandler is notified.
 */
@Singleton
public class InNetworkCallback implements DownloadCallback {
    
    public void addDownload(Downloader d) {
    }

    public void removeDownload(Downloader d) {
        InNetworkDownloader downloader = (InNetworkDownloader) d;
        ProviderHacks.getUpdateHandler().inNetworkDownloadFinished(
                downloader.getSHA1Urn(),
                downloader.getState() == DownloadStatus.COMPLETE);
    }

    public void downloadsComplete() {
    }

    public void showDownloads() {
    }

    // always discard corruption.
    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(true);
    }

    public String getHostValue(String key) {
        return null;
    }
}