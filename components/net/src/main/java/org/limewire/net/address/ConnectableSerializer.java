package org.limewire.net.address;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.nio.ByteOrder;
import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.StrictIpPortSet;

@Singleton
public class ConnectableSerializer implements AddressSerializer {
    
    private static final int IP_V4 = 0;
    private static final int IP_V6 = 1;
    
    public String getAddressType() {
        return "direct-connect";
    }

    public Class<? extends Address> getAddressClass() {
        return Connectable.class;
    }

    public Connectable deserialize(byte[] serializedAddress) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(serializedAddress);
        return deserialize(in);
    }
    
    public Connectable deserialize(InputStream in) throws IOException {
        int hostPortLength = (ByteUtils.readByte(in) == IP_V4 ? 4 : 16) + 2;
        byte[] hostPort = new byte[hostPortLength];
        ByteUtils.readFully(in, hostPort);
        try {
            IpPort ipPort = NetworkUtils.getIpPort(hostPort, ByteOrder.BIG_ENDIAN);
            boolean supportsTLS = ByteUtils.readByte(in) == (byte)1;
            return new ConnectableImpl(ipPort.getAddress(), ipPort.getPort(), supportsTLS);
        } catch (InvalidDataException e) {
            throw new IOException(e);
        }
    }
    
    public Set<Connectable> deserializeSet(InputStream in) throws IOException {
        StrictIpPortSet<Connectable> set = new StrictIpPortSet<Connectable>();
        while (in.available() > 0) {
            set.add(deserialize(in));
        }
        return set;
    }

    public byte[] serialize(Address address) throws IOException {
        Connectable connectable = (Connectable)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int type = connectable.getInetAddress() instanceof Inet4Address ? IP_V4 : IP_V6;
        bos.write(type);
        bos.write(NetworkUtils.getBytes(connectable, ByteOrder.BIG_ENDIAN));
        bos.write(connectable.isTLSCapable() ? (byte)1 : (byte) 0);
        return bos.toByteArray();
    }
    
    public byte[] serialize(Set<Connectable> addresses) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Connectable connectable : addresses) {
            out.write(serialize(connectable));
        }
        return out.toByteArray();
    }

    @Inject
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }
}
