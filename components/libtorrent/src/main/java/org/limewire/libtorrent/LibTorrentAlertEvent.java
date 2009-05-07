package org.limewire.libtorrent;

import org.limewire.listener.SourcedEvent;

public class LibTorrentAlertEvent implements SourcedEvent<String> {

    private final LibTorrentAlert alert;

    private final String source;

    public LibTorrentAlertEvent(String source, LibTorrentAlert alert) {
        this.source = source;
        this.alert = alert;
    }

    public LibTorrentAlert getAlert() {
        return alert;
    }

    @Override
    public String getSource() {
        return source;
    }
}
