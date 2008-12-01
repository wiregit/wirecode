package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.XMPPAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Serializes and deserializes {@link org.limewire.xmpp.api.client.XMPPAddress} objects.
 */
@Singleton
public class XMPPAddressSerializer implements AddressSerializer {

    @Override
    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }
    
    @Override
    public Class<? extends Address> getAddressClass() {
        return XMPPAddress.class;
    }

    @Override
    public String getAddressType() {
        return "xmpp-address";
    }

    public Address deserialize(String address) throws IOException {
        // TODO replace with a real email address parser
        int atIndex = address.indexOf('@');
        if(atIndex != -1 && atIndex != address.length() - 1) {
            String host = address.substring(atIndex + 1);
            int dotIndex = host.indexOf('.');
            if(dotIndex != -1 && dotIndex != 0 && dotIndex != host.length() - 1) {
                return new XMPPAddress(address);
            }
        }
        throw new IOException();
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        return new XMPPAddress(StringUtils.getUTF8String(serializedAddress));
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        return StringUtils.toUTF8Bytes(xmppAddress.getFullId());
    }

}
