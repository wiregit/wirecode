package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.DHTTestCase;

public class PushProxiesValueTest extends DHTTestCase {
    
    public PushProxiesValueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushProxiesValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerialization() {
        byte[] guid = GUID.makeGuid();
        int futures = 1;
        int fwtVersion = 2;
        int port = 1234;
        
        Set<IpPort> proxies = new IpPortSet();
        try {
            proxies.add(new IpPortImpl("localhost", 4321));
            proxies.add(new IpPortImpl("localhost", 3333));
        } catch (UnknownHostException err) {
            fail("UnknownHostException", err);
        }
        
        PushProxiesValue value1 
            = PushProxiesValue.createPushProxiesValue(Version.ZERO, 
                    guid, futures, fwtVersion, port, proxies);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(futures, value1.getFeatures());
        assertEquals(fwtVersion, value1.getFwtVersion());
        assertEquals(port, value1.getPort());
        assertEquals(2, value1.getPushProxies().size());
        assertEquals(proxies.iterator().next().getInetAddress(), 
                value1.getPushProxies().iterator().next().getInetAddress());
        assertEquals(proxies.iterator().next().getPort(), 
                value1.getPushProxies().iterator().next().getPort());
        
        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            value1.write(baos);
        } catch (IOException err) {
            fail("IOException", err);
        }
        
        // Get the raw bytes
        byte[] serialized = baos.toByteArray();
        
        // De-serialize it
        PushProxiesValue value2 = null;
        try {
            value2 = PushProxiesValue.createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be equal
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getFeatures(), value2.getFeatures());
        assertEquals(value1.getFwtVersion(), value2.getFwtVersion());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(2, value2.getPushProxies().size());
        assertEquals(value1.getPushProxies().iterator().next().getInetAddress(), 
                value2.getPushProxies().iterator().next().getInetAddress());
        assertEquals(value1.getPushProxies().iterator().next().getPort(), 
                value2.getPushProxies().iterator().next().getPort());
    }
}
