package com.limegroup.gnutella.downloader;

/**
 * An interface for the {@link LWSIntegrationServices} to talk to. Currently
 * {@link DownloadManager} implements this.
 */
public interface LWSIntegrationServicesDelegate {

    Iterable<CoreDownloader> getAllDownloaders();

}
