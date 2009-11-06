package com.limegroup.bittorrent;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.libtorrent.TorrentImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

public class LimeWireBittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Torrent.class).to(TorrentImpl.class);
        bind(TorrentManagerSettings.class).annotatedWith(TorrentSettingsAnnotation.class)
                .toProvider(new Provider<TorrentManagerSettings>() {
                    @Override
                    public TorrentManagerSettings get() {
                        return new LimeWireTorrentSettings();
                    }
                });
        bind(TorrentManager.class).to(LimeWireTorrentManager.class);
    }
}
