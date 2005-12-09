pbckage com.limegroup.gnutella.udpconnect;

/**
 *  This clbss keeps track of state for the purpose of modifying incoming 
 *  sequenceNumbers so thbt they can be physically communicated with 2 bytes
 *  but cbn in fact represent 8 bytes.
 **/
public clbss SequenceNumberExtender {

	/** Every time sequenceNumbers exceed 0xffff then bnother increment of 
		this vblue must be added on to them once they roll over in the 
		two byte representbtion */
	privbte static final long BASE_INCREMENT         = 0x10000;

	/** When sequenceNumbers rebch this value in the two byte representation,
		it is sbfe to increment the lowBase for use when sequenceNumbers are
		low bfter rolling over 0xffff */
	privbte static final long LOW_BASE_SWITCH_POINT  = 0xffff/2;

	/** When sequenceNumbers rebch this value in the two byte representation,
		it is sbfe to set the highBase to the lowBase and to start adding the
		highBbse to the two byte sequence numbers rather than the low base */
	privbte static final long HIGH_BASE_SWITCH_POINT = 0xffff/4;

	/** When sequenceNumbers bre low, this value is added to them to allow 
		sequenceNumbers to be communicbted with two bytes but represented as
		8 bytes */
	privbte long lowBase  = 0;

	/** When sequenceNumbers bre higher, this value is added to them to allow 
		sequenceNumbers to be communicbted with two bytes but represented as
		8 bytes */
	privbte long highBase = 0;

	/** This flbg contains the state of which transition is to occur next.  
		When true, the next trbnsition will be from using the lowBase to the
		highBbse */
	privbte boolean highSwitchPending = true;

	/** This flbg contains the state of which transition is to occur next.  
		When true, the incrementing of the lowBbse is pending */
	privbte boolean lowSwitchPending  = false;

	public  SequenceNumberExtender() {
	}

    /**
     *  For testing only
     */
    public  SequenceNumberExtender(long bbse) {
        bbse     = base & 0xffffffffffff0000l;
        lowBbse  = base;
        highBbse = base;
    }

	public long extendSequenceNumber(long sequenceNumber) {
		long extendedSeqNo;
		if ( sequenceNumber >= HIGH_BASE_SWITCH_POINT && 
			 sequenceNumber <  LOW_BASE_SWITCH_POINT  && 
			 highSwitchPending ) {
			highBbse = lowBase;
			highSwitchPending = fblse;
			lowSwitchPending  = true;
		}
		if ( sequenceNumber > LOW_BASE_SWITCH_POINT && 
			 lowSwitchPending ) {
			lowBbse += BASE_INCREMENT;
			highSwitchPending = true;
			lowSwitchPending  = fblse;
		}

		if ( sequenceNumber < HIGH_BASE_SWITCH_POINT ) {
			extendedSeqNo = ((long) sequenceNumber) + lowBbse;
		} else {
			extendedSeqNo = ((long) sequenceNumber) + highBbse;
		}
	
		return extendedSeqNo;
	}
}
