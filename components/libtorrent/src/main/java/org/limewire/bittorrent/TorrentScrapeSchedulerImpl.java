package org.limewire.bittorrent;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.TorrentTrackerScraper.RequestShutdown;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;
import org.limewire.collection.CollectionUtils;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Returning the data from torrent scraping by asynchronously 
 *  queueing then staggering the requests
 *  
 * <p> NOTE: Will go to sleep in periods of inactivity.  Will
 *            clear cache entries randomly if the entry 
 *            threshold is achieved when going to sleep. 
 *            Will ban trackers that consistently fail
 *            after scrapes are attempted. 
 *            
 * TODO: 
 *        tracker ban
 */
@LazySingleton
public class TorrentScrapeSchedulerImpl implements TorrentScrapeScheduler {
    
    private static final Log LOG = LogFactory.getLog(TorrentScrapeSchedulerImpl.class);
    
    /**
     * Time between processing cycles, ie. submitting new
     *  scrapes, deciding to cancel scrapes, etc..
     */
    private static final long PERIOD = 1200;

    /**
     * Threshold before clearing cached results.
     */
    private static final int MAX_RESULTS_TO_KEEP_CACHED = 100;
    
    /**
     * Threshold before clearing cached fails
     */
    private static final int MAX_FAILURES_TO_KEEP_CACHED = 100;

    /**
     * Number of cycles to wait with an empty
     *  request queue and no job before stopping this scheduler.
     *  
     *  <p> This will result in a cache cleansing cycle
     *       if required
     */
    private static final int EMPTY_PERIOD_MAX = 4;    

    /**
     * Number of cycles before cancelling a request if it hasn't
     *  completed
     */
    private static final int PROCESSING_PERIOD_MAX = 3;
    
    
    private int processingPeriodsCount = 0;
    
    private final TorrentTrackerScraper scraper;
    
    /**
     * Whether the processing thread is active.
     */
    private final AtomicBoolean awake = new AtomicBoolean(false);
    
    /**
     * The future for the processing thread.  Used to go to sleep.
     */
    private ScheduledFuture<?> processingThreadFuture = null;
    
    /**
     * This list of fetched results.  Dually serves to cache.
     */
    private final Map<String,TorrentScrapeData> resultsMap 
        = new HashMap<String, TorrentScrapeData>();
        
    /**
     * List of failed torrents to not try again.
     */
    private final Set<String> failedTorrents = new HashSet<String>();
    
    /**
     * Processing queue.
     */
    private final Queue<Torrent> torrentsToScrape
        = new LinkedList<Torrent>();
    
    /**
     * The current thread being processed.
     */
    private final AtomicReference<Torrent> currentlyScrapingTorrent
        = new AtomicReference<Torrent>(null);
 
    /**
     * A shutoff for the current job if it has been running too long.
     */
    private RequestShutdown currentScrapeAttemptShutdown = null;
    
    
    /**
     * Used to decide when to give up waiting for requests 
     *  and shut down this scheduler instance.
     */
    private int queueEmptyPeriodsCount = 0;
    
    private final Runnable command = new Runnable() {
        @Override
        public void run() {
            LOG.debugf("process");
            process();
        }
    };
    
    private final ScheduledExecutorService backgroundExecutor;
    
    @Inject
    public TorrentScrapeSchedulerImpl(TorrentTrackerScraper scraper,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        
        this.scraper = scraper;
        this.backgroundExecutor = backgroundExecutor;
    }
    
    private ScheduledFuture<?> scheduleProcessor() {
        return backgroundExecutor.scheduleWithFixedDelay(command,
                PERIOD*2, PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
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
     
            if (LOG.isDebugEnabled()) {
                LOG.debugf("{0} queued", torrent.getName());
            }
            
            torrentsToScrape.add(torrent);
            wakeup();
        }
    }
    
    /**
     * Wakeup the processing thread if needed.
     */
    private void wakeup() {
        if (awake.compareAndSet(false, true)) {
            LOG.debugf("wakeup");
            processingThreadFuture = scheduleProcessor();
        }
    }
    
    /**
     * Put the processing thread to sleep, clear cache 
     *  entries if the threshold has been reached. 
     */
    private void sleep() {
        LOG.debugf("GOING TO SLEEP");
        if (awake.compareAndSet(true, false)) {
            processingThreadFuture.cancel(false);
            processingThreadFuture = null;
        }
        
        synchronized (failedTorrents) {
            int failedTorrentsToRemove = failedTorrents.size() - MAX_FAILURES_TO_KEEP_CACHED;
            if (failedTorrentsToRemove > 0) {
                LOG.debugf("purging {0} failed torrents", failedTorrentsToRemove);
                CollectionUtils.randomPurge(failedTorrents, failedTorrentsToRemove);
            }
        }
        
        synchronized (resultsMap) {
            int resultsToRemove = resultsMap.size() - MAX_RESULTS_TO_KEEP_CACHED;
            if (resultsToRemove > 0) {
                LOG.debugf("purging {0} results", resultsToRemove);
                CollectionUtils.randomPurge(resultsMap, resultsToRemove);
            }
        }        
    }
    
    @Override
    public TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent) {
        synchronized (resultsMap) {
            return resultsMap.get(torrent.getSha1());
        }
    }
    
    /**
     * Mark a torrent that failed so we dont attempt to scrape it again.
     */
    private void markCurrentTorrentFailure() {
        LOG.debugf("  {0} MARK FAIL", currentlyScrapingTorrent.get().getName());
        synchronized (failedTorrents) {
            failedTorrents.add(currentlyScrapingTorrent.getAndSet(null).getSha1());
        }
    }
    
    /**
     * Used to ban consistently failing trackers.
     * 
     * TODO!
     */
    private void markCurrentTrackerFailure() {
    }
   
    /**
     * The processing thread.  Handles submitting jobs.
     */
    private void process() {
        
        synchronized (torrentsToScrape) {
            if (!currentlyScrapingTorrent.compareAndSet(null, torrentsToScrape.peek())) {
                if (processingPeriodsCount++ >= PROCESSING_PERIOD_MAX) {
                    LOG.debugf("CANCEL SCRAPE REQUEST");
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
        if (trackers.size() < 1) {
            // Has no trackers
            markCurrentTorrentFailure();
            return;
        }

        // Find first HTTP tracker since right now we only support them
        URI tracker = null;
        for ( URI potentialTracker : trackers ) {
            if (potentialTracker.toString().toLowerCase(Locale.US).startsWith("http")) {
                tracker = potentialTracker;
                break;
            }
        }
        
        if (tracker == null) {
            // Could not find any HTTP trackers.
            markCurrentTorrentFailure();
            return;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debugf(" {0} submit", torrent.getName());
        }

        currentScrapeAttemptShutdown = scraper.submitScrape(tracker,
                torrent.getSha1(), 
                new ScrapeCallback() {
            @Override
            public void success(TorrentScrapeData data) {
                synchronized (resultsMap) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf("  {0} FOUND", torrent.getName());
                    }
                    resultsMap.put(currentlyScrapingTorrent.getAndSet(null).getSha1(),
                            data);
                }
            }
            @Override
            public void failure(String reason) {
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("   {0} FAILED", torrent.getName());
                }
                markCurrentTrackerFailure();
                markCurrentTorrentFailure();
            }
        });
        if (currentScrapeAttemptShutdown == null) {
            markCurrentTorrentFailure();
            return;
        }
    }
}
