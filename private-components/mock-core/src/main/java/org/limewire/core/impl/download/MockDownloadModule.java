package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadListManager;

import com.google.inject.AbstractModule;

public class MockDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
       
    	bind(DownloadListManager.class).to(MockDownloadListManager.class);
        
    }

}
