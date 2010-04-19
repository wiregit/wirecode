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
import org.limewire.inject.LazySingleton;
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
 * TODO: cache management,
 *        tracker ban
 */
@LazySingleton
public class TorrentScrapeScheduler {
    
    private static final long PERIOD = 1200;

    private static final int MAX_RESULTS_TO_KEEP_CACHED = 100;
    private static final int MAX_FAILURES_TO_KEEP_CACHED = 100;

    /**
     * Number of cycles to wait with an empty
     *  request queue before stopping this scheduler.
     *  
     *  <p> This will result in a cache cleansing cycle
     *       if required
     */
    private static final int EMPTY_PERIOD_MAX = 4;    

    private static final int PROCESSING_PERIOD_MAX = 3;
    private int processingPeriodsCount = 0;
    
    private final TrackerScraper scraper;
    
    private final AtomicBoolean awake = new AtomicBoolean(false);
    
    private ScheduledFuture<?> future = null;
    
    private final Map<String,TorrentScrapeData> resultsMap 
        = new HashMap<String, TorrentScrapeData>();
        
    private final List<String> failedTorrents = new ArrayList<String>();
    
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
        
        Torrent current = currentlyScrapingTorrent.get();
        if (current != null && torrent.getSha1().equals(current.getSha1())) {
            return;
        }
        
        synchronized (resultsMap) {
            if (resultsMap.containsKey(torrent.getSha1())) {
                return;
            }
        }

        synchronized (failedTorrents) {
            if (failedTorrents.contains(torrent.getSha1())) {
                return;
            }
        }

        
        synchronized (torrentsToScrape) {
            for ( Torrent torrentToScrape : torrentsToScrape ) {
                if (torrentToScrape.getSha1().equals(torrent.getSha1())) {
                    return;
                }
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
     * Put the processing thread to sleep, clear cache 
     *  entries if the threshold has been reached. 
     */
    private void sleep() {
        System.out.println("GOING TO SLEEP");
        if (awake.compareAndSet(true, false)) {
            future.cancel(false);
            future = null;
        }
        
        synchronized (failedTorrents) {
            int failedTorrentsToRemove = failedTorrents.size() - MAX_FAILURES_TO_KEEP_CACHED;
            if (failedTorrentsToRemove > 0) {
                for ( int i=0 ; i<failedTorrentsToRemove ; i++ ) {
                    failedTorrents.remove(0);
                }
            }
        }
        
        synchronized (resultsMap) {
            int resultsToRemove = resultsMap.size() - MAX_RESULTS_TO_KEEP_CACHED;
            if (resultsToRemove > 0) {
                List<String> keys = new ArrayList<String>(resultsMap.keySet());
                for ( int i=0 ; i<resultsToRemove ; i++ ) {
                    int randomKeyIndex = (int) (keys.size()*Math.random());
                    String keyToRemove = keys.remove(randomKeyIndex);
                    resultsMap.remove(keyToRemove);
                }
            }
        }        
    }
    
    /**
     * Get any scrape results if available.
     * 
     * @return null if no scrape data available.
     */
    public TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent) {
        synchronized (resultsMap) {
            return resultsMap.get(torrent.getSha1());
        }
    }
    
    /**
     * Mark a torrent that failed so we dont attempt to scrape it again.
     */
    private void markCurrentTorrentFailure() {
        System.out.println("  " + currentlyScrapingTorrent.get().getName() + " MARK FAIL");
        synchronized (failedTorrents) {
            failedTorrents.add(currentlyScrapingTorrent.getAndSet(null).getSha1());
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
                sleep();
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
                                resultsMap.put(currentlyScrapingTorrent.getAndSet(null).getSha1(),
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
