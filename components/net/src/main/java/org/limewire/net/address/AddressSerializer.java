package org.limewire.net.address;

import java.io.IOException;

import com.google.inject.Inject;

/**
 * Used to (de)serialize <code>Addresses</code> over a network connection
 */
public interface AddressSerializer {
    @Inject
    public void register(AddressFactory factory);
    public String getAddressType();
    public Class<? extends Address> getAddressClass();
    public Address deserialize(byte [] serializedAddress) throws IOException;
    public byte [] serialize(Address address) throws IOException;    
}
