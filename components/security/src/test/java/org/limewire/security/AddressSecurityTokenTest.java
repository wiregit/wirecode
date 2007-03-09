package org.limewire.security;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class AddressSecurityTokenTest extends BaseTestCase {
    public AddressSecurityTokenTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AddressSecurityTokenTest.class);
    }

    public void testConstruction() throws Exception {
        try {
            new AddressSecurityToken(new byte[3]);
            fail("exception should have been thrown.");
        }
        catch (InvalidSecurityTokenException ignored) {}
        try {
            new AddressSecurityToken(new byte[17]);
            fail("exception should have been thrown.");
        }
        catch (InvalidSecurityTokenException ignored) {}
        
        byte[] qk = new byte[8];
        Random rand = new Random();
        Arrays.sort(qk);
        while (Arrays.binarySearch(qk, (byte) 0x1c) < 0) {
            rand.nextBytes(qk);
            Arrays.sort(qk);
        }
        AddressSecurityToken key1 = null, key2 = null;
        key1 = new AddressSecurityToken(qk);
        key2 = new AddressSecurityToken(qk);
        assertEquals(key1,key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        
        byte []qk3 = new byte[8];
        rand.nextBytes(qk3);
        AddressSecurityToken key3 = new AddressSecurityToken(qk3);
        assertNotEquals(key1, key3);
        assertNotEquals(key2, key3);
        assertEquals(key3, key3);
        // don't test key1.hashCode() vs. key3.hashCode() - they may very easily
        // conflict
    }

    public void testSimpleGeneration() throws Exception {
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        AbstractSecurityToken qk1 = new AddressSecurityToken(ip, port);
        AbstractSecurityToken qk2 = new AddressSecurityToken(ip, port);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("10.254.0.42");
        qk1 = new AddressSecurityToken(ip, port);
        qk2 = new AddressSecurityToken(ip, port);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("127.0.0.1");
        qk1 = new AddressSecurityToken(ip, port);
        qk2 = new AddressSecurityToken(ip, port);
        assertEquals(qk1,qk2);
    }

    public void testQueryKeys() throws Exception {
        InetAddress addr1 = InetAddress.getByName("www.limewire.com");
        InetAddress addr2 = InetAddress.getByName("www.microsoft.com");
        
        int port1 = 1234;
        int port2 = 4321;
        
        AddressSecurityToken key1 = new AddressSecurityToken(addr1, port1);
        AddressSecurityToken key2 = new AddressSecurityToken(addr1, port2);
        AddressSecurityToken key3 = new AddressSecurityToken(addr2, port1);
        
        assertTrue(key1.isFor(addr1, port1));
        assertFalse(key1.equals(key2));
        assertFalse(key1.equals(key3));
        assertFalse(key1.isFor(addr1, port2));
        assertFalse(key1.isFor(addr2, port1));
        assertFalse(key1.isFor(addr2, port2));
        
        assertTrue(key2.isFor(addr1, port2));
        assertFalse(key2.equals(key1));
        assertFalse(key2.equals(key3));
        assertFalse(key2.isFor(addr1, port1));
        assertFalse(key2.isFor(addr2, port1));
        assertFalse(key2.isFor(addr2, port2));
        
        assertTrue(key3.isFor(addr2, port1));
        assertFalse(key3.equals(key1));
        assertFalse(key3.equals(key2));
        assertFalse(key3.isFor(addr1, port1));
        assertFalse(key3.isFor(addr1, port2));
        assertFalse(key3.isFor(addr2, port2));
    }
    
    public void testOddsAndEnds() throws Exception {
        // test to make clover 100% for QK
        AbstractSecurityToken qk = new AddressSecurityToken(InetAddress.getLocalHost(), 6346);
        assertFalse(qk.equals(new Object()));
        qk.toString();
    }
    
    public void testQueryKeyExpiration() throws Exception {
        NotifyingSettingsProvider settings = new NotifyingSettingsProvider();
        MACCalculatorRepositoryManager.setSettingsProvider(settings);
        
        AddressSecurityToken key = new AddressSecurityToken(InetAddress.getLocalHost(), 4545);
        
        // wait for secret key change
        Thread.sleep(450);
        
        // key should still be valid
        assertTrue(key.isFor(InetAddress.getLocalHost(), 4545));
        // different port
        assertFalse(key.isFor(InetAddress.getLocalHost(), 4544));
        
        // wait for grace period to be over
        Thread.sleep(100);
        
        assertFalse(key.isFor(InetAddress.getLocalHost(), 4545));
    }
    
    private static class NotifyingSettingsProvider implements SettingsProvider {

        boolean notifyed = false;
        
        public synchronized long getChangePeriod() {
            notifyed = true;
            notify();
            return 400;
        }

        public long getGracePeriod() {
            return 100;
        }
        
        public synchronized void waitForRotation() throws InterruptedException {
            while (!notifyed) {
                wait();
            }
        }
        
    }
    
}
