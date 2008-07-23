package org.limewire.xmpp.client;

import java.util.List;

import org.limewire.xmpp.client.impl.XMPPServiceImpl;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPModule extends AbstractModule {
    private final Provider<FileOfferHandler> fileAcceptor;
    private final Provider<List<XMPPConnectionConfiguration>> configurations;

    public LimeWireXMPPModule(Provider<List<XMPPConnectionConfiguration>> configurations,
                              Provider<FileOfferHandler> fileAcceptor) {
        this.fileAcceptor = fileAcceptor;
        this.configurations = configurations;
    }
    
    protected void configure() {
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(configurations);
        bind(FileOfferHandler.class).toProvider(fileAcceptor);
    }
}
