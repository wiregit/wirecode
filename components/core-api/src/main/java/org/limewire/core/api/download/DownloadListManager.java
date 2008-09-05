package org.limewire.core.api.download;

import java.net.URI;

import ca.odell.glazedlists.EventList;

public interface DownloadListManager extends SearchResultDownloader {

    /**
     * Returns all items currently being downloaded.
     */
    public EventList<DownloadItem> getDownloads();

    /**
     * Downloads the file from the given uri saving to filename. This method
     * should handle allowing the user to select a directory to save the file to
     * and whether or not the file should be overwritten.
     * 
     * @param uri location to download the file from
     * @param fileName name to save the file as
     */
    public void addDownload(URI uri, String fileName);

}
