package com.limegroup.bittorrent.tracking;

import java.net.URI;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;

public interface TrackerFactory {

    /**
     * Creates a instance of a Tracker for a torrent
     * @param uri URI of the tracker
     * @param context context of the torrent
     * @param torrent a ManagedTorrent instance of the torrent
     * @return
     */
    public Tracker create(URI uri, TorrentContext context,
            ManagedTorrent torrent);

}