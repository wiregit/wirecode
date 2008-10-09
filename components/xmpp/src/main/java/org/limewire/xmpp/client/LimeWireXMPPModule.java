package org.limewire.xmpp.client;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.XMPPServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPModule extends AbstractModule {
    
    protected void configure() {
        bind(XMPPService.class).to(XMPPServiceImpl.class);

        EventMulticaster<RosterEvent> rosterMulticaster = new EventMulticasterImpl<RosterEvent>(); 
        bind(new TypeLiteral<EventListener<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventListener<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventListener<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);

        EventMulticaster<XMPPConnectionEvent> connectionMulticaster = new EventMulticasterImpl<XMPPConnectionEvent>();
        bind(new TypeLiteral<EventListener<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
    }
}
