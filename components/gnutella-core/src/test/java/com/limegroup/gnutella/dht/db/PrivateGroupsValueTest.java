package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;

import org.limewire.mojito.routing.Version;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.dht.DHTTestCase;

public class PrivateGroupsValueTest extends DHTTestCase {

    private PrivateGroupsValueFactoryImpl privateGroupsValueFactory;
    
    public PrivateGroupsValueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocValueTest.class);
    }
    
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        privateGroupsValueFactory = (PrivateGroupsValueFactoryImpl) injector.getInstance(PrivateGroupsValueFactory.class);
    }

    public void testSerializationVersion() throws Exception {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        
        PrivateGroupsValue value1 = privateGroupsValueFactory.createPrivateGroupsValue(Version.ZERO, guid, port, -1L);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        
        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        value1.write(baos);
        
        // Get the raw bytes
        byte[] serialized = baos.toByteArray();
        
        // De-serialize it
        PrivateGroupsValue value2 = privateGroupsValueFactory.createFromData(Version.ZERO, serialized);
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
    }

}
