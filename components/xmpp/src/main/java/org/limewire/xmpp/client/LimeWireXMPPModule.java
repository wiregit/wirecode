package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.client.impl.XMPPServiceImpl;
import org.limewire.xmpp.client.service.FileMetaData;
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
    
    public LimeWireXMPPModule() {
        fileAcceptor = getNoOpFileAcceptor();
        configurations = getEmptyConfigList();
    }
    
    protected void configure() {
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        //bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(configurations);
        //bind(FileOfferHandler.class).toProvider(fileAcceptor);
    }

    public Provider<FileOfferHandler> getNoOpFileAcceptor() {
        return new Provider<FileOfferHandler>() {
            public FileOfferHandler get() {
                return new FileOfferHandler() {
                    public boolean fileOfferred(FileMetaData f) {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                };
            }
        };
    }

    public Provider<List<XMPPConnectionConfiguration>> getEmptyConfigList() {
        return new Provider<List<XMPPConnectionConfiguration>>() {

            public List<XMPPConnectionConfiguration> get() {
                return new ArrayList<XMPPConnectionConfiguration>();
            }
        };
    }
}
