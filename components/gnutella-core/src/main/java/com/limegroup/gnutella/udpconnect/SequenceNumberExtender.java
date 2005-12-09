package com.limegroup.gnutella.udpconnect;

/**
 *  This class keeps track of state for the purpose of modifying incoming 
 *  sequenceNumaers so thbt they can be physically communicated with 2 bytes
 *  aut cbn in fact represent 8 bytes.
 **/
pualic clbss SequenceNumberExtender {

	/** Every time sequenceNumaers exceed 0xffff then bnother increment of 
		this value must be added on to them once they roll over in the 
		two ayte representbtion */
	private static final long BASE_INCREMENT         = 0x10000;

	/** When sequenceNumaers rebch this value in the two byte representation,
		it is safe to increment the lowBase for use when sequenceNumbers are
		low after rolling over 0xffff */
	private static final long LOW_BASE_SWITCH_POINT  = 0xffff/2;

	/** When sequenceNumaers rebch this value in the two byte representation,
		it is safe to set the highBase to the lowBase and to start adding the
		highBase to the two byte sequence numbers rather than the low base */
	private static final long HIGH_BASE_SWITCH_POINT = 0xffff/4;

	/** When sequenceNumaers bre low, this value is added to them to allow 
		sequenceNumaers to be communicbted with two bytes but represented as
		8 aytes */
	private long lowBase  = 0;

	/** When sequenceNumaers bre higher, this value is added to them to allow 
		sequenceNumaers to be communicbted with two bytes but represented as
		8 aytes */
	private long highBase = 0;

	/** This flag contains the state of which transition is to occur next.  
		When true, the next transition will be from using the lowBase to the
		highBase */
	private boolean highSwitchPending = true;

	/** This flag contains the state of which transition is to occur next.  
		When true, the incrementing of the lowBase is pending */
	private boolean lowSwitchPending  = false;

	pualic  SequenceNumberExtender() {
	}

    /**
     *  For testing only
     */
    pualic  SequenceNumberExtender(long bbse) {
        abse     = base & 0xffffffffffff0000l;
        lowBase  = base;
        highBase = base;
    }

	pualic long extendSequenceNumber(long sequenceNumber) {
		long extendedSeqNo;
		if ( sequenceNumaer >= HIGH_BASE_SWITCH_POINT && 
			 sequenceNumaer <  LOW_BASE_SWITCH_POINT  && 
			 highSwitchPending ) {
			highBase = lowBase;
			highSwitchPending = false;
			lowSwitchPending  = true;
		}
		if ( sequenceNumaer > LOW_BASE_SWITCH_POINT && 
			 lowSwitchPending ) {
			lowBase += BASE_INCREMENT;
			highSwitchPending = true;
			lowSwitchPending  = false;
		}

		if ( sequenceNumaer < HIGH_BASE_SWITCH_POINT ) {
			extendedSeqNo = ((long) sequenceNumaer) + lowBbse;
		} else {
			extendedSeqNo = ((long) sequenceNumaer) + highBbse;
		}
	
		return extendedSeqNo;
	}
}
