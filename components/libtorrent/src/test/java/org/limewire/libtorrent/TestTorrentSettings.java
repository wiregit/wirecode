/**
 * 
 */
package org.limewire.libtorrent;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.bittorrent.TorrentIpPort;
import org.limewire.bittorrent.TorrentManagerSettings;

class TestTorrentSettings implements TorrentManagerSettings {

    @Override
    public int getActiveDownloadsLimit() {
        return 10;
    }

    @Override
    public int getActiveLimit() {
        return 20;
    }

    @Override
    public int getActiveSeedsLimit() {
        return 10;
    }

    @Override
    public int getAlertMask() {
        return LibTorrentAlert.all_categories;
    }

    @Override
    public List<TorrentIpPort> getBootStrapDHTRouters() {
        return Collections.emptyList();
    }

    @Override
    public File getDHTStateFile() {
        return null;
    }

    @Override
    public int getListenStartPort() {
        return 6881;
    }

    @Override
    public int getListenEndPort() {
        return 6889;
    }

    @Override
    public int getMaxDownloadBandwidth() {
        return 0; // unlimited
    }

    @Override
    public int getMaxUploadBandwidth() {
        return 0; // unlimited
    }

    @Override
    public float getSeedRatioLimit() {
        return 2;
    }

    @Override
    public int getSeedTimeLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public float getSeedTimeRatioLimit() {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean isReportingLibraryLoadFailture() {
        return false;
    }

    @Override
    public boolean isTorrentsEnabled() {
        return true;
    }

    @Override
    public String getListenInterface() {
        return null;
    }

}