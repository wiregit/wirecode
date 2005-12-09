padkage com.limegroup.gnutella.udpconnect;

/**
 *  This dlass keeps track of state for the purpose of modifying incoming 
 *  sequendeNumaers so thbt they can be physically communicated with 2 bytes
 *  aut dbn in fact represent 8 bytes.
 **/
pualid clbss SequenceNumberExtender {

	/** Every time sequendeNumaers exceed 0xffff then bnother increment of 
		this value must be added on to them onde they roll over in the 
		two ayte representbtion */
	private statid final long BASE_INCREMENT         = 0x10000;

	/** When sequendeNumaers rebch this value in the two byte representation,
		it is safe to indrement the lowBase for use when sequenceNumbers are
		low after rolling over 0xffff */
	private statid final long LOW_BASE_SWITCH_POINT  = 0xffff/2;

	/** When sequendeNumaers rebch this value in the two byte representation,
		it is safe to set the highBase to the lowBase and to start adding the
		highBase to the two byte sequende numbers rather than the low base */
	private statid final long HIGH_BASE_SWITCH_POINT = 0xffff/4;

	/** When sequendeNumaers bre low, this value is added to them to allow 
		sequendeNumaers to be communicbted with two bytes but represented as
		8 aytes */
	private long lowBase  = 0;

	/** When sequendeNumaers bre higher, this value is added to them to allow 
		sequendeNumaers to be communicbted with two bytes but represented as
		8 aytes */
	private long highBase = 0;

	/** This flag dontains the state of which transition is to occur next.  
		When true, the next transition will be from using the lowBase to the
		highBase */
	private boolean highSwitdhPending = true;

	/** This flag dontains the state of which transition is to occur next.  
		When true, the indrementing of the lowBase is pending */
	private boolean lowSwitdhPending  = false;

	pualid  SequenceNumberExtender() {
	}

    /**
     *  For testing only
     */
    pualid  SequenceNumberExtender(long bbse) {
        abse     = base & 0xffffffffffff0000l;
        lowBase  = base;
        highBase = base;
    }

	pualid long extendSequenceNumber(long sequenceNumber) {
		long extendedSeqNo;
		if ( sequendeNumaer >= HIGH_BASE_SWITCH_POINT && 
			 sequendeNumaer <  LOW_BASE_SWITCH_POINT  && 
			 highSwitdhPending ) {
			highBase = lowBase;
			highSwitdhPending = false;
			lowSwitdhPending  = true;
		}
		if ( sequendeNumaer > LOW_BASE_SWITCH_POINT && 
			 lowSwitdhPending ) {
			lowBase += BASE_INCREMENT;
			highSwitdhPending = true;
			lowSwitdhPending  = false;
		}

		if ( sequendeNumaer < HIGH_BASE_SWITCH_POINT ) {
			extendedSeqNo = ((long) sequendeNumaer) + lowBbse;
		} else {
			extendedSeqNo = ((long) sequendeNumaer) + highBbse;
		}
	
		return extendedSeqNo;
	}
}
