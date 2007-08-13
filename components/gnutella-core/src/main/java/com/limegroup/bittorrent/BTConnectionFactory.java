package com.limegroup.bittorrent;

public interface BTConnectionFactory {
    public BTConnection createBTConnection(TorrentContext context, TorrentLocation loc);
}
