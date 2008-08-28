package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.impl.browse.CoreGlueBrowseModule;
import org.limewire.core.impl.download.CoreGlueDownloadModule;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.library.CoreGlueLibraryModule;
import org.limewire.core.impl.player.CoreGluePlayerModule;
import org.limewire.core.impl.search.CoreGlueSearchModule;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.xmpp.CoreGlueXMPPModule;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.ActivityCallback;

public class CoreGlueModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ActivityCallback.class).to(GlueActivityCallback.class);
        bind(QueryReplyListenerList.class).to(GlueActivityCallback.class);
        bind(DownloadListenerList.class).to(GlueActivityCallback.class);
        bind(Application.class).to(ApplicationImpl.class);
        
        install(new CoreGlueSearchModule());
        install(new CoreGlueDownloadModule());
        install(new CoreGlueLibraryModule());
        install(new CoreGlueBrowseModule());
        install(new CoreGlueXMPPModule());
        install(new CoreGluePlayerModule());
    }

}
