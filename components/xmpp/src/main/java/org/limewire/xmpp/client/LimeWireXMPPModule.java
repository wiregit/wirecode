package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.client.impl.XMPPServiceImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPModule extends AbstractModule {
    
    protected void configure() {
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        EventMulticaster<RosterEvent> rosterMulticaster = new EventMulticasterImpl<RosterEvent>(); 
        bind(new TypeLiteral<EventListener<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);
    }

    public Provider<FileOfferHandler> getNoOpFileAcceptor() {
        return new Provider<FileOfferHandler>() {
            public FileOfferHandler get() {
                return new FileOfferHandler() {
                    public void register(XMPPService xmppService) {
                        
                    }

                    public void fileOfferred(FileMetaData f, String fromJID) {
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
