package com.limegroup.gnutella.net.address.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.net.address.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;

@Singleton
public class PushProxyMediatorAddressSerializer implements AddressSerializer {
    
    private final AddressFactory factory;
    
    @Inject
    PushProxyMediatorAddressSerializer(AddressFactory factory) {
        this.factory = factory;
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
        byte [] guidBytes = new byte[16];
        System.arraycopy(serializedAddress, 0, guidBytes, 0, 16);
        final GUID guid = new GUID(guidBytes);
        final Set<PushProxyAddress> pushProxyAddresses = new HashSet<PushProxyAddress>();
        for(int i = 0; i < (serializedAddress.length - 16) / 7; i++) {
            byte [] pushProxy = new byte[7];
            System.arraycopy(serializedAddress, (i * 7) + 16, pushProxy, 0, 7);
            pushProxyAddresses.add((PushProxyAddress)factory.deserialize("push-proxy-info", pushProxy));    
        }
        return new PushProxyMediatorAddressImpl(guid, pushProxyAddresses);
    }

    public byte[] serialize(Address address) throws IOException {
        PushProxyMediatorAddress mediatorAddress = (PushProxyMediatorAddress)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(mediatorAddress.getClientID().bytes());
        Set<PushProxyAddress> pushProxyAddresses = mediatorAddress.getPushProxies();
        for(PushProxyAddress pushProxyAddress : pushProxyAddresses) {
            bos.write(factory.serialize(pushProxyAddress));
        }
        return bos.toByteArray();
    }
}
