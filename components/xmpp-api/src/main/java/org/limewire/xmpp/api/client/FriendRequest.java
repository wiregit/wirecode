package org.limewire.xmpp.api.client;

public class FriendRequest {

    public enum EventType {REQUESTED}

    private final String friendUsername;
    private final String friendJID;
    private final FriendRequestDecisionHandler decisionHandler;

    public FriendRequest(String friendUsername, String friendJID,
            FriendRequestDecisionHandler decisionHandler) {
        this.friendUsername = friendUsername;
        this.friendJID = friendJID;
        this.decisionHandler = decisionHandler;
    }

    public String getFriendUsername() {
        return friendUsername;
    }
    
    public String getFriendJID() {
        return friendJID;
    }
    
    public FriendRequestDecisionHandler getDecisionHandler() {
        return decisionHandler;
    }
}
