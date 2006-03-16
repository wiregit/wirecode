package com.limegroup.gnutella.handshaking;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.connection.WriteBufferChannel;
import com.limegroup.gnutella.util.BaseTestCase;

public class AsyncHandshakerTest extends BaseTestCase {
    
    public AsyncHandshakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testInterestSetCorrectly() {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        StubHandshakeState state2 = new StubHandshakeState();
        state2.setWriting(true);
        state2.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        states.add(state2);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        
        assertFalse(readChannel.isInterested());
        shake.setReadChannel(readChannel);
        assertTrue(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        shake.setWriteChannel(writeChannel);
        assertTrue(writeChannel.interested());
 
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // First state is reading, so let's see what happens if we set a write hit.
        readChannel.interest(false);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertFalse(state2.isProcessed());
 
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());

        // Now try a correct read...
        readChannel.interest(true);
        shake.handleRead();
        assertTrue(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertTrue(state1.isProcessed());
        assertFalse(state2.isProcessed());
        state1.clear();

        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // What'll happen when this state finishes...
        state1.setReturnTrueOnProcess(false);
        shake.handleRead();
        assertFalse(readChannel.isInterested());
        assertTrue(writeChannel.interested());
        assertTrue(state1.isProcessed());
        assertFalse(state2.isProcessed());
        state1.clear();

        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // State is now state2, which is writing.. let's see what happens if we hit a read.
        writeChannel.interest(null, false);
        shake.handleRead();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertFalse(state2.isProcessed());
 
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // Now let's try a correct write...
        writeChannel.interest(null, true);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertTrue(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertTrue(state2.isProcessed());
        state2.clear();
 
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // And if the write is set to not continue?
        state2.setReturnTrueOnProcess(false);
        shake.handleWrite();
        assertFalse(readChannel.isInterested());
        assertFalse(writeChannel.interested());
        assertFalse(state1.isProcessed());
        assertTrue(state2.isProcessed());
        state2.clear();
        
        assertTrue(observer.isHandshakeFinished());
        assertEquals(handshaker, observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());        
    }
    
    public void testStateIOX() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        state1.setThrowIOX(true);
        shake.handleRead();
        
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        observer.clear();
        shake.shutdown(); // make sure it doesn't call stuff again
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
    }
    
    public void testStateNGOK() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        state1.setThrowNGOK(true, 303);
        shake.handleRead();
        
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertTrue(observer.isNoGOK());
        assertEquals(303, observer.getCode());
        assertFalse(observer.isShutdown());
        
        observer.clear();
        shake.shutdown(); // make sure it doesn't call stuff again
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
    }
    
    public void testShutdownShakerWhileOpen() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        shake.shutdown();
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());
        
        // no more processing after this point!
        shake.handleRead();
        assertFalse(state1.isProcessed());
    }
    
    public void testShutdownShakerWhileClosed() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        readChannel.setClosed(true);
        shake.shutdown();
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isShutdown());
        
        observer.clear();
        shake.shutdown(); // make sure it doesn't call stuff again
        assertFalse(observer.isHandshakeFinished());
        assertNull(observer.getShaker());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertFalse(observer.isShutdown());        
        
        // no more processing after this point!
        shake.handleRead();
        assertFalse(state1.isProcessed());        
    }
    
    public void testReadBuffer() throws Exception {
        ReadBufferChannel readChannel = new ReadBufferChannel();
        WriteBufferChannel writeChannel = new WriteBufferChannel();
        StubHandshakeObserver observer = new StubHandshakeObserver();
        StubHandshaker handshaker = new StubHandshaker();
        StubHandshakeState state1 = new StubHandshakeState();
        state1.setReading(true);
        state1.setReturnTrueOnProcess(true);
        List states = new LinkedList();
        states.add(state1);
        
        AsyncHandshaker shake = new AsyncHandshaker(handshaker, observer, states);
        shake.setReadChannel(readChannel);
        shake.setWriteChannel(writeChannel);
        
        state1.setDataToPutInBuffer("DATA".getBytes());
        shake.handleRead();
        
        ByteBuffer buffer = ByteBuffer.allocate(5);
        shake.read(buffer);
        assertEquals("DATA\0", new String(buffer.array()));
        assertEquals(4, buffer.position());
    }

}
