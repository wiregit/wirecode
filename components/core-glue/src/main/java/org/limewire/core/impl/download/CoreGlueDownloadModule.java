package org.limewire.core.impl.download;


import org.limewire.core.api.download.DownloadManager;

import com.google.inject.AbstractModule;

public class CoreGlueDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(DownloadManager.class).to(CoreDownloadManager.class);
    }

}
