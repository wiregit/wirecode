package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ProviderHacks;

// DPINJ: Remove!!
public class DownloadProviderHacks {
    
    public static DownloadReferences createDownloadReferences(DownloadManager downloadManager,
            FileManager fileManager, DownloadCallback downloadCallback) {
        return new DownloadReferences(downloadManager, fileManager,
                downloadCallback, ProviderHacks.getNetworkManager(),
                ProviderHacks.getAlternateLocationFactory(), ProviderHacks
                        .getQueryRequestFactory(),
                ProviderHacks.getOnDemandUnicaster(), 
                ProviderHacks.getDownloadWorkerFactory(),
                ProviderHacks.getManagedTorrentFactory(), 
                ProviderHacks.getAltLocManager(),
                ProviderHacks.getContentManager(),
                ProviderHacks.getSourceRankerFactory(),
                ProviderHacks.getUrnCache(),
                ProviderHacks.getSavedFileManager(),
                ProviderHacks.getVerifyingFileFactory(),
                ProviderHacks.getDiskController(),
                ProviderHacks.getIpFilter(),
                ProviderHacks.getRequeryManagerFactory()
                );
    }

}
