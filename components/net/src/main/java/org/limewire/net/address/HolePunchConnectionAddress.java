package org.limewire.net.address;

public interface HolePunchConnectionAddress extends Address{
    public int getVersion();
    public DirectConnectionAddress getDirectConnectionAddress();
    public MediatedConnectionAddress getMediatedConnectionAddress();
}
