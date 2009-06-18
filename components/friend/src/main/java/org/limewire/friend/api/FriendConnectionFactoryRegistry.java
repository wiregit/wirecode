package org.limewire.friend.api;


public interface FriendConnectionFactoryRegistry {
    public void register(Network.Type type, FriendConnectionFactory factory);
}
