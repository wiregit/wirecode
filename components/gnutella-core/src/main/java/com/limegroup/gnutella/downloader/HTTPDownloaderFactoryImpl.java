package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;

@Singleton
public class HTTPDownloaderFactoryImpl implements HTTPDownloaderFactory {

    private final NetworkManager networkManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final DownloadManager downloadManager;
    private final Provider<CreationTimeCache> creationTimeCache;
    private final BandwidthManager bandwidthManager;
    private final Provider<PushEndpointCache> pushEndpointCache;
    private final PushEndpointFactory pushEndpointFactory;


    /**
     * @param networkManager
     * @param alternateLocationFactory
     * @param downloadManager
     * @param creationTimeCache
     * @param bandwidthManager
     */
    @Inject
    public HTTPDownloaderFactoryImpl(NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            DownloadManager downloadManager,
            Provider<CreationTimeCache> creationTimeCache,
            BandwidthManager bandwidthManager,
            Provider<PushEndpointCache> pushEndpointCache,
            PushEndpointFactory pushEndpointFactory) {
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.bandwidthManager = bandwidthManager;
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.HTTPDownloaderFactory#create(java.net.Socket, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile, boolean)
     */
    public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
            VerifyingFile incompleteFile, boolean inNetwork) {
        return new HTTPDownloader(socket, rfd, incompleteFile, inNetwork, true,
                networkManager, alternateLocationFactory, downloadManager,
                creationTimeCache.get(), bandwidthManager, pushEndpointCache, pushEndpointFactory);
    }


}
