package org.limewire.core.impl.bittorrent;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentScrapeScheduler;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;

public class MockTorrentScrapeScheduler implements TorrentScrapeScheduler {

    @Override
    public TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void queueScrapeIfNew(Torrent torrent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void queueScrape(Torrent torrent, ScrapeCallback callback) {
        // TODO Auto-generated method stub
        
    }

}
