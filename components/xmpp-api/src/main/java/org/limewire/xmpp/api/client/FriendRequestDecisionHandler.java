package org.limewire.xmpp.api.client;

public interface FriendRequestDecisionHandler {
    public void handleDecision(String friendUsername, boolean accepted);
}
