package org.limewire.xmpp.api.client;

public interface FriendRequestDecisionHandler {
    public void handleDecision(String friendJID, boolean accepted);
}
