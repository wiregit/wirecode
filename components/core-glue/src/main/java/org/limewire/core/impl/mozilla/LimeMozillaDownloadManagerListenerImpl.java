package org.limewire.core.impl.mozilla;

import java.util.Map;
import java.util.WeakHashMap;

import org.limewire.util.Objects;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;

/**
 * This is an example of listening to a mozilla download and putting this info
 * into the downloader list for the limewire client.
 */
@Singleton
public class LimeMozillaDownloadManagerListenerImpl implements org.limewire.core.api.mozilla.LimeMozillaDownloadManagerListener {

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final Map<Long, LimeMozillaDownloadProgressListener> listeners;

    private final DownloadManager downloadManager;

    @Inject
    public LimeMozillaDownloadManagerListenerImpl(DownloadManager downloadManager) {
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.listeners = new WeakHashMap<Long, LimeMozillaDownloadProgressListener>();
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
            downloadManager.downloadFromMozilla(listener);
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