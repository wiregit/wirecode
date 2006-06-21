package com.limegroup.gnutella.io;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;

import com.limegroup.gnutella.connection.WriteBufferChannel;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.SchedulingThreadPool;


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
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(1,200, new StubScheduler());
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
        
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(5,200, new StubScheduler());
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
        
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(2,200, new StubScheduler());
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
        
        DelayedBufferWriter delayer = new DelayedBufferWriter(2,200, new StubScheduler());
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
     * tests that if nobody is interested in the buffer but its not empty,
     * it will turn its interest off but will schedule a flushing.
     * @throws Exception
     */
    public void testFlushingScheduled() throws Exception {
        byte [] data = new byte[]{(byte)2};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        StubScheduler scheduler = new StubScheduler();
        DelayedBufferWriter delayer = new DelayedBufferWriter(2,200, scheduler);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(1);
        
        delayer.handleWrite(); // source->buf and source turns itself off
        // delayer should have turned itself off
        assertFalse(sink.status);
        
        // but there should be a scheduled cleaner
        assertFalse(scheduler.tasks.isEmpty());
        StubFuture future = scheduler.tasks.get(0);
        assertGreaterThan(190, future.delay);
        
        // that should turn interest on
        future.r.run();
        assertTrue(sink.status);
    }
    
    public void testFlushingCancelled() throws Exception {
    	byte [] data = new byte[]{(byte)2};
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        StubScheduler scheduler = new StubScheduler();
        DelayedBufferWriter delayer = new DelayedBufferWriter(2,200, scheduler);
        setupChain(delayer);
        source.setBuffer(buf);
        sink.resize(1);
        
        delayer.handleWrite(); // source->buf and source turns itself off
        // delayer should have turned itself off
        assertFalse(sink.status);
        
        // but there should be a scheduled cleaner
        assertFalse(scheduler.tasks.isEmpty());
        
        // buf if in the meantime someone becomes interested in the
        // delayer again the cleaner should be cancelled.
        delayer.interest(source, true);
        assertTrue(scheduler.tasks.get(0).cancelled);
        
    }
    
    private void setupChain(DelayedBufferWriter delayer) {
        source.setWriteChannel(delayer);
        delayer.interest(source,true);
        delayer.setWriteChannel(sink);
    }
    
    private class StubScheduler implements SchedulingThreadPool {

    	List<StubFuture> tasks = new ArrayList<StubFuture>();
		public Future invokeLater(Runnable r, long delay) {
			StubFuture future = new StubFuture(delay, r);
			tasks.add(future);
			return future;
		}

		public void invokeLater(Runnable runner) {
			throw new IllegalArgumentException();
		}
    }
    
    private class StubFuture implements Future {
    	final long delay;
    	final Runnable r;
    	boolean cancelled;
    	StubFuture(long delay, Runnable r) {
    		this.delay = delay;
    		this.r = r;
    	}
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return false;
		}

		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return false;
		}
    	
    }
}
