package org.limewire.core.impl.browser;

import org.limewire.core.api.browser.LoadURLEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class CoreGlueBrowserModule extends AbstractModule {
    @Override
    protected void configure() {
        EventMulticaster<LoadURLEvent> loadURLMulticaster = new EventMulticasterImpl<LoadURLEvent>();
        bind(new TypeLiteral<EventListener<LoadURLEvent>>(){}).toInstance(loadURLMulticaster);
        bind(new TypeLiteral<ListenerSupport<LoadURLEvent>>(){}).toInstance(loadURLMulticaster);
    }
}
