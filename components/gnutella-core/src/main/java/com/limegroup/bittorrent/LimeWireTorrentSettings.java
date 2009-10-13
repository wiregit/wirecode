package com.limegroup.bittorrent;

import java.io.File;

import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.libtorrent.LibTorrentAlert;

/**
 * Implements the TorrentSetting interface with limewire specific settings. 
 */
class LimeWireTorrentSettings implements TorrentManagerSettings {
    @Override
    public int getMaxDownloadBandwidth() {
        if (!DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue()) {
            return 0;
        }
        return DownloadSettings.MAX_DOWNLOAD_SPEED.getValue();
    }

    @Override
    public int getMaxUploadBandwidth() {
        if (!UploadSettings.LIMIT_MAX_UPLOAD_SPEED.getValue()) {
            return 0;
        }
        return UploadSettings.MAX_UPLOAD_SPEED.getValue();
    }

    @Override
    public File getTorrentDownloadFolder() {
        return SharingSettings.INCOMPLETE_DIRECTORY.get();
    }

    @Override
    public boolean isTorrentsEnabled() {
        return BittorrentSettings.LIBTORRENT_ENABLED.getValue();
    }

    @Override
    public boolean isReportingLibraryLoadFailture() {
        return BittorrentSettings.LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE.getValue();
    }

    @Override
    public int getListenStartPort() {
        return BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue();
    }

    @Override
    public int getListenEndPort() {
        return BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue();
    }

    @Override
    public float getSeedRatioLimit() {

        if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
            // fake unlimited value for using the
            return Float.MAX_VALUE;
        }
        return BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue();
    }

    @Override
    public int getSeedTimeLimit() {
        if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
            // fake unlimited value for using the
            return Integer.MAX_VALUE;
        }
        return BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue();
    }

    @Override
    public float getSeedTimeRatioLimit() {
        return BittorrentSettings.LIBTORRENT_SEED_TIME_RATIO_LIMIT.getValue();
    }

    @Override
    public int getActiveDownloadsLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT.getValue();
    }

    @Override
    public int getActiveLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_LIMIT.getValue();
    }

    @Override
    public int getActiveSeedsLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_SEEDS_LIMIT.getValue();
    }

    @Override
    public int getMaxSeedingLimit() {
        if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
            return Integer.MAX_VALUE;
        }
        return BittorrentSettings.TORRENT_SEEDING_LIMIT.getValue();
    }

    @Override
    public int getAlertMask() {
        return LibTorrentAlert.storage_notification | LibTorrentAlert.progress_notification
                | LibTorrentAlert.status_notification;
    }

    @Override
    public File getTorrentUploadsFolder() {
        return BittorrentSettings.TORRENT_UPLOADS_FOLDER.get();
    }

    @Override
    public boolean isUPNPEnabled() {
        return BittorrentSettings.TORRENT_USE_UPNP.getValue();
    }
}