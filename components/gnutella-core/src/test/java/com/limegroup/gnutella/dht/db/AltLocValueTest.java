package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

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
    
    public void testSerializationVersionZero() {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        boolean firewalled = true;
        
        AltLocValue value1 = AltLocValue.createAltLocValue(
                Version.ZERO, guid, port, -1L, null, firewalled);
        
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
        assertEquals(-1L, value2.getFileSize());
        assertEquals(null, value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
    }
    
    public void testSerializationVersionOne() {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        long fileSize = 334455;
        byte[] ttroot = new byte[20];
        boolean firewalled = true;
        
        Random random = new Random();
        random.nextBytes(ttroot);
        
        AltLocValue value1 = AltLocValue.createAltLocValue(
            AltLocValue.VERSION_ONE, guid, port, fileSize, ttroot, firewalled);
        
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
            value2 = AltLocValue.createFromData(AltLocValue.VERSION_ONE, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(value1.getFileSize(), value2.getFileSize());
        assertEquals(value1.getRootHash(), value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
        
        // De-serialize it but do as if it's a Version 0 value!
        // The File size and TigerTree root hash should be missing!
        AltLocValue value3 = null;
        try {
            value3 = AltLocValue.createFromData(Version.ZERO, serialized);
        } catch (DHTValueException err) {
            fail("DHTValueException", err);
        }
        
        // Should be the same
        assertEquals(value1.getGUID(), value3.getGUID());
        assertEquals(value1.getPort(), value3.getPort());
        assertEquals(-1L, value3.getFileSize());
        assertEquals(null, value3.getRootHash());
        assertEquals(value1.isFirewalled(), value3.isFirewalled());
    }
}
