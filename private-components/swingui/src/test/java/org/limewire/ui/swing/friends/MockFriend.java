package org.limewire.ui.swing.friends;

import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.impl.PresenceEvent;
import org.limewire.listener.EventListener;

public class MockFriend implements Friend {
    
    private String localID;
    private boolean anonymous;
    private String name;
    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this(localID, true);
    }
    
    public MockFriend(String localID, boolean anonymous) {
        this.localID = localID;
        this.anonymous = anonymous;
        this.name = localID;
    }
    
    @Override
    public String getId() {
        return localID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
                return Network.Type.XMPP;
            }
        };
    }

    @Override
    public String getFirstName() {
        return name;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }                                                                                      

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeChatListener() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasActivePresence() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSignedIn() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSubscribed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
