package com.limegroup.gnutella.net.address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;

public class PushProxyMediatorAddressSerializerTest extends BaseTestCase {

    private PushProxyMediatorAddressSerializer serializer;

    public PushProxyMediatorAddressSerializerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxyMediatorAddressSerializerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        serializer = new PushProxyMediatorAddressSerializer(new ConnectableSerializer());
    }
    
    public void testSerialize() throws Exception {
        GUID guid = new GUID();
        Set<Connectable> proxies = new LinkedHashSet<Connectable>();
        Connectable proxy1 = new ConnectableImpl("127.0.0.1", 10, true);
        Connectable proxy2 = new ConnectableImpl("129.0.0.1", 15, false);
        proxies.add(proxy1);
        proxies.add(proxy2);
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(guid, proxies);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(guid.bytes());
        out.write(0); // ipv4
        out.write(NetworkUtils.getBytes(proxy1, ByteOrder.BIG_ENDIAN));
        out.write(1); // tls enabled
        out.write(0); // ipv4
        out.write(NetworkUtils.getBytes(proxy2, ByteOrder.BIG_ENDIAN));
        out.write(0); // tls disabled
        assertEquals(out.toByteArray(), serializer.serialize(address));
    }
    
    public void testDeserialize() throws Exception {
        GUID guid = new GUID();
        Set<Connectable> proxies = new LinkedHashSet<Connectable>();
        Connectable proxy1 = new ConnectableImpl("127.0.0.1", 10, true);
        Connectable proxy2 = new ConnectableImpl("129.0.0.1", 15, false);
        proxies.add(proxy1);
        proxies.add(proxy2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(guid.bytes());
        out.write(0); // ipv4
        out.write(NetworkUtils.getBytes(proxy1, ByteOrder.BIG_ENDIAN));
        out.write(1); // tls enabled
        out.write(0); // ipv4
        out.write(NetworkUtils.getBytes(proxy2, ByteOrder.BIG_ENDIAN));
        out.write(0); // tls disabled
        Address readAddress = serializer.deserialize(out.toByteArray());
        assertTrue(readAddress instanceof PushProxyMediatorAddress);
        PushProxyMediatorAddress mediatorAddress = (PushProxyMediatorAddress)readAddress;
        assertEquals(guid, mediatorAddress.getClientID());
        assertContains(mediatorAddress.getPushProxies(), proxy1);
        assertContains(mediatorAddress.getPushProxies(), proxy2);
    }
    
    public void testDeserializeNotEnoughData() {
        try {
            serializer.deserialize(new byte[0]);
            fail("io exception excepted, because of end of stream");
        } catch (IOException ie) {
        }
        try {
            serializer.deserialize(new byte[1]);
            fail("io exception excepted, because of end of stream");
        } catch (IOException ie) {
        }
    }

}
