package org.limewire.ui.swing.library.nav;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryNavModule extends AbstractModule {
    
    @Override
    protected void configure() {
        EventMulticaster<FriendSelectEvent> friendSelectionMulticaster = new EventMulticasterImpl<FriendSelectEvent>();
        bind(new TypeLiteral<ListenerSupport<FriendSelectEvent>>(){}).toInstance(friendSelectionMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendSelectEvent>>(){}).toInstance(friendSelectionMulticaster);
       
        bind(NavPanelFactory.class).toProvider(FactoryProvider.newFactory(NavPanelFactory.class, NavPanel.class));
        bind(LibraryNavigator.class).to(LibraryNavigatorImpl.class);
    }
}
