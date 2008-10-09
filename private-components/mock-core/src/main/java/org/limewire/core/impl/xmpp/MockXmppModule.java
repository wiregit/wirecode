package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class MockXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XMPPService.class).to(MockXmppService.class);
        //FIXME: Commented out until build configuration that would allow XMPPConnectionConfigurationMock can be sorted out.
//        bind(XMPPConnectionConfiguration.class).to(XMPPConnectionConfigurationMock.class);
        EventMulticaster<RosterEvent> rosterMulticaster = new EventMulticasterImpl<RosterEvent>(); 
        bind(new TypeLiteral<EventListener<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventListener<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<XMPPConnectionEvent> connectionMulticaster = new EventMulticasterImpl<XMPPConnectionEvent>();
        bind(new TypeLiteral<EventListener<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
    }

}
