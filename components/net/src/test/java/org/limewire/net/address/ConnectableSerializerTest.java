package org.limewire.net.address;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;

public class ConnectableSerializerTest extends BaseTestCase {

    private ConnectableSerializer serializer;

    public ConnectableSerializerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ConnectableSerializerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        serializer = new ConnectableSerializer();
    }
    
    public void testDeserializeIPv4Address() throws Exception {
        byte[] data = { 0, 127, 0, 0, 1, 0, 5, 1 };
        Address address = serializer.deserialize(data);
        assertTrue(address instanceof Connectable);
        Connectable connectable = (Connectable)address;
        assertEquals(new InetSocketAddress("127.0.0.1", 5), connectable.getInetSocketAddress());
        assertTrue(connectable.isTLSCapable());
    }
    
    public void testDeserializeIPv6Address() throws Exception {
        byte[] data = new byte[20];
        Arrays.fill(data, (byte)5);
        data[0] = 1;  // address type
        data[17] = 0; // port
        data[18] = 5; // port
        data[19] = 1; // tls
        Address address = serializer.deserialize(data);
        assertTrue(address instanceof Connectable);
        Connectable connectable = (Connectable)address;
        assertEquals(new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(data, 1, 17)), 5), connectable.getInetSocketAddress());
        assertTrue(connectable.isTLSCapable());
    }

    public void testSerializeIPv4Address() throws Exception {
        Connectable connectable = new ConnectableImpl(new InetSocketAddress("127.0.0.1", 5), true);
        assertEquals(new byte[] { 0, 127, 0, 0, 1, 0, 5, 1 }, serializer.serialize(connectable)); 
    }
    
    public void testSerializeIPv6Address() throws Exception {
        byte[] data = new byte[20];
        Arrays.fill(data, (byte)5);
        data[0] = 1;  // address type
        data[17] = 0; // port
        data[18] = 5; // port
        data[19] = 1; // tls
        Connectable connectable = new ConnectableImpl(new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(data, 1, 17)), 5), true);
        assertEquals(data, serializer.serialize(connectable));
    }

    public void testSerializeDeserialize() throws Exception {
        Connectable connectable = new ConnectableImpl("192.168.0.1", 5555, false);
        byte[] data = serializer.serialize(connectable);
        assertEquals(0, Connectable.COMPARATOR.compare(connectable, serializer.deserialize(data)));
    }
}
