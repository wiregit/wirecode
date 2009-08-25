package org.limewire.core.settings;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.util.CommonUtils;

/**
 * BitTorrent settings.
 */
public class BittorrentSettings extends LimeProps {

    private BittorrentSettings() {
        // empty constructor
    }

    /**
     * Setting for whether or not be want to report issues loading the
     * libtorrent libraries.
     */
    public static final BooleanSetting LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE = FACTORY
            .createRemoteBooleanSetting("LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE", false,
                    "libtorrent.reportLibraryLoadFailure");

    /**
     * Whether or not libtorrent is enabled and we should try loading the
     * libtorrent libraries.
     */
    public static final BooleanSetting LIBTORRENT_ENABLED = FACTORY.createBooleanSetting(
            "LIBTORRENT_ENABLED", true);

    /**
     * The start listening port for listening for bittorrent connections.
     * Libtorrent picks the first available port with in the range of the start
     * to end ports.
     */
    public static final IntSetting LIBTORRENT_LISTEN_START_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_START_PORT", 6881);

    /**
     * The end listening port for listening for bittorrent connections.
     * Libtorrent picks the first available port with in the range of the start
     * to end ports.
     */
    public static final IntSetting LIBTORRENT_LISTEN_END_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_END_PORT", 6889);

    /**
     * The folder where all the upload mementos are saved for torrents.
     */
    public static FileSetting LIBTORRENT_UPLOADS_FOLDER = FACTORY
            .createFileSetting("LIBTORRENT_UPLOADS_FOLDER", new File(CommonUtils
                    .getUserSettingsDir(), "uploads.dat/"));

    /**
     * The target seed ratio for torrents. Torrents which have met this ratio
     * will be removed. This number is also used via the libtorrent queuing
     * algorithm when trying to decide to queue/dequeue automanaged torrents.
     * This limit only effect automanaged torrents.
     * 
     * When the UPLOAD_TORRENTS_FOREVER setting is set to true, no matter what
     * the value of this setting is, it will pass the maximum value to
     * libtorrent.
     */
    public static final FloatSetting LIBTORRENT_SEED_RATIO_LIMIT = FACTORY.createFloatSetting(
            "LIBTORRENT_SEED_RATIO_LIMIT", 2.0f, 0.00f, Float.MAX_VALUE);

    /**
     * The target seed time ratio limit. The amount of time trying to seed over
     * the amount of time it took to download the torrent. This number is also
     * used via the libtorrent queuing algorithm when trying to decide to
     * queue/dequeue automanaged torrents. This limit only effect automanaged
     * torrents.
     */
    public static final FloatSetting LIBTORRENT_SEED_TIME_RATIO_LIMIT = FACTORY.createFloatSetting(
            "LIBTORRENT_SEED_TIME_RATIO_LIMIT", 7f);

    /**
     * The target seed time limit. The amount time the torrent will actively try
     * to be seeded for. This number is also used via the libtorrent queuing
     * algorithm when trying to decide to queue/dequeue automanaged torrents.
     * This limit only effect automanaged torrents, but will take other torrents
     * into account as we.
     * 
     * When the UPLOAD_TORRENTS_FOREVER setting is set to true, no matter what
     * the value of this setting is, it will pass the maximum value to
     * libtorrent.
     */
    public static final IntSetting LIBTORRENT_SEED_TIME_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_SEED_TIME_LIMIT", 60 * 60 * 24, 0, Integer.MAX_VALUE);// 24

    // hours
    // default

    /**
     * The total number of active downloads limit. This number is also used via
     * the libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT", 8);

    /**
     * The total number of active downlaods limit. This number is also used via
     * the libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_SEEDS_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_SEEDS_LIMIT", 5);

    /**
     * The total number of active torrents limit. This number is also used via
     * the libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_LIMIT", 15);

    /**
     * This setting will cause torrents to upload forever, and will not limit
     * how long or to what seed ratio the torrents will seed.
     */
    public static final BooleanSetting UPLOAD_TORRENTS_FOREVER = FACTORY.createBooleanSetting(
            "UPLOAD_TORRENTS_FOREVER", false);
}
