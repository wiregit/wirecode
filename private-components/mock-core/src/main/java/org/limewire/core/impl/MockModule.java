package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.api.connection.ConnectionManager;
import org.limewire.core.api.lifecycle.MockLifeCycleModule;
import org.limewire.core.impl.browse.MockBrowseModule;
import org.limewire.core.impl.connection.MockConnectionManagerImpl;
import org.limewire.core.impl.download.MockDownloadModule;
import org.limewire.core.impl.library.MockLibraryModule;
import org.limewire.core.impl.player.MockPlayerModule;
import org.limewire.core.impl.search.MockSearchModule;
import org.limewire.core.impl.spam.MockSpamModule;
import org.limewire.core.impl.xmpp.MockXmppModule;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.connection.Connection;


public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Application.class).to(MockApplication.class);
        bind(ConnectionManager.class).to(MockConnectionManagerImpl.class);
        install(new MockLifeCycleModule());
        install(new MockSpamModule());
        install(new MockSearchModule());
        install(new MockDownloadModule());
        install(new MockLibraryModule());
        install(new MockBrowseModule());
        install(new MockPlayerModule());
        install(new MockXmppModule());
        
    }

}
