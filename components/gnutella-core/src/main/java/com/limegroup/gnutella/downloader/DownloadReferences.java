package com.limegroup.gnutella.downloader;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Provider;
import com.limegroup.bittorrent.BTContextFactory;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.tigertree.TigerTreeCache;

//DPINJ: get rid of this!  see: CORE-306
public class DownloadReferences {
    
    private final DownloadManager downloadManager;
    private final FileManager fileManager;
    private final DownloadCallback downloadCallback;
    private final NetworkManager networkManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final QueryRequestFactory queryRequestFactory;
    private final OnDemandUnicaster onDemandUnicaster;
    private final DownloadWorkerFactory downloadWorkerFactory;
    private final ManagedTorrentFactory managedTorrentFactory;
    private final AltLocManager altLocManager;
    private final ContentManager contentManager;
    private final SourceRankerFactory sourceRankerFactory;
    private final UrnCache urnCache;
    private final SavedFileManager savedFileManager;
    private final VerifyingFileFactory verifyingFileFactory;
    private final DiskController diskController;
    private final IPFilter ipFilter;
    private final RequeryManagerFactory requeryManagerFactory;
    private final BTContextFactory btContextFactory;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<TigerTreeCache> tigerTreeCache;
    private final Provider<TorrentManager> torrentManager;
    private final BTUploaderFactory btUploaderFactory;
    private final ApplicationServices applicationServices;

    public DownloadReferences(DownloadManager downloadManager,
            FileManager fileManager, DownloadCallback downloadCallback,
            NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            QueryRequestFactory queryRequestFactory,
            OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory,
            ManagedTorrentFactory managedTorrentFactory,
            AltLocManager altLocManager, ContentManager contentManager,
            SourceRankerFactory sourceRankerFactory, UrnCache urnCache,
            SavedFileManager savedFileManager,
            VerifyingFileFactory verifyingFileFactory,
            DiskController diskController, IPFilter ipFilter,
            RequeryManagerFactory requeryManagerFactory,
            BTContextFactory btContextFactory,
            ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<TigerTreeCache> tigerTreeCache,
            Provider<TorrentManager> torrentManager,
            BTUploaderFactory btUploaderFactory,
            ApplicationServices applicationServices) {
        this.downloadManager = downloadManager;
        this.fileManager = fileManager;
        this.downloadCallback = downloadCallback;
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.queryRequestFactory = queryRequestFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.downloadWorkerFactory = downloadWorkerFactory;
        this.managedTorrentFactory = managedTorrentFactory;
        this.altLocManager = altLocManager;
        this.contentManager = contentManager;
        this.sourceRankerFactory = sourceRankerFactory;
        this.urnCache = urnCache;
        this.savedFileManager = savedFileManager;
        this.verifyingFileFactory = verifyingFileFactory;
        this.diskController = diskController;
        this.ipFilter = ipFilter;
        this.requeryManagerFactory = requeryManagerFactory;
        this.btContextFactory = btContextFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.messageRouter = messageRouter;
        this.tigerTreeCache = tigerTreeCache;
        this.torrentManager = torrentManager;
        this.btUploaderFactory = btUploaderFactory;
        this.applicationServices = applicationServices;
    }
    
    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public DownloadCallback getDownloadCallback() {
        return downloadCallback;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public AlternateLocationFactory getAlternateLocationFactory() {
        return alternateLocationFactory;
    }

    public QueryRequestFactory getQueryRequestFactory() {
        return queryRequestFactory;
    }

    public OnDemandUnicaster getOnDemandUnicaster() {
        return onDemandUnicaster;
    }
    
    public DownloadWorkerFactory getDownloadWorkerFactory() {
        return downloadWorkerFactory;
    }
    
    public ManagedTorrentFactory getManagedTorrentFactory() {
        return managedTorrentFactory;
    }

    public RequeryManagerFactory getRequeryManagerFactory() {
        return requeryManagerFactory;
    }

    public AltLocManager getAltLocManager() {
        return altLocManager;
    }

    public ContentManager getContentManager() {
        return contentManager;
    }

    public SourceRankerFactory getSourceRankerFactory() {
        return sourceRankerFactory;
    }

    public UrnCache getUrnCache() {
        return urnCache;
    }

    public SavedFileManager getSavedFileManager() {
        return savedFileManager;
    }

    public VerifyingFileFactory getVerifyingFileFactory() {
        return verifyingFileFactory;
    }

    public DiskController getDiskController() {
        return diskController;
    }

    public IPFilter getIpFilter() {
        return ipFilter;
    }
    
    public BTContextFactory getBTContextFactory() {
        return btContextFactory;
    }

    public ScheduledExecutorService getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public Provider<MessageRouter> getMessageRouter() {
        return messageRouter;
    }

    public Provider<TigerTreeCache> getTigerTreeCache() {
        return tigerTreeCache;
    }
    
    public Provider<TorrentManager> getTorrentManager() {
        return torrentManager;
    }
    
    public BTUploaderFactory getBtUploaderFactory() {
        return btUploaderFactory;
    }
    
    public ApplicationServices getApplicationServices() {
        return applicationServices;
    }
}