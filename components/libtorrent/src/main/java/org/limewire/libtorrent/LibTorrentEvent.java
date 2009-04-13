package org.limewire.libtorrent;

public class LibTorrentEvent {

    private final LibTorrentAlert alert;

    private final LibTorrentStatus torrentStatus;

    public LibTorrentEvent(LibTorrentAlert alert, LibTorrentStatus torrentStatus) {
        this.alert = alert;
        this.torrentStatus = torrentStatus;
    }

    public LibTorrentAlert getAlert() {
        return alert;
    }

    public LibTorrentStatus getTorrentStatus() {
        return torrentStatus;
    }
}
