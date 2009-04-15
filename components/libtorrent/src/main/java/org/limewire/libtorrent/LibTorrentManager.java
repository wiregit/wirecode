package org.limewire.libtorrent;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

public class LibTorrentManager {
    private final LibTorrent libTorrent;

    private final Map<String, EventListenerList<LibTorrentEvent>> listeners;

    // TODO use SourcedEventMulticaster

    public LibTorrentManager() {
        this.libTorrent = new LibTorrentWrapper();
        this.listeners = new ConcurrentHashMap<String, EventListenerList<LibTorrentEvent>>();
        // TODO init torrent manager elsewhere.
        init();
    }

    public void init() {
        // TODO this location can change, so need to be able to update it.
        libTorrent.init("/home/pvertenten/Desktop");
        EventPoller eventPoller = new EventPoller();
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
        LibTorrentInfo info = libTorrent.add_torrent(torrent.getAbsolutePath());
        return info;
    }

    public void removeTorrent(String id) {
        // TODO
        pauseTorrent(id);
        listeners.remove(id);
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
                libTorrent.get_alerts(new AlertCallback() {
                    @Override
                    public void callback(LibTorrentAlert alert, LibTorrentStatus torrentStatus) {
                        alert.read();
                        String sha1 = alert.sha1;
                        if (sha1 != null) {
                            EventListenerList<LibTorrentEvent> listenerList = listeners
                                    .get(alert.sha1);
                            if (listenerList != null) {
                                // TODO asynchronous broadcast
                                listenerList.broadcast(new LibTorrentEvent(alert, torrentStatus));
                            }
                        }
                    }
                });
            }
        }
    }

}
