package org.limewire.core.impl.bittorrent;

import java.io.File;

import org.limewire.bittorrent.TorrentSettings;

public class MockTorrentSettings implements TorrentSettings {

    @Override
    public int getMaxDownloadBandwidth() {
        return 0;
    }

    @Override
    public int getMaxUploadBandwidth() {
        return 0;
    }

    @Override
    public File getTorrentDownloadFolder() {
        return null;
    }

    @Override
    public boolean isTorrentsEnabled() {
        return true;
    }
}
