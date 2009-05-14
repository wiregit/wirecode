package org.limewire.libtorrent;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ManagedThread;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

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
    
    private final List<String> torrents;

    private final SourcedEventMulticaster<LibTorrentStatusEvent, String> statusListeners;
    private final SourcedEventMulticaster<LibTorrentAlertEvent, String> alertListeners;

    private final EventPoller eventPoller;

    private final AlertCallback alertCallback = new FastResumeAlertCallback();
    
    @Inject
    public TorrentManagerImpl(@TorrentDownloadFolder File torrentDownloadFolder,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        
        this.torrentDownloadFolder = torrentDownloadFolder;
        this.backgroundExecutor = backgroundExecutor;
        
        this.libTorrent = new LibTorrentWrapper();
        this.torrents = new CopyOnWriteArrayList<String>();
        this.statusListeners = new SourcedEventMulticasterImpl<LibTorrentStatusEvent, String>();
        this.alertListeners = new SourcedEventMulticasterImpl<LibTorrentAlertEvent, String>();
        this.eventPoller = new EventPoller();
    }

    @Override
    public void addStatusListener(String id, EventListener<LibTorrentStatusEvent> listener) {
        statusListeners.addListener(id, listener);
    }
    
    @Override
    public void addAlertListener(String id, EventListener<LibTorrentAlertEvent> listener) {
        alertListeners.addListener(id, listener);
    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    @Override
    public List<String> getPeers(String id) {
        return libTorrent.get_peers(id);
    }
    
    @Override
    public void addTorrent(String sha1, File torrent, File fastResumeFile) {
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        synchronized (eventPoller) {
            libTorrent.add_torrent(torrent.getAbsolutePath(), fastResumePath);
            torrents.add(sha1);
            updateStatus(sha1);
        }
    }
    
    @Override
    public void addTorrent(String sha1, String trackerURI, File fastResumeFile) {
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        synchronized (eventPoller) {
            libTorrent.add_torrent_existing(sha1, trackerURI, fastResumePath);
            torrents.add(sha1);
            updateStatus(sha1);
        }
    }

    @Override
    public void removeTorrent(final String id) {
        synchronized (eventPoller) {
            torrents.remove(id);
        }

        new ManagedThread("TorrentManagerImpl.removeTorrent : " + id) {
            @Override
            public void run() {
                synchronized (eventPoller) {
                    libTorrent.remove_torrent(id);
                    statusListeners.removeListeners(id);
                    alertListeners.removeListeners(id);
                }
            }
        }.start();

    }

    @Override
    public void pauseTorrent(String id) {
        libTorrent.pause_torrent(id);
        updateStatus(id);
    }

    @Override
    public void resumeTorrent(String id) {
        libTorrent.resume_torrent(id);
        updateStatus(id);
    }

    @Override
    public LibTorrentStatus getStatus(String id) {
        LibTorrentStatus status = new LibTorrentStatus();
        
        libTorrent.get_torrent_status(id, status);
        LibTorrentStatus statusCopy = new LibTorrentStatus(status);
        libTorrent.free_torrent_status(status);
        return statusCopy;
    }

    private void updateStatus(String id) {
        LibTorrentStatus torrentStatus = getStatus(id);
        // TODO broadcast asynchronously
        statusListeners.broadcast(new LibTorrentStatusEvent(id, torrentStatus));
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
        //TODO what if path changes.
    }

    @Override
    public void start() {
        backgroundExecutor.scheduleAtFixedRate(eventPoller, 1000, 50, TimeUnit.MILLISECONDS);

        if (PERIODICALLY_SAVE_FAST_RESUME_DATA) {
            backgroundExecutor.scheduleAtFixedRate(new ResumeDataScheduler(), 6000, 6000,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        libTorrent.freeze_and_save_all_fast_resume_data(alertCallback);

        libTorrent.abort_torrents();
        for (String id : torrents) {
            statusListeners.removeListeners(id);
        }
        
        torrents.clear();
    }

    private class EventPoller implements Runnable {

        @Override
        public void run() {
            synchronized (this) {
                pumpStatus();

                libTorrent.get_alerts(alertCallback);
            }
        }

        private void pumpStatus() {
            for (String id : torrents) {
                updateStatus(id);
            }
        }
    }

    private class ResumeDataScheduler implements Runnable {

        private Iterator<String> torrentIterator = torrents.iterator();

        @Override
        public void run() {
            synchronized (eventPoller) {
                if (!torrentIterator.hasNext()) {
                    torrentIterator = torrents.iterator();
                    if (!torrentIterator.hasNext()) {
                        return;
                    }
                }

                libTorrent.signal_fast_resume_data_request(torrentIterator.next());
            }
        }
    }

    @Override
    public void free(LibTorrentStatus oldStatus) {
        if (oldStatus != null) {
            libTorrent.free_torrent_status(oldStatus);
        }
    }
    
    private class FastResumeAlertCallback implements AlertCallback {  
    
        @Override
        public void callback(LibTorrentAlert alert) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }
            if (alert.sha1 != null) {
                alertListeners.broadcast(new LibTorrentAlertEvent(alert.sha1, alert));
            }
        }
    }
}
