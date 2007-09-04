package com.limegroup.gnutella.downloader;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.version.DownloadInformation;
import com.limegroup.store.StoreDescriptor;

@Singleton
public class GnutellaDownloaderFactoryImpl implements GnutellaDownloaderFactory {

    private final Provider<SaveLocationManager> saveLocationManager;

    @Inject
    public GnutellaDownloaderFactoryImpl(Provider<SaveLocationManager> saveLocationManager) {
        this.saveLocationManager = saveLocationManager;
    }
    
    public ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, IncompleteFileManager ifc,
            GUID originalQueryGUID, File saveDirectory, String fileName,
            boolean overwrite) throws SaveLocationException {
        return new ManagedDownloader(files, ifc, originalQueryGUID,
                saveDirectory, fileName, overwrite, saveLocationManager.get());
    }

    public ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, IncompleteFileManager ifc,
            GUID originalQueryGUID) {
        return new ManagedDownloader(files, ifc, originalQueryGUID, saveLocationManager.get());
    }

    public MagnetDownloader createMagnetDownloader(
            IncompleteFileManager ifm, MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException {
        return new MagnetDownloader(ifm, magnet, overwrite, saveDir, fileName, saveLocationManager.get());
    }

    public InNetworkDownloader createInNetworkDownloader(
            IncompleteFileManager incompleteFileManager,
            DownloadInformation info, File dir, long startTime)
            throws SaveLocationException {
        return new InNetworkDownloader(incompleteFileManager, info, dir,
                startTime, saveLocationManager.get());
    }

    public ResumeDownloader createResumeDownloader(
            IncompleteFileManager incompleteFileManager, File incompleteFile,
            String name, long size) {
        return new ResumeDownloader(incompleteFileManager, incompleteFile,
                name, size, saveLocationManager.get());
    }

    public StoreDownloader createStoreDownloader(StoreDescriptor store, IncompleteFileManager ifm, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
        return new StoreDownloader(store, ifm, saveDirectory, fileName, 
                overwrite, saveLocationManager.get());
    }

}
