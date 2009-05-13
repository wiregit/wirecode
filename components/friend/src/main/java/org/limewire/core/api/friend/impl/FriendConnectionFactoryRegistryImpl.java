package org.limewire.core.api.friend.impl;

import java.util.HashMap;
import java.util.Map;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.xmpp.api.client.XMPPConnection;

import com.google.inject.Singleton;

@Singleton
public class FriendConnectionFactoryRegistryImpl implements FriendConnectionFactoryRegistry, FriendConnectionFactory {
     private final Map<Network.Type, FriendConnectionFactory> factories =
             new HashMap<Network.Type, FriendConnectionFactory>();

    @Override
    public void register(Network.Type type, FriendConnectionFactory factory) {
        factories.put(type, factory);
    }

    @Override
    public ListeningFuture<XMPPConnection> login(FriendConnectionConfiguration configuration) {
        FriendConnectionFactory factory = factories.get(configuration.getType());
        if(factory != null) {
            return factory.login(configuration);
        } else {
            return null; // TODO
        }
    }

    @Override
    public void register(FriendConnectionFactoryRegistry registry) {
        // TODO throw?
    }
}
