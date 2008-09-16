package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.io.Connectable;

/**
 * An <code>Address</code> representing the information necessary to execute
 * a firewall connection
 */
public interface HolePunchAddress extends Address{
    public int getVersion();
    public Connectable getDirectConnectionAddress();
    public MediatorAddress getMediatorAddress();
}
