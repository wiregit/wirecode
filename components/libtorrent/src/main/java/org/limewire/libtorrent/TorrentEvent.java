package org.limewire.libtorrent;

public class TorrentEvent {
    public static final TorrentEvent STATUS_CHANGED = new TorrentEvent();

    public static final TorrentEvent STOPPED = new TorrentEvent();

    public static final TorrentEvent COMPLETED = new TorrentEvent();

    private TorrentEvent() {
        //public static fields have all data, might change to enum
    };
}
