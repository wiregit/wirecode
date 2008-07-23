package org.limewire.core.impl.download;


import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SearchResultDownloader;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class CoreGlueDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // All this crazy singleton binding is necessary for hierarchical injectors not
        // to fail -- but shouldn't be absolutely required.
        bind(CoreDownloadListManager.class).in(Scopes.SINGLETON);
        bind(DownloadListManager.class).to(CoreDownloadListManager.class).in(Scopes.SINGLETON);
        bind(SearchResultDownloader.class).to(CoreDownloadListManager.class).in(Scopes.SINGLETON);
    }

}
