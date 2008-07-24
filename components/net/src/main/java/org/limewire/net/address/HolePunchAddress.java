package org.limewire.net.address;

/**
 * An <code>Address</code> representing the information necessary to execute
 * a firewall connection
 */
public interface HolePunchAddress extends Address{
    public int getVersion();
    public DirectConnectionAddress getDirectConnectionAddress();
    public MediatorAddress getMediatorAddress();
}
