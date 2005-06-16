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
        // Set up prng so our ideal location is always the 5th block boundary
        prng.setLongs(new long[] {5, 5, 5, 5});
        // Set ints so that the strategy picks two chunks 
        // after idealLocation (two odd ints)
        // followed by two chunks before idealLocation (two even ints)
        prng.setInts(new int[] {-1,487137, -2, 65538});

        // Set the expected assignments
        Interval[] expectations = new Interval[4];
        expectations[0] = new Interval(5 * blockSize, 6 * blockSize - 1);
        expectations[1] = new Interval(6 * blockSize, 7 * blockSize - 1);
        expectations[2] = new Interval(4 * blockSize, 5 * blockSize - 1);
        expectations[3] = new Interval(3 * blockSize, 4 * blockSize - 1);
        
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
    
    /** Test the case where the availableBytes
     * contains the Interval we return and makes
     * sure a new Interval is not created if not necessary.
     */
    public void testReturnOptimization() {
        availableBytes = IntervalSet.createSingletonSet(blockSize,2*blockSize-1);
        prng.setInt(1);
        prng.setLong(0);
        
        Interval assignment = strategy.pickAssignment(availableBytes, availableBytes, blockSize);
        assertTrue("Return optimization not working", availableBytes.getFirst() == assignment);
    }
    
    /** Test the case where the download is almost finished.
     *  There's only a sliver left.
     */
    public void testSliverDownload() {
        availableBytes = IntervalSet.createSingletonSet(2475,2483);
        prng.setLong(17);
        availableBytes = IntervalSet.createSingletonSet(2475, 2483);
        Interval assignment = strategy.pickAssignment(availableBytes, availableBytes, blockSize);
        assertEquals("Only a sliver of the file left, and something other than the sliver"+
                "was returned", availableBytes.getFirst(), assignment);
    }
    
    public void testInvalidBlockSize() {
        // Try an invalid block size and see if it throws
        // an InvalidInputException
        try {
            strategy.pickAssignment(availableBytes, 
                    availableBytes, 0);
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about invalid block size", false);
    }
    
    /** Make sure we throw NoSuchElement exception if there is nothing to
     * download.
     */
    public void testNoAvailableBytes() {
        availableBytes = new IntervalSet();
        prng.setInt(15);
        prng.setLong(2);
        try {
            strategy.pickAssignment(availableBytes,
                    IntervalSet.createSingletonSet(0,fileSize-1), blockSize);
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
            Interval assignment = strategy.pickAssignment(availableBytes, 
                    IntervalSet.createSingletonSet(0, fileSize - 1), blockSize);
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
