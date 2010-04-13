package org.limewire.ui.swing.search.resultpanel.classic;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.lifecycle.ServiceScheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.TrackerScraper;
import com.limegroup.bittorrent.TrackerScraper.ScrapeCallback;
import com.limegroup.gnutella.URN;

/**
 * Returning the data from torrent scraping by asynchronously 
 *  queueing then staggering the requests
 */

// TODO: clean up mess!! unregister service or provide a secondary queue 
//        processor that is not created for every search tab
public class TorrentScrapeScheduler {
    
    private static final long PERIOD = 1200;
    
    private final TrackerScraper scraper;
    
    private ScheduledFuture<?> future = null;
    
    private final Map<Torrent,TorrentScrapeData> resultsMap 
        = new HashMap<Torrent, TorrentScrapeData>();
        
    private final List<Torrent> failedTorrents = new ArrayList<Torrent>();
    
    private final Queue<Torrent> torrentsToScrape
        = new LinkedList<Torrent>();
    
    private final AtomicReference<Torrent> currentlyScrapingTorrent
        = new AtomicReference<Torrent>(null);
 
    @Inject
    public TorrentScrapeScheduler(TrackerScraper scraper) {
        this.scraper = scraper;
    }
    
    @Inject 
    void registerService(ServiceScheduler scheduler, 
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
          final Runnable command = new Runnable() {
              @Override
              public void run() {
                  System.out.println("process");
                  process();
              }
          };
     
          future = backgroundExecutor.scheduleWithFixedDelay(command,
                  PERIOD*2, PERIOD, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Initiate a scrape request asyncronously results will be available
     *  by {link #getScrapeDataIfAvailable()}
     */
    public void queueScrapeIfNew(Torrent torrent) {
        
        if (currentlyScrapingTorrent.get() == torrent) {
            return;
        }
        
        synchronized (resultsMap) {
            if (resultsMap.containsKey(torrent)) {
                return;
            }
        }

        synchronized (failedTorrents) {
            if (failedTorrents.contains(torrent)) {
                return;
            }
        }

        
        synchronized (torrentsToScrape) {
            if (torrentsToScrape.contains(torrent)) {
                return;
            }
            
            System.out.println(torrent.getName() + " queued");
            torrentsToScrape.add(torrent);
        }
    }
    
    /**
     * Get any scrape results if available.
     * 
     * @return null if no scrape data available.
     */
    public TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent) {
        synchronized (resultsMap) {
            return resultsMap.get(torrent);
        }
    }
    
    /**
     * Mark a torrent that failed so we dont attempt to scrape it again.
     */
    private void markCurrentTorrentFailure() {
        synchronized (failedTorrents) {
            failedTorrents.add(currentlyScrapingTorrent.getAndSet(null));
        }
    }
    
    private void process() {
        
        synchronized (torrentsToScrape) {
            if (!currentlyScrapingTorrent.compareAndSet(null, torrentsToScrape.poll())) {
                return;
            }
        }
        
        final Torrent torrent = currentlyScrapingTorrent.get();
        if (torrent == null) {
            return;
        }
        
        List<URI> trackers = torrent.getTrackerURIS();
        if (trackers == null || trackers.size() < 1) {
            markCurrentTorrentFailure();
            return;
        }

        // Find first HTTP tracker since right now we only support them
        URI tracker = null;
        for ( URI potentialTracker : trackers ) {
            if (potentialTracker.toString().startsWith("http")) {
                tracker = potentialTracker;
                break;
            }
        }
        
        if (tracker == null) {
            // Could not find any HTTP trackers.
            markCurrentTorrentFailure();
            return;
        }
        
        try {
            System.out.println(torrent.getName() + " submit");

            boolean submitted = scraper.submitScrape(tracker,
                    URN.createSha1UrnFromHex(torrent.getSha1()), 
                    new ScrapeCallback() {
                        @Override
                        public void success(TorrentScrapeData data) {
                            synchronized (resultsMap) {
                                System.out.println(torrent.getName() + " found");
                                resultsMap.put(currentlyScrapingTorrent.getAndSet(null),
                                            data);
                            }
                        }
                        @Override
                        public void failure(String reason) {
                            System.out.println(torrent.getName() + " failed");
                            markCurrentTorrentFailure();
                        }
                    });
            if (!submitted) {
                markCurrentTorrentFailure();
                return;
            }
        } catch (IOException e) {
            markCurrentTorrentFailure();
            return;
        }
    }
}
