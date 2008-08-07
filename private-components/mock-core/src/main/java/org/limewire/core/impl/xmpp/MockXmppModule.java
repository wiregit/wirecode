package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.AbstractModule;

public class MockXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XMPPService.class).to(MockXmppService.class);
    }

}
