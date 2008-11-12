package org.limewire.core.impl;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.version.UpdateInformation;

@Singleton
class GlueActivityCallback implements ActivityCallback, QueryReplyListenerList,
        DownloadListenerList {

    private final SortedMap<byte[], List<QueryReplyListener>> queryReplyListeners;

    private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<DownloadListener>();

    private final DownloadManager downloadManager;
    
    @Inject
    public GlueActivityCallback(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        queryReplyListeners = new ConcurrentSkipListMap<byte[], List<QueryReplyListener>>(
                GUID.GUID_BYTE_COMPARATOR);
    }

    @Override
    public void addQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<QueryReplyListener>();
                queryReplyListeners.put(guid, listeners);
            }
            listeners.add(listener);
        }
    }

    @Override
    public void removeQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    queryReplyListeners.remove(guid);
                }
            }
        }
    }

    @Override
    public void addDownloadListener(DownloadListener listener) {
        downloadListeners.add(listener);
    }

    @Override
    public void removeDownloadListener(DownloadListener listener) {
        downloadListeners.remove(listener);
    }

    @Override
    public void acceptChat(InstantMessenger ctr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void acceptedIncomingChanged(boolean status) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addUpload(Uploader u) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void chatErrorMessage(InstantMessenger chatter, String str) {
        // TODO Auto-generated method stub

    }

    @Override
    public void chatUnavailable(InstantMessenger chatter) {
        // TODO Auto-generated method stub
    }

    public void handleAddressStateChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean handleDAAPConnectionError(Throwable t) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMagnets(MagnetOptions[] magnets) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        List<QueryReplyListener> listeners = queryReplyListeners.get(queryReply.getGUID());
        if (listeners != null) {
            for (QueryReplyListener listener : listeners) {
                listener.handleQueryReply(rfd, queryReply, locs);
            }
        }
    }

    @Override
    public void handleQueryString(String query) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleSharedFileUpdate(File file) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleTorrent(File torrentFile) {
        try {
            downloadManager.downloadTorrent(torrentFile, true);
        } catch (SaveLocationException e) {
          throw new UnsupportedOperationException("give user feedback", e);
        }
    }

    @Override
    public void installationCorrupted() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isQueryAlive(GUID guid) {
        return queryReplyListeners.containsKey(guid.bytes());
    }

    @Override
    public void receiveMessage(InstantMessenger chr, String messsage) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeUpload(Uploader u) {
        // TODO Auto-generated method stub

    }

    @Override
    public void restoreApplication() {
        // TODO Auto-generated method stub

    }

    @Override
    public String translate(String s) {
        return s; // TODO: Plug this to the UI somehow.
    }

    @Override
    public void updateAvailable(UpdateInformation info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadsComplete() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean warnAboutSharingSensitiveDirectory(File dir) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addDownload(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadAdded(d);
        }
    }
    
    @Override
    public void downloadsComplete() {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void promptAboutCorruptDownload(Downloader dloader) {
        //just kill the download if it is corrupt
        dloader.discardCorruptDownload(true);

    }
    
    @Override
    public void removeDownload(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadRemoved(d);
        }
    }
    
    @Override
    public void showDownloads() {
        // TODO Auto-generated method stub
    }

}
