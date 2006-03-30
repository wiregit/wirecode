
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

/**
 * Use a SequenceNumberExtender object to expand a number truncated to 2 bytes into the full original 8-byte number.
 * Show it a randomly varying and gradually increasing stream of numbers, and it will detect the rollover each time it happens and account for it.
 * 
 * UDP connection packets carry 512 bytes of file data, and are numbered with 2 bytes.
 * Before rolling over, a computer will transmit 0x10000 * 512 bytes = 32 MB of data.
 * 
 * This class keeps track of state for the purpose of modifying incoming
 * sequenceNumbers so that they can be physically communicated with 2 bytes
 * but can in fact represent 8 bytes.
 */
public class SequenceNumberExtender {

	/**
     * 0x10000, the number of different numbers we can express in 2 bytes.
     * 
     * Every time sequenceNumbers exceed 0xffff then another increment of
	 * this value must be added on to them once they roll over in the
	 * two byte representation
     */
	private static final long BASE_INCREMENT = 0x10000;

	/**
     * 0xffff / 2, the midpoint in the number of different numbers 2 bytes can store.
     * 
     * When sequenceNumbers reach this value in the two byte representation,
	 * it is safe to increment the lowBase for use when sequenceNumbers are
	 * low after rolling over 0xffff
     */
	private static final long LOW_BASE_SWITCH_POINT = 0xffff / 2;

	/**
     * 0xffff / 4, the quarter-point in the number of different numbers 2 bytes can store.
     * 
     * When sequenceNumbers reach this value in the two byte representation,
     * it is safe to set the highBase to the lowBase and to start adding the
     * highBase to the two byte sequence numbers rather than the low base
     */
	private static final long HIGH_BASE_SWITCH_POINT = 0xffff / 4;

	/**
     * We'll add lowBase to sequence numbers in the first quarter of the range of numbers 2 bytes can hold.
     * We'll increment lowBase when we cross the midpoint.
     * 
     * When sequenceNumbers are low, this value is added to them to allow
     * sequenceNumbers to be communicated with two bytes but represented as
     * 8 bytes
     */
	private long lowBase = 0;

	/**
     * We'll add highBase to sequence numbers in the last 3 quarters of the range of numbers 2 bytes can hold.
     * We'll increment highBase when we cross from the first quarter to the second quarter.
     * 
     * When sequenceNumbers are higher, this value is added to them to allow
     * sequenceNumbers to be communicated with two bytes but represented as
     * 8 bytes
     */
	private long highBase = 0;

	/**
     * This flag contains the state of which transition is to occur next.
     * When true, the next transition will be from using the lowBase to the
     * highBase
     */
	private boolean highSwitchPending = true;

	/**
     * This flag contains the state of which transition is to occur next.
     * When true, the incrementing of the lowBase is pending
     */
	private boolean lowSwitchPending  = false;

    /**
     * Make a new SequenceNumberExtender object to watch the sequence numbers we've been getting, and extend them from 2 bytes to 8 bytes.
     * The UDPConnectionProcessor() constructor does this to extend the remote computer's sequence numbers.
     * The SequenceNumberExtender object will look at the numbers we give it, and notice when they roll over from numbers around 0xffff to numbers around 0x0000.
     */
	public SequenceNumberExtender() {}

    /** Only used for testing. */
    public  SequenceNumberExtender(long base) {
        base     = base & 0xffffffffffff0000l;
        lowBase  = base;
        highBase = base;
    }

    /**
     * Translate a given number that fits into 2 bytes and may have just wrapped around into a full 8-byte number.
     * The given number can grow from 0x0000 to 0xffff and then wrap around to small numbers again, and the returned number will just keep growing.
     * This works even if the numbers aren't in order, they just can't be wildly out of order.
     * 
     * @param sequenceNumber A sequence number that has been truncated to 2 bytes
     * @return               The full 8-byte number
     */
	public long extendSequenceNumber(long sequenceNumber) {

        // We'll calculate this 8-byte extended sequence number from the given 2-byte sequence number, and return it
		long extendedSeqNo;

        // We're a quarter in, and just got a sequence number in the second quarter
		if (sequenceNumber >= HIGH_BASE_SWITCH_POINT && sequenceNumber < LOW_BASE_SWITCH_POINT && highSwitchPending) {

            // Increment the high base
			highBase = lowBase;

            // We're in the second quarter
			highSwitchPending = false;
			lowSwitchPending  = true;
		}

        // We're a half in, and just got a sequence number in the second half
		if (sequenceNumber > LOW_BASE_SWITCH_POINT && lowSwitchPending) {

            // Increment the low base
            lowBase += BASE_INCREMENT;

            // We're outside the second quarter
			highSwitchPending = true;
			lowSwitchPending  = false;
		}

        // We're in the first quarter
		if (sequenceNumber < HIGH_BASE_SWITCH_POINT) {

            // Add the base we incremented at the last midpoint
			extendedSeqNo = ((long)sequenceNumber) + lowBase;

        // We're in the last 3 quarters
		} else {

            // Add the base we incremented at the quarter point
			extendedSeqNo = ((long)sequenceNumber) + highBase;
		}

        // Return the number we extended
		return extendedSeqNo;
	}
}
