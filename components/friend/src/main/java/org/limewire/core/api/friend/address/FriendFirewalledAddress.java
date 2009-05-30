package org.limewire.core.api.friend.address;

import org.limewire.io.Address;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.limewire.core.api.friend.address.FriendAddress;

/**
 * A composite of {@link FriendAddress} and {@link FirewalledAddress} to
 * allow connect back requests to be sent over xmpp. 
 */
public class FriendFirewalledAddress implements Address {

    private final FriendAddress friendAddress;
    private final FirewalledAddress resolvedAddress;

    /**
     * Constructs an {@link FriendFirewalledAddress}.
     * @param friendAddress cannot be null
     * @param resolvedAddress cannot be null
     */
    public FriendFirewalledAddress(FriendAddress friendAddress, FirewalledAddress resolvedAddress) {
        this.friendAddress = Objects.nonNull(friendAddress, "xmppAddress");
        this.resolvedAddress = Objects.nonNull(resolvedAddress, "resolvedAddress");
    }

    @Override
    public String getAddressDescription() {
        return friendAddress.getAddressDescription();
    }
    
    /**
     * @return the {@link FriendAddress} 
     */
    public FriendAddress getXmppAddress() {
        return friendAddress;
    }
    
    /**
     * @return the {@link FirewalledAddress} 
     */
    public FirewalledAddress getFirewalledAddress() {
        return resolvedAddress;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FriendFirewalledAddress) {
            FriendFirewalledAddress other = (FriendFirewalledAddress)obj;
            return this.friendAddress.equals(other.friendAddress) && this.resolvedAddress.equals(other.resolvedAddress);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return friendAddress.hashCode() + 31 * resolvedAddress.hashCode();
    }
}
