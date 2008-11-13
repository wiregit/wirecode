package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.lifecycle.MockLifeCycleModule;
import org.limewire.core.impl.browse.MockBrowseModule;
import org.limewire.core.impl.connection.MockConnectionManagerImpl;
import org.limewire.core.impl.daap.MockDaapModule;
import org.limewire.core.impl.download.MockDownloadModule;
import org.limewire.core.impl.library.MockLibraryModule;
import org.limewire.core.impl.mojito.MockMojitoModule;
import org.limewire.core.impl.network.MockNetworkModule;
import org.limewire.core.impl.player.MockPlayerModule;
import org.limewire.core.impl.search.MockSearchModule;
import org.limewire.core.impl.spam.MockSpamModule;
import org.limewire.core.impl.support.MockSupportModule;
import org.limewire.core.impl.xmpp.MockXmppModule;

import com.google.inject.AbstractModule;

public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Application.class).to(MockApplication.class);
        bind(GnutellaConnectionManager.class).to(MockConnectionManagerImpl.class);

        install(new MockLifeCycleModule());
        install(new MockDaapModule());
        install(new MockSpamModule());
        install(new MockSearchModule());
        install(new MockNetworkModule());
        install(new MockDownloadModule());
        install(new MockLibraryModule());
        install(new MockMojitoModule());
        install(new MockBrowseModule());
        install(new MockPlayerModule());
        install(new MockXmppModule());
        install(new MockSupportModule());
    }

}
