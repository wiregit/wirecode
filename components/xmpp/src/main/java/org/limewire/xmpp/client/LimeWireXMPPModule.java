package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.XMPPServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;

public class LimeWireXMPPModule extends AbstractModule {
    
    protected void configure() {
        bind(XMPPServiceImpl.class).in(Scopes.SINGLETON);
        bind(XMPPService.class).to(XMPPServiceImpl.class).in(Scopes.SINGLETON);
    }

    public Provider<FileOfferHandler> getNoOpFileAcceptor() {
        return new Provider<FileOfferHandler>() {
            public FileOfferHandler get() {
                return new FileOfferHandler() {
                    public void register(XMPPService xmppService) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void fileOfferred(FileMetaData f) {
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
