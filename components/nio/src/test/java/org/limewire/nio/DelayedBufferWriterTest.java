package org.limewire.nio;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;

import org.limewire.concurrent.SchedulingThreadPool;
import org.limewire.nio.DelayedBufferWriter;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;


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
        
        // test closing/opening
        assertTrue(delayer.isOpen());
        delayer.close();
        assertFalse(delayer.isOpen());
        
        // closing of the sink should propagate to the delayer
        setUp();
        delayer = new DelayedBufferWriter(1,200, new StubScheduler());
        setupChain(delayer);
        assertTrue(delayer.isOpen());
        sink.close();
        assertFalse(delayer.isOpen());
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
     * Tests that excplicitly invoked flush writes as much data as it can.
     * @throws Exception
     */
    public void testExplicitFlush() throws Exception {
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
        
        // flushing should write out everything
        assertTrue(delayer.flush());
        assertEquals(2, sink.position());
        
        // flushing an empty buffer does nothing
        assertTrue(delayer.flush());
        assertEquals(2, sink.position());
        
        // add more data
        buf = ByteBuffer.allocate(5);
        source.setBuffer(buf);
        delayer.handleWrite();
        
        // some should be written, not yet flushed
        assertEquals(5, source.position());
        assertEquals(2, sink.position());
        
        // a flush will not be able to write everything
        assertFalse(delayer.flush());
        assertEquals(5, sink.position());
        
        // no matter how many times we call it...
        assertFalse(delayer.flush());
        assertFalse(delayer.flush());
        assertFalse(delayer.flush());
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
        
        // but there should be a scheduled flusher
        assertFalse(scheduler.tasks.isEmpty());
        Runnable r = scheduler.tasks.get(0);
        
        // that should turn interest on
        Thread.sleep(200);
        r.run();
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
        assertFalse(scheduler.futures.isEmpty());
        assertFalse(scheduler.futures.get(0).isCancelled());
        
        // buf if in the meantime someone becomes interested in the
        // delayer again the cleaner should be cancelled.
        delayer.interest(source, true);
        assertTrue(scheduler.futures.get(0).isCancelled());
        
        // even if the cancelling happens too late, the interester
        // should still do nothing.
        PrivilegedAccessor.setValue(sink,"status", Boolean.FALSE);
        Thread.sleep(200);
        scheduler.tasks.get(0).run();
        assertFalse(sink.status);
        
    }
    
    private void setupChain(DelayedBufferWriter delayer) {
        source.setWriteChannel(delayer);
        delayer.interest(source,true);
        delayer.setWriteChannel(sink);
    }
    
    private class StubScheduler implements SchedulingThreadPool {

    	List<Runnable> tasks = new ArrayList<Runnable>();
    	List<StubFuture> futures = new ArrayList<StubFuture>();
		public Future invokeLater(Runnable r, long delay) {
			tasks.add(r);
			StubFuture f = new StubFuture(delay, r); 
			futures.add(f);
			return f;
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
			return cancelled;
		}

		public boolean isDone() {
			return false;
		}
    	
    }
}
