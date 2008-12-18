package org.limewire.xmpp.activity;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireXMPPActivityModule extends AbstractModule {

    @Override
    protected void configure() {
        EventMulticaster<ActivityEvent> activityMulticaster = new EventMulticasterImpl<ActivityEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<ActivityEvent>>(){}).toInstance(activityMulticaster);
        bind(new TypeLiteral<ListenerSupport<ActivityEvent>>(){}).toInstance(activityMulticaster);
    }
}
