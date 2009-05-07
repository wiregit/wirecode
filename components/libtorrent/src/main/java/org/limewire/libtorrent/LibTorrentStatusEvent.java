package org.limewire.libtorrent;

import org.limewire.listener.SourcedEvent;

public class LibTorrentStatusEvent implements SourcedEvent<String> {

    private final LibTorrentStatus torrentStatus;

    private final String source;

    public LibTorrentStatusEvent(String source, LibTorrentStatus torrentStatus) {
        this.source = source;
        this.torrentStatus = torrentStatus;
    }

    public LibTorrentStatus getTorrentStatus() {
        return torrentStatus;
    }

    @Override
    public String getSource() {
        return source;
    }
}
