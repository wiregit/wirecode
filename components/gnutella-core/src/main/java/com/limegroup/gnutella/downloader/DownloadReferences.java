package com.limegroup.gnutella.downloader;

import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequestFactory;

public class DownloadReferences {
    
    private final DownloadManager downloadManager;
    private final FileManager fileManager;
    private final DownloadCallback downloadCallback;
    private final NetworkManager networkManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final QueryRequestFactory queryRequestFactory;
    private final Provider<OnDemandUnicaster> onDemandUnicaster; // DPINJ: Convert to non-provider when onDemandUnicaster's constructor doesn't schedule!
    private final DownloadWorkerFactory downloadWorkerFactory;
    private final ManagedTorrentFactory managedTorrentFactory;
    
    public DownloadReferences(DownloadManager downloadManager,
            FileManager fileManager, DownloadCallback downloadCallback,
            NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            QueryRequestFactory queryRequestFactory,
            Provider<OnDemandUnicaster> onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory,
            ManagedTorrentFactory managedTorrentFactory) {
        this.downloadManager = downloadManager;
        this.fileManager = fileManager;
        this.downloadCallback = downloadCallback;
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.queryRequestFactory = queryRequestFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.downloadWorkerFactory = downloadWorkerFactory;
        this.managedTorrentFactory = managedTorrentFactory;
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

    public Provider<OnDemandUnicaster> getOnDemandUnicaster() {
        return onDemandUnicaster;
    }
    
    public DownloadWorkerFactory getDownloadWorkerFactory() {
        return downloadWorkerFactory;
    }
    
    public ManagedTorrentFactory getManagedTorrentFactory() {
        return managedTorrentFactory;
    }
    
}