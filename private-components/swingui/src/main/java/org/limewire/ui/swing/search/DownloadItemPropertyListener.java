package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public class DownloadItemPropertyListener implements PropertyChangeListener {
    
    /** Reference to visual search result.  We use a WeakReference to allow
     *  the result to be cleared if the search is closed. 
     */
    private final WeakReference<VisualSearchResult> vsrReference;

    public DownloadItemPropertyListener(VisualSearchResult vsr) {
        this.vsrReference = new WeakReference<VisualSearchResult>(vsr);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            DownloadState state = (DownloadState) evt.getNewValue();
            switch (state) {
            case CANCELLED:
                setDownloadState(BasicDownloadState.NOT_STARTED);
                break;
            case DONE:
                setDownloadState(BasicDownloadState.DOWNLOADED);
                break;
            case ERROR:
            case TRYING_AGAIN:
            case CONNECTING:
            case LOCAL_QUEUED:
            case PAUSED:
            case REMOTE_QUEUED:
            case STALLED:
            case FINISHING:
            case DOWNLOADING:
                setDownloadState(BasicDownloadState.DOWNLOADING);
                break;
            }
        }
    }

    /**
     * Sets the download state in the visual search result.
     */
    private void setDownloadState(BasicDownloadState downloadState) {
        VisualSearchResult vsr = vsrReference.get();
        if (vsr != null) {
            vsr.setDownloadState(downloadState);
        }
    }
}
