package org.limewire.core.impl.download;


import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.ResultDownloader;

import com.google.inject.AbstractModule;

public class CoreGlueDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(DownloadListManager.class).to(CoreDownloadListManager.class);
        bind(ResultDownloader.class).to(CoreDownloadListManager.class);
    }

}
