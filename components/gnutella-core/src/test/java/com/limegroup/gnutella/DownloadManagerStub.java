package com.limegroup.gnutella;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.inject.Providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;


public class DownloadManagerStub extends DownloadManager {
    
    // DPINJ: remove me
    public DownloadManagerStub() {
        super(ProviderHacks.getNetworkManager(), ProviderHacks
                .getDownloadReferencesFactory(), ProviderHacks
                .getInNetworkCallback(),
                ProviderHacks.getBTDownloaderFactory(), Providers
                        .of(ProviderHacks.getDownloadCallback()), Providers
                        .of(ProviderHacks.getMessageRouter()), ProviderHacks
                        .getBackgroundExecutor(), Providers.of(ProviderHacks
                        .getTorrentManager()), Providers.of(ProviderHacks
                        .getPushDownloadManager()), ProviderHacks.getBrowseHostHandlerManager(),
                        ProviderHacks.getGnutellaDownloaderFactory());
    }
	
    @Inject
    public DownloadManagerStub(NetworkManager networkManager,
            DownloadReferencesFactory downloadReferencesFactory,
            @Named("inNetwork") DownloadCallback innetworkCallback,
            BTDownloaderFactory btDownloaderFactory,
            Provider<DownloadCallback> downloadCallback,
            Provider<MessageRouter> messageRouter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<TorrentManager> torrentManager,
            Provider<PushDownloadManager> pushDownloadManager,
            BrowseHostHandlerManager browseHostHandlerManager,
            GnutellaDownloaderFactory gnutellaDownloaderFactory) {
        super(networkManager, downloadReferencesFactory, innetworkCallback, btDownloaderFactory, downloadCallback, messageRouter, backgroundExecutor, torrentManager, pushDownloadManager, browseHostHandlerManager, gnutellaDownloaderFactory);
    }

    @Override
    public void initialize() {
        postGuiInit();
    }

    @Override
    public synchronized int downloadsInProgress() { return 0; }
    @Override
    public synchronized boolean readSnapshot(File file) { return false; }
    @Override
    public synchronized boolean writeSnapshot() { return true; }

    /*
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            boolean overwrite) 
            throws FileExistsException, AlreadyDownloadingException, 
				   java.io.FileNotFoundException { 
        throw new AlreadyDownloadingException(); 
    }
    public synchronized Downloader download(
    public synchronized Downloader download(File incompleteFile)
    public synchronized Downloader download(String query,
    */
    
    @Override
    public void handleQueryReply(QueryReply qr) { }
    //public void remove(ManagedDownloader downloader, boolean success) { }
    @Override
    public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) { 

		return !GUID.isLimeRequeryGUID(query.getGUID());
    }
    @Override
    public synchronized void measureBandwidth() { }
    @Override
	public synchronized float getMeasuredBandwidth() { return 0.f; }
    @Override
    public IncompleteFileManager getIncompleteFileManager() { return null; }
        
    public final Object pump = new Object();
    
    @Override
    protected void pumpDownloads() {
        super.pumpDownloads();
        synchronized(pump) {
            pump.notifyAll();
        }
    }
}
