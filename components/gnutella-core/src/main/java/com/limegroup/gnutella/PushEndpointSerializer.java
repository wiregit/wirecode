package com.limegroup.gnutella;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PushEndpointSerializer implements AddressSerializer {

    private final PushEndpointFactory pushEndpointFactory;

    @Inject
    public PushEndpointSerializer(PushEndpointFactory pushEndpointFactory) {
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        String httpString = StringUtils.getUTF8String(serializedAddress);
        return pushEndpointFactory.createPushEndpoint(httpString);
    }

    @Override
    public Class<? extends Address> getAddressClass() {
        return PushEndpoint.class;
    }

    @Override
    public String getAddressType() {
        return "push-endpoint";
    }

    @Inject
    @Override
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        return StringUtils.toUTF8Bytes(((PushEndpoint)address).httpStringValue());
    }

}
