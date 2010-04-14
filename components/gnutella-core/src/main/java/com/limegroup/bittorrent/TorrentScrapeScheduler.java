package com.limegroup.bittorrent;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.TrackerScraper.ScrapeCallback;
import com.limegroup.gnutella.URN;

/**
 * Returning the data from torrent scraping by asynchronously 
 *  queueing then staggering the requests
 *  
 * <p> NOTE: Will go to sleep in periods of inactivity.  Will
 *            clear cache entries randomly after the entry 
 *            threshold is achieved.  Will ban trackers
 *            that consistently fail after scrapes are attempted. 
 *            
 * TODO: convert to singleton, use sha1 to hash, cache management,
 *        tracker ban, figure out random openbt fails.
 */
public class TorrentScrapeScheduler {
    
    private static final long PERIOD = 1200;
    /**
     * Number of cycles to wait with an empty
     *  request queue before stopping this scheduler.
     *  
     *  TODO: not needed anymore with wakeups
     */
    private static final int EMPTY_PERIOD_MAX = 2;    

    private static final int PROCESSING_PERIOD_MAX = 3;
    private int processingPeriodsCount = 0;
    
    private final TrackerScraper scraper;
    
    private final AtomicBoolean awake = new AtomicBoolean(false);
    
    private ScheduledFuture<?> future = null;
    
    private final Map<Torrent,TorrentScrapeData> resultsMap 
        = new HashMap<Torrent, TorrentScrapeData>();
        
    private final List<Torrent> failedTorrents = new ArrayList<Torrent>();
    
    private final Queue<Torrent> torrentsToScrape
        = new LinkedList<Torrent>();
    
    private final AtomicReference<Torrent> currentlyScrapingTorrent
        = new AtomicReference<Torrent>(null);
 
    private Shutdownable currentScrapeAttemptShutdown = null;
    
    /**
     * Used to decide when to give up waiting for requests 
     *  and shut down this scheduler instance.
     */
    private int queueEmptyPeriodsCount = 0;
    
    private final Runnable command = new Runnable() {
        @Override
        public void run() {
            System.out.println("process");
            process();
        }
    };
    private final ScheduledExecutorService backgroundExecutor;
    
    @Inject
    public TorrentScrapeScheduler(TrackerScraper scraper,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        
        this.scraper = scraper;
        this.backgroundExecutor = backgroundExecutor;
        
        awake.set(true);
        future = scheduleProcessor();
    }
    
    private ScheduledFuture<?> scheduleProcessor() {
        return backgroundExecutor.scheduleWithFixedDelay(command,
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
            wakeup();
        }
    }
    
    /**
     * Wakeup the processing thread if needed.
     */
    private void wakeup() {
        if (awake.compareAndSet(false, true)) {
            System.out.println("wakeup");
            future = scheduleProcessor();
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
        System.out.println("  " + currentlyScrapingTorrent.get().getName() + " MARK FAIL");
        synchronized (failedTorrents) {
            failedTorrents.add(currentlyScrapingTorrent.getAndSet(null));
        }
    }
    
    /**
     * Used to ban consitently failing trackers
     */
    private void markCurrentTrackerFailure() {
    }
   
    private void process() {
        
        synchronized (torrentsToScrape) {
            if (!currentlyScrapingTorrent.compareAndSet(null, torrentsToScrape.peek())) {
                if (processingPeriodsCount++ >= PROCESSING_PERIOD_MAX) {
                    System.out.println("CANCEL SCRAPE REQUEST");
                    currentScrapeAttemptShutdown.shutdown();
                    // Don't need to mark fail here... will get it on shutdown
                }
                return;
            } else {
                torrentsToScrape.poll();
                processingPeriodsCount = 0;
            }
        }
        
        final Torrent torrent = currentlyScrapingTorrent.get();
        if (torrent == null) {
            if (queueEmptyPeriodsCount++ > EMPTY_PERIOD_MAX) {
                System.out.println("GOING TO SLEEP");
                if (awake.compareAndSet(true, false)) {
                    future.cancel(false);
                    future = null;
                }
                
            }
            return;
        } else {
            queueEmptyPeriodsCount = 0;
        }
        
        List<URI> trackers = torrent.getTrackerURIS();
        if (trackers == null || trackers.size() < 1) {
            // Has no trackers
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

            currentScrapeAttemptShutdown = scraper.submitScrape(tracker,
                    URN.createSha1UrnFromHex(torrent.getSha1()), 
                    new ScrapeCallback() {
                        @Override
                        public void success(TorrentScrapeData data) {
                            synchronized (resultsMap) {
                                System.out.println("  " + torrent.getName() + " FOUND");
                                resultsMap.put(currentlyScrapingTorrent.getAndSet(null),
                                            data);
                            }
                        }
                        @Override
                        public void failure(String reason) {
                            System.out.println("   " + torrent.getName() + " FAILED");
                            markCurrentTrackerFailure();
                            markCurrentTorrentFailure();
                        }
                    });
            if (currentScrapeAttemptShutdown == null) {
                markCurrentTorrentFailure();
                return;
            }
        } catch (IOException e) {
            markCurrentTorrentFailure();
            return;
        }
    }
}
