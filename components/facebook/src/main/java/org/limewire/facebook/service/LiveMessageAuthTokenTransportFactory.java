package org.limewire.facebook.service;

public interface LiveMessageAuthTokenTransportFactory {
    LiveMessageAuthTokenTransport create(FacebookFriendConnection connection);
}
