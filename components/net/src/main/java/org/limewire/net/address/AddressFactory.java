package org.limewire.net.address;

import java.io.IOException;

public interface AddressFactory {
    public void addSerializer(AddressSerializer serializer);
    public AddressSerializer getSerializer(Class <? extends Address> addressClass);
    public AddressSerializer getSerializer(String addressType);
    public Address deserialize(String type, byte [] serializedAddress) throws IOException;
    public byte [] serialize(Address address) throws IOException;
}
