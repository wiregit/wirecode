package org.limewire.ui.swing.search.model;

import org.limewire.core.api.download.DownloadState;

/**
 * This differs from the DownloadState enum
 * which doesn't have a value equivalent to NOT_STARTED and
 * has many values that represent states between DOWNLOADING and DOWNLOADED.
 * @author R. Mark Volkmann
 */
public enum BasicDownloadState {
    NOT_STARTED,
    DOWNLOADING,
    DOWNLOADED,
    LIBRARY,
    
    ;
    
    public static BasicDownloadState fromState(DownloadState state) {
        switch (state) {
        case CANCELLED:
            return BasicDownloadState.NOT_STARTED;
        case DONE:
        case SCAN_FAILED:
        case SCAN_FAILED_DOWNLOADING_DEFINITIONS:
            return BasicDownloadState.DOWNLOADED;
        case ERROR:
        case TRYING_AGAIN:
        case CONNECTING:
        case LOCAL_QUEUED:
        case PAUSED:
        case REMOTE_QUEUED:
        case STALLED:
        case FINISHING:
        case RESUMING:
        case DOWNLOADING:
        case DANGEROUS:
        case SCANNING:
        case SCANNING_FRAGMENT:
        case THREAT_FOUND:
            return BasicDownloadState.DOWNLOADING;
        default:
            return null;
        }
    }
}