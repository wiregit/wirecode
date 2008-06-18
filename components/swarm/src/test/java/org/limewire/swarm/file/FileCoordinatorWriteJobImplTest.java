package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

import junit.framework.Test;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCache;
import org.limewire.util.BaseTestCase;

public class FileCoordinatorWriteJobImplTest extends BaseTestCase {
    
    private Mockery mockery;
    private FileCoordinatorWriteJobImpl writeJob;
    
    private IOControl ioctrl;
    private ExecutorService executorService;
    private FileCoordinator fileCoordinator;
    private ByteBufferCache byteBufferCache;
    private SwarmFile swarmFile;
    private long position;
    
    private ContentDecoder decoder;
    private ByteBuffer consumeBuffer;
    private ByteBuffer writeBuffer;
    private ScheduledFuture<Void> scheduledFuture;
    
    private ByteBuffer expectedBuffer;

    public FileCoordinatorWriteJobImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FileCoordinatorWriteJobImplTest.class);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        expectedBuffer = ByteBuffer.allocate(8192);
        for(int i = 0; i < expectedBuffer.limit(); i++) {
            expectedBuffer.put((byte)i);
        }
        expectedBuffer.flip();
        
        mockery = new Mockery();
        
        ioctrl = mockery.mock(IOControl.class);
        executorService = mockery.mock(ExecutorService.class);
        fileCoordinator = mockery.mock(FileCoordinator.class);
        byteBufferCache = mockery.mock(ByteBufferCache.class);
        swarmFile = mockery.mock(SwarmFile.class);
        position = 1024L;
        
        writeJob = new FileCoordinatorWriteJobImpl(position, ioctrl, executorService,
                fileCoordinator, byteBufferCache, swarmFile);
        
        decoder = mockery.mock(ContentDecoder.class);
        consumeBuffer = ByteBuffer.allocate(8192);
        consumeBuffer.put(expectedBuffer);
        consumeBuffer.flip();
        expectedBuffer.rewind();
        scheduledFuture = mockery.mock(ScheduledFuture.class);
        
        writeBuffer = ByteBuffer.allocate(8192);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }
    
    public void testCancelBeforeDoesNothing() {
        writeJob.cancel();
    }
    
    public void testAssertionsAreOn() {
        assertTrue("Assertions must be on for the tests.",
                FileCoordinatorWriteJobImpl.class.desiredAssertionStatus());
    }
    
    public void testConsumeMarksPending() throws Exception {        
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
    }
    
    public void testAnotherConsumeDoesntScheduleIfFirstDidntRunButWillIfFirstIsDone() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        mockery.checking(new Expectations() {{
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(2048, 3071));
            one(scheduledFuture).isDone();
                will(returnValue(false));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        mockery.checking(new Expectations() {{
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(3072, 4095));
            one(scheduledFuture).isDone();
                will(returnValue(true));
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
    }
    
    public void testReadEofThrowsIox() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(consumeBuffer);
                will(returnValue(-1));
        }});
        try {
            writeJob.consumeContent(decoder);
            fail();
        } catch(IOException iox) {
            assertEquals("Read EOF", iox.getMessage());
        }
    }
    
    public void testBufferFillsSuspendsIO() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 8192)));
                will(returnValue(8192));
            one(fileCoordinator).pending(Range.createRange(1024, 1024+8192-1));
            one(ioctrl).suspendInput();
            one(ioctrl).suspendOutput();
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(8192, writeJob.consumeContent(decoder));
    }
    
    public void testReadNothingPendsNothingAndDoesntSchedule() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(consumeBuffer);
                will(returnValue(0));
        }});        
        assertEquals(0, writeJob.consumeContent(decoder));
    }
    
    public void testCancelKillsJobAndUnpendsAndReleasesBuffer() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
         mockery.checking(new Expectations() {{
             one(scheduledFuture).cancel(false);
             one(fileCoordinator).unpending(Range.createRange(1024, 2047));
             one(byteBufferCache).release(consumeBuffer);
         }});
         writeJob.cancel();
         
         try {
             writeJob.consumeContent(decoder);
             fail();
         } catch(IOException expected) {
             assertEquals("Cancelled", expected.getMessage());
         }
    }
    
    public void testCancelCoalescesMultipleConsumesForUnpending() throws Exception {
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(new CallableMatcher()));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        mockery.checking(new Expectations() {{
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(2048, 3071));
            one(scheduledFuture).isDone();
                will(returnValue(false));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
         mockery.checking(new Expectations() {{
             one(scheduledFuture).cancel(false);
             one(fileCoordinator).unpending(Range.createRange(1024, 3071));
             one(byteBufferCache).release(consumeBuffer);
         }});
         writeJob.cancel();
         
         try {
             writeJob.consumeContent(decoder);
             fail();
         } catch(IOException expected) {
             assertEquals("Cancelled", expected.getMessage());
         }
    }
    
    public void testWriteSendsRightData() throws Exception {
        final CallableMatcher caller = new CallableMatcher();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(caller));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        final DecoderReader reader = new DecoderReader();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(writeBuffer));
            one(byteBufferCache).release(consumeBuffer);
            one(ioctrl).requestInput();
            one(ioctrl).requestOutput();
            one(swarmFile).transferFrom(with(reader), with(equal(1024L)));
                will(returnValue(1024L));
            one(fileCoordinator).wrote(Range.createRange(1024, 2047));
            one(byteBufferCache).release(writeBuffer);
        }});
        caller.callable.call();
        
        assertEquals(1024, reader.read.position());
        expectedBuffer.limit(1024);
        reader.read.flip();
        assertEquals(expectedBuffer, reader.read);
        
        writeJob.cancel(); // Make sure nothing gets called, since everything is written
    }
    
    public void testMultipleConsumesCoalesceIntoOneWrite() throws Exception {
        final CallableMatcher caller = new CallableMatcher();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(caller));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        mockery.checking(new Expectations() {{
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(2048, 3071));
            one(scheduledFuture).isDone();
                will(returnValue(false));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        final DecoderReader reader = new DecoderReader();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(writeBuffer));
            one(byteBufferCache).release(consumeBuffer);
            one(ioctrl).requestInput();
            one(ioctrl).requestOutput();
            one(swarmFile).transferFrom(with(reader), with(equal(1024L)));
                will(returnValue(2048L));
            one(fileCoordinator).wrote(Range.createRange(1024, 3071));
            one(byteBufferCache).release(writeBuffer);
        }});
        caller.callable.call();
        
        assertEquals(2048, reader.read.position());
        expectedBuffer.limit(2048);
        reader.read.flip();
        assertEquals(expectedBuffer, reader.read);
        
        writeJob.cancel(); // Make sure nothing gets called, since everything is written.
    }
    
    public void testWriteInIncrements() throws Exception {
        final CallableMatcher caller = new CallableMatcher();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(caller));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        final DecoderReader reader = new DecoderReader(500);
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(writeBuffer));
            one(byteBufferCache).release(consumeBuffer);
            one(ioctrl).requestInput();
            one(ioctrl).requestOutput();
            one(swarmFile).transferFrom(with(reader), with(equal(1024L)));
                will(returnValue(500L));
            one(fileCoordinator).wrote(Range.createRange(1024, 1523));
            one(swarmFile).transferFrom(with(reader), with(equal(1524L)));
                will(returnValue(500L));
            one(fileCoordinator).wrote(Range.createRange(1524, 2023));
            one(swarmFile).transferFrom(with(reader), with(equal(2024L)));
                will(returnValue(24L));                
            one(fileCoordinator).wrote(Range.createRange(2024, 2047));
            one(byteBufferCache).release(writeBuffer);
        }});
        caller.callable.call();
        
        assertEquals(1024, reader.read.position());
        expectedBuffer.limit(1024);
        reader.read.flip();
        assertEquals(expectedBuffer, reader.read);
        
        writeJob.cancel(); // Make sure nothing gets called, since everything is written
    }
    
    public void testIoxOnWriteUnpends() throws Exception {
        final CallableMatcher caller = new CallableMatcher();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(caller));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        final IOException thrownException = new IOException();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(writeBuffer));
            one(byteBufferCache).release(consumeBuffer);
            one(ioctrl).requestInput();
            one(ioctrl).requestOutput();
            one(swarmFile).transferFrom(with(any(ContentDecoder.class)), with(any(Long.class)));
               will(throwException(thrownException));
            one(fileCoordinator).unpending(Range.createRange(1024, 2047));
            one(ioctrl).shutdown();
            one(byteBufferCache).release(writeBuffer);
        }});
        
        try {
            caller.callable.call();
            fail();
        } catch(IOException iox) {
            assertEquals(thrownException, iox);
        }
    }
    
    public void testAnyExceptionOnWriteUnpends() throws Exception {
        final CallableMatcher caller = new CallableMatcher();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(consumeBuffer));
            one(decoder).read(with(new BufferAdvancer(consumeBuffer, 1024)));
                will(returnValue(1024));
            one(fileCoordinator).pending(Range.createRange(1024, 2047));
            one(executorService).submit(with(caller));
                will(returnValue(scheduledFuture));
        }});        
        assertEquals(1024, writeJob.consumeContent(decoder));
        
        final Throwable thrownException = new RuntimeException();
        mockery.checking(new Expectations() {{
            one(byteBufferCache).get(8192);
                will(returnValue(writeBuffer));
            one(byteBufferCache).release(consumeBuffer);
            one(ioctrl).requestInput();
            one(ioctrl).requestOutput();
            one(swarmFile).transferFrom(with(any(ContentDecoder.class)), with(any(Long.class)));
               will(throwException(thrownException));
            one(fileCoordinator).unpending(Range.createRange(1024, 2047));
            one(ioctrl).shutdown();
            one(byteBufferCache).release(writeBuffer);
        }});
        
        try {
            caller.callable.call();
            fail();
        } catch(RuntimeException iox) {
            assertEquals(thrownException, iox);
        }
    }
    
    private static class DecoderReader extends TypeSafeMatcher<ContentDecoder> {
        private final ByteBuffer read = ByteBuffer.allocate(8192);
        private final int allowPerRead;
        
        public DecoderReader(int allowPerRead) {
            this.allowPerRead = allowPerRead;
        }
        
        public DecoderReader() {
            this.allowPerRead = Integer.MAX_VALUE;
        }
        
        public void describeTo(Description description) {
            description.appendText("DecoderMatcher");
        }
        @Override
        public boolean matchesSafely(ContentDecoder item) {
            int oldLimit = read.limit();
            read.limit(Math.min(read.position() + allowPerRead, oldLimit));
            try {
                item.read(read);
            } catch(IOException iox) {
                throw new RuntimeException(iox);
            }
            read.limit(oldLimit);
            return true;
        }
    }
    
    private static class BufferAdvancer extends TypeSafeMatcher<ByteBuffer> {
        private final int advance;
        private final ByteBuffer expectedBuffer;
        
        public BufferAdvancer(ByteBuffer expectedBuffer, int advance) {
            this.advance = advance;
            this.expectedBuffer = expectedBuffer;
        }
        
        public void describeTo(Description description) {
            description.appendText("BufferAdvancer");
        }
        
        @Override
        public boolean matchesSafely(ByteBuffer item) {
            if(item != expectedBuffer)
                return false;
            
            item.position(item.position() + advance);
            return true;
        }
    }
    
    private static class CallableMatcher extends TypeSafeMatcher<Callable<Void>> {
        private Callable<Void> callable;
        
        public void describeTo(Description description) {
            description.appendText("CallableMatcher");
        }
        @Override
        public boolean matchesSafely(Callable<Void> item) {
            this.callable = item;
            return true;
        }
    }

}
