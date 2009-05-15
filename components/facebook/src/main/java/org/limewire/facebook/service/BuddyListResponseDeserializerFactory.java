package org.limewire.facebook.service;

public interface BuddyListResponseDeserializerFactory {
    BuddyListResponseDeserializer create(FacebookFriendConnection connection);
}
