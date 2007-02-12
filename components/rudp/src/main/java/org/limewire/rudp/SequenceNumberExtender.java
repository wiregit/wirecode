package org.limewire.rudp;

/**
 *  This class keeps track of state for the purpose of modifying incoming 
 *  sequenceNumbers so that they can be physically communicated with 2 bytes
 *  but can in fact represent 8 bytes.
 **/
public class SequenceNumberExtender {

	/** Every time sequenceNumbers exceed 0xffff then another increment of 
		this value must be added on to them once they roll over in the 
		two byte representation */
	private static final long BASE_INCREMENT         = 0x10000;

	/** When sequenceNumbers reach this value in the two byte representation,
		it is safe to increment the lowBase for use when sequenceNumbers are
		low after rolling over 0xffff */
	private static final long LOW_BASE_SWITCH_POINT  = 0xffff/2;

	/** When sequenceNumbers reach this value in the two byte representation,
		it is safe to set the highBase to the lowBase and to start adding the
		highBase to the two byte sequence numbers rather than the low base */
	private static final long HIGH_BASE_SWITCH_POINT = 0xffff/4;

	/** When sequenceNumbers are low, this value is added to them to allow 
		sequenceNumbers to be communicated with two bytes but represented as
		8 bytes */
	private long lowBase  = 0;

	/** When sequenceNumbers are higher, this value is added to them to allow 
		sequenceNumbers to be communicated with two bytes but represented as
		8 bytes */
	private long highBase = 0;

	/** This flag contains the state of which transition is to occur next.  
		When true, the next transition will be from using the lowBase to the
		highBase */
	private boolean highSwitchPending = true;

	/** This flag contains the state of which transition is to occur next.  
		When true, the incrementing of the lowBase is pending */
	private boolean lowSwitchPending  = false;

	public  SequenceNumberExtender() {
	}

    /**
     *  For testing only
     */
    public  SequenceNumberExtender(long base) {
        base     = base & 0xffffffffffff0000l;
        lowBase  = base;
        highBase = base;
    }

	public long extendSequenceNumber(long sequenceNumber) {
		long extendedSeqNo;
		if ( sequenceNumber >= HIGH_BASE_SWITCH_POINT && 
			 sequenceNumber <  LOW_BASE_SWITCH_POINT  && 
			 highSwitchPending ) {
			highBase = lowBase;
			highSwitchPending = false;
			lowSwitchPending  = true;
		}
		if ( sequenceNumber > LOW_BASE_SWITCH_POINT && 
			 lowSwitchPending ) {
			lowBase += BASE_INCREMENT;
			highSwitchPending = true;
			lowSwitchPending  = false;
		}

		if ( sequenceNumber < HIGH_BASE_SWITCH_POINT ) {
			extendedSeqNo = sequenceNumber + lowBase;
		} else {
			extendedSeqNo = sequenceNumber + highBase;
		}
	
		return extendedSeqNo;
	}
}
