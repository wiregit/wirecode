package org.limewire.core.impl;

import org.limewire.core.impl.download.CoreGlueDownloadModule;
import org.limewire.core.impl.search.CoreGlueSearchModule;
import org.limewire.core.impl.search.QueryReplyListenerList;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.ActivityCallback;

public class CoreGlueModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ActivityCallback.class).to(GlueActivityCallback.class);
        bind(QueryReplyListenerList.class).to(GlueActivityCallback.class);
        
        install(new CoreGlueSearchModule());
        install(new CoreGlueDownloadModule());
    }

}
