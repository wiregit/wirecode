package org.limewire.net.address.gnutella;

import java.io.IOException;

import org.limewire.net.address.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.HolePunchConnectionAddress;
import org.limewire.net.address.HolePunchConnectionAddressSerializer;
import org.limewire.net.address.MediatedConnectionAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PushProxyHolePunchConnectionAddressSerializer extends HolePunchConnectionAddressSerializer {
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
        return PushProxyHolePunchConnectionAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        final HolePunchConnectionAddress address = (HolePunchConnectionAddress)super.deserialize(serializedAddress);
        byte [] directConnectBytes = new byte[7];
        System.arraycopy(serializedAddress, 1, directConnectBytes, 0, directConnectBytes.length);
        final DirectConnectionAddress directConnect = (DirectConnectionAddress)factory.deserialize("direct-connect", directConnectBytes);
        byte [] pushProxyBytes = new byte[serializedAddress.length - 8];
        System.arraycopy(serializedAddress, 8, pushProxyBytes, 0, pushProxyBytes.length);
        final PushProxyMediatedAddress pushProxy = (PushProxyMediatedAddress)factory.deserialize("push-proxy", pushProxyBytes); 
        return new PushProxyHolePunchConnectionAddress() {
            public int getVersion() {
                return address.getVersion();
            }

            public DirectConnectionAddress getDirectConnectionAddress() {
                return directConnect;
            }

            public MediatedConnectionAddress getMediatedConnectionAddress() {
                return pushProxy;
            }
        };
    }

    public byte[] serialize(Address address) throws IOException {
        byte [] superSerializedAddress = super.serialize(address);
        PushProxyHolePunchConnectionAddress holePunchAddress = (PushProxyHolePunchConnectionAddress)address;
        byte [] directAddress = factory.serialize(holePunchAddress.getDirectConnectionAddress());
        byte [] pushProxyAddress = factory.serialize(holePunchAddress.getMediatedConnectionAddress());
        byte [] serializedAddress = new byte[superSerializedAddress.length + directAddress.length + pushProxyAddress.length];
        System.arraycopy(superSerializedAddress, 0, serializedAddress, 0, superSerializedAddress.length);
        System.arraycopy(directAddress, 0, serializedAddress, superSerializedAddress.length, directAddress.length);
        System.arraycopy(pushProxyAddress, 0, serializedAddress, superSerializedAddress.length + directAddress.length, pushProxyAddress.length);
        return serializedAddress;
    }
}
