package org.limewire.core.impl.bittorrent;

import java.io.File;
import java.util.List;

import org.limewire.bittorrent.TorrentIpPort;
import org.limewire.bittorrent.TorrentManagerSettings;

public class MockTorrentManagerSettings implements TorrentManagerSettings {

    private boolean reportLibraryLoadFailure = false;

    @Override
    public int getMaxDownloadBandwidth() {
        return 0;
    }

    @Override
    public int getMaxUploadBandwidth() {
        return 0;
    }

    @Override
    public boolean isTorrentsEnabled() {
        return true;
    }

    @Override
    public boolean isReportingLibraryLoadFailture() {
        return reportLibraryLoadFailure;
    }

    @Override
    public int getListenEndPort() {
        return 0;
    }

    @Override
    public int getListenStartPort() {
        return 0;
    }

    @Override
    public float getSeedRatioLimit() {
        return 0;
    }

    @Override
    public int getSeedTimeLimit() {
        return 0;
    }

    @Override
    public float getSeedTimeRatioLimit() {
        return 0;
    }

    @Override
    public int getActiveDownloadsLimit() {
        return 0;
    }

    @Override
    public int getActiveLimit() {
        return 0;
    }

    @Override
    public int getActiveSeedsLimit() {
        return 0;
    }

    @Override
    public int getAlertMask() {
        return 0;
    }

    @Override
    public List<TorrentIpPort> getBootStrapDHTRouters() {
        return null;
    }

    @Override
    public File getDHTStateFile() {
        return null;
    }

    @Override
    public String getListenInterface() {
        return null;
    }
}
