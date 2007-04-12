package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.dht.DHTTestCase;

public class PushProxiesDHTValueTest extends DHTTestCase {
    
    public PushProxiesDHTValueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushProxiesDHTValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerialization() {
        int futures = 1;
        int fwtVersion = 2;
        int port = 1234;
        
        Set<? extends IpPort> proxies = null;
        try {
            proxies = Collections.singleton(new IpPortImpl("localhost", 4321));
        } catch (UnknownHostException err) {
            fail("UnknownHostException", err);
        }
        
        PushProxiesDHTValue value1 
            = new PushProxiesDHTValueImpl(Version.ZERO, 
                    futures, fwtVersion, port, proxies);
        
        assertEquals(futures, value1.getFeatures());
        assertEquals(fwtVersion, value1.getFwtVersion());
        assertEquals(port, value1.getPort());
        assertEquals(1, value1.getPushProxies().size());
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
        
        // Serialize it
        PushProxiesDHTValue value2 = null;
        try {
            value2 = (PushProxiesDHTValue)PushProxiesDHTValueImpl
                        .createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be equal
        assertEquals(value1.getFeatures(), value2.getFeatures());
        assertEquals(value1.getFwtVersion(), value2.getFwtVersion());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(1, value2.getPushProxies().size());
        assertEquals(value1.getPushProxies().iterator().next().getInetAddress(), 
                value2.getPushProxies().iterator().next().getInetAddress());
        assertEquals(value1.getPushProxies().iterator().next().getPort(), 
                value2.getPushProxies().iterator().next().getPort());
        
        // Screw around the with raw data
        serialized[0] = (byte)~serialized[0]; // change the futures
        serialized[4] = (byte)~serialized[4]; // change the fwtVersion
        serialized[9] = (byte)~serialized[9]; // change the lower byte of the port
        
        PushProxiesDHTValue value3 = null;
        try {
            value3 = (PushProxiesDHTValue)PushProxiesDHTValueImpl
                        .createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // And nothing should match
        assertNotEquals(value1.getFeatures(), value3.getFeatures());
        assertNotEquals(value1.getFwtVersion(), value3.getFwtVersion());
        assertNotEquals(value1.getPort(), value3.getPort());
        
        // And this should fail!
        serialized[10] = 2; // 2 proxies
        try {
            DHTValue value = PushProxiesDHTValueImpl.createFromData(Version.ZERO, serialized);
            fail("Should have failed: " + value);
        } catch (DHTValueException expected) {
        }
    }
}
