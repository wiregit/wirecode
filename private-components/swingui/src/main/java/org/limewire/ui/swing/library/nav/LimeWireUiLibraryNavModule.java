package org.limewire.ui.swing.library.nav;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class LimeWireUiLibraryNavModule extends AbstractModule {
    
    @Override
    protected void configure() {
        EventMulticaster<FriendSelectEvent> friendSelectionMulticaster = new EventMulticasterImpl<FriendSelectEvent>();
        bind(new TypeLiteral<ListenerSupport<FriendSelectEvent>>(){}).annotatedWith(Names.named("friendSelection")).toInstance(friendSelectionMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendSelectEvent>>(){}).annotatedWith(Names.named("friendSelection")).toInstance(friendSelectionMulticaster);
       
        bind(NavPanelFactory.class).to(NavPanelFactoryImpl.class);
        bind(LibraryNavigator.class).to(LibraryNavigatorImpl.class);
    }
}
