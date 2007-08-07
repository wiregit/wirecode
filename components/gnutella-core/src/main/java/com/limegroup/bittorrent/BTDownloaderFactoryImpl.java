package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BTDownloaderFactoryImpl implements BTDownloaderFactory {
    
    private final BTContextFactory btContextFactory;

    @Inject
    public BTDownloaderFactoryImpl(BTContextFactory btContextFactory) {
        this.btContextFactory = btContextFactory;
    }

    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTDownloaderFactory#createBTDownloader(com.limegroup.bittorrent.BTMetaInfo)
     */
    public BTDownloader createBTDownloader(BTMetaInfo info) {
        return new BTDownloader(info, btContextFactory);
    }
}
