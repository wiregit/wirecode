package com.limegroup.bittorrent;

import java.io.File;

import org.limewire.core.settings.SharingSettings;
import org.limewire.libtorrent.TorrentManager;
import org.limewire.libtorrent.TorrentManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.metadata.TorrentMetaReader;

public class LimeWireBittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        // bound eagerly so it registers itself with MetaDataFactory
        bind(TorrentMetaReader.class).asEagerSingleton();
        bind(File.class).annotatedWith(Names.named("TorrentDownloadFolder")).toProvider(SharingSettings.INCOMPLETE_DIRECTORY);
        bind(TorrentManager.class).to(TorrentManagerImpl.class);
    }
    
    

}
