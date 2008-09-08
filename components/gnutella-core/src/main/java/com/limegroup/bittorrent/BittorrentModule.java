package com.limegroup.bittorrent;

import com.google.inject.AbstractModule;
import com.limegroup.bittorrent.metadata.TorrentMetaReaderFactory;

public class BittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        // bound eagerly so it registers itself with MetaDataFactory
        bind(TorrentMetaReaderFactory.class).asEagerSingleton();
    }

}
