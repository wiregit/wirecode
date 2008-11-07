/**
 * 
 */
package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public class DownloadItemPropertyListener implements PropertyChangeListener {
    private final VisualSearchResult vsr;

    private final boolean preExistingDownload;

    public DownloadItemPropertyListener(VisualSearchResult vsr, boolean preExistingDownload) {
        this.vsr = vsr;
        this.preExistingDownload = preExistingDownload;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            DownloadState state = (DownloadState) evt.getNewValue();
            switch (state) {
            case CANCELLED:
                // TODO, this listener is not properly getting the cancel state
                // change.
                // find out why.
                vsr.setDownloadState(BasicDownloadState.NOT_STARTED);
                break;
            case DONE:
                if (preExistingDownload) {
                    vsr.setDownloadState(BasicDownloadState.LIBRARY);
                } else {
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADED);
                }
                break;
            case ERROR:
            case CONNECTING:
            case LOCAL_QUEUED:
            case PAUSED:
            case REMOTE_QUEUED:
            case STALLED:
            case FINISHING:
            case DOWNLOADING:
                if (preExistingDownload) {
                    vsr.setDownloadState(BasicDownloadState.PRE_EXISTING_DOWNLOADING);
                } else {
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                }
                break;
            }
        }
    }
}