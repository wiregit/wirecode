package com.limegroup.gnutella.downloader;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.util.SocketsManager;

@Singleton
public class DownloadWorkerFactoryImpl implements DownloadWorkerFactory {
    
    private final HTTPDownloaderFactory httpDownloaderFactory;
    private final ScheduledExecutorService backgroundExecutor;
    private final ScheduledExecutorService nioExecutor;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final SocketsManager socketsManager;
    
    @Inject
    public DownloadWorkerFactoryImpl(
            HTTPDownloaderFactory httpDownloaderFactory,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            @Named("nioExecutor") ScheduledExecutorService nioExecutor,
            Provider<PushDownloadManager> pushDownloadManager,
            SocketsManager socketsManager) {
        this.httpDownloaderFactory = httpDownloaderFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.nioExecutor = nioExecutor;
        this.pushDownloadManager = pushDownloadManager;
        this.socketsManager = socketsManager;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.DownloadWorkerFactory#create(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile)
     */
    public DownloadWorker create(ManagedDownloader manager,
            RemoteFileDesc rfd, VerifyingFile vf) {
        return new DownloadWorker(manager, rfd, vf, httpDownloaderFactory,
                backgroundExecutor, nioExecutor, pushDownloadManager,
                socketsManager);
    }

}
