package org.limewire.net.address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HolePunchAddressSerializer implements AddressSerializer {
    @Inject
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }

    public String getAddressType() {
        return "generic-hole-punch";
    }

    public Class<? extends Address> getAddressClass() {
        return HolePunchAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        final int version = serializedAddress[0];
        return new HolePunchAddress() {
            public int getVersion() {
                return version;
            }

            public DirectConnectionAddress getDirectConnectionAddress() {
                return null;
            }

            public MediatorAddress getMediatorAddress() {
                return null; 
            }
        };
    }

    public byte[] serialize(Address address) throws IOException {
        HolePunchAddress holePunchAddress = (HolePunchAddress)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(holePunchAddress.getVersion());
        return bos.toByteArray();
    }
}
