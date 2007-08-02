package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.RemoteFileDesc;

public class DownloadWorkerFactoryImpl implements DownloadWorkerFactory {
    
    private final HTTPDownloaderFactory httpDownloaderFactory;
    
    public DownloadWorkerFactoryImpl(HTTPDownloaderFactory httpDownloaderFactory) {
        this.httpDownloaderFactory = httpDownloaderFactory;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.DownloadWorkerFactory#create(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile)
     */
    public DownloadWorker create(ManagedDownloader manager,
            RemoteFileDesc rfd, VerifyingFile vf) {
        return new DownloadWorker(manager, rfd, vf, httpDownloaderFactory);
    }

}
