package org.limewire.core.impl.xmpp;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.xmpp.api.client.XMPPConnection;

class MockXmppConnectionFactory implements FriendConnectionFactory {

    @Override
    public ListeningFuture<XMPPConnection> login(FriendConnectionConfiguration configuration) {
        return new SimpleFuture<XMPPConnection>((XMPPConnection)null);    
    }

    @Override
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.XMPP, this);
    }
}
