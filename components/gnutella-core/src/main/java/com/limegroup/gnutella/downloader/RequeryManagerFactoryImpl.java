package com.limegroup.gnutella.downloader;

import org.limewire.activation.api.ActivationManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht2.DHTManagerImpl;

@Singleton
public class RequeryManagerFactoryImpl implements RequeryManagerFactory {
    
    private final Provider<DownloadManager> downloadManager;
    private final Provider<AltLocFinder> altLocFinder;
    private final Provider<DHTManagerImpl> dhtManager;
    private final ConnectionServices connectionServices;
    private final ActivationManager activationManager;

    @Inject
    public RequeryManagerFactoryImpl(Provider<DownloadManager> downloadManager,
            Provider<AltLocFinder> altLocFinder,
            Provider<DHTManagerImpl> dhtManager,
            ConnectionServices connectionServices,
            ActivationManager activationManager) {
        this.downloadManager = downloadManager;
        this.altLocFinder = altLocFinder;
        this.dhtManager = dhtManager;
        this.connectionServices = connectionServices;
        this.activationManager = activationManager;
    }    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.RequeryManagerFactory#createRequeryManager(com.limegroup.gnutella.downloader.ManagedDownloader)
     */
    public RequeryManager createRequeryManager(
            RequeryListener requeryListener) {
        return new RequeryManager(requeryListener, downloadManager.get(),
                altLocFinder.get(), dhtManager.get(), connectionServices,
                activationManager);
    }
}
