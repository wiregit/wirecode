package org.limewire.libtorrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

public class TorrentManager {
    private final LibTorrent libTorrent;

    private final Map<String, EventListenerList<LibTorrentEvent>> listeners;

    private final List<String> torrents;

    private EventPoller eventPoller;

    // TODO use SourcedEventMulticaster

    public TorrentManager() {
        this.libTorrent = new LibTorrentWrapper();
        this.listeners = new ConcurrentHashMap<String, EventListenerList<LibTorrentEvent>>();
        this.torrents = new ArrayList<String>();
        // TODO init torrent manager elsewhere.
        init();
    }

    public void init() {
        // TODO this location can change, so need to be able to update it.
        libTorrent.init("/home/pvertenten/Desktop");
        this.eventPoller = new EventPoller();
        eventPoller.setName("Libtorrent Event Poller");
        eventPoller.start();
    }

    public void addListener(String id, EventListener<LibTorrentEvent> listener) {
        synchronized (listeners) {
            EventListenerList<LibTorrentEvent> listenerList = listeners.get(id);
            if (listenerList == null) {
                listenerList = new EventListenerList<LibTorrentEvent>();
                listeners.put(id, listenerList);
            }
            listenerList.addListener(listener);
        }
    }

    public LibTorrentInfo addTorrent(File torrent) {
        synchronized (eventPoller) {
            LibTorrentInfo info = libTorrent.add_torrent(torrent.getAbsolutePath());
            String id = info.sha1;
            torrents.add(id);
            return info;
        }
    }

    public void removeTorrent(String id) {
        synchronized (eventPoller) {
            torrents.remove(id);
            libTorrent.remove_torrent(id);
            listeners.remove(id);
        }
    }

    public void pauseTorrent(String id) {
        libTorrent.pause_torrent(id);
    }

    public void resumeTorrent(String id) {
        libTorrent.resume_torrent(id);
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
                LibTorrentStatus torrentStatus = libTorrent.get_torrent_status(id);
                EventListenerList<LibTorrentEvent> listenerList = listeners.get(id);
                if (listenerList != null) {
                    // TODO asynchronous broadcast
                    listenerList.broadcast(new LibTorrentEvent(null, torrentStatus));
                }
            }
        }
    }

}
