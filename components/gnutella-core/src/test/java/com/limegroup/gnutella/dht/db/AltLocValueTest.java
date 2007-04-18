package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.DHTTestCase;

public class AltLocValueTest extends DHTTestCase {
    
    public AltLocValueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerialization() {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        boolean firewalled = true;
        
        AltLocValue value1 = AltLocValue.createAltLocValue(
                Version.ZERO, guid, port, firewalled);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        assertEquals(firewalled, value1.isFirewalled());
        
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
        AltLocValue value2 = null;
        try {
            value2 = AltLocValue.createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
    }
}
