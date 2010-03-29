package org.limewire.bittorrent;

/**
 * Represents data for a torrent tracker.
 */
public interface TorrentTracker {

    /**
     * Returns the url for this tracker.
     */
    public String getURL();

    /**
     * Returns the tier for this tracker.
     */
    public int getTier();
}
