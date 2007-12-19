package com.limegroup.bittorrent;


public interface PeerLocatorFactory {

    public PeerLocator create(ManagedTorrent torrent, BTMetaInfo torrentMeta);
}
