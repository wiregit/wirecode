package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;

public class SocketlessHTTPDownloaderFactory implements HTTPDownloaderFactory {
    
    private final NetworkManager networkManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final DownloadManager downloadManager;
    private final CreationTimeCache creationTimeCache;
    private final BandwidthManager bandwidthManager;

    public SocketlessHTTPDownloaderFactory(NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            DownloadManager downloadManager,
            CreationTimeCache creationTimeCache,
            BandwidthManager bandwidthManager) {
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.bandwidthManager = bandwidthManager;
    }

    public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
            VerifyingFile incompleteFile, boolean inNetwork) {
        return new HTTPDownloader(socket, rfd, incompleteFile, inNetwork,
                false, networkManager, alternateLocationFactory,
                downloadManager, creationTimeCache, bandwidthManager);
    }

}
