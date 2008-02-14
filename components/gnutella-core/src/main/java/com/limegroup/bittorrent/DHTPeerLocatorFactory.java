package com.limegroup.bittorrent;


public interface DHTPeerLocatorFactory {

    public DHTPeerLocator createDHTPeerLocator(ManagedTorrent torrent, BTMetaInfo torrentMeta);
}
