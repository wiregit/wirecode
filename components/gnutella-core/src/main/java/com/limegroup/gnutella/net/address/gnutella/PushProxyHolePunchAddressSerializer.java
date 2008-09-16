package com.limegroup.gnutella.net.address.gnutella;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.HolePunchAddress;
import org.limewire.net.address.HolePunchAddressSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PushProxyHolePunchAddressSerializer extends HolePunchAddressSerializer {
    protected AddressFactory factory;

    @Inject
    public void register(AddressFactory factory) {
        this.factory = factory;
        this.factory.addSerializer(this);
    }

    public String getAddressType() {
        return "push-proxy-firewall-transfer";
    }

    public Class<? extends Address> getAddressClass() {
        return PushProxyHolePunchAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        final HolePunchAddress address = (HolePunchAddress)super.deserialize(serializedAddress);
        byte [] directConnectBytes = new byte[7];
        System.arraycopy(serializedAddress, 1, directConnectBytes, 0, directConnectBytes.length);
        final Connectable directConnect = (Connectable)factory.deserialize("direct-connect", directConnectBytes);
        byte [] pushProxyBytes = new byte[serializedAddress.length - 8];
        System.arraycopy(serializedAddress, 8, pushProxyBytes, 0, pushProxyBytes.length);
        final PushProxyMediatorAddress pushProxy = (PushProxyMediatorAddress)factory.deserialize("push-proxy", pushProxyBytes);
        return new PushProxyHolePunchAddressImpl(address.getVersion(), directConnect, pushProxy);
    }

    public byte[] serialize(Address address) throws IOException {
        byte [] superSerializedAddress = super.serialize(address);
        PushProxyHolePunchAddress holePunchAddress = (PushProxyHolePunchAddress)address;
        byte [] directAddress = factory.serialize(holePunchAddress.getDirectConnectionAddress());
        byte [] pushProxyAddress = factory.serialize(holePunchAddress.getMediatorAddress());
        byte [] serializedAddress = new byte[superSerializedAddress.length + directAddress.length + pushProxyAddress.length];
        System.arraycopy(superSerializedAddress, 0, serializedAddress, 0, superSerializedAddress.length);
        System.arraycopy(directAddress, 0, serializedAddress, superSerializedAddress.length, directAddress.length);
        System.arraycopy(pushProxyAddress, 0, serializedAddress, superSerializedAddress.length + directAddress.length, pushProxyAddress.length);
        return serializedAddress;
    }
}
