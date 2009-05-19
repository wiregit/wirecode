package org.limewire.libtorrent;

import java.math.BigInteger;

import org.limewire.util.StringUtils;

import com.sun.jna.Structure;

/**
 * Structure used to pass data from the native code regarding a torrents state
 * back to java.
 */
public class LibTorrentStatus extends Structure {

    /**
     * String containing the total amount of the torrent downloaded and
     * verified.
     */
    public String total_done;

    /**
     * String containing the total amount of the torrent downloaded.
     */
    public String total_download;

    /**
     * String containing the total amount of the torrent uploaded.
     */
    public String total_upload;

    /**
     * The current rate of the torrent download in bytes/second
     */
    public float download_rate;

    /**
     * The current rate of the torrent upload in bytes/second
     */
    public float upload_rate;

    /**
     * The number of peers this torrent has.
     */
    public int num_peers;

    /**
     * The number of uplaod peers this torrent has.
     */
    public int num_uploads;

    /**
     * The number of seeds this torrent has.
     */
    public int num_seeds;

    /**
     * The number of connections this torrent has.
     */
    public int num_connections;

    /**
     * The state of this torrent, aligning with LibTorrentState.
     */
    public int state;

    /**
     * The progress of this torretns download, from 0 to 1.0.
     */
    public float progress;

    /**
     * boolean of whether this torrent is paused.
     */
    public int paused;

    /**
     * boolean of whether this torrent is finished.
     */
    public int finished;

    /**
     * boolean of whether this torrent is valid.
     */
    public int valid;

    /**
     * String containing the error message for the torrent. Null/empty if there is no error.
     */
    public String error;

    public LibTorrentStatus() {

    }

    /**
     * Returns the total amount of the torrent downloaded and verified.
     */
    public long getTotalDone() {
        if (total_done == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_done);
            return total.longValue();
        }
    }

    /**
     * Returns the total amount of the torrent downloaded.
     */
    public long getTotalDownload() {
        if (total_download == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_download);
            return total.longValue();
        }
    }

    /**
     * Returns the total amount of the torrent uploaded.
     */
    public long getTotalUpload() {
        if (total_upload == null) {
            return -1;
        } else {
            BigInteger total = new BigInteger(total_upload);
            return total.longValue();
        }
    }

    /**
     * Returns true if the torrent is paused.
     */
    public boolean isPaused() {
        return paused != 0;
    }

    /**
     * Returns true if the torrent is finished.
     */
    public boolean isFinished() {
        return finished != 0;
    }

    /**
     * Returns true if the torrent is in an error state.
     */
    public boolean isError() {
        return !StringUtils.isEmpty(error);
    }

    /**
     * Returns the LibTorrentState for this torrent.
     */
    public LibTorrentState getState() {
        return LibTorrentState.forId(state);
    }
}
