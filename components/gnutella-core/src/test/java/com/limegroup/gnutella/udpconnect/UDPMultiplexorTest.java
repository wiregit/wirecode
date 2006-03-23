package com.limegroup.gnutella.udpconnect;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class UDPMultiplexorTest extends BaseTestCase {


    public UDPMultiplexorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UDPMultiplexorTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testRegister() throws Exception {
        Selector selector = UDPSelectorProvider.instance().openSelector();
        assertInstanceof(UDPMultiplexor.class, selector);
        
        SocketChannel channels[] = new SocketChannel[256];
        SelectionKey  keys[] = new SelectionKey[256];
        for(int i = 0; i < channels.length; i++) {
            channels[i] = UDPSelectorProvider.instance().openSocketChannel();
            assertFalse(channels[i].isRegistered());
            keys[i] = channels[i].register(selector, 1);
            assertTrue(channels[i].isRegistered());
            assertSame(keys[i], channels[i].keyFor(selector));
            assertEquals(1, keys[i].interestOps());
            assertSame(selector, keys[i].selector());
        }

        Set allKeys = selector.keys();
        assertEquals(256, allKeys.size());
        for(int i = 0; i < keys.length; i++)
            assertContains(allKeys, keys[i]);
        
        // All could register except the last (they were all full)
        
        int n = selector.selectNow();
        assertEquals(1, n);
        Set selected = selector.selectedKeys();
        assertEquals(1, selected.size());
        SelectionKey key = (SelectionKey)selected.iterator().next();
        assertFalse(key.isValid());
        try {
            assertEquals(0, key.readyOps());
            fail("should have failed");
        } catch(CancelledKeyException expected) {}
        
        assertSame(keys[255], key);
 
        assertNotContains(selector.keys(), key);
    }
    
    public void testClosedChannelsRemoved() throws Exception {
        Selector selector = UDPSelectorProvider.instance().openSelector();
        assertInstanceof(UDPMultiplexor.class, selector);
        
        SocketChannel channel = UDPSelectorProvider.instance().openSocketChannel();
        SelectionKey key = channel.register(selector, 0);
        assertSame(key, channel.keyFor(selector));
        Set keys = selector.keys();
        assertEquals(1, keys.size());
        assertContains(keys, key);
        assertTrue(key.isValid());
        
        channel.close();
        assertFalse(key.isValid());
        assertSame(key, channel.keyFor(selector));
        // Selector still has it because no select has been performed.
        keys = selector.keys();
        assertEquals(1, keys.size());
        assertContains(keys, key);
        
        // Now do a select and it'll return remove the channel
        int n = selector.selectNow();
        assertEquals(0, n);
        
        assertEquals(0, selector.keys().size());
    }
    
    public void testSelectedKeys()  throws Exception {
        Selector selector = UDPSelectorProvider.instance().openSelector();
        assertInstanceof(UDPMultiplexor.class, selector);
        
        StubUDPSocketChannel channel = new StubUDPSocketChannel();
        SelectionKey key = channel.register(selector, 0);
        
        assertEquals(0, selector.selectNow());
        
        key.interestOps(SelectionKey.OP_CONNECT);
        assertEquals(0, selector.selectNow());
        
        channel.setReadyOps(SelectionKey.OP_CONNECT);
        assertEquals(1, selector.selectNow());
        Set selectedKeys = selector.selectedKeys();
        assertEquals(1, selectedKeys.size());
        assertContains(selectedKeys, key);
        assertEquals(SelectionKey.OP_CONNECT, key.readyOps());
        
        key.interestOps(0);
        assertEquals(0, selector.selectNow());
        
        key.interestOps(SelectionKey.OP_READ);
        channel.setReadyOps(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
        assertEquals(0, selector.selectNow());
        channel.setReadyOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        assertEquals(1, selector.selectNow());
        selectedKeys = selector.selectedKeys();
        assertEquals(1, selectedKeys.size());
        assertContains(selectedKeys, key);
        assertEquals(SelectionKey.OP_READ, key.readyOps());
        
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        assertEquals(1, selector.selectNow());
        selectedKeys = selector.selectedKeys();
        assertEquals(1, selectedKeys.size());
        assertContains(selectedKeys, key);
        assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key.readyOps());        
    }
    
    
    private static class StubUDPSocketChannel extends UDPSocketChannel {
        private StubProcessor stubProcessor = new StubProcessor(this);
        
        StubUDPSocketChannel() {
            super(null);
        }
        
        UDPConnectionProcessor getProcessor() {
            return stubProcessor;
        }
        
        void setReadyOps(int readyOps) {
            stubProcessor.setReadyOps(readyOps);
        }
    }
    
    private static class StubProcessor extends UDPConnectionProcessor {
        private int readyOps;
        
        StubProcessor(UDPSocketChannel channel) {
            super(channel);
        }
        
        int readyOps() {
            return readyOps;
        }
        
        void setReadyOps(int readyOps) {
            this.readyOps = readyOps;
        }
    }
}
