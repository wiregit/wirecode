package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

/**
 * Serializes and deserializes {@link XMPPAddress} objects.
 */
@Singleton
public class XMPPAddressSerializer implements AddressSerializer {

    @Override
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }
    
    @Override
    public Class<? extends Address> getAddressClass() {
        return XMPPAddress.class;
    }

    @Override
    public String getAddressType() {
        return "xmpp-address";
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        return new XMPPAddress(StringUtils.getUTF8String(serializedAddress));
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        return StringUtils.toUTF8Bytes(xmppAddress.getId());
    }

}
