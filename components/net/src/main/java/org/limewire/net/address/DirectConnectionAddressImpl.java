package org.limewire.net.address;

import java.net.UnknownHostException;

import org.limewire.io.ConnectableImpl;

public class DirectConnectionAddressImpl extends ConnectableImpl implements DirectConnectionAddress {

    public DirectConnectionAddressImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
        super(host, port, tlsCapable);
    }

    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof DirectConnectionAddress)) {
            return false;    
        }
        DirectConnectionAddress address2 = (DirectConnectionAddress)obj;
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
}
