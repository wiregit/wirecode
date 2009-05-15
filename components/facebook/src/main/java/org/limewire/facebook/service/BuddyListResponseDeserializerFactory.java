package org.limewire.facebook.service;

import org.limewire.core.api.friend.Network;

public interface BuddyListResponseDeserializerFactory {
    BuddyListResponseDeserializer create(Network network);
}
