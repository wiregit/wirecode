package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;

/**
 * A collection of <code>AddressSerializer</code>s.  <code>Address</code>s should register
 * themselves with this factory via the <code>addSerializer()</code> method at injection time.
 */
public interface AddressFactory {
    public void registerSerializer(AddressSerializer serializer);
    public AddressSerializer getSerializer(Class <? extends Address> addressClass);
    /**
     * Looks up serializer by {@link AddressSerializer#getAddressType()}. 
     * @return null if no serializer is registered for that type
     */
    public AddressSerializer getSerializer(String addressType);
    public Address deserialize(String type, byte [] serializedAddress) throws IOException;
}
