package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.BaseTestCase;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import java.util.Random;

public class RandomDownloadStrategyTest extends BaseTestCase {

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

    public void testFragmentationAvoidance() {
        // Set up a couple of gaps in our available bytes
        long gap1 = blockSize + 1;
        long gap2 = 3 * blockSize + 4;
        availableBytes.delete(new Interval(gap1));
        availableBytes.delete(new Interval(gap2));

        // Ensure that randomLocations[0] will be 0
        prng.setFloat(0.0f);

        // Create the expected assignments
        Interval[] expectation = new Interval[3];
        expectation[0] = new Interval(0, blockSize - 1);
        expectation[1] = new Interval(blockSize + 2, 2 * blockSize - 1);
        expectation[2] = new Interval(3 * blockSize + 5, 4 * blockSize - 1);

        // Use indexes corresponding to pre-existing
        // fragments in the download
        int[] testInts = { 0, 1, 2 };
        prng.setInts(testInts);

        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectation);
        
        // Run the above test, but use setFloat to ensure
        // randomLocations[0] is random rather than aligned
        // with the beginning of the file
        prng.setFloat(1.0f);
        availableBytes = IntervalSet.createSingletonSet(0,fileSize-1);
        availableBytes.delete(new Interval(gap1));
        availableBytes.delete(new Interval(gap2));
       
        // Expected assignments are the same, except for the
        // assignment corresponding to randomLocations index 0
        prng.setInts(testInts);
        long randomBlock = 7;
        prng.setLong(randomBlock); // Random index zero will consume one random long
        expectation[0] = new Interval(randomBlock*blockSize, (randomBlock+1)*blockSize-1);
        
        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectation);
    }
    
    public void testWrappingAssignment() {
        // Set up available bytes to be typical
        // situation where wrapping occurs
        availableBytes = IntervalSet.createSingletonSet(blockSize,
                4 * blockSize - 1);

        prng.setLong(3);
        // Identical indexes into the random locations array
        int[] testInts = { 7, 7 };
        prng.setInts(testInts);

        // We expect to be assigned blocks #3 and #2
        Interval[] expectations = new Interval[2];
        expectations[0] = new Interval(3 * blockSize, 4 * blockSize - 1);
        expectations[1] = new Interval(2 * blockSize, 3 * blockSize - 1);

        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectations);
    }

    /** White-box test to make sure the random block number gets properly 
     * wrapped inside pickAssignment.
     */
    public void testRandomNumberWraps() throws Exception {
        // corner case of first block
        testRandomNumberWraps(0, 0, blockSize*107+4, 972);
        // corner case of last block
        testRandomNumberWraps(fileSize/blockSize, 2000, fileSize, 123);
        // And some arbitrary tests, keeping in mind availableBlocks
        // only represents blocks 0-10
        testRandomNumberWraps(10, 2000, blockSize*107+4, 412);
        testRandomNumberWraps(6, blockSize*4, blockSize*11+19, 8437);
        testRandomNumberWraps(3, 7, blockSize*4193+8, 82);
    }
    
    public void testRandomNumberWraps(long targetBlockNumber, 
            long previewLength, long lastNeededByte, 
            long arbitraryWrapCount) throws Exception{
        // Reset all of strategy's state
        strategy = createRandomStrategy(fileSize, prng);
        // Give the strategy an arbitrary index of 3 into 
        // the random locations table
        prng.setInt(3);
        
        long previewBlockNumber = previewLength/blockSize;
        long lastBlockNumber    = lastNeededByte/blockSize; 
        
        // Wrap an arbitrary numbe of times, remembering
        // that RandomDownloadStrategy intentionally never starts
        // on the partial block at the end of the file
       
        // This next part relies on knowledge of how the random long is converted
        // into a block number inside RandomDownloadStrategy
        prng.setLong(arbitraryWrapCount*(lastBlockNumber-previewBlockNumber+1)+
                targetBlockNumber-previewBlockNumber);
     
        
        Interval assignment = strategy.pickAssignment(availableBytes, previewLength,
                lastNeededByte, blockSize);
        
        assertEquals("Internal wrapping of random longs not working as expected",
                targetBlockNumber*blockSize, assignment.low);
    }
    
    // ///////////// helper methods ////////////////

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
