package org.limewire.core.impl.mozilla;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.mozilla.LimeMozillaDownloadProgressListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;
import org.mozilla.xpcom.XPCOMException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;

/**
 * Provides a means of tracking what downloads have listeners already, and
 * adding listeners for those downloads which need them.
 */
@Singleton
public class LimeMozillaDownloadManagerListenerImpl implements
        org.limewire.core.api.mozilla.LimeMozillaDownloadManagerListener,
        nsIDownloadProgressListener {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadManagerListenerImpl.class);

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final Map<Long, LimeMozillaDownloadProgressListener> listeners;

    private final DownloadManager downloadManager;

    @Inject
    public LimeMozillaDownloadManagerListenerImpl(DownloadManager downloadManager) {
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.listeners = new HashMap<Long, LimeMozillaDownloadProgressListener>();
        synchronized (this) {
            addMissingDownloads();
            resumeDownloads();
        }
    }

    private synchronized void resumeDownloads() {
        nsIDownloadManager downloadManager = getDownloadManager();
        nsISimpleEnumerator enumerator = downloadManager.getActiveDownloads();
        while (enumerator.hasMoreElements()) {
            nsISupports elem = enumerator.getNext();
            nsIDownload download = XPCOMUtils.proxy(elem, nsIDownload.class);
            long downloadId = download.getId();
            try {
                downloadManager.resumeDownload(downloadId);
            } catch (XPCOMException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
    }

    private synchronized void addMissingDownloads() {
        nsIDownloadManager downloadManager = getDownloadManager();
        nsISimpleEnumerator enumerator = downloadManager.getActiveDownloads();
        while (enumerator.hasMoreElements()) {
            nsISupports elem = enumerator.getNext();
            nsIDownload download = XPCOMUtils.proxy(elem, nsIDownload.class);
            addListener(download, nsIDownloadManager.DOWNLOAD_QUEUED);
        }
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(NS_IDOWNLOADMANAGER_CID,
                nsIDownloadManager.class);
        return downloadManager;
    }

    @Override
    public void onDownloadStateChange(short state, nsIDownload download) {
        addMissingDownloads();
    }

    private synchronized boolean addListener(nsIDownload download, short state) {
        long downloadId = download.getId();
        LimeMozillaDownloadProgressListener listener = listeners.get(downloadId);
        if (listener == null) {
            LimeMozillaDownloadProgressListenerImpl listenerImpl = new LimeMozillaDownloadProgressListenerImpl(
                    this, download, state);
            listeners.put(download.getId(), listenerImpl);
            getDownloadManager().addListener(listenerImpl);
            downloadManager.downloadFromMozilla(listenerImpl);
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
        addMissingDownloads();
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

    @Override
    public synchronized void remove(
            LimeMozillaDownloadProgressListener limeMozillaDownloadProgressListener) {
        listeners.remove(limeMozillaDownloadProgressListener);
    }

}