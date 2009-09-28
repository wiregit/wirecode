package org.limewire.bittorrent;

import java.util.List;

public interface TorrentInfo {
    // TODO maybe we don't want to use the same TorrentFileEntry object here as
    // on the Torrent. It has some additional fields that do not make sense.
    public List<TorrentFileEntry> getTorrentFileEntries();
}
