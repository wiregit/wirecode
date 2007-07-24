package org.limewire.rudp;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests the SequenceNumberExtender class.
 */
public final class SequenceNumberExtenderTest extends BaseTestCase {

	/*
	 * Constructs the test.
	 */
	public SequenceNumberExtenderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(SequenceNumberExtenderTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    /**
     * Test that increments remain in sync with a full long being incremented
	 * and with the values being mapped down to 2 byte sequenceNumbers.
     * 
     * @throws Exception if an error occurs
     */
    public void testSequenceNumberIncrements() throws Exception {

		// Init the extender
		SequenceNumberExtender extender = new SequenceNumberExtender();

		//  Make sure that the sequence remains in sync with prior number
		//  and its own extended value - test up to 100000000
		long finalValue = 10000000;
		long lasti;
		long lastiand;
		long iand = -1;
		lasti = 0;
		lastiand = 0;
		for (long i = 1; i <= finalValue; i++) {

			// Shrink the sequenceNumber down to 2 bytes
			iand     = i & 0xffff;

			// Extend the sequenceNumber back to 8 bytes
			iand = extender.extendSequenceNumber( ( iand) );
			
			assertEquals("Previous sequence number plus 1 isn't current one", lastiand + 1, iand);
			assertEquals(i, iand);
			lasti = i;
			lastiand = iand;
		}

        assertEquals("final value should equal "+finalValue, 
            finalValue, iand);
    }

    public void testOutOfOrderNumbers() {
	SequenceNumberExtender extender = new SequenceNumberExtender();
	int outOfOrderWindowSize = 20;
	for (int i = 1; i < 0xFFFFFF; i++) {
	    long extendedI = extender.extendSequenceNumber((i & 0xFFFF));
	    long outOfOrder = (long)Math.max(i - Math.random() * 20, 1);
	    long extendedOutOfOrder = extender.extendSequenceNumber((outOfOrder & 0xFFFF));
	    assertEquals(i, extendedI);
	    assertEquals(outOfOrder, extendedOutOfOrder);
	    assertEquals(i, extender.extendSequenceNumber((i & 0xFFFF)));
	}
    }
}
