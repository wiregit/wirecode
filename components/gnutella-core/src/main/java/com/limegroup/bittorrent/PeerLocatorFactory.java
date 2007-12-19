package com.limegroup.bittorrent;


public interface PeerLocatorFactory {

    public DHTPeerLocator create(ManagedTorrent torrent, BTMetaInfo torrentMeta);
}
