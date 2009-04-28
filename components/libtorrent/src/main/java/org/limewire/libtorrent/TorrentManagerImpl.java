package org.limewire.libtorrent;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TorrentManagerImpl implements TorrentManager {
    private final LibTorrent libTorrent;

    private final List<String> torrents;

    private final File torrentDownloadFolder;

    private final SourcedEventMulticaster<LibTorrentEvent, String> listeners;

    private EventPoller eventPoller;

    @Inject
    public TorrentManagerImpl(@TorrentDownloadFolder File torrentDownloadFolder) {
        this.torrentDownloadFolder = torrentDownloadFolder;
        this.libTorrent = new LibTorrentWrapper();
        this.torrents = new CopyOnWriteArrayList<String>();
        this.listeners = new SourcedEventMulticasterImpl<LibTorrentEvent, String>();
    }

    @Override
    public void addListener(String id, EventListener<LibTorrentEvent> listener) {
        listeners.addListener(id, listener);
    }
    
    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    @Override
    public LibTorrentInfo addTorrent(File torrent) {
        synchronized (eventPoller) {
            LibTorrentInfo info = libTorrent.add_torrent(torrent.getAbsolutePath());
            String id = info.sha1;
            torrents.add(id);
            updateStatus(id);
            return info;
        }
    }

    @Override
    public void removeTorrent(String id) {
        synchronized (eventPoller) {
            torrents.remove(id);
            libTorrent.remove_torrent(id);
            listeners.removeListeners(id);
        }
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
        return libTorrent.get_torrent_status(id);
    }

    private void updateStatus(String id) {
        LibTorrentStatus torrentStatus = libTorrent.get_torrent_status(id);
        // TODO broadcast asynchronously
        listeners.broadcast(new LibTorrentEvent(id, null, torrentStatus));
    }

    @Override
    public File getTorrentDownloadFolder() {
        return torrentDownloadFolder;
    }

    @Override
    public boolean moveTorrent(String id, File directory) {
        boolean moved = libTorrent.move_torrent(id, directory.getAbsolutePath());
        updateStatus(id);
        return moved;
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
        libTorrent.initialize();
        libTorrent.init(torrentDownloadFolder.getAbsolutePath());
        this.eventPoller = new EventPoller();
        eventPoller.setName("Libtorrent Event Poller");
    }

    @Override
    public void start() {
        eventPoller.start();
    }

    @Override
    public void stop() {
        eventPoller.interrupt();
        libTorrent.abort_torrents();
        for(String id : torrents) {
            listeners.removeListeners(id);
        }
        torrents.clear();
    }
    
    

    private class EventPoller extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (this) {
                    try {
                        pumpStatus();
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }

        private void pumpStatus() {
            for (String id : torrents) {
                updateStatus(id);
            }
        }
    }
}
