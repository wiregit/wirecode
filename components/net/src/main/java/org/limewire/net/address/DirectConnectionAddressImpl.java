package org.limewire.net.address;

import java.net.UnknownHostException;

import org.limewire.io.ConnectableImpl;
import org.limewire.io.Connectable;

public class DirectConnectionAddressImpl extends ConnectableImpl implements DirectConnectionAddress {

    public DirectConnectionAddressImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
        super(host, port, tlsCapable);
    }

    public DirectConnectionAddressImpl(Connectable connectable) {
        super(connectable);
    }

    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof DirectConnectionAddress)) {
            return false;    
        }
        DirectConnectionAddress address2 = (DirectConnectionAddress)obj;
        // TODO push up - but what are the side affects,
        // TODO especially for existing callers.
        return isEqualOrNull(getAddress(), address2.getAddress()) && getPort() == address2.getPort() && isTLSCapable() == address2.isTLSCapable();
    }

    private boolean isEqualOrNull(String s, String s2) {
        if(s == null && s2 == null) {
            return true;
        }
        if(s == null || s2 == null) {
            return false;
        }
        return s.equals(s2);
    }

    public int hashCode() {
        // TODO push up - but what are the side affects,
        // TODO especially for existing callers.
        int hash = 7;
        hash = hash * 31 + getAddress() == null ? 0 : getAddress().hashCode();
        hash = hash * 31 + getPort();
        hash = hash * 31 + (isTLSCapable() ? 1 : 0);
        return hash;
    }
}
