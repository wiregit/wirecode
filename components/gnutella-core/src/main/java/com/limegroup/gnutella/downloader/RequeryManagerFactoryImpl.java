package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;

@Singleton
public class RequeryManagerFactoryImpl implements RequeryManagerFactory {
    
    private final DownloadManager downloadManager;
    private final AltLocFinder altLocFinder;
    private final DHTManager dhtManager;

    @Inject
    public RequeryManagerFactoryImpl(DownloadManager downloadManager,
            AltLocFinder altLocFinder, DHTManager dhtManager) {
        this.downloadManager = downloadManager;
        this.altLocFinder = altLocFinder;
        this.dhtManager = dhtManager;
    }    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.RequeryManagerFactory#createRequeryManager(com.limegroup.gnutella.downloader.ManagedDownloader)
     */
    public RequeryManager createRequeryManager(ManagedDownloader managedDownloader) {
        return new RequeryManager(managedDownloader, downloadManager, altLocFinder, dhtManager);
    }
}
