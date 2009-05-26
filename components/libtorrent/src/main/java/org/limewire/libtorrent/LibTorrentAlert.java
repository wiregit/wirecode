package org.limewire.libtorrent;

import com.sun.jna.Structure;

/**
 * Structure mapping to the wrapper_alert_info structure in the
 * libtorrentwrapper library.
 */
public class LibTorrentAlert extends Structure {

    /**
     * Category of this alert
     */
    public int category;

    /**
     * Sha1 of this alert, null or empty if not an alert on a specific torrent.
     */
    public String sha1;

    /**
     * Message associated with this alert.
     */
    public String message;

    /**
     * Additional data associated with this alert. In the case of a
     * saveResumeDataAlert it is the path to the newly save fastresume file.
     */
    public String data;

    /**
     * Category number of a SAVE_RESUME_DATE_ALERT
     */
    public final static int SAVE_RESUME_DATA_ALERT = 8;

    @Override
    public String toString() {
        return sha1 + " " + message + " [" + category + "] " + data;
    }
}
