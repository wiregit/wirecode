package org.limewire.core.api.friend.impl;

import java.util.HashMap;
import java.util.Map;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;

import com.google.inject.Singleton;

@Singleton
class FriendConnectionFactoryRegistryImpl implements FriendConnectionFactoryRegistry, FriendConnectionFactory {
     
    private final Map<Network.Type, FriendConnectionFactory> factories =
             new HashMap<Network.Type, FriendConnectionFactory>();

    @Override
    public void register(Network.Type type, FriendConnectionFactory factory) {
        factories.put(type, factory);
    }
    
    private FriendConnectionFactory getFactory(FriendConnectionConfiguration configuration) {
        FriendConnectionFactory factory = factories.get(configuration.getType());
        if (factory != null) {
            return factory;
        }
        throw new IllegalArgumentException("no factory for: " + configuration);
    }

    @Override
    public ListeningFuture<FriendConnection> login(FriendConnectionConfiguration configuration) {
        return getFactory(configuration).login(configuration);
    }

    @Override
    public void register(FriendConnectionFactoryRegistry registry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration) {
        return getFactory(configuration).requestLoginUrl(configuration);
    }

}
