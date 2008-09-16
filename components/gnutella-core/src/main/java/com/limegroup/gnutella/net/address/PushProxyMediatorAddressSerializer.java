package com.limegroup.gnutella.net.address;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.net.address.ConnectableSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.util.StrictIpPortSet;

@Singleton
public class PushProxyMediatorAddressSerializer implements AddressSerializer {
    
    private final ConnectableSerializer proxySerializer;

    @Inject
    PushProxyMediatorAddressSerializer(ConnectableSerializer proxySerializer) {
        this.proxySerializer = proxySerializer;
    }   

    @Inject
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }
    
    public String getAddressType() {
        return "push-proxy";
    }

    public Class<? extends Address> getAddressClass() {
        return PushProxyMediatorAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        final GUID guid = new GUID(Arrays.copyOf(serializedAddress, 16)); 
        final Set<Connectable> pushProxyAddresses = new StrictIpPortSet<Connectable>();
        ByteArrayInputStream in = new ByteArrayInputStream(serializedAddress, 16, serializedAddress.length - 16);
        while (in.available() > 0) {
            pushProxyAddresses.add(proxySerializer.deserialize(in));    
        }
        return new PushProxyMediatorAddressImpl(guid, pushProxyAddresses);
    }

    public byte[] serialize(Address address) throws IOException {
        PushProxyMediatorAddress mediatorAddress = (PushProxyMediatorAddress)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(mediatorAddress.getClientID().bytes());
        Set<Connectable> pushProxyAddresses = mediatorAddress.getPushProxies();
        for(Connectable pushProxyAddress : pushProxyAddresses) {
            bos.write(proxySerializer.serialize(pushProxyAddress));
        }
        return bos.toByteArray();
    }
}
