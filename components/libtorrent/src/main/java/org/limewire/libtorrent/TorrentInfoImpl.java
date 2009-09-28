package org.limewire.libtorrent;

import java.util.Arrays;
import java.util.List;

import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;

public class TorrentInfoImpl implements TorrentInfo {
    private final List<TorrentFileEntry> fileEntries;
    private final LibTorrentInfo libTorrentInfo;

    public TorrentInfoImpl(LibTorrentInfo libTorrentInfo, TorrentFileEntry[] fileEntries) {
        this.fileEntries = Arrays.asList(fileEntries);
        this.libTorrentInfo = libTorrentInfo;
    }

    public List<TorrentFileEntry> getTorrentFileEntries() {
        return fileEntries;
    }
}
