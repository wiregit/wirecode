package com.limegroup.bittorrent;


public interface DHTPeerLocatorFactory {

    public DHTPeerLocator create(ManagedTorrent torrent, BTMetaInfo torrentMeta);
}
