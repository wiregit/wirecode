package org.limewire.swarm.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.apache.http.nio.IOControl;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.BaseTestCase;

public class FileCoordinatorImplTest extends BaseTestCase {
    
    private Mockery mockery;
    private FileCoordinatorImpl fileCoordinator;
    
    private long fileSize;
    private SwarmFile swarmFile;
    private SwarmFileVerifier swarmFileVerifier;
    private ExecutorService executorService;
    private SelectionStrategy selectionStrategy;
    private long minBlockSize;

    public FileCoordinatorImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FileCoordinatorImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        swarmFile = mockery.mock(SwarmFile.class);
        swarmFileVerifier = mockery.mock(SwarmFileVerifier.class);
        executorService = mockery.mock(ExecutorService.class);
        selectionStrategy = mockery.mock(SelectionStrategy.class);
        fileSize = 32 * 1024;
        minBlockSize = 1024;
        
        fileCoordinator = new FileCoordinatorImpl(fileSize, swarmFile, swarmFileVerifier,
                executorService, selectionStrategy, minBlockSize);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }
    
    public void testLeaseGetsWholeFile() throws Exception {
        assertTrue(fileCoordinator.isRangeAvailableForLease());
        
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(
                        IntervalSet.createSingletonSet(0, fileSize - 1),
                        IntervalSet.createSingletonSet(0, fileSize - 1), fileSize);
                will(returnValue(Range.createRange(0, fileSize-1)));
        }});
        
        Range leased = fileCoordinator.lease();
        assertEquals(Range.createRange(0, fileSize-1), leased);
        assertFalse(fileCoordinator.isRangeAvailableForLease());        
        assertNull(fileCoordinator.lease());
    }
    
    public void testLeasePortionAllAvailableWithDifferentBlockSizes() throws Exception {
        assertTrue(fileCoordinator.isRangeAvailableForLease());
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).getBlockSize();
                will(returnValue(500L));
            one(selectionStrategy).pickAssignment(
                        IntervalSet.createSingletonSet(0, fileSize - 1),
                        IntervalSet.createSingletonSet(0, fileSize - 1), minBlockSize);
                will(returnValue(Range.createRange(0, 1023)));
        }});
        
        Range leased = fileCoordinator.leasePortion(null);
        assertEquals(Range.createRange(0, 1023), leased);
        
        assertTrue(fileCoordinator.isRangeAvailableForLease()); 
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1023)));
        
        // Getting another range will return just above that.
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).getBlockSize();
                will(returnValue(2048L));
            one(selectionStrategy).pickAssignment(
                    IntervalSet.createSingletonSet(1024, fileSize - 1),
                    IntervalSet.createSingletonSet(1024, fileSize - 1), 2048L);
                will(returnValue(Range.createRange(1024, 3191)));
        }});
        
        leased = fileCoordinator.leasePortion(null);
        assertEquals(Range.createRange(1024, 3191), leased);
        assertTrue(fileCoordinator.isRangeAvailableForLease()); 
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 3191)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 3192)));
    }
    
    public void testLeasePortionWithSomeAvailable() throws Exception {
        final IntervalSet available = new IntervalSet();
        available.add(Range.createRange(1024, 2047));
        available.add(Range.createRange(3192, 4095));
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).getBlockSize();
                will(returnValue(1024L));
            one(selectionStrategy).pickAssignment(
                    available, 
                    IntervalSet.createSingletonSet(0, fileSize - 1), minBlockSize);
                will(returnValue(Range.createRange(1024, 2047)));
        }});
        
        Range leased = fileCoordinator.leasePortion(available);
        assertEquals(Range.createRange(1024, 2047), leased);
        
        assertTrue(fileCoordinator.isRangeAvailableForLease()); 
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(1024, 2047)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(available));
        
        // Getting another range will return just above that.
        final IntervalSet needed = new IntervalSet();
        needed.add(Range.createRange(0, 1023));
        needed.add(Range.createRange(2048, fileSize - 1));
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).getBlockSize();
                will(returnValue(2048L));
            one(selectionStrategy).pickAssignment(
                    IntervalSet.createSingletonSet(3192, 4095),
                    needed, 2048L);
                will(returnValue(Range.createRange(3192, 4095)));
        }});
        
        leased = fileCoordinator.leasePortion(available);
        assertEquals(Range.createRange(3192, 4095), leased);
        assertTrue(fileCoordinator.isRangeAvailableForLease()); 
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 4095)));
        assertFalse(fileCoordinator.isRangeAvailableForLease(available));
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).getBlockSize();
                will(returnValue(2048L));
        }});
        assertNull(fileCoordinator.leasePortion(available));
    }
    
    public void testAssertionsAreOn() {
        assertTrue("This failed -- other tests may sporadically fail.",
                   FileCoordinatorImpl.class.desiredAssertionStatus());
    }
    
    public void testUnleaseReturnsForLeasingAndUnleaseRequiresLeased() {
        mockery.checking(new Expectations() {{
            allowing(swarmFileVerifier).getBlockSize();
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, 1024)));
            
        }});

        try {
            fileCoordinator.unlease(Range.createRange(0, 1024));
            fail();
        } catch(AssertionError expected) {}
        
        Range leased = fileCoordinator.lease();
        assertEquals(Range.createRange(0, 1024), leased);
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1024)));
        fileCoordinator.unlease(leased);
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1024)));
    }
    
    public void testPendingRequiresLeasedAndUnpendingRequiresPending() {        
        mockery.checking(new Expectations() {{
            allowing(swarmFileVerifier).getBlockSize();
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, 1024)));
            
        }});

        try {
            fileCoordinator.pending(Range.createRange(0, 1024));
            fail();
        } catch(AssertionError expected) {}
        
        Range leased = fileCoordinator.lease();
        
        try {
            fileCoordinator.unpending(leased);
            fail();
        } catch(AssertionError expected) {}
        
        fileCoordinator.pending(leased);
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1024)));
        
        try {
            fileCoordinator.unlease(leased);
            fail();
        } catch(AssertionError expected) {}
        
        fileCoordinator.unpending(leased);
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1024)));
    }
    
    public void testWroteRequiresPendingAndScansForVerifiable() {
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, 1024)));
            
        }});
        
        Range leased = fileCoordinator.lease();
        
        try {
            fileCoordinator.wrote(leased);
            fail();
        } catch(AssertionError expected) {}
        
        fileCoordinator.pending(leased);
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(IntervalSet.createSingletonSet(0, 1024), fileSize);
                will(returnValue(Collections.emptyList()));
        }});
        
        fileCoordinator.wrote(leased);
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1024)));
        
        try {
            fileCoordinator.wrote(leased);
            fail();
        } catch(AssertionError expected) {}
    }
    
    public void testVerifiesAfterWrote() {
        mockery.checking(new Expectations() {{
            allowing(swarmFileVerifier).getBlockSize();
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, 1024)));
            
        }});        
        Range leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(IntervalSet.createSingletonSet(0, 1024), fileSize);
                will(returnValue(Collections.emptyList()));
        }});        
        fileCoordinator.wrote(leased);
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(0, fileCoordinator.getAmountLost());
        
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(1025, 2048)));            
        }});        
        leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(IntervalSet.createSingletonSet(0, 2048), fileSize);
                will(returnValue(Collections.emptyList()));
        }});   
        fileCoordinator.wrote(leased);
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(0, fileCoordinator.getAmountLost());
        
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(4096, 5192)));            
        }});        
        leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);
        final IntervalSet written = new IntervalSet();
        written.add(Range.createRange(0, 2048));
        written.add(Range.createRange(4096, 5192));
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(written, fileSize);
                will(returnValue(Arrays.asList(Range.createRange(0, 1023), Range.createRange(1024, 2048), Range.createRange(4096, 5000))));
            
            one(swarmFileVerifier).verify(Range.createRange(0, 1023), swarmFile);
                will(returnValue(true));
            one(swarmFileVerifier).verify(Range.createRange(1024, 2048), swarmFile);
                will(returnValue(false));
            one(swarmFileVerifier).verify(Range.createRange(4096, 5000), swarmFile);
                will(returnValue(true));
            
        }});   
        fileCoordinator.wrote(leased);
        assertEquals(1929, fileCoordinator.getAmountVerified());
        assertEquals(1025, fileCoordinator.getAmountLost());
        
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1023)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(1024, 2048)));
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(4096, 5192)));
    }
    
    public void testVerifyAndReverify() {
        mockery.checking(new Expectations() {{
            allowing(swarmFileVerifier).getBlockSize();
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, 1024)));
            
        }});        
        Range leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Collections.emptyList()));
        }});        
        fileCoordinator.wrote(leased);
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(0, fileCoordinator.getAmountLost());
        
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(1025, 2048)));            
        }});        
        leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Collections.emptyList()));
        }});   
        fileCoordinator.wrote(leased);
        assertEquals(0, fileCoordinator.getAmountVerified());
        assertEquals(0, fileCoordinator.getAmountLost());
        
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(4096, 5192)));            
        }});        
        leased = fileCoordinator.lease();
        fileCoordinator.pending(leased);
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Collections.emptyList()));
        }});   
        fileCoordinator.wrote(leased);
        
        assertEquals(0, fileCoordinator.getAmountLost());
        assertEquals(0, fileCoordinator.getAmountVerified());
        
        final IntervalSet written = new IntervalSet();
        written.add(Range.createRange(0, 2048));
        written.add(Range.createRange(4096, 5192));
        final AtomicReference<Runnable> runRef = new AtomicReference<Runnable>();
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(written, fileSize);
                will(returnValue(Arrays.asList(Range.createRange(0, 1023), Range.createRange(1024, 2048), Range.createRange(4096, 5000))));

            one(executorService).execute(with(new TypeSafeMatcher<Runnable>() {
                public void describeTo(Description description) {
                    description.appendText("RunnableMatcher");
                }
                @Override
                public boolean matchesSafely(Runnable item) {
                    runRef.set(item);
                    return true;
                }
            }));
        }});
        fileCoordinator.verify();
        
        mockery.checking(new Expectations() {{   
            one(swarmFileVerifier).verify(Range.createRange(0, 1023), swarmFile);
                will(returnValue(true));
            one(swarmFileVerifier).verify(Range.createRange(1024, 2048), swarmFile);
                will(returnValue(false));
            one(swarmFileVerifier).verify(Range.createRange(4096, 5000), swarmFile);
                will(returnValue(true));
            
        }});   
        runRef.get().run();
        assertEquals(1929, fileCoordinator.getAmountVerified());
        assertEquals(1025, fileCoordinator.getAmountLost());
        
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1023)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(1024, 2048)));
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(4096, 5192)));
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(IntervalSet.createSingletonSet(5001, 5192), fileSize);
                will(returnValue(Collections.emptyList()));
        }});
        fileCoordinator.verify(); // Verify again checks the remaining written blocks.
        assertEquals(1929, fileCoordinator.getAmountVerified());
        assertEquals(1025, fileCoordinator.getAmountLost());
        
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1023)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(1024, 2048)));
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(4096, 5192)));
        
        written.clear();
        written.add(Range.createRange(0, 1023));
        written.add(Range.createRange(4096, 5192));
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(written, fileSize);
                will(returnValue(Arrays.asList(Range.createRange(0, 1023), Range.createRange(4096, 5000))));

            one(executorService).execute(with(new TypeSafeMatcher<Runnable>() {
                public void describeTo(Description description) {
                    description.appendText("RunnableMatcher");
                }
                @Override
                public boolean matchesSafely(Runnable item) {
                    runRef.set(item);
                    return true;
                }
            }));
        }});
        fileCoordinator.reverify(); // Verifies all written + preverified
        
        mockery.checking(new Expectations() {{   
            one(swarmFileVerifier).verify(Range.createRange(0, 1023), swarmFile);
                will(returnValue(false));
            one(swarmFileVerifier).verify(Range.createRange(4096, 5000), swarmFile);
                will(returnValue(true));
            
        }});   
        runRef.get().run();
        assertEquals(905, fileCoordinator.getAmountVerified());
        assertEquals(1025 + 1024, fileCoordinator.getAmountLost()); // prior lost + new lost
        
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(0, 1023)));
        assertTrue(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(1024, 2048)));
        assertFalse(fileCoordinator.isRangeAvailableForLease(IntervalSet.createSingletonSet(4096, 5192)));
    }
    
    public void testListenersAreNotifiedWhenCompleteWithoutVerified() {
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, fileSize-1)));
            
        }});
        Range leased = fileCoordinator.lease();
        assertEquals(Range.createRange(0, fileSize-1), leased);
        fileCoordinator.pending(leased);
        
        final SwarmFileCompletionListener listener = mockery.mock(SwarmFileCompletionListener.class);
        fileCoordinator.addCompletionListener(listener);
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Collections.emptyList()));
            one(listener).fileCompleted(fileCoordinator, swarmFile);
        }});
        fileCoordinator.wrote(leased);
    }
    
    public void testListenersAreNotifiedWhenVerified() {
        mockery.checking(new Expectations() {{
            one(selectionStrategy).pickAssignment(with(any(IntervalSet.class)),
                        with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Range.createRange(0, fileSize-1)));
            
        }});
        Range leased = fileCoordinator.lease();
        assertEquals(Range.createRange(0, fileSize-1), leased);
        fileCoordinator.pending(leased);
        
        final SwarmFileCompletionListener listener = mockery.mock(SwarmFileCompletionListener.class);
        fileCoordinator.addCompletionListener(listener);
        
        mockery.checking(new Expectations() {{
            one(swarmFileVerifier).scanForVerifiableRanges(with(any(IntervalSet.class)), with(any(Long.class)));
                will(returnValue(Arrays.asList(Range.createRange(0, fileSize-1))));
            one(swarmFileVerifier).verify(Range.createRange(0, fileSize-1), swarmFile);
                will(returnValue(true));
            one(listener).fileCompleted(fileCoordinator, swarmFile);
        }});
        fileCoordinator.wrote(leased);
    }
    
    public void testWriteJobGivesRightJob() {
        assertInstanceof(FileCoordinatorWriteJobImpl.class, fileCoordinator.newWriteJob(0, mockery.mock(IOControl.class)));
        // See FileCoordinatorWriteJobImpl tests for tests of that class.
    }
    
    
}
