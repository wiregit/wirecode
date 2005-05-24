package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.BaseTestCase;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import java.util.Random;

public class BiasedRandomDownloadStrategyTest extends BaseTestCase {
    
    /** Smallest number that can be subtracted from 1.0f 
     * and get something different from 1.0f.  Note that
     * this differs slightly from the standard IEE 754
     * definition of EPSILON by using subtraction instead
     * of addition.
     */
    private static float EPSILON;
    static {
        float epsilon = 1.0f;
        while((1.0f-epsilon)!=1.0f) {
            EPSILON = epsilon;
            epsilon /= 2.0f;
        }
    }
    
    private long fileSize;

    private long blockSize;

    private TestPredeterminedRandom prng;

    private SelectionStrategy strategy;

    private IntervalSet availableBytes;
    
    public BiasedRandomDownloadStrategyTest(String s) {
        super(s);
    }
    
    public void setUp() throws Exception {
        fileSize = 1024 * 1024 * 5; // 5MB
        blockSize = 128 * 1024; // 128k
        prng = new TestPredeterminedRandom();
        strategy = createBiasedRandomStrategy(fileSize, prng);
        availableBytes = IntervalSet.createSingletonSet(0, fileSize - 1);
    }

    /////////////// Tests /////////////////////////////////
    /** Test behavior for a file just over MIN_PREVIEW_BYTES (1 MB).
     *  Make sure downloads are sequential before MIN_PREVIEW_BYTES,
     *  and then never sequential after MIN_PREVIEW_BYTES.
     */
    public void testSmallFile() throws Exception {
        long previewLimit = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        fileSize = previewLimit + 2 * blockSize + 7;
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below 1MB, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(new Interval(2*blockSize-7, previewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.0f, 0.3728f, 0.18281828f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Interval[] expectations = new Interval[4];
        // full block chunk
        expectations[0] = new Interval(0, blockSize-1);
        // partial block chunk
        expectations[1] = new Interval(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = new Interval(previewLimit-blockSize-1);
        // full block
        expectations[3] = new Interval(previewLimit-blockSize,
                previewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Interval assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random", 
                previewLimit, assignment.low);
        
        // Check that we cannot still force a sequential download, since the preview
        // length is now greater than 50%
        prng.setFloat(0.0001f);
        prng.setInt(7);
        prng.setLong(1);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().low,
                assignment.low);
    }
    
    /** Test behavior for a file just over twice MIN_PREVIEW_BYTES.
     *  Make sure downloads are sequential before MIN_PREVIEW_BYTES, 
     *  50% random after MIN_PREVIEW_BYTES, and 100% random after the
     *  first 50% of the file has been downloaded.
     */
    public void testMediumFile() throws Exception {
        long previewLimit = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        fileSize = 2*previewLimit + 3;
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below 1MB, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(new Interval(2*blockSize-7, previewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.6298f, 0.77f, 0.0f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Interval[] expectations = new Interval[4];
        // full block chunk
        expectations[0] = new Interval(0, blockSize-1);
        // partial block chunk
        expectations[1] = new Interval(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = new Interval(previewLimit-blockSize-1);
        // full block
        expectations[3] = new Interval(previewLimit-blockSize,
                previewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Interval assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random.", 
                previewLimit, assignment.low);
        
        // Check that we still force a sequential download while the preview
        // length is less than 50%
        prng.setFloat(0.5f-EPSILON);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        assertEquals("Expected sequential assignment.",
                availableBytes.getFirst().low,
                assignment.low);
        assertEquals("Expected a full block assignment",
                blockSize, assignment.high-assignment.low+1);
        availableBytes.delete(assignment);
        
        // This last download pushed us over 50% previewable.
        // Test that we can no longer force the download to be non-random
        prng.setFloat(1.0f-EPSILON); // Nearly highest probability of being sequential
        prng.setInt(5);  // index into random locations array
        prng.setLong(1); // random location block offset
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().low,
                assignment.low);
        assertEquals("Expected a full block assignment",
                blockSize, assignment.high-assignment.low+1);
    }
    
   
    /** Test behavior for a file where fileSize * MIN_PREVIEW_FRACTION
     * is just over MIN_PREVIEW_BYTES. 
     */
    public void testLargeFile() throws Exception {
        long minPreviewBytes = ((Integer)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_BYTES")
                ).longValue(); // 1 MB
        double minPreviewFraction = ((Float)
                PrivilegedAccessor.getValue(strategy,"MIN_PREVIEW_FRACTION")
                ).doubleValue();
        fileSize = (long) ((minPreviewBytes / minPreviewFraction)+2*blockSize);
        
        // For this test, fileSize has been defined such that the preview limit
        // will be fileSize * minPreviewFraction
        long previewLimit = (long) Math.ceil(fileSize*minPreviewFraction);
        
        long alignedPreviewLimit = previewLimit + blockSize;
        alignedPreviewLimit -= alignedPreviewLimit % blockSize;
        
        availableBytes = IntervalSet.createSingletonSet(0, fileSize-1);
        // Cut out everything below previewFraction, except 2 blocks at the beginning
        // and end of that range.
        availableBytes.delete(new Interval(2*blockSize-7, 
                alignedPreviewLimit-blockSize-2));
        // 4 arbitrary floats for our 4 sequential downloads... make sure to test
        // 1.0 and 0.0
        float[] floats = {1.0f-EPSILON, 0.6298f, 0.77f, 0.0f};
        prng.setFloats(floats);
        
        strategy = createBiasedRandomStrategy(fileSize, prng);
        Interval[] expectations = new Interval[4];
        // full block chunk
        expectations[0] = new Interval(0, blockSize-1);
        // partial block chunk
        expectations[1] = new Interval(blockSize, 2*blockSize-8);
        // Skip a few...
        // single byte chunk
        expectations[2] = new Interval(alignedPreviewLimit-blockSize-1);
        // full block
        expectations[3] = new Interval(alignedPreviewLimit-blockSize,
                alignedPreviewLimit-1);
        
        // Check that we got our 4 sequential downloads
        testAssignments(strategy, availableBytes, fileSize,
                blockSize, expectations);
        
        // Now test that we go into non-sequential downloads
        // Set the float so that we get a non-sequential download durring the
        // 50% random phase.
        prng.setFloat(0.500001f);
        // Set the random index location and a non-sequential random location
        prng.setInt(3);
        prng.setLong(1);
        Interval assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        // Check that it wasn't a sequential download
        assertNotEquals("Got a sequential download when it should have been random.", 
                previewLimit, assignment.low);
        
        // Check that we still force a sequential download while the preview
        // length is less than 50%
        prng.setFloat(0.5f-EPSILON);
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        assertEquals("Expected sequential assignment.",
                availableBytes.getFirst().low,
                assignment.low);
        assertEquals("Expected a full block assignment",
                blockSize, assignment.high-assignment.low+1);
        availableBytes.delete(assignment);
        
        // Fast-forward to the case where the download is 
        // over 50% previewable.
        
        availableBytes.delete(new Interval(0, 1+fileSize/2));
        
        // Test that we can no longer force the download to be non-random
        prng.setFloat(1.0f-EPSILON); // Nearly highest probability of being sequential
        prng.setInt(5);  // index into random locations array
        prng.setLong(1); // random location block offset
        
        assignment = strategy.pickAssignment(availableBytes,
                availableBytes.getFirst().low, fileSize-1, blockSize);
        
        assertNotEquals("Expected non-sequential assignment",
                availableBytes.getFirst().low,
                assignment.low);
        assertEquals("Expected a full block assignment",
                blockSize, assignment.high-assignment.low+1);
    }
    
    public void testInvalidBlockSize() {
        // Try an invalid block size and see if it throws
        // an InvalidInputException
        try {
            strategy.pickAssignment(availableBytes, 0, 
                    fileSize-1, 0);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about invalid block size", false);
    }
    
    public void testInvalidPreviewLength() {
        // Try an invalid preview length and see if it throws
        // an InvalidInputException
        try {
            strategy.pickAssignment(availableBytes, -1, 
                    fileSize-1, blockSize);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about invalid preview length", false);
    }
    
    public void testInvalidLastNeededByte() {
        // Try calling with lastNeededByte > previewLength
        // and see if it throws an exception
        try {
            strategy.pickAssignment(availableBytes, 2001, 
                    2000, blockSize);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about invalid preview length", false);
    }
    
    ///////////// Helper Methods //////////////////////////
    /**
     * A helper method that simulates chosing blocks to download and removes
     * them from availableBytes.
     * 
     * @param strategy
     * @param availableBytes
     * @param fileSize
     * @param blockSize
     * @param blockCount
     * @param expectedAssignments
     */
    private void testAssignments(SelectionStrategy strategy,
            IntervalSet availableBytes, long fileSize, long blockSize,
            Interval[] expectedAssignments) {
        for (int i = 0; i < expectedAssignments.length; i++) {
            Interval assignment = strategy.pickAssignment(availableBytes, 0,
                    fileSize - 1, blockSize);
            availableBytes.delete(assignment);
            assertEquals("Wrong assignment for chunk #" + i,
                    expectedAssignments[i], assignment);
        }
    }
     
    /** Creates a BiasedRandomDownloadStrategy, and sets its private static Random. */
    private SelectionStrategy createBiasedRandomStrategy(long fileSize, Random rng)
            throws IllegalAccessException, NoSuchFieldException {
        BiasedRandomDownloadStrategy brds = new BiasedRandomDownloadStrategy(fileSize);
        PrivilegedAccessor.setValue(brds, "pseudoRandom", rng);
        return brds;
    }
}
