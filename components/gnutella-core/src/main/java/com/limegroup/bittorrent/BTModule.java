package com.limegroup.bittorrent;

import com.google.inject.AbstractModule;
import com.limegroup.bittorrent.metadata.TorrentMetaReaderFactory;

public class BTModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TorrentMetaReaderFactory.class).to(TorrentMetaReaderFactory.class).asEagerSingleton();
    }

}
