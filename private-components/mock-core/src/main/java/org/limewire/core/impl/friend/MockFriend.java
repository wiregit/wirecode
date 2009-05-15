package org.limewire.core.impl.friend;

import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.listener.EventListener;

public class MockFriend implements Friend {
    
    private String localID;
    private boolean anonymous;

    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this(localID, true);
    }
    
    public MockFriend(String localID, boolean anonymous) {
        this.localID = localID;
        this.anonymous = anonymous;
    }
    
    @Override
    public String getId() {
        return localID;
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRenderName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public Network getNetwork() {
        return new Network() {
            @Override
            public String getCanonicalizedLocalID() {
                return localID;
            }

            @Override
            public String getNetworkName() {
                return "";
            }

            @Override
            public Type getType() {
                return Type.XMPP;
            }
        };
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFirstName() {
        return null;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return null;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }
}
