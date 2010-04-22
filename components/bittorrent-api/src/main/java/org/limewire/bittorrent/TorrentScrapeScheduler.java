package org.limewire.bittorrent;

public interface TorrentScrapeScheduler {

    /**
     * Initiate a scrape request asyncronously.  Results will be available
     *  by {link #getScrapeDataIfAvailable()}
     */
    void queueScrapeIfNew(Torrent torrent);

    /**
     * Get any scrape results if available.
     * 
     * @return null if no scrape data available.
     */
    TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent);

}
