package com.limegroup.bittorrent;

import org.limewire.libtorrent.Torrent;

public interface BTUploaderFactory {

    public abstract BTUploader createBTUploader(Torrent torrent);

}