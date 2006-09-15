package com.limegroup.mojito.messages;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.BaseTestCase;

public class MessageIDTest extends BaseTestCase {
    
    public MessageIDTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageIDTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testEquals() {
        MessageID messageId1 = MessageID.create(new InetSocketAddress("localhost", 1024));
        MessageID messageId2 = MessageID.create(new InetSocketAddress("localhost", 1024));
        
        // Except for the first four bytes (QueryKey) they shouldn't be equal
        assertNotEquals(messageId1, messageId2);
        for (int i = 0; i < 4; i++) {
            assertEquals(messageId1.getBytes()[i], messageId2.getBytes()[i]);
        }
        
        // Same if created from the bytes
        MessageID messageId3 = MessageID.create(messageId2.getBytes());
        assertEquals(messageId2, messageId3);
        
        // Same if created from the hex string
        MessageID messageId4 = MessageID.create(messageId2.toHexString());
        assertEquals(messageId4, messageId3);
    }
    
    public void testEmbeddedQueryKey() throws Exception {
        InetSocketAddress addr1 = new InetSocketAddress("localhost", 1234);
        QueryKey key1 = QueryKey.getQueryKey(addr1);
        
        MessageID messageId1 = MessageID.create(addr1);
        Method m = MessageID.class.getDeclaredMethod("getQueryKey", new Class[0]);
        m.setAccessible(true);
        QueryKey key2 = (QueryKey)m.invoke(messageId1, new Object[0]);
        
        assertTrue(key1.equals(key2));
        assertTrue(messageId1.verifyQueryKey(addr1));
        
        InetSocketAddress addr2 = new InetSocketAddress("www.google.com", 1234);
        assertFalse(messageId1.verifyQueryKey(addr2));
    }
}
