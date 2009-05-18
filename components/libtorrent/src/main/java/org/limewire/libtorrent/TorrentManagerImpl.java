package org.limewire.libtorrent;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class TorrentManagerImpl implements TorrentManager {

    private static final boolean PERIODICALLY_SAVE_FAST_RESUME_DATA = true;

    private static final Log LOG = LogFactory.getLog(TorrentManagerImpl.class);

    private final Provider<File> torrentDownloadFolder;

    private final ScheduledExecutorService torrentExecutor;

    private final ScheduledExecutorService alertExecutor;

    private final LibTorrentWrapper libTorrent;

    private final Map<String, Torrent> torrents;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Inject
    public TorrentManagerImpl(@TorrentDownloadFolder Provider<File> torrentDownloadFolder) {
        this.torrentDownloadFolder = torrentDownloadFolder;
        this.torrentExecutor = new ScheduledThreadPoolExecutor(1);
        this.alertExecutor = new ScheduledThreadPoolExecutor(1);

        this.libTorrent = new LibTorrentWrapper();
        this.torrents = new ConcurrentHashMap<String, Torrent>();
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        addTorrent(torrent);
    }

    private void addTorrent(Torrent torrent) {
        File torrentFile = torrent.getTorrentFile();
        File fastResumefile = torrent.getFastResumeFile();

        String trackerURI = torrent.getTrackerURL();
        String fastResumePath = fastResumefile != null ? fastResumefile.getAbsolutePath() : null;
        String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;

        try {
            lock.writeLock().lock();
            libTorrent.add_torrent(torrent.getSha1(), trackerURI, torrentPath, fastResumePath);
            updateStatus(torrent);
        } finally {
            lock.writeLock().unlock();
        }

        torrents.put(torrent.getSha1(), torrent);
    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    @Override
    public List<String> getPeers(Torrent torrent) {
        return libTorrent.get_peers(torrent.getSha1());
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        try {
            lock.writeLock().lock();
            torrents.remove(torrent.getSha1());
        } finally {
            lock.writeLock().unlock();
        }
        libTorrent.remove_torrent(torrent.getSha1());
    }

    @Override
    public void pauseTorrent(Torrent torrent) {
        try {
            lock.readLock().lock();
            String sha1 = torrent.getSha1();
            libTorrent.pause_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void resumeTorrent(Torrent torrent) {
        try {
            lock.readLock().lock();
            String sha1 = torrent.getSha1();
            libTorrent.resume_torrent(sha1);
            updateStatus(torrent);
        } finally {
            lock.readLock().unlock();
        }
    }

    private LibTorrentStatus getStatus(Torrent torrent) {
        LibTorrentStatus status = new LibTorrentStatus();

        String sha1 = torrent.getSha1();
        libTorrent.get_torrent_status(sha1, status);
        libTorrent.free_torrent_status(status);
        return status;
    }

    private void updateStatus(Torrent torrent) {
        try {
            lock.readLock().lock();
            LibTorrentStatus torrentStatus = getStatus(torrent);
            torrent.updateStatus(torrentStatus);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public File getTorrentDownloadFolder() {
        return torrentDownloadFolder.get();
    }

    @Override
    public void moveTorrent(Torrent torrent, File directory) {
        String sha1 = torrent.getSha1();
        libTorrent.move_torrent(sha1, directory.getAbsolutePath());
        updateStatus(torrent);
    }

    @Override
    public int getNumActiveTorrents() {
        return torrents.size();
    }

    @Override
    public String getServiceName() {
        return "TorrentManager";
    }

    @Override
    public void initialize() {
        libTorrent.initialize(torrentDownloadFolder.get().getAbsolutePath());
        // TODO what if path changes.
    }

    @Override
    public void start() {
        torrentExecutor.scheduleAtFixedRate(new EventPoller(), 1000, 500, TimeUnit.MILLISECONDS);

        if (!OSUtils.isMacOSX()) {
            // TODO disabling for now on the mac, on osx there is an error
            // calling the alert callback, need toi investigate.
            // but disabling for now so that it does not crash the jvm.

            if (PERIODICALLY_SAVE_FAST_RESUME_DATA) {
                alertExecutor.scheduleAtFixedRate(new AlertPoller(), 1000, 500,
                        TimeUnit.MILLISECONDS);
                alertExecutor.scheduleAtFixedRate(new ResumeDataScheduler(), 10000, 10000,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void stop() {
        try {
            lock.writeLock().lock();
            try {
                torrentExecutor.shutdown();
                torrentExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("Error shutting down Torrent Executor", e);
            }
            try {
                alertExecutor.shutdown();
                alertExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("Error shutting down Alert Executor", e);
            }

            if (!OSUtils.isMacOSX()) {
                // TODO disabling for now on the mac, on osx there is an error
                // calling the alert callback, need to investigate.
                // but disabling for now so that it does not crash the jvm.
                libTorrent.freeze_and_save_all_fast_resume_data(new BasicAlertCallback());
            }
            libTorrent.abort_torrents();
            torrents.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isDownloading(File torrentFile) {
        if (torrentFile != null) {
            synchronized (torrents) {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDownloading(String sha1) {
        return torrents.containsKey(sha1);
    }

    @Override
    public Executor getTorrentExecutor() {
        return torrentExecutor;
    }

    /**
     * Basic implemenation of the AlertCallback interface used to delegate
     * alerts back to the appropriate torrent.
     */
    private class BasicAlertCallback implements AlertCallback {

        @Override
        public void callback(LibTorrentAlert alert) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }

            if (alert.sha1 != null) {
                Torrent torrent = torrents.get(alert.sha1);
                if (torrent != null) {
                    torrent.alert(alert);
                }
            }
        }
    }

    /**
     * Used to clear the alert queue in the native code passing event back to
     * the java code through the alertCallback interface.
     */
    private class AlertPoller implements Runnable {
        @Override
        public void run() {
            libTorrent.get_alerts(new BasicAlertCallback());
        }
    }

    /**
     * Iterates through the torrents updating the status of each one to the
     * correct current state.
     */
    private class EventPoller implements Runnable {

        @Override
        public void run() {
            pumpStatus();
        }

        private void pumpStatus() {
            for (Torrent torrent : torrents.values()) {
                updateStatus(torrent);
            }
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
            try {
                lock.readLock().lock();
                if (!torrentIterator.hasNext()) {
                    torrentIterator = torrents.keySet().iterator();
                    if (!torrentIterator.hasNext()) {
                        return;
                    }
                }

                String sha1 = torrentIterator.next();
                Torrent torrent = torrents.get(sha1);

                if (torrent != null && !torrent.isFinished()) {
                    libTorrent.signal_fast_resume_data_request(sha1);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
