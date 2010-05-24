package com.limegroup.gnutella.dht.db;

import java.util.Set;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

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
    
    public void testSerialization() throws Exception {
        byte[] guid = GUID.makeGuid();
        byte features = 1;
        int fwtVersion = 2;
        int port = 1234;
        
        Set<IpPort> proxies = new IpPortSet();
        proxies.add(new IpPortImpl("localhost", 4321));
        proxies.add(new IpPortImpl("localhost", 3333));
        
        PushProxiesValue value1 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                fwtVersion, port, proxies);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(features, value1.getFeatures());
        assertEquals(fwtVersion, value1.getFwtVersion());
        assertEquals(port, value1.getPort());
        assertEquals(2, value1.getPushProxies().size());
        assertEquals(proxies.iterator().next().getInetAddress(), 
                value1.getPushProxies().iterator().next().getInetAddress());
        assertEquals(proxies.iterator().next().getPort(), 
                value1.getPushProxies().iterator().next().getPort());
        
        // Serialize and de-serialize it again
        PushProxiesValue value2 
            = new DefaultPushProxiesValue(
                    value1.serialize());
        
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
        
        assertEquals(value1, value2);
    }
}
