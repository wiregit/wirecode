package com.limegroup.bittorrent;

import java.io.File;

import org.limewire.core.settings.SharingSettings;
import org.limewire.libtorrent.TorrentManager;
import org.limewire.libtorrent.TorrentManagerImpl;

import com.limegroup.bittorrent.metadata.TorrentMetaReader;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class LimeWireBittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        // bound eagerly so it registers itself with MetaDataFactory
        bind(TorrentMetaReader.class).asEagerSingleton();
         bind(File.class).annotatedWith(Names.named("TorrentDownloadFolder")).toProvider(new Provider<File>() {
           @Override
            public File get() {
                return SharingSettings.INCOMPLETE_DIRECTORY.get();
            } 
        });
        bind(TorrentManager.class).to(TorrentManagerImpl.class);
    }
    
    

}
