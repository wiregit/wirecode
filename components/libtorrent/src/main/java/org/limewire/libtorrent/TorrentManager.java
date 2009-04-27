package org.limewire.libtorrent;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;

public class TorrentManager {
    private final LibTorrent libTorrent;

    private final List<String> torrents;

    private EventPoller eventPoller;

    private File torrentDownloadFolder = null;

    private final SourcedEventMulticaster<LibTorrentEvent, String> listeners;

    public TorrentManager() {
        this.libTorrent = new LibTorrentWrapper();
        this.torrents = new CopyOnWriteArrayList<String>();
        this.listeners = new SourcedEventMulticasterImpl<LibTorrentEvent, String>();
        // TODO init torrent manager elsewhere.
        libTorrent.print();
        init(SharingSettings.INCOMPLETE_DIRECTORY.getValueAsString());
    }

    public void init(String path) {
        torrentDownloadFolder = new File(path);
        // TODO this location can change, so need to be able to update it.
        libTorrent.init(path);
        this.eventPoller = new EventPoller();
        eventPoller.setName("Libtorrent Event Poller");
        eventPoller.start();
    }

    public void addListener(String id, EventListener<LibTorrentEvent> listener) {
        listeners.addListener(id, listener);
    }

    public LibTorrentInfo addTorrent(File torrent) {
        synchronized (eventPoller) {
            LibTorrentInfo info = libTorrent.add_torrent(torrent.getAbsolutePath());
            String id = info.sha1;
            torrents.add(id);
            updateStatus(id);
            return info;
        }
    }

    public void removeTorrent(String id) {
        synchronized (eventPoller) {
            torrents.remove(id);
            libTorrent.remove_torrent(id);
            listeners.removeListeners(id);
        }
    }

    public void pauseTorrent(String id) {
        libTorrent.pause_torrent(id);
        updateStatus(id);
    }

    public void resumeTorrent(String id) {
        libTorrent.resume_torrent(id);
        updateStatus(id);
    }

    public LibTorrentStatus getStatus(String id) {
        return libTorrent.get_torrent_status(id);
    }

    public boolean isPaused(String id) {
        return libTorrent.is_torrent_paused(id);

    }

    public boolean isFinished(String id) {
        return libTorrent.is_torrent_finished(id);
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

    private void updateStatus(String id) {
        LibTorrentStatus torrentStatus = libTorrent.get_torrent_status(id);
        //TODO broadcast asynchronously
        listeners.broadcast(new LibTorrentEvent(id, null, torrentStatus));
    }

    public File getTorrentDownloadFolder() {
        return torrentDownloadFolder;
    }

    public boolean moveTorrent(String id, File directory) {
        boolean moved = libTorrent.move_torrent(id, directory.getAbsolutePath());
        updateStatus(id);
        return moved;
    }

    public int getNumActiveTorrents() {
       return torrents.size();
    }
}
