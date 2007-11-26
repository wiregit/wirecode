package com.limegroup.gnutella.downloader;

/**
 * An interface for the {@link LWSIntegrationServices} to talk to. Currently
 * {@link DownloadManager} immplements this.
 */
public interface LWSIntegrationServicesDelegate {

    Iterable<AbstractDownloader> getAllDownloaders();

}
