package org.limewire.net.address;

public interface HolePunchAddress extends Address{
    public int getVersion();
    public DirectConnectionAddress getDirectConnectionAddress();
    public MediatorAddress getMediatorAddress();
}
