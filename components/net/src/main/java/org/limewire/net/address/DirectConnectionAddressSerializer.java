package org.limewire.net.address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DirectConnectionAddressSerializer implements AddressSerializer{
    public String getAddressType() {
        return "direct-connect";
    }

    public Class<? extends Address> getAddressClass() {
        return DirectConnectionAddress.class;
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        boolean supportsTLS = (serializedAddress[6] == (byte)1);
        byte [] hostPort = new byte[6];
        System.arraycopy(serializedAddress, 0, hostPort, 0, 6);
        try {
            IpPort ipPort = NetworkUtils.getIpPort(hostPort, ByteOrder.BIG_ENDIAN);
            return new DirectConnectionAddressImpl(ipPort.getAddress(), ipPort.getPort(), supportsTLS);
        } catch (InvalidDataException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    public byte[] serialize(Address address) throws IOException {
        DirectConnectionAddress directConnectionAddress = (DirectConnectionAddress)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(NetworkUtils.getBytes(directConnectionAddress, ByteOrder.BIG_ENDIAN));
        bos.write(directConnectionAddress.isTLSCapable() ? (byte)1 : (byte) 0);
        return bos.toByteArray();
    }

    @Inject
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }
}
