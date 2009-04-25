package org.limewire.libtorrent;

import org.limewire.listener.SourcedEvent;

public class LibTorrentEvent implements SourcedEvent<String> {

    private final LibTorrentAlert alert;

    private final LibTorrentStatus torrentStatus;

    private final String source;

    public LibTorrentEvent(String source, LibTorrentAlert alert, LibTorrentStatus torrentStatus) {
        this.source = source;
        this.alert = alert;
        this.torrentStatus = torrentStatus;
    }

    public LibTorrentAlert getAlert() {
        return alert;
    }

    public LibTorrentStatus getTorrentStatus() {
        return torrentStatus;
    }

    @Override
    public String getSource() {
        return source;
    }
}
