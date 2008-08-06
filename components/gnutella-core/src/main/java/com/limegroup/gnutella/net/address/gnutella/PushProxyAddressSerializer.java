package com.limegroup.gnutella.net.address.gnutella;

import java.io.IOException;

import org.limewire.net.address.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.DirectConnectionAddressSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.net.address.gnutella.PushProxyAddressImpl;

@Singleton
public class PushProxyAddressSerializer extends DirectConnectionAddressSerializer {
    public String getAddressType() {
        return "push-proxy-info";
    }

    public Class<? extends Address> getAddressClass() {
        return PushProxyAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        DirectConnectionAddress address = (DirectConnectionAddress)super.deserialize(serializedAddress);
        return new PushProxyAddressImpl(address.getAddress(), address.getPort(), address.isTLSCapable());
    }

    public byte[] serialize(Address address) throws IOException {
        return super.serialize(address);
    }
}
