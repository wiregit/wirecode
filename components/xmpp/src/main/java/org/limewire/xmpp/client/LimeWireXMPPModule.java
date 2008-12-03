package org.limewire.xmpp.client;

import org.limewire.friend.impl.LimeWireFriendXmppModule;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.ConnectRequestSender;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;
import org.limewire.xmpp.client.impl.XMPPAddressSerializer;
import org.limewire.xmpp.client.impl.XMPPAuthenticator;
import org.limewire.xmpp.client.impl.XMPPServiceImpl;
import org.limewire.xmpp.client.impl.XMPPAddressRegistry;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPModule extends AbstractModule {
    
    protected void configure() {
        install(new LimeWireFriendXmppModule());
        
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        bind(ConnectRequestSender.class).to(XMPPServiceImpl.class);

        EventMulticaster<RosterEvent> rosterMulticaster = new EventMulticasterImpl<RosterEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);

        EventMulticaster<XMPPConnectionEvent> connectionMulticaster = new EventMulticasterImpl<XMPPConnectionEvent>();
        bind(new TypeLiteral<EventMulticaster<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<EventBroadcaster<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        
        
        // bind egearly, so it registers itself with SocketsManager
        bind(XMPPAddressResolver.class).asEagerSingleton();
        // dito
        bind(XMPPAddressSerializer.class).asEagerSingleton();
        
        bind(XMPPAuthenticator.class).asEagerSingleton();
        
        bind(XMPPAddressRegistry.class);
    }
}
