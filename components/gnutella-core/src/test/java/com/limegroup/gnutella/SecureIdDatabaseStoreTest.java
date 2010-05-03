package com.limegroup.gnutella;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.GUID;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.Clock;
import org.limewire.util.ClockImpl;
import org.limewire.util.SequencedExpectations;

public class SecureIdDatabaseStoreTest extends LimeTestCase {

    private SecureIdDatabaseStore secureIdDatabaseStore;

    public static Test suite() { 
        return buildTestSuite(SecureIdDatabaseStoreTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        secureIdDatabaseStore = new SecureIdDatabaseStore(new ClockImpl());
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
        result = secureIdDatabaseStore.get(guid);
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
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(new ClockImpl());
        secureIdDatabaseStore.start();
        result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
    }
    
    public void testPutLargeValue() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] largeValue = createRandomBytes(250);
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
    
    public void testOldEntriesGetExpired() {
        Mockery context = new Mockery();
        final Clock clock = context.mock(Clock.class);
        final long currentTime = System.currentTimeMillis();
        context.checking(new SequencedExpectations(context) {{
            // for first start()
            one(clock).now();
            will(returnValue(System.currentTimeMillis()));
            // for put()
            one(clock).now();
            will(returnValue(System.currentTimeMillis()));
            // for second start()
            one(clock).now();
            will(returnValue(currentTime + TimeUnit.DAYS.toMillis(400)));
        }});
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(clock);
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(clock);
        secureIdDatabaseStore.start();
        
        assertNull(secureIdDatabaseStore.get(guid));
        secureIdDatabaseStore.stop();
    }
    
    public void testRecentlyReadEntriesAreNotExpired() {
        Mockery context = new Mockery();
        final Clock clock = context.mock(Clock.class);
        final long currentTime = System.currentTimeMillis();
        context.checking(new SequencedExpectations(context) {{
            // initial start()
            one(clock).now();
            will(returnValue(System.currentTimeMillis()));
            // initial store
            one(clock).now();
            will(returnValue(System.currentTimeMillis()));
            // second start()
            one(clock).now();
            will(returnValue(System.currentTimeMillis()));
            // update on read access
            one(clock).now();
            will(returnValue(currentTime + TimeUnit.DAYS.toMillis(365)));
            // expiration check
            one(clock).now();
            will(returnValue(currentTime + TimeUnit.DAYS.toMillis(400)));
            // update on read access
            one(clock).now();
            will(returnValue(currentTime + TimeUnit.DAYS.toMillis(400)));
        }});
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(clock);
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        secureIdDatabaseStore.stop();
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(clock);
        secureIdDatabaseStore.start();
        
        // this get will update the timestamp
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(clock);
        secureIdDatabaseStore.start();
        
        assertEquals(value, secureIdDatabaseStore.get(guid));
        secureIdDatabaseStore.stop();
    }
    
    public void testSetLocalDataResetsDatabase() {
        secureIdDatabaseStore.start();
        GUID guid = new GUID();
        byte[] value = createRandomBytes(100);
        secureIdDatabaseStore.put(guid, value);
        byte[] result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        result = secureIdDatabaseStore.get(guid);
        assertEquals(value, result);
        secureIdDatabaseStore.stop();
        
        secureIdDatabaseStore = new SecureIdDatabaseStore(new ClockImpl());
        secureIdDatabaseStore.setLocalData(new byte[] { 0, 0, 0, 0 });
        secureIdDatabaseStore.start();
        
        assertNull(secureIdDatabaseStore.get(guid));
        assertEquals(new byte[] { 0, 0, 0, 0, }, secureIdDatabaseStore.getLocalData());
        secureIdDatabaseStore.stop();
    }

    public void testStoppedStoreDoesNotThrowRuntimeException() {
        secureIdDatabaseStore.start();
        secureIdDatabaseStore.stop();
        assertNull(secureIdDatabaseStore.get(new GUID()));
    }
    
    private byte[] createRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }
    
}
