package org.limewire.core.settings;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.util.CommonUtils;

/**
 * BitTorrent settings.
 */
public class BittorrentSettings extends LimeProps {

    private BittorrentSettings() {
        // empty constructor
    }

    public static final BooleanSetting LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE = FACTORY
            .createRemoteBooleanSetting("LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE", false,
                    "libtorrent.reportLibraryLoadFailure");

    public static final BooleanSetting LIBTORRENT_ENABLED = FACTORY.createBooleanSetting(
            "LIBTORRENT_ENABLED", true);

    // TODO integrate with DownloadSettings.DOWNLOAD_SPEED
    public static final IntSetting LIBTORRENT_DOWNLOAD_SPEED = FACTORY.createIntSetting(
            "LIBTORRENT_DOWNLOAD_SPEED", 100);

    // TODO integrate with DownloadSettings.UPLOAD_SPEED
    public static final IntSetting LIBTORRENT_UPLOAD_SPEED = FACTORY.createIntSetting(
            "LIBTORRENT_UPLOAD_SPEED", 100);
    
    public static final IntSetting LIBTORRENT_LISTEN_START_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_START_PORT", 6881);
    
    public static final IntSetting LIBTORRENT_LISTEN_END_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_END_PORT", 6889);
    
    public static FileSetting LIBTORRENT_UPLOADS_FOLDER = FACTORY.createFileSetting("LIBTORRENT_UPLOADS_FOLDER", new File(CommonUtils.getUserSettingsDir(), "uploads.dat/"));
    
    
}
