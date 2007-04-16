package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.DHTTestCase;

public class AltLocDHTValueTest extends DHTTestCase {
    
    public AltLocDHTValueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocDHTValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerialization() {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        boolean firewalled = true;
        
        AltLocDHTValue value1 = new AltLocDHTValueImpl(
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
        AltLocDHTValue value2 = null;
        try {
            value2 = (AltLocDHTValue)AltLocDHTValueImpl
                        .createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
        
        // Screw around with the raw data
        serialized = baos.toByteArray();
        serialized[0] = (byte)~serialized[0]; // change the first byte of the GUID
        serialized[17] = (byte)~serialized[17]; // change the lower byte of the port
        serialized[serialized.length-1] = 0; // change to false
        
        AltLocDHTValue value3 = null;
        try {
            value3 = (AltLocDHTValue)AltLocDHTValueImpl
                        .createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // And nothing should match
        assertNotEquals(value1.getGUID(), value3.getGUID());
        assertNotEquals(value1.getPort(), value3.getPort());
        assertNotEquals(value1.isFirewalled(), value3.isFirewalled());
        
        // Try to create AltLocDHTValue from invalid input
        byte[] invalid = new byte[serialized.length-1];
        System.arraycopy(serialized, 0, invalid, 0, invalid.length);
        try {
            DHTValue value = AltLocDHTValueImpl.createFromData(Version.ZERO, invalid);
            fail("Should have failed: " + value);
        } catch (DHTValueException expected) {
        }
    }
}
