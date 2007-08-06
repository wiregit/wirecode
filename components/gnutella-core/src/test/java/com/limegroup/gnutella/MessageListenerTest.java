package com.limegroup.gnutella;

import java.util.Map;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Tests Message Listener usage in MessageRouter.
 */
public class MessageListenerTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super

    public MessageListenerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MessageListenerTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testMessageListener() throws Exception {
        final GUID guid = new GUID(GUID.makeGuid());
        final MLImpl ml = new MLImpl();

        PingReply pong = ProviderHacks.getPingReplyFactory().create(guid.bytes(), (byte) 2);

        ProviderHacks.getMessageRouter().registerMessageListener(guid.bytes(), ml);
        Map map = getMap();
        assertEquals(1, map.size());
        assertEquals(0, ml.count);
        assertTrue(ml.registered);

        // send off the pong for processing
        testUP[0].send(pong);
        testUP[0].flush();
        Thread.sleep(2000);
        
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid.bytes(), ml);
        map = getMap();
        assertEquals(0, map.size());
        assertEquals(1, ml.count);
        assertTrue(ml.unregistered);
    }
    
    public void testMultipleListenersWithOneGUID() throws Exception {
        final GUID guid = new GUID(GUID.makeGuid());
        final MLImpl ml1 = new MLImpl();
        final MLImpl ml2 = new MLImpl();
        
        PingReply pong = ProviderHacks.getPingReplyFactory().create(guid.bytes(), (byte) 2);

        ProviderHacks.getMessageRouter().registerMessageListener(guid.bytes(), ml1);
        ProviderHacks.getMessageRouter().registerMessageListener(guid.bytes(), ml2);
        Map map = getMap();
        assertEquals(1, map.size());
        assertEquals(0, ml1.count);
        assertEquals(0, ml2.count);        
        assertTrue(ml1.registered);
        assertTrue(ml2.registered);

        // send off the pong for processing
        testUP[0].send(pong);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(1, ml2.count);
        
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid.bytes(), ml1);
        assertTrue(ml1.unregistered);
        assertFalse(ml2.unregistered);
        map = getMap();
        assertEquals(1, map.size());
        
        // send off the pong for processing
        testUP[0].send(pong);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(2, ml2.count);

        ProviderHacks.getMessageRouter().unregisterMessageListener(guid.bytes(), ml2);
        assertTrue(ml2.unregistered);
        map = getMap();
        assertEquals(0, map.size());
    }
    
    public void testMultipleGUIDListeners() throws Exception {
        final GUID guid1 = new GUID(GUID.makeGuid());
        final GUID guid2 = new GUID(GUID.makeGuid());
        final MLImpl ml1 = new MLImpl();
        final MLImpl ml2 = new MLImpl();
        
        PingReply pong1 = ProviderHacks.getPingReplyFactory().create(guid1.bytes(), (byte) 2);
        PingReply pong2 = ProviderHacks.getPingReplyFactory().create(guid2.bytes(), (byte) 2);

        ProviderHacks.getMessageRouter().registerMessageListener(guid1.bytes(), ml1);
        ProviderHacks.getMessageRouter().registerMessageListener(guid2.bytes(), ml2);
        Map map = getMap();
        assertEquals(2, map.size());
        assertEquals(0, ml1.count);
        assertEquals(0, ml2.count);        
        assertTrue(ml1.registered);
        assertTrue(ml2.registered);

        // send off the pong for processing
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(0, ml2.count);
        
        // send off the pong for processing
        testUP[0].send(pong2);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(1, ml2.count);        
        
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid1.bytes(), ml1);
        assertTrue(ml1.unregistered);
        assertFalse(ml2.unregistered);
        map = getMap();
        assertEquals(1, map.size());
        
        // send off the pong for processing -- nothing should get it.
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(1, ml2.count);

        ProviderHacks.getMessageRouter().unregisterMessageListener(guid2.bytes(), ml2);
        assertTrue(ml2.unregistered);
        map = getMap();
        assertEquals(0, map.size());
    }
    
    public void testFunkyAddingAndRemoving() throws Exception {
        final GUID guid1 = new GUID(GUID.makeGuid());
        final GUID guid2 = new GUID(GUID.makeGuid());
        final MLImpl ml1 = new MLImpl();
        final MLImpl ml2 = new MLImpl();
        
        PingReply pong1 = ProviderHacks.getPingReplyFactory().create(guid1.bytes(), (byte) 2);
        PingReply pong2 = ProviderHacks.getPingReplyFactory().create(guid2.bytes(), (byte) 2);

        ProviderHacks.getMessageRouter().registerMessageListener(guid1.bytes(), ml1);
        ProviderHacks.getMessageRouter().registerMessageListener(guid2.bytes(), ml2);
        Map map = getMap();
        assertEquals(2, map.size());       
        assertTrue(ml1.registered);
        assertTrue(ml2.registered);
        
        // send off the pong for processing
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(1, ml1.count);
        assertEquals(0, ml2.count);        
        
        // add ml1 again to ml1 -- should be notified twice per message now
        ProviderHacks.getMessageRouter().registerMessageListener(guid1.bytes(), ml1);
        map = getMap();
        assertEquals(2, map.size());       
        assertTrue(ml1.registered);
        assertTrue(ml2.registered);
        
        // send off the pong for processing
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(3, ml1.count);
        assertEquals(0, ml2.count);        
        
        // remove ml1 -- should be notified just once on the next send.
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid1.bytes(), ml1);
        map = getMap();
        assertEquals(2, map.size());       
        assertTrue(ml1.registered);
        assertTrue(ml1.unregistered);
        assertTrue(ml2.registered);
        
        // send off the pong for processing
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(4, ml1.count);
        assertEquals(0, ml2.count);
        
        // try removing ml1 from guid2's bytes, nothing should happen.
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid2.bytes(), ml1);
        map = getMap();
        assertEquals(2, map.size());       
        assertTrue(ml1.registered);
        assertTrue(ml1.unregistered);
        assertTrue(ml2.registered);
        
        // send off the pong for processing
        testUP[0].send(pong1);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(5, ml1.count);
        assertEquals(0, ml2.count);
        
        // now send ml2's pong.
        testUP[0].send(pong2);
        testUP[0].flush();
        Thread.sleep(2000);
        
        assertEquals(5, ml1.count);
        assertEquals(1, ml2.count);
        
        // and unregister the two of'm.
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid1.bytes(), ml1);
        map = getMap();
        assertEquals(1, map.size());
        ProviderHacks.getMessageRouter().unregisterMessageListener(guid2.bytes(), ml2);
        map = getMap();
        assertEquals(0, map.size());
        assertTrue(ml2.unregistered);
        
        // send pongs again -- nothing should get'm.
        testUP[0].send(pong1);
        testUP[0].send(pong2);
        testUP[0].flush();
        Thread.sleep(2000);

        assertEquals(5, ml1.count);
        assertEquals(1, ml2.count);
    }
    //////////////////////////////////////////////////////////////////
    
    private final Map getMap() throws Exception {
        return (Map) PrivilegedAccessor.getValue(ProviderHacks.getMessageRouter(),
                                          "_messageListeners");
    }
    
    private class MLImpl implements MessageListener {
        public int count = 0;
        public boolean registered = false;
        public boolean unregistered = false;
        public void processMessage(Message m, ReplyHandler handler) {
            assertTrue(m instanceof PingReply);
            count++;
        }
        public void registered(byte[] guid) {
            registered = true;
        }
        public void unregistered(byte[] guid) {
            unregistered = true;
        }
    }

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
}
