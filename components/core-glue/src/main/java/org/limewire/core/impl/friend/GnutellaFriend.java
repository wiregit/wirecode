package org.limewire.core.impl.friend;

import java.net.InetAddress;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.util.Objects;

import com.limegroup.gnutella.PushEndpoint;

class GnutellaFriend implements Friend {

    private final Address address;
    private final FriendPresence presence;
    
    public GnutellaFriend(Address address, FriendPresence presence) {
        this.address = Objects.nonNull(address, "address");
        this.presence = presence;
    }
    
    Address getAddress() {
        return address;
    }
    
    private String describe(Address address) {
        if(address instanceof Connectable || address instanceof PushEndpoint) {
            IpPort ipp = (IpPort)address;
            InetAddress inetAddr = ipp.getInetAddress();
            return inetAddr == null ? ipp.getAddress() : inetAddr.getHostAddress();
        } else {
            return address.getAddressDescription();
        }
    }
    
    @Override
    public String getId() {
        return presence.getPresenceId();
    }

    @Override
    public String getName() {
        return describe(address);
    }

    @Override
    public String getRenderName() {
        return describe(address);
    }

    @Override
    public String toString() {
        return "renderName[" + getRenderName() + "], name[" + getName() + "], id[" + getId() + "]"; 
    }
}
