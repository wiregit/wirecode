package org.limewire.core.impl;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.limegroup.gnutella.ActivityCallback;
import org.limewire.core.api.Application;
import org.limewire.core.impl.download.CoreGlueDownloadModule;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.library.CoreGlueLibraryModule;
import org.limewire.core.impl.search.CoreGlueSearchModule;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.xmpp.CoreGlueXMPPModule;

public class CoreGlueModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(GlueActivityCallback.class).in(Scopes.SINGLETON);
        bind(ActivityCallback.class).to(GlueActivityCallback.class);
        bind(QueryReplyListenerList.class).to(GlueActivityCallback.class);
        bind(DownloadListenerList.class).to(GlueActivityCallback.class);
        bind(Application.class).to(ApplicationImpl.class).in(Scopes.SINGLETON);
        
        install(new CoreGlueSearchModule());
        install(new CoreGlueDownloadModule());
        install(new CoreGlueLibraryModule());
        install(new CoreGlueXMPPModule());
    }

}
