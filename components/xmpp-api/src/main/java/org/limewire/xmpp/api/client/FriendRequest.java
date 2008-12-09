package org.limewire.xmpp.api.client;

public class FriendRequest {

    public enum EventType {REQUESTED}

    private final String friendUsername;
    private final FriendRequestDecisionHandler decisionHandler;

    public FriendRequest(String friendUsername,
            FriendRequestDecisionHandler decisionHandler) {
        this.friendUsername = friendUsername;
        this.decisionHandler = decisionHandler;
    }

    public String getFriendUsername() {
        return friendUsername;
    }
    
    public FriendRequestDecisionHandler getDecisionHandler() {
        return decisionHandler;
    }
}
