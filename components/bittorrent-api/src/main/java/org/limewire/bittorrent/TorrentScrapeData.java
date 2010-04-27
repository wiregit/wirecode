package org.limewire.bittorrent;

/**
 * Data returned by a tracker scrape for a given torrent.
 */
public interface TorrentScrapeData {
    
    /**
     * @return number of peers on the tracker that have the entire torrent.
     *          aka seeders
     */
    public long getComplete();
    
    /**
     * @return number of peers on the tracker that have the do not 
     *          have the entire torrent.
     *          aka leechers
     */
    public long getIncomplete();
    
    /**
     * @return number of times the torrent has been downloaded.
     */
    public long getDownloaded();
}
