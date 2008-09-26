package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPErrorListener;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendRemover.class).to(FriendsPane.class);
        bind(IconLibrary.class).to(IconLibraryImpl.class);
        bind(ChatLoginState.class).to(ChatLoginStateImpl.class);
        bind(ConversationPaneFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ConversationPaneFactory.class, ConversationPane.class));
        bind(FriendsPaneRosterListener.class);
        bind(XMPPErrorListener.class).to(LoginPanel.class);
        bind(FileOfferHandlerImpl.class);
        bind(XMPPConnectionListener.class).to(XMPPConnectionListenerImpl.class);
    }
}
