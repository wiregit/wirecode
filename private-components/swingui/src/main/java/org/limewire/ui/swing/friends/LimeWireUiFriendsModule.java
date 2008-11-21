package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(IconLibrary.class).to(IconLibraryImpl.class);
        bind(ConversationPaneFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ConversationPaneFactory.class, ConversationPane.class));
        bind(FileOfferHandlerImpl.class);
        bind(XMPPConnectionListenerImpl.class);
        bind(XMPPAccountConfigurationManager.class).to(XMPPAccountConfigurationManagerImpl.class);
    }
}
