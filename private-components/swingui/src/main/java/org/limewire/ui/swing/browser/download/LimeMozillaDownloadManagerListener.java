/**
 * 
 */
package org.limewire.ui.swing.browser.download;

import java.util.Map;
import java.util.WeakHashMap;

import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;

import com.limegroup.gnutella.DownloadServices;

/**
 * This is an example of listening to a mozilla download and putting this info
 * into the downloader list for the limewire client.
 * 
 * TODO remove this class if we decide we will not need to use mozilla to download files.
 */
class LimeMozillaDownloadManagerListener implements nsIDownloadProgressListener {

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final Map<Long, LimeMozillaDownloadProgressListener> listeners;

    private final DownloadServices downloadServices;

    public LimeMozillaDownloadManagerListener(DownloadServices downloadServices) {
        this.listeners = new WeakHashMap<Long, LimeMozillaDownloadProgressListener>();
       this.downloadServices = downloadServices;
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(NS_IDOWNLOADMANAGER_CID,
                nsIDownloadManager.class);
        return downloadManager;
    }

    @Override
    public void onDownloadStateChange(short state, nsIDownload download) {
        addListener(download, state);
    }

    private synchronized boolean addListener(nsIDownload download, short state) {
        LimeMozillaDownloadProgressListener listener = listeners.get(download.getId());
        if (listener == null) {
            listener = new LimeMozillaDownloadProgressListener(download, state);
            listeners.put(download.getId(), listener);
            getDownloadManager().addListener(listener);
            // TODO would need to add back these methods for the download
            // manager.
            // downloadManager.downloadFromMozilla(listener);
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
            long maxTotalProgress, nsIDownload download) {
        // nothing to do for this listener
    }

    @Override
    public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state,
            nsIDownload download) {
        // don't care about this event.
    }

    @Override
    public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
            long status, nsIDownload download) {
        // no longer used by mozilla api
    }

    @Override
    public void setDocument(nsIDOMDocument document) {
        // no mozilla window to use
    }

    @Override
    public nsIDOMDocument getDocument() {
        // no mozilla window to use
        return null;
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }

}