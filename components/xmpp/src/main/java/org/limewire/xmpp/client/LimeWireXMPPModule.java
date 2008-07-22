package org.limewire.xmpp.client;

import java.util.List;

import org.limewire.xmpp.client.impl.XMPPServiceImpl;
import org.limewire.xmpp.client.service.FileTransferProgressListener;
import org.limewire.xmpp.client.service.IncomingFileAcceptor;
import org.limewire.xmpp.client.service.LibraryProvider;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPModule extends AbstractModule {
    private final Provider<LibraryProvider> libraryProvider;
    private final Provider<FileTransferProgressListener> progressListener;
    private final Provider<IncomingFileAcceptor> fileAcceptor;
    private final Provider<List<XMPPConnectionConfiguration>> configurations;

    public LimeWireXMPPModule(Provider<List<XMPPConnectionConfiguration>> configurations,
                              Provider<LibraryProvider> libraryProvider,
                              Provider<FileTransferProgressListener> progressListener,
                              Provider<IncomingFileAcceptor> fileAcceptor) {
        this.libraryProvider = libraryProvider;
        this.progressListener = progressListener;
        this.fileAcceptor = fileAcceptor;
        this.configurations = configurations;
    }
    
    protected void configure() {
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(configurations);
        bind(LibraryProvider.class).toProvider(libraryProvider);
        bind(IncomingFileAcceptor.class).toProvider(fileAcceptor);
        bind(FileTransferProgressListener.class).toProvider(progressListener);
    }
}
