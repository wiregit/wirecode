package org.limewire.net.address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HolePunchConnectionAddressSerializer implements AddressSerializer {
    @Inject
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }

    public String getAddressType() {
        return "generic-hole-punch";
    }

    public Class<? extends Address> getAddressClass() {
        return HolePunchConnectionAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        final int version = serializedAddress[0];
        return new HolePunchConnectionAddress() {
            public int getVersion() {
                return version;
            }

            public DirectConnectionAddress getDirectConnectionAddress() {
                return null;
            }

            public MediatedConnectionAddress getMediatedConnectionAddress() {
                return null; 
            }
        };
    }

    public byte[] serialize(Address address) throws IOException {
        HolePunchConnectionAddress holePunchConnectionAddress = (HolePunchConnectionAddress)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(holePunchConnectionAddress.getVersion());
        return bos.toByteArray();
    }
}
