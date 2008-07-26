package com.limegroup.gnutella.net.address.gnutella;

import java.net.UnknownHostException;

import org.limewire.io.Connectable;
import org.limewire.net.address.DirectConnectionAddressImpl;

import com.limegroup.gnutella.net.address.gnutella.PushProxyAddress;

public class PushProxyAddressImpl extends DirectConnectionAddressImpl implements PushProxyAddress {
    public PushProxyAddressImpl(Connectable connectable) {
        super(connectable);
    }
    
    public PushProxyAddressImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
        super(host, port, tlsCapable);
    }
    
    public boolean equals(Object o) {        
        if(o == null || !(o instanceof PushProxyAddress)) {
            return false;    
        }
        return super.equals(o);
    }    
}
