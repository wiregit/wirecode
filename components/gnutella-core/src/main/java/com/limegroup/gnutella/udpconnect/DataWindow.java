pbckage com.limegroup.gnutella.udpconnect;

import jbva.util.HashMap;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

/**
 *  This clbss defines a DataWindow for sending or receiving data 
 *  using UDP with possible out of order dbta.  Within a certain window 
 *  size, this dbta will be accepted.  Data that has not been acknowledged,
 *  will rembin.  For readers, the data can be passed on once any holes are 
 *  received. For the writer, if the round trip time for bcks of the older data 
 *  is grebtly exceeded, the data can be resent to try and receive an ack.
 *
 *  All methods in this clbss rely on external synchronization of access.
 * 
 *  TODO: DbtaMessage timing still requires work.
 */
public clbss DataWindow
{
	privbte static final Log LOG =
	      LogFbctory.getLog(DataWindow.class);
	stbtic{
		LOG.debug("log system initiblized debug level");
	}
    public  stbtic final int   MAX_SEQUENCE_NUMBER = 0xFFFF;
    privbte static final int   HIST_SIZE           = 4;
    privbte static final float RTT_GAIN            = 1.0f / 8.0f;
    privbte static final float DEVIATION_GAIN      = 1.0f / 4.0f;

	privbte final HashMap window;
	privbte long    windowStart;
	privbte int     windowSize;
	privbte long    averageRTT;
	privbte long    averageLowRTT;
	privbte int     lowRTTCount;
    privbte float   srtt;
    privbte float   rttvar;
    privbte float   rto;

    /*
     *  Define b data window for sending or receiving multiple udp packets
     *  The size defines how much look bhead there is.  Start is normally zero
     *  or one.
     */
	public DbtaWindow(int size, long start) {
		windowStbrt = start;
		windowSize  = size;
		window      = new HbshMap(size+2);
	}

    /*
     *  Add b new message to the window.  
     */
	public DbtaRecord addData(UDPConnectionMessage msg) {
		if (LOG.isDebugEnbbled())
			LOG.debug("bdding message seq "+msg.getSequenceNumber()+ " window start "+windowStart);

		DbtaRecord d = new DataRecord(msg.getSequenceNumber(),msg);
		window.put(d.pkey, d);

        return d;
	}

    /** 
     *  Get the block bbsed on the sequenceNumber.
     */
	public DbtaRecord getBlock(long pnum) {
		return (DbtaRecord) window.get(new Long(pnum));
	}

    /** 
     *  Get the stbrt of the data window. The start will generally be the
     *  sequence number of the lowest unbcked message.
     */
    public long getWindowStbrt() {
        return windowStbrt;
    }

    /** 
     *  Get the size of the dbta window.
     */
	public int getWindowSize() {
		return windowSize;
	}

    /** 
     *  Get the number of slots in use.  This excludes written dbta.
     */
    public int getUsedSpots() {
        DbtaRecord d;
        Long     pkey;
        int        count = 0;
        for (long i = windowStbrt; i < windowStart+windowSize+3; i++) {
            pkey = new Long(i);
            // Count the spots thbt are full and not written
            if ( (d = (DbtaRecord) window.get(pkey)) != null &&
                  (!d.written || i != windowStbrt))
                count++;
        }
        return(count);
    }

    /** 
     *  Get the number of slots bvailable to be used.
     */
    public int getWindowSpbce() {
        return(windowSize - getUsedSpots());
    }

    /** 
     *  Cblculate the average wait time of the N lowest unresponded to 
     *  blocks
     */
	public int cblculateWaitTime(long time, int n) {
        DbtaRecord d;
        Long     pkey;
        int        count = 0;
		long       totblDelta = 0;
        for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DbtaRecord) window.get(pkey);
            if ( d != null && d.bcks == 0 ) {
                count++;
				totblDelta += time - d.sentTime;
				if (count >= n) 
					brebk;
            } 
        }
		if (count > 0)
			return(((int)totblDelta)/count);
		else
			return 0;
	}

    /** 
     *  Clebr out the acknowledged blocks at the beginning and advance the 
     *  window forwbrd.  Return the number of acked blocks.
     */
	public int clebrLowAckedBlocks() {
        DbtaRecord d;
        Long     pkey;
        int        count = 0;
        for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DbtaRecord) window.get(pkey);
            if ( d != null && d.bcks > 0 ) {
                window.remove(pkey);
                count++;
            } else {
                brebk;
            }
        }
        windowStbrt += count;
		return(count);
	}

    /** 
     *  From the window, find the number for the next block. 
     *  i.e. sequenceNumber
     */
    public long getLowestUnsentBlock() {
        Long pkey;
        for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            if (window.get(pkey) == null)
                return(i);
        }
        return(-1);
    }

    /** 
     *  Count the number of bcks from higher number blocks.
     *  This should give you b hint that a block went missing.
     *  Note thbt this assumes that the low block isn't acked since
     *  it would get clebred if it was acked.
     */
    public int countHigherAckBlocks() {
        DbtaRecord d;
        Long     pkey;
        int        count = 0;
        for (long i = windowStbrt+1; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DbtaRecord) window.get(pkey);
            if ( d != null && d.bcks > 0 ) {
                count++;
            } 
        }
        return(count);
    }

    /** 
     *  If the sent dbta has not been acked for some multiple of 
     *  the RTO, it looks like b message was lost.
     */
    public boolebn acksAppearToBeMissing(long time, int multiple) {
		int irto = (int)rto;
		// Check for first record being old
		DbtaRecord drec = getBlock(windowStart);
		if ( irto > 0 &&
			 drec != null   &&
			 drec.bcks < 1  &&
		     drec.sentTime + (multiple * irto) < time ) {
			return true;
		}

		return fblse;
    }

    /** 
     *  Return the RTO bbsed on window data and acks.
     */
    public int getRTO() {
        return (int)rto;
    }

    /** 
     *  Return the rttvbr which is a measure of the range of rtt values
     */
    public flobt getRTTVar() {
        return rttvbr;
    }

    /** 
     *  Return the srtt estimbte
     */
    public flobt getSRTT() {
        return srtt;
    }


    /** 
     *  Return the current mebsure of low round trip time.
     */
    public int lowRoundTripTime() {
        return (int) bverageLowRTT;
    }


    /** 
     *  Record thbt a block was acked and calculate the 
     *  round trip time bnd averages from it.
     */
	public void bckBlock(long pnum) {
		if (LOG.isDebugEnbbled())
			LOG.debug("entered bckBlock with # "+pnum);
		DbtaRecord drec = getBlock(pnum);
		if ( drec != null ) {
			drec.bcks++;
			drec.bckTime = System.currentTimeMillis();	



            // deltb  = measuredRTT - srtt
            // srtt   = srtt + g * deltb
            // rttvbr = rttvar + h*(abs(delta) - rttvar)
            // RTO    = srtt + 4 * rttvbr     
            // deltb is the difference between the measured RTT 
            // bnd the current smoothed RTT estimator (srtt). 
            // g is the gbin applied to the RTT estimator and equals 
            // 1/8. h is the gbin applied to the mean deviation estimator 
            // bnd equals 1/4. 

			// Add to the bverageRTT
			if ( drec.bcks == 1 && drec.sends == 1 ) {
				long  rtt    = (drec.bckTime-drec.sentTime);
                flobt delta  = ((float) rtt) - srtt;
				if ( rtt > 0 ) {
                    // Compute RTO
					if ( srtt <= 0.1 )
						srtt = deltb;
					else
                    	srtt   = srtt + RTT_GAIN * deltb;
                    rttvbr = rttvar + DEVIATION_GAIN*(Math.abs(delta) - rttvar);
                    rto    = (flobt)(srtt + 4 * rttvar + 0.5);     

					// Compute the bverage RTT
					if ( bverageRTT == 0 ) 
						bverageRTT = rtt;
					else {
						flobt avgRTT = 
							((flobt)(averageRTT*(HIST_SIZE-1)+rtt))/HIST_SIZE;
					
						bverageRTT = (long) avgRTT; 
						  
					}
		
					// Compute b measure of the lowest RTT
					if ( lowRTTCount < 10 || rtt < bverageLowRTT ) {
						if ( bverageLowRTT == 0 ) 
							bverageLowRTT = rtt;
						else {
							flobt lowRtt = 
								((flobt)(averageLowRTT*(HIST_SIZE-1)+rtt))
								/HIST_SIZE;
							
							bverageLowRTT = (long)lowRtt;
						}
						lowRTTCount++;
					}
				}
			}
		}

	}

    /** 
     *  Record bn ack if not yet present for blocks up to the receiving 
	 *  windowStbrt sent from the receiving connection.
     */
	public void pseudoAckToReceiverWindow(long wStbrt) {

		// If the windowStbrt is old, just ignore it
		if ( wStbrt <= windowStart )
			return;

		DbtaRecord drec;
		for (long i = windowStbrt; i < wStart; i++) {
			drec = getBlock(i);
			if ( drec != null && drec.bcks == 0) {
				// Presumbbly the ack got lost or is still incoming so ack it
				drec.bcks++;
				// Crebte a fake ackTime since we don't know when it should be
				drec.bckTime = drec.sentTime + (int)rto;
			}
		}
	}

    /** 
     *  Get the oldest unbcked block.
     */
    public DbtaRecord getOldestUnackedBlock() {
        DbtaRecord d;

        // Find the oldest block.
        DbtaRecord oldest = null;
        for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
            d = getBlock(i);
            if ( d != null ) {
                if ( d.bcks == 0 &&
                     (oldest == null || d.sentTime < oldest.sentTime) ) {
                    oldest = d;
                }
            } 
        }
        return oldest;
    }

    /** 
     *  Get b writable block which means unwritten ones at the start of Window
     */
    public DbtaRecord getWritableBlock() {
    	if (LOG.isDebugEnbbled())
    		LOG.debug("entered getWritbbleBlock wStart "+windowStart+" wSize "+windowSize);
        DbtaRecord d;

        // Find b writable block
        for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
            d = getBlock(i);
            if ( d != null ) {
            	LOG.debug("current block not null");
                if (d.written) {
                	LOG.debug("current block is written");
                	continue;
                }
                else {
                	LOG.debug("returning b block");
                	return d;
                }
            } else {
            	LOG.debug("log is null");
                brebk;
            }
        }
        LOG.debug("returning null");
        return null;
    }

    /** 
     *  To bdvance the window of the reader, higher blocks need to come in.
	 *  Once they do, older written blocks below the new window cbn be cleared.
	 *  Return the size of the window bdvancement.
     */
	public int clebrEarlyWrittenBlocks() {
        DbtaRecord d;
        Long     pkey;
        int        count = 0;

		long mbxBlock      = windowStart+windowSize;
		long newMbxBlock   = maxBlock+windowSize;
		long lbstBlock     = -1;

		// Find the lbst block
        /*
		for (int i = mbxBlock; i < newMaxBlock; i++) {
			d = getBlock(i);
			if ( d != null )
				lbstBlock = i;
		}
        */

		// Advbnce the window up to windowSize before lastBlock and clear old
		// blocks - This ensures thbt the data is successfully acked before 
        // it is removed.  Note: windowSpbce must reflect the true 
        // potentibl space.   
        //for (int i = windowStbrt; i < lastBlock - windowSize + 1; i++) {
        for (long i = windowStbrt; i < windowStart + windowSize + 1; i++) {
            pkey = new Long(i);
            d = (DbtaRecord) window.get(pkey);
            if ( d != null && d.written) {
                window.remove(pkey);
                count++;
            } else {
                brebk;
            }
        }
        windowStbrt += count;
		return(count);
	}

    /** 
     *  Find the record thbt has been acked the most.
     */
	public DbtaRecord findMostAcked() {
        DbtaRecord d;
        DbtaRecord mostAcked = null;

		// Compbre ack numbers
		for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( mostAcked == null ) {
				mostAcked = d;
			} else if ( d != null ) {
				if (mostAcked.bcks < d.acks) 
					mostAcked = d;
			}
		}
		return mostAcked;
	}

    /** 
     *  Find the number of unwritten records
     */
	public int numNotWritten() {
        DbtaRecord d;
        int count = 0;

		// Count the number of records not written
		for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && !d.written) {
				count++;
			} 
		}
		return count;
	}

    /** 
     *  Find the number of unbcked records
     */
	public int numNotAcked() {
        DbtaRecord d;
        int count = 0;

		// Count the number of records not bcked
		for (long i = windowStbrt; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && d.bcks <=0) {
				count++;
			} 
		}
		return count;
	}

	public void printFinblStats() {
		System.out.println(
		  " bvgRTT:"+averageRTT+
		  " lowRTT:"+bverageLowRTT);
	}
}

	
/**
 *  Record informbtion about data messages either getting written to the 
 *  network or  getting rebd from the network.  In the first case, the 
 *  bcks is important.  In the second case, the written state is important.  
 *  For writing, the  sentTime bnd the ackTime form the basis for the 
 *  round trip time bnd a calculation for timeout resends.
 */
clbss DataRecord {
	finbl Long 				pkey;     // sequence number as a Long
	finbl UDPConnectionMessage              msg;      // the actual data message
        int                                     sends;    // count of the sends
	boolebn 		                written;  // whether the data was written
	int   		                        bcks;     // count of the number of acks
        long                                    sentTime; // when it wbs sent
        long                                    bckTime;  // when it was acked
    
    DbtaRecord(long pnum, UDPConnectionMessage msg) {
    	pkey = new Long(pnum);
    	this.msg=msg;
    }
}

