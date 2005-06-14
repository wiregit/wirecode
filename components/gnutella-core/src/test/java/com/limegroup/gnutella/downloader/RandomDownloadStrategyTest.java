package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.BaseTestCase;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import java.util.Random;
import java.util.NoSuchElementException;

public class RandomDownloadStrategyTest extends BaseTestCase {

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
    
    public RandomDownloadStrategyTest(String s) {
        super(s);
    }

    private long fileSize;

    private long blockSize;

    private TestPredeterminedRandom prng;

    private SelectionStrategy strategy;

    private IntervalSet availableBytes;

    public void setUp() throws Exception {
        fileSize = 12345;
        blockSize = 1234;
        prng = new TestPredeterminedRandom();
        strategy = createRandomStrategy(fileSize, prng);
        availableBytes = IntervalSet.createSingletonSet(0, fileSize - 1);
    }

    public void testSequentialLocations() throws Exception {
        // Set up prng so we should download
        // blocks 5 and 6 (6th and 7th block)
        prng.setLong(5);
        // Two identical non-zero indexes into
        // the random locations pool
        int[] testIndexes = { 9, 9 };
        prng.setInts(testIndexes);

        // Set the expected assignments
        Interval[] expectations = new Interval[2];
        expectations[0] = new Interval(5 * blockSize, 6 * blockSize - 1);
        expectations[1] = new Interval(6 * blockSize, 7 * blockSize - 1);

        // Test
        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectations);
    }

    public void testSingleByteChunk() throws Exception {
        // Remove a single byte
        availableBytes.delete(new Interval(5 * blockSize + 1));

        // Set up the random number generator so that
        // block #5 (6th block) is selected
        prng.setLong(5);
        prng.setInt(3);

        // We expect a single byte assignment
        Interval[] expectation = new Interval[1];
        expectation[0] = new Interval(5 * blockSize);

        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectation);
    }
    
    public void testUpdateSkippedLocations() {
        // Skip forward
        // Set up RandomLocation[7] to be 1*blockSize
        // Then set up RandomLocation[3] to be 0.
        // Downloading two blocks starting at 0
        // should cause location 7 to be skipped
        // and re-set to 5*blockSize
        prng.setInts(new int[]{7,3,3,7});
        prng.setLongs(new long[]{1L, 0L, 5L});
        
        Interval assignment = null;
        for(int i=4; i >= 1; i--) {
            assignment = strategy.pickAssignment(availableBytes, 0, 
                fileSize-1, blockSize);
            availableBytes.delete(assignment);
        }
        assertEquals("random location is not getting reset when skipped.", 
                assignment.low, 5*blockSize);
        
        // Skip backward
        fileSize = 8*blockSize;
        availableBytes = IntervalSet.createSingletonSet(0,fileSize-1);
        // Set up location 8 to point to the second-to last block of the file.
        // Set up location 5 to point to the last block of the file.
        // The second download from location 5 should cause location 8 to
        // be skipped over and therefore lazily updated to block 2
        prng.setInts(new int[]{8,5,5,8});
        prng.setLongs(new long[]{6L, 7L, 2L});
        
        assignment = null;
        for(int i=4; i >= 1; i--) {
            assignment = strategy.pickAssignment(availableBytes, 0, 
                fileSize-1, blockSize);
            availableBytes.delete(assignment);
        }
        assertEquals("random location is not getting reset when skipped backwards.", 
                assignment.low, 2*blockSize);
    }
    
    /** Tests lazy updating of randomLocations */
    public void testLazyLocationUpdates() {
        // Randomly select the last block, and then the 7th
        long[] randomLocations = {fileSize/blockSize, 7};
        prng.setLongs(randomLocations);
        int[] randomIndexes = {11,11};
        prng.setInts(randomIndexes);
        
        // First download sets the random location
        Interval assignment = strategy.pickAssignment(availableBytes, 0, 
                fileSize-1, blockSize);
        availableBytes.delete(assignment);
        assertEquals("Downloaded wrong block", 
                new Interval(blockSize*(fileSize/blockSize), fileSize-1),
                assignment);
        
        // Now download a second block, remembering to indicate that
        // the last block is no longer available
        assignment = strategy.pickAssignment(availableBytes, 0, 
                availableBytes.getFirst().high, blockSize);
        assertEquals("Downloaded wrong block", 
                new Interval(7*blockSize,8*blockSize-1),
                assignment);
    }
    
    public void testWrappingAssignment() {
        // Set up available bytes to be typical
        // situation where wrapping occurs.
        // We need only the first half block and the second
        // to fourth blocks.  
        availableBytes = IntervalSet.createSingletonSet(5,
                4 * blockSize - 1);
        availableBytes.delete(new Interval(blockSize/2, blockSize-1));
        // The following line increases test coverage by triggering
        // an optimization that prevents unneccessary Interval creation
        availableBytes.delete(new Interval(2*blockSize-1));
        
        prng.setLong(3);
        // Identical indexes into the random locations array
        int[] testInts = { 7, 7, 7, 7};
        prng.setInts(testInts);

        // We expect to be assigned blocks #3,most of #2, #1, 
        // and the final part of #0
        Interval[] expectations = new Interval[4];
        expectations[0] = new Interval(3 * blockSize, 4 * blockSize - 1);
        expectations[1] = new Interval(2 * blockSize, 3 * blockSize - 1);
        expectations[2] = new Interval(    blockSize, 2 * blockSize - 2);
        expectations[3] = new Interval(5            ,   blockSize/2 - 1);
        
        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectations);
    }
    
    /** Test the case where the availableBytes
     * contains the Interval we return.
     */
    public void testReturnOptimization() {
        availableBytes = IntervalSet.createSingletonSet(blockSize,2*blockSize-1);
        prng.setInt(1);
        prng.setLong(0);
        
        Interval assignment = strategy.pickAssignment(availableBytes, 0, fileSize-1, blockSize);
        assertTrue("Return optimization not working", availableBytes.getFirst() == assignment);
    }
    
    /** Test the case where the download is almost finished.
     *  There's only a sliver left.
     */
    public void testSliverDownload() {
        availableBytes = IntervalSet.createSingletonSet(2475,2483);
        prng.setInt(12);
        prng.setLong(17);
        Interval assignment = strategy.pickAssignment(availableBytes, 2475, 2483, blockSize);
        assertEquals("Only a sliver of the file left, and something other than the sliver"+
                "was returned", availableBytes.getFirst(), assignment);
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
    
    public void testInvalidLowerBound() {
        // Try an invalid preview length and see if it throws
        // an InvalidInputException
        try {
            strategy.pickAssignment(availableBytes, -1, 
                    fileSize-1, blockSize);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about negative lowerBound", false);
    }
    
    public void testInvalidUpperBound() {
        // Try calling with lowerBound > upperBound
        // and see if it throws an exception
        boolean caughtException = false;
        try {
            strategy.pickAssignment(availableBytes, 2001, 
                    2000, blockSize);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            caughtException = true;
        }
        assertTrue("Failed to complain about lowerBound > upperBound", caughtException);
        
        caughtException = false;
        try {
            strategy.pickAssignment(availableBytes, 2001, 
                    fileSize+1, blockSize);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            caughtException = true;
        }
        assertTrue("Failed to complain about upperBound larger than file", caughtException);
    }
    
    /** Make sure we throw NoSuchElement exception if there is nothing to
     * download.
     */
    public void testNoAvailableBytes() {
        availableBytes = new IntervalSet();
        prng.setInt(15);
        prng.setLong(2);
        try {
            strategy.pickAssignment(availableBytes, 0, 
                    fileSize, blockSize);
        } catch (NoSuchElementException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about no available bytes", false);
    }
    
    /////////////// helper methods ////////////////
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

    private SelectionStrategy createRandomStrategy(long fileSize, Random rng)
            throws IllegalAccessException, NoSuchFieldException {
        RandomDownloadStrategy rds = new RandomDownloadStrategy(fileSize);
        PrivilegedAccessor.setValue(rds, "pseudoRandom", rng);
        return rds;
    }
}
