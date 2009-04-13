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

    public LibTorrentManager() {
        this.libTorrent = new LibTorrentWrapper();
        this.listeners = new ConcurrentHashMap<String, EventListenerList<LibTorrentEvent>>();
    }

    public void init() {
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

    public String addTorrent(File torrent) {
        String id = libTorrent.add_torrent(torrent.getAbsolutePath());
        return id;
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

                        System.out.println("sha1: " + alert.sha1);
                        System.out.println("category: " + alert.category);
                        System.out.println("message: " + alert.message);

                        System.out.println("total_done_java: " + torrentStatus.total_done);
                        System.out.println("download_rate_java: " + torrentStatus.download_rate);
                        System.out.println("num_peers_java: " + torrentStatus.num_peers);
                        System.out.println("state_java: " + torrentStatus.state + " - "
                                + LibTorrentState.forId(torrentStatus.state));
                        System.out.println("progress_java: " + torrentStatus.progress);
                        System.out.println("paused_java: " + torrentStatus.paused);
                    }
                });
            }
        }
    }

}
