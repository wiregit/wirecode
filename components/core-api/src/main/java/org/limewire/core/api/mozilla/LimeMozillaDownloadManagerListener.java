package org.limewire.core.api.mozilla;

import org.limewire.core.impl.mozilla.LimeMozillaDownloadProgressListener;
import org.mozilla.interfaces.nsIDownloadProgressListener;


/**
 * This is an example of listening to a mozilla download and putting this info
 * into the downloader list for the limewire client.
 */
public interface LimeMozillaDownloadManagerListener extends nsIDownloadProgressListener {

    void remove(LimeMozillaDownloadProgressListener limeMozillaDownloadProgressListener);

}