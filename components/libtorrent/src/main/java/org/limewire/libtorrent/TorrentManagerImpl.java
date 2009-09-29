package org.limewire.libtorrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.inject.LazySingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
public class TorrentManagerImpl implements TorrentManager {

    private static final Log LOG = LogFactory.getLog(TorrentManagerImpl.class);

    private static final int GLOBAL_ALERT_MASK = LibTorrentAlert.storage_notification 
                                               | LibTorrentAlert.progress_notification
                                               | LibTorrentAlert.status_notification;
    
    private static final int CALLBACK_ALERT_MASK = GLOBAL_ALERT_MASK;
    
    private final ScheduledExecutorService fastExecutor;

    private final LibTorrentWrapper libTorrent;

    private final Map<String, Torrent> torrents;
    
    private final BasicAlertCallback alertCallback = new BasicAlertCallback();

    private final AtomicReference<TorrentManagerSettings> torrentSettings = new AtomicReference<TorrentManagerSettings>(
            null);

    EventListener<TorrentEvent> torrentListener = new EventListener<TorrentEvent>() {
        @Override
        public void handleEvent(TorrentEvent event) {
            handleTorrentEvent(event);
        }
    };

    /**
     * Used to protect from calling libtorrent code with invalid torrent data.
     * Locks access around libtorrent and removing/updating the torrents map to
     * make sure the torrents torrent manger knows about are the same as what
     * libtorrent knows about.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Future for the job updating the torrent status.
     */
    private ScheduledFuture<?> torrentFuture;

    /**
     * Future for the job listening to torrent alerts.
     */
    private ScheduledFuture<?> alertFuture;

    /**
     * Future for the job creating resume files. The alert job must be running
     * for the resume files to be created properly.
     */
    private ScheduledFuture<?> resumeFileFuture;

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint(value = "torrent manager", category = DataCategory.USAGE)
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                int active = 0;
                int seeding = 0;
                int starting = 0;

                lock.readLock().lock();
                try {
                    for (Torrent torrent : torrents.values()) {
                        if (!torrent.isStarted()) {
                            starting++;
                        } else if (torrent.isFinished()) {
                            seeding++;
                        } else {
                            active++;
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }
                data.put("active", active);
                data.put("seeding", seeding);
                data.put("starting", starting);
                return data;
            }
        };
    }

    @Inject
    public TorrentManagerImpl(LibTorrentWrapper torrentWrapper,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.fastExecutor = fastExecutor;
        this.libTorrent = torrentWrapper;
        this.torrents = new ConcurrentHashMap<String, Torrent>();
        this.torrentSettings.set(torrentSettings);
    }

    private void validateLibrary() {
        if (!torrentSettings.get().isTorrentsEnabled()) {
            throw new TorrentException("LibTorrent is disabled (through settings)",
                    TorrentException.DISABLED_EXCEPTION);
        }
        if (!isValid()) {
            throw new TorrentException("There was a problem loading LibTorrent",
                    TorrentException.LOAD_EXCEPTION);
        }
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        validateLibrary();
        addTorrent(torrent);
        torrent.addListener(torrentListener);
    }

    @Override
    public boolean isValid() {
        return libTorrent.isLoaded();
    }

    private void addTorrent(Torrent torrent) {
        File torrentFile = torrent.getTorrentFile();
        File fastResumefile = torrent.getFastResumeFile();

        String trackerURI = torrent.getTrackerURL();
        String fastResumePath = fastResumefile != null ? fastResumefile.getAbsolutePath() : null;
        String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;
        String saveDirectory = torrent.getTorrentDataFile().getParentFile().getAbsolutePath();

        lock.writeLock().lock();
        try {
            libTorrent.add_torrent(torrent.getSha1(), trackerURI, torrentPath, saveDirectory,
                    fastResumePath);
            updateStatus(torrent);
            torrents.put(torrent.getSha1(), torrent);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        validateLibrary();
        lock.writeLock().lock();
        try {
            torrents.remove(torrent.getSha1());
            libTorrent.remove_torrent(torrent.getSha1());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void pauseTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.pause_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void resumeTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.resume_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void recoverTorrent(Torrent torrent) {
        validateLibrary();
        lock.readLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.clear_error_and_retry(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    private LibTorrentStatus getStatus(Torrent torrent) {
        validateLibrary();
        LibTorrentStatus status = new LibTorrentStatus();

        String sha1 = torrent.getSha1();
        libTorrent.get_torrent_status(sha1, status);
        libTorrent.free_torrent_status(status);
        return status;
    }

    private void updateStatus(Torrent torrent) {
        lock.readLock().lock();
        try {
            LibTorrentStatus torrentStatus = getStatus(torrent);
            torrent.updateStatus(torrentStatus);
            addMetaData(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void addMetaData(Torrent torrent) {
        if (!torrent.hasMetaData() && libTorrent.has_metadata(torrent.getSha1())) {
            // TODO add more data to the torrentInfo object
            List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
            if (fileEntries.size() > 0) {
                TorrentInfo torrentInfo = new TorrentInfo(fileEntries);
                torrent.setTorrentInfo(torrentInfo);
            }
        }
    }

    @Override
    public void moveTorrent(Torrent torrent, File directory) {
        validateLibrary();
        lock.writeLock().lock();
        try {
            String sha1 = torrent.getSha1();
            libTorrent.move_torrent(sha1, directory.getAbsolutePath());
            updateStatus(torrent);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void handleTorrentEvent(TorrentEvent event) {
        if (event == TorrentEvent.COMPLETED) {
            limitSeedingTorrents();
        }
    }

    private void limitSeedingTorrents() {
        // Check the number of seeding torrents and stop any long running
        // torrents
        // if there are more there are more than the limit

        lock.writeLock().lock();

        try {
            int seedingTorrents = 0;
            int maxSeedingTorrents = torrentSettings.get().getMaxSeedingLimit();

            // Cut out early if the limit is infinite
            if (maxSeedingTorrents == Integer.MAX_VALUE) {
                return;
            }

            for (Torrent torrent : torrents.values()) {
                if (torrent.isFinished()) {
                    seedingTorrents++;
                }
            }

            if (seedingTorrents <= maxSeedingTorrents) {
                return;
            }

            List<Torrent> ratioSortedTorrents = new ArrayList<Torrent>(torrents.values());
            Collections.sort(ratioSortedTorrents, new Comparator<Torrent>() {
                @Override
                public int compare(Torrent o1, Torrent o2) {
                    // Sort smallest first
                    int compare = Double.compare(o2.getSeedRatio(), o1.getSeedRatio());

                    // Compare by seeding time if seeding ratio is the same
                    // (generally at 0:0)
                    // -- Older values are discarded first. --
                    if (compare == 0) {
                        TorrentStatus status1 = o1.getStatus();
                        TorrentStatus status2 = o2.getStatus();
                        if (status1 != null && status2 != null) {
                            int time1 = status1.getSeedingTime();
                            int time2 = status2.getSeedingTime();
                            if (time1 > time2) {
                                return -1;
                            } else if (time2 > time1) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }

                    return compare;
                }
            });

            for (int i = 0; i < seedingTorrents - maxSeedingTorrents
                    && ratioSortedTorrents.size() > 0;) {
                Torrent torrent = ratioSortedTorrents.remove(0);

                if (torrent.isFinished()) {
                    torrent.stop();
                    torrent.removeListener(torrentListener);
                    i++;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void initialize() {
        if (torrentSettings.get().isTorrentsEnabled()) {
            lock.writeLock().lock();
            try {
                libTorrent.initialize(torrentSettings.get());
                if (libTorrent.isLoaded()) {
                    setTorrentManagerSettings(torrentSettings.get());
                    libTorrent.start_upnp();
                    libTorrent.start_natpmp();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void start() {
        if (isValid()) {
            lock.writeLock().lock();
            try {
                alertFuture = fastExecutor.scheduleWithFixedDelay(new AlertPoller(), 1000, 500,
                        TimeUnit.MILLISECONDS);
                resumeFileFuture = fastExecutor.scheduleWithFixedDelay(new ResumeDataScheduler(),
                        10000, 10000, TimeUnit.MILLISECONDS);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void stop() {
        lock.writeLock().lock();
        try {
            if (resumeFileFuture != null) {
                resumeFileFuture.cancel(true);
            }

            if (torrentFuture != null) {
                torrentFuture.cancel(true);
            }
            if (alertFuture != null) {
                alertFuture.cancel(true);
            }

            if (isValid()) {
                libTorrent.freeze_and_save_all_fast_resume_data(alertCallback);
                libTorrent.abort_torrents();
            }
            torrents.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Torrent getTorrent(File torrentFile) {
        if (torrentFile != null) {
            synchronized (torrents) {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile())) {
                        return torrent;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Torrent getTorrent(String sha1) {
        return torrents.get(sha1);
    }

    /**
     * Basic implemenation of the AlertCallback interface used to delegate
     * alerts back to the appropriate torrent.
     */
    private class BasicAlertCallback implements AlertCallback {

        private final Set<String> updatedTorrents = new HashSet<String>();
        
        @Override
        public void callback(LibTorrentAlert alert) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }

            String sha1 = alert.getSha1();
            if (sha1 != null) {
                updatedTorrents.add(sha1);
                
                if (alert.getCategory() == LibTorrentAlert.storage_notification) {
                    Torrent torrent = torrents.get(sha1);
                    if (torrent != null) {
                        torrent.handleFastResumeAlert(alert);
                    }
                }
            }
        }
        
        /** 
         * Updates the status of all torrents that have recently recieved an event
         */
        public void updateAlertedTorrents() {
            for ( String sha1 : updatedTorrents ) {
                Torrent torrent = torrents.get(sha1);
                if (torrent != null) {
                    updateStatus(torrent);
                }
            }
            updatedTorrents.clear();
        }
    }

    /**
     * Used to clear the alert queue in the native code passing event back to
     * the java code through the alertCallback interface.
     */
    private class AlertPoller implements Runnable {

        @Override
        public void run() {
            // Handle any alerts for fastresume/progress/status changes
            libTorrent.get_alerts(alertCallback, CALLBACK_ALERT_MASK);
            
            // Update status of alerted torrents
            alertCallback.updateAlertedTorrents();
        }
    }

    /**
     * Iterates through the torrents periodically saving a fastresume file for
     * each file.
     */
    private class ResumeDataScheduler implements Runnable {

        private Iterator<String> torrentIterator = torrents.keySet().iterator();

        @Override
        public void run() {
            lock.readLock().lock();
            try {
                if (!torrentIterator.hasNext()) {
                    torrentIterator = torrents.keySet().iterator();
                    if (!torrentIterator.hasNext()) {
                        return;
                    }
                }

                String sha1 = torrentIterator.next();
                Torrent torrent = torrents.get(sha1);

                if (torrent != null && torrent.hasMetaData()) {
                    libTorrent.signal_fast_resume_data_request(sha1);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        if (torrentFile != null) {
            synchronized (torrents) {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile()) && !torrent.isFinished()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        validateLibrary();
        libTorrent.set_alert_mask(GLOBAL_ALERT_MASK);
        torrentSettings.set(settings);
        libTorrent.update_settings(settings);
        limitSeedingTorrents();
    }

    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        return torrentSettings.get();
    }

    @Override
    public float getTotalDownloadRate() {
        float rate = 0;
        for (Torrent torrent : torrents.values()) {
            TorrentStatus torrentStatus = torrent.getStatus();
            if (torrentStatus != null) {
                rate += torrentStatus.getDownloadRate();
            }
        }
        return rate;
    }

    @Override
    public float getTotalUploadRate() {
        float rate = 0;
        for (Torrent torrent : torrents.values()) {
            TorrentStatus torrentStatus = torrent.getStatus();
            if (torrentStatus != null) {
                rate += torrentStatus.getUploadRate();
            }
        }
        return rate;
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries(Torrent torrent) {
        validateLibrary();
        TorrentFileEntry[] files = libTorrent.get_files(torrent.getSha1());
        return Arrays.asList(files);
    }

    @Override
    public List<TorrentPeer> getTorrentPeers(Torrent torrent) {
        validateLibrary();
        TorrentPeer[] peers = libTorrent.get_peers(torrent.getSha1());
        return Arrays.asList(peers);
    }

    @Override
    public void setAutoManaged(Torrent torrent, boolean autoManaged) {
        libTorrent.set_auto_managed_torrent(torrent.getSha1(), autoManaged);
    }

    @Override
    public void setTorrenFileEntryPriority(Torrent torrent, TorrentFileEntry torrentFileEntry,
            int priority) {
        libTorrent.set_file_priority(torrent.getSha1(), torrentFileEntry.getIndex(), priority);
    }

    @Override
    public boolean isInitialized() {
        return isValid();
    }

    @Override
    public List<Torrent> getTorrents() {
        List<Torrent> torrents = null;
        synchronized (this.torrents) {
             torrents = new ArrayList<Torrent>(this.torrents.values());    
        }
        return torrents;
    }
}
