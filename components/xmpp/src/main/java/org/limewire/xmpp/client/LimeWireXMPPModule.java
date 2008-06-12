package org.limewire.xmpp.client;

import com.google.inject.AbstractModule;

public class LimeWireXMPPModule extends AbstractModule {
    protected void configure() {
        bind(XMPPService.class);
    }
}
