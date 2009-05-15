package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class TorrentManagerImpl implements TorrentManager {

    private static final boolean PERIODICALLY_SAVE_FAST_RESUME_DATA = true;

    private static final Log LOG = LogFactory.getLog(TorrentManagerImpl.class);

    private final File torrentDownloadFolder;

    private final ScheduledExecutorService backgroundExecutor;

    private final LibTorrentWrapper libTorrent;

    private final Map<String, Torrent> torrents;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AlertCallback alertCallback = new FastResumeAlertCallback();

    @Inject
    public TorrentManagerImpl(@TorrentDownloadFolder File torrentDownloadFolder,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {

        this.torrentDownloadFolder = torrentDownloadFolder;
        // TODO move back to background executor?
        this.backgroundExecutor = new ScheduledThreadPoolExecutor(1);

        this.libTorrent = new LibTorrentWrapper();
        this.torrents = new ConcurrentHashMap<String, Torrent>();
    }

    @Override
    public void registerTorrent(Torrent torrent) {
        addTorrent(torrent);
    }

    private void addTorrent(Torrent torrent) {
        // TODO clean up this logic for picking which addTorrent method to
        // use

        torrents.put(torrent.getSha1(), torrent);
        File torrentFile = torrent.getTorrentFile();
        String sha1 = torrent.getSha1();
        File fastResumeFile = torrent.getFastResumeFile();
        String trackerURL = torrent.getTrackerURL();

        if (torrentFile != null) {
            addTorrent(sha1, torrentFile, fastResumeFile);
        } else {
            addTorrent(sha1, trackerURL, fastResumeFile);
        }

    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    @Override
    public List<String> getPeers(String id) {
        return libTorrent.get_peers(id);
    }

    private void addTorrent(String sha1, File torrent, File fastResumeFile) {
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        try {
            lock.writeLock().lock();
            libTorrent.add_torrent(torrent.getAbsolutePath(), fastResumePath);
            updateStatus(sha1);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addTorrent(String sha1, String trackerURI, File fastResumeFile) {
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        try {
            lock.writeLock().lock();
            libTorrent.add_torrent_existing(sha1, trackerURI, fastResumePath);
            updateStatus(sha1);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void removeTorrent(final String id) {
        try {
            lock.writeLock().lock();
            torrents.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
        libTorrent.remove_torrent(id);
    }

    @Override
    public void pauseTorrent(String id) {
        try {
            lock.readLock().lock();
            libTorrent.pause_torrent(id);
            updateStatus(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void resumeTorrent(String id) {
        try {
            lock.readLock().lock();
            libTorrent.resume_torrent(id);
            updateStatus(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    private LibTorrentStatus getStatus(String id) {
        LibTorrentStatus status = new LibTorrentStatus();

        libTorrent.get_torrent_status(id, status);
        LibTorrentStatus statusCopy = new LibTorrentStatus(status);
        libTorrent.free_torrent_status(status);
        return statusCopy;
    }

    private void updateStatus(String id) {
        try {
            lock.readLock().lock();
            LibTorrentStatus torrentStatus = getStatus(id);
            Torrent torrent = torrents.get(id);

            torrent.updateStatus(torrentStatus);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public File getTorrentDownloadFolder() {
        return torrentDownloadFolder;
    }

    @Override
    public void moveTorrent(String id, File directory) {
        libTorrent.move_torrent(id, directory.getAbsolutePath());
        updateStatus(id);
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
        libTorrent.initialize(torrentDownloadFolder.getAbsolutePath());
        // TODO what if path changes.
    }

    @Override
    public void start() {
        backgroundExecutor
                .scheduleAtFixedRate(new EventPoller(), 1000, 500, TimeUnit.MILLISECONDS);

        if (PERIODICALLY_SAVE_FAST_RESUME_DATA) {
            backgroundExecutor.scheduleAtFixedRate(new ResumeDataScheduler(), 6000, 6000,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        try {
            lock.writeLock().lock();
            libTorrent.freeze_and_save_all_fast_resume_data(alertCallback);
            libTorrent.abort_torrents();
            torrents.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class EventPoller implements Runnable {

        @Override
        public void run() {
            pumpStatus();
            //libTorrent.get_alerts(alertCallback);
        }

        private void pumpStatus() {
            for (String id : torrents.keySet()) {
                updateStatus(id);
            }
        }
    }

    private class ResumeDataScheduler implements Runnable {

        // private Iterator<String> torrentIterator =
        // torrents.keySet().iterator();

        @Override
        public void run() {
            // TODO add back in
            // try {
            // lock.readLock().lock();
            // if (!torrentIterator.hasNext()) {
            // torrentIterator = torrents.keySet().iterator();
            // if (!torrentIterator.hasNext()) {
            // return;
            // }
            // }
            //
            // String sha1 = torrentIterator.next();
            // if (torrentIds.contains(sha1)) {
            // libTorrent.signal_fast_resume_data_request(sha1);
            // }
            // } finally {
            // lock.readLock().unlock();
            // }
        }
    }

    @Override
    public boolean isDownloading(File torrentFile) {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);

            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);

            String sha1 = TorrentSHA1ConversionUtils.toHexString(btData.getInfoHash());
            return torrents.containsKey(sha1);
        } catch (IOException e) {
            // TODO do somthing intelligent here. Returning true will make hte
            // file not try to resume, maybe a better method name would be
            // fine.
            throw new RuntimeException(e);
        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }
    }

    private class FastResumeAlertCallback implements AlertCallback {

        @Override
        public void callback(LibTorrentAlert alert) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }
            String sha1 = alert.sha1;

            if (!StringUtils.isEmpty(sha1)) {
                try {
                    lock.readLock().lock();
                    Torrent torrent = torrents.get(sha1);
                    if (torrent != null) {
                        // TODO null check could be eating errors
                        torrent.alert(alert);
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }
        }
    }
}
