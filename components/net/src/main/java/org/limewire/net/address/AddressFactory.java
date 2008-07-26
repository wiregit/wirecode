package org.limewire.net.address;

import java.io.IOException;

/**
 * A collection of <code>AddressSerializer</code>s.  <code>Address</code>s should register
 * themselves with this factory via the <code>addSerializer()</code> method at injection time.
 */
public interface AddressFactory {
    public void addSerializer(AddressSerializer serializer);
    public AddressSerializer getSerializer(Class <? extends Address> addressClass);
    public AddressSerializer getSerializer(String addressType);
    public Address deserialize(String type, byte [] serializedAddress) throws IOException;
    public byte [] serialize(Address address) throws IOException;
}
