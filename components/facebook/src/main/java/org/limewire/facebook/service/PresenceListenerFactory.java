package org.limewire.facebook.service;

public interface PresenceListenerFactory {
    PresenceListener createPresenceListener(String postFormID, FacebookFriendConnection connection);
}
