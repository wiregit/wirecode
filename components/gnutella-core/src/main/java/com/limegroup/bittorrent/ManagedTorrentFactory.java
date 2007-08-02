package com.limegroup.bittorrent;

public interface ManagedTorrentFactory {

    public ManagedTorrent create(TorrentContext context);

}