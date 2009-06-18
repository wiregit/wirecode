package org.limewire.ui.swing.library.sharing;

import java.util.Collections;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.listener.EventListener;

public class SharingTarget {
    
    private final Friend friend;
    
    public final static SharingTarget GNUTELLA_SHARE = new SharingTarget(new Gnutella());

    public SharingTarget(Friend friend){
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((friend == null) ? 0 : friend.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        
        if (!(obj instanceof SharingTarget)) {            
                return false;
        }

        return getFriend().getId().equals(((SharingTarget)obj).getFriend().getId());
    }
    
    public boolean isGnutellaNetwork(){
        return this == GNUTELLA_SHARE;
    }
    
    @Override
    public String toString(){
        return getFriend().getRenderName();
    }

    private static class Gnutella implements Friend {
        @Override
        public boolean isAnonymous() {
            return true;
        }
        
        @Override
        public String getId() {
            return Friend.P2P_FRIEND_ID;
        }

        @Override
        public String getName() {
            return I18n.tr("P2P Network");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }

        public void setName(String name) {
            
        }

        @Override
        public Network getNetwork() {
            return null;
        }

        @Override
        public String getFirstName() {
            return getName();
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
            return Collections.emptyMap();
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }
    }

}
