package org.limewire.bittorrent;

import java.util.Collections;
import java.util.List;

public class TorrentInfo {
    private List<TorrentFileEntry> fileEntries;

    public List<TorrentFileEntry> getTorrentFileEntries() {
        if (fileEntries == null) {
            return Collections.emptyList();
        }
        return fileEntries;
    }

    public void setTorrentFileEntries(List<TorrentFileEntry> fileEntries) {
        this.fileEntries = fileEntries;
    }
}
