package org.limewire.core.api.friend.client;

import org.limewire.core.api.friend.Network;

public interface FriendConnectionFactoryRegistry {
    public void register(Network.Type type, FriendConnectionFactory factory);
}
