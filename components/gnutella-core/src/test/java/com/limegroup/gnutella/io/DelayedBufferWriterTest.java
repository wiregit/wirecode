package com.limegroup.gnutella.io;


import java.nio.ByteBuffer;

import junit.framework.Test;

import com.limegroup.gnutella.connection.WriteBufferChannel;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;


public class DelayedBufferWriterTest extends BaseTestCase {

    private WriteBufferChannel source;
    private WriteBufferChannel sink;
    
    public DelayedBufferWriterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DelayedBufferWriterTest.class);
    }

    public void setUp() throws Exception {
        source = new WriteBufferChannel();
        sink = new WriteBufferChannel();
    }
    
    /**
     * tests that the chain is created properly
     */
    public void testChainCreation() throws Exception {
        assertFalse(sink.status);
        assertNull(sink.observer);
        assertNull(source.channel);
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(1);
        setupChain(delayer);
        // test that when we set the sink we register that we are interested in events
        assertEquals(delayer, sink.observer);
        assertTrue(sink.status);
        assertEquals(delayer, source.channel);
        assertEquals(sink,delayer.getWriteChannel());
        WriteObserver observer = (WriteObserver) PrivilegedAccessor.getValue(delayer, "observer");
        assertEquals(source,observer);
    }
    
    /**
     * tests that data is buffered until some time elapses
     */
    public void testTimeFlush() throws Exception {
        byte [] data = new byte[]{(byte)1,(byte)2};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(5);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(5);
        
        // data should go from the source to the buffer, but not to the sink
        delayer.handleWrite();
        
        assertEquals(2,source.position());
        assertEquals(0,sink.position());
        
        // after sleeping a while, data should move from the buffer to the sink
        Thread.sleep(300);
        delayer.handleWrite();
        
        assertEquals(2,source.position());
        assertEquals(2,sink.position());
        
        // data should be 1,2
        buf = sink.getBuffer();
        assertEquals(1,buf.get());
        assertEquals(2,buf.get());
    }
    
    /**
     * tests that if the buffer is filled up it gets flushed immediately
     */
    public void testForcedFlush() throws Exception {
        byte [] data = new byte[]{(byte)1,(byte)2,(byte)3};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(2);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(3);
        
        // data the size of the buffer should immediately be flushed,
        // and the rest should stay in the buffer
        
        delayer.handleWrite();

        assertEquals(3,source.position());
        assertEquals(2,sink.position());
        
        // data should be 1,2
        buf = sink.getBuffer();
        assertEquals(1,buf.get());
        assertEquals(2,buf.get());
    }
    
    /**
     * tests that the buffer always looks if there's more data available
     * before writing
     */
    public void testPartialBuffer() throws Exception {
        byte [] data = new byte[]{(byte)1};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(2);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(3);        
        
        // data should be buffered, and not written out
        delayer.handleWrite();
        assertEquals(1,source.position());
        assertEquals(0,sink.position());
        
        // add more data and sleep some time -
        // on the next write signal both new and old data should go out
        buf.rewind();
        source.setBuffer(buf);
        Thread.sleep(300);
        
        delayer.handleWrite();
        assertEquals(1,source.position());
        assertEquals(2,sink.position());
        
        // data should be 1,1
        buf = sink.getBuffer();
        assertEquals(1,buf.get());
        assertEquals(1,buf.get());
    }
    
    /**
     * tests that if nobody is interested in the buffer and its empty,
     * it turns itself off from the chain
     */
    public void testTurnsInterestOff() throws Exception {
        byte [] data = new byte[]{(byte)1};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(1);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(1);
        
        delayer.handleWrite(); // source->buf and source turns itself off
        Thread.sleep(300);
        delayer.handleWrite(); // buf-> sink
        
        // delayer should have turned itself off
        assertFalse(sink.status);
    }
    
    private void setupChain(DelayedBufferWriter delayer) {
        source.setWriteChannel(delayer);
        delayer.interest(source,true);
        delayer.setWriteChannel(sink);
    }
}
