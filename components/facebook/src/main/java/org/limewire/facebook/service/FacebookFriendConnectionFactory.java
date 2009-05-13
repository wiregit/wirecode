package org.limewire.facebook.service;

import org.limewire.core.api.friend.client.FriendConnectionConfiguration;

public interface FacebookFriendConnectionFactory {
    FacebookFriendConnection create(FriendConnectionConfiguration configuration);    
}
