package com.limegroup.gnutella;

import java.util.Random;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.GUID;

public class SecureIdDatabaseStoreTest extends LimeTestCase {

    private SecureIdDatabaseStore secureIdDatabaseStore;

    public static Test suite() { 
        return buildTestSuite(SecureIdDatabaseStoreTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        secureIdDatabaseStore = new SecureIdDatabaseStore();
    }
    
    public void testGet() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        assertNull(secureIdDatabaseStore.get(guid));
        secureIdDatabaseStore.stop();
    }
    
    public void testPutGet() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testGetInNewSession() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
        
        secureIdDatabaseStore = new SecureIdDatabaseStore();
        secureIdDatabaseStore.start();
        result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testPutLargeValue() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] largeValue = createRandomBytes(200);
        secureIdDatabaseStore.put(guid, largeValue);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(largeValue, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testPutEmptyValue() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] emptyValue = new byte[0];
        secureIdDatabaseStore.put(guid, emptyValue);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(emptyValue, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testPutTooLargeValue() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] tooLargeValue = createRandomBytes(500);
        secureIdDatabaseStore.put(guid, tooLargeValue);
        byte[] result = secureIdDatabaseStore.get(guid);
        // TODO this should not pass...
        assertEquals(tooLargeValue, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testPutDoesNotOverride() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        
        byte[] newValue = createRandomBytes(150);
        secureIdDatabaseStore.put(guid, newValue);
        result = secureIdDatabaseStore.get(guid);
        // should still be the old value
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
    }
    
    private byte[] createRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }
    
}
