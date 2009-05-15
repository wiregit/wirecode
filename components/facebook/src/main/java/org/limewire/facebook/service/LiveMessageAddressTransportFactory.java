package org.limewire.facebook.service;

public interface LiveMessageAddressTransportFactory {
    LiveMessageAddressTransport create(FacebookFriendConnection connection);
}
