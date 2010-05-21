package com.limegroup.gnutella.dht.db;

import java.util.Random;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.mojito2.routing.Version;

import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.security.MerkleTree;

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
    
    public void testSerializationVersionZero() throws Exception {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        boolean firewalled = true;
        
        DefaultAltLocValue value1 = new DefaultAltLocValue(Version.ZERO, 
                guid, port, -1L, null, firewalled, false);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        assertEquals(firewalled, value1.isFirewalled());
        
        // Serialize and de-serialize it gain
        AltLocValue value2 = new DefaultAltLocValue(
                value1.serialize());
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(-1L, value2.getFileSize());
        assertEquals(null, value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
    }
    
    public void testSerializationVersionOne() throws Exception {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        long fileSize = 334455;
        byte[] ttroot = new byte[MerkleTree.HASHSIZE];
        boolean firewalled = true;
        
        Random random = new Random();
        random.nextBytes(ttroot);
        
        DefaultAltLocValue value1 = new DefaultAltLocValue(
                AltLocValue.VERSION_ONE, guid, port, 
                fileSize, ttroot, firewalled, false);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        assertEquals(firewalled, value1.isFirewalled());
        
        // Serialize and de-serialize it again
        AltLocValue value2 = new DefaultAltLocValue(
                value1.serialize());
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(value1.getFileSize(), value2.getFileSize());
        assertEquals(value1.getRootHash(), value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
        
        // De-serialize it but do as if it's a Version 0 value!
        // The File size and TigerTree root hash should be missing!
        AltLocValue value3 = new DefaultAltLocValue(Version.ZERO, 
                value1.serialize().getValue());
        
        // Should be the same
        assertEquals(value1.getGUID(), value3.getGUID());
        assertEquals(value1.getPort(), value3.getPort());
        assertEquals(-1L, value3.getFileSize());
        assertEquals(null, value3.getRootHash());
        assertEquals(value1.isFirewalled(), value3.isFirewalled());
    }
}
