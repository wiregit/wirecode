package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPService;

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
    }

}
