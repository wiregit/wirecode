package com.limegroup.bittorrent.tracking;

import java.net.URI;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;

public interface TrackerFactory {

    public Tracker create(URI uri, TorrentContext context,
            ManagedTorrent torrent);

}