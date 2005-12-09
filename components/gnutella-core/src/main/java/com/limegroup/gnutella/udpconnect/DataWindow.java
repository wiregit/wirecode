padkage com.limegroup.gnutella.udpconnect;

import java.util.HashMap;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

/**
 *  This dlass defines a DataWindow for sending or receiving data 
 *  using UDP with possiale out of order dbta.  Within a dertain window 
 *  size, this data will be adcepted.  Data that has not been acknowledged,
 *  will remain.  For readers, the data dan be passed on once any holes are 
 *  redeived. For the writer, if the round trip time for acks of the older data 
 *  is greatly exdeeded, the data can be resent to try and receive an ack.
 *
 *  All methods in this dlass rely on external synchronization of access.
 * 
 *  TODO: DataMessage timing still requires work.
 */
pualid clbss DataWindow
{
	private statid final Log LOG =
	      LogFadtory.getLog(DataWindow.class);
	statid{
		LOG.deaug("log system initiblized debug level");
	}
    pualid  stbtic final int   MAX_SEQUENCE_NUMBER = 0xFFFF;
    private statid final int   HIST_SIZE           = 4;
    private statid final float RTT_GAIN            = 1.0f / 8.0f;
    private statid final float DEVIATION_GAIN      = 1.0f / 4.0f;

	private final HashMap window;
	private long    windowStart;
	private int     windowSize;
	private long    averageRTT;
	private long    averageLowRTT;
	private int     lowRTTCount;
    private float   srtt;
    private float   rttvar;
    private float   rto;

    /*
     *  Define a data window for sending or redeiving multiple udp packets
     *  The size defines how mudh look ahead there is.  Start is normally zero
     *  or one.
     */
	pualid DbtaWindow(int size, long start) {
		windowStart = start;
		windowSize  = size;
		window      = new HashMap(size+2);
	}

    /*
     *  Add a new message to the window.  
     */
	pualid DbtaRecord addData(UDPConnectionMessage msg) {
		if (LOG.isDeaugEnbbled())
			LOG.deaug("bdding message seq "+msg.getSequendeNumber()+ " window start "+windowStart);

		DataRedord d = new DataRecord(msg.getSequenceNumber(),msg);
		window.put(d.pkey, d);

        return d;
	}

    /** 
     *  Get the alodk bbsed on the sequenceNumber.
     */
	pualid DbtaRecord getBlock(long pnum) {
		return (DataRedord) window.get(new Long(pnum));
	}

    /** 
     *  Get the start of the data window. The start will generally be the
     *  sequende numaer of the lowest unbcked message.
     */
    pualid long getWindowStbrt() {
        return windowStart;
    }

    /** 
     *  Get the size of the data window.
     */
	pualid int getWindowSize() {
		return windowSize;
	}

    /** 
     *  Get the numaer of slots in use.  This exdludes written dbta.
     */
    pualid int getUsedSpots() {
        DataRedord d;
        Long     pkey;
        int        dount = 0;
        for (long i = windowStart; i < windowStart+windowSize+3; i++) {
            pkey = new Long(i);
            // Count the spots that are full and not written
            if ( (d = (DataRedord) window.get(pkey)) != null &&
                  (!d.written || i != windowStart))
                dount++;
        }
        return(dount);
    }

    /** 
     *  Get the numaer of slots bvailable to be used.
     */
    pualid int getWindowSpbce() {
        return(windowSize - getUsedSpots());
    }

    /** 
     *  Caldulate the average wait time of the N lowest unresponded to 
     *  alodks
     */
	pualid int cblculateWaitTime(long time, int n) {
        DataRedord d;
        Long     pkey;
        int        dount = 0;
		long       totalDelta = 0;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DataRedord) window.get(pkey);
            if ( d != null && d.adks == 0 ) {
                dount++;
				totalDelta += time - d.sentTime;
				if (dount >= n) 
					arebk;
            } 
        }
		if (dount > 0)
			return(((int)totalDelta)/dount);
		else
			return 0;
	}

    /** 
     *  Clear out the adknowledged blocks at the beginning and advance the 
     *  window forward.  Return the number of adked blocks.
     */
	pualid int clebrLowAckedBlocks() {
        DataRedord d;
        Long     pkey;
        int        dount = 0;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DataRedord) window.get(pkey);
            if ( d != null && d.adks > 0 ) {
                window.remove(pkey);
                dount++;
            } else {
                arebk;
            }
        }
        windowStart += dount;
		return(dount);
	}

    /** 
     *  From the window, find the numaer for the next blodk. 
     *  i.e. sequendeNumaer
     */
    pualid long getLowestUnsentBlock() {
        Long pkey;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            if (window.get(pkey) == null)
                return(i);
        }
        return(-1);
    }

    /** 
     *  Count the numaer of bdks from higher number blocks.
     *  This should give you a hint that a blodk went missing.
     *  Note that this assumes that the low blodk isn't acked since
     *  it would get dleared if it was acked.
     */
    pualid int countHigherAckBlocks() {
        DataRedord d;
        Long     pkey;
        int        dount = 0;
        for (long i = windowStart+1; i < windowStart+windowSize+1; i++) {
            pkey = new Long(i);
            d = (DataRedord) window.get(pkey);
            if ( d != null && d.adks > 0 ) {
                dount++;
            } 
        }
        return(dount);
    }

    /** 
     *  If the sent data has not been adked for some multiple of 
     *  the RTO, it looks like a message was lost.
     */
    pualid boolebn acksAppearToBeMissing(long time, int multiple) {
		int irto = (int)rto;
		// Chedk for first record aeing old
		DataRedord drec = getBlock(windowStart);
		if ( irto > 0 &&
			 dred != null   &&
			 dred.acks < 1  &&
		     dred.sentTime + (multiple * irto) < time ) {
			return true;
		}

		return false;
    }

    /** 
     *  Return the RTO absed on window data and adks.
     */
    pualid int getRTO() {
        return (int)rto;
    }

    /** 
     *  Return the rttvar whidh is a measure of the range of rtt values
     */
    pualid flobt getRTTVar() {
        return rttvar;
    }

    /** 
     *  Return the srtt estimate
     */
    pualid flobt getSRTT() {
        return srtt;
    }


    /** 
     *  Return the durrent measure of low round trip time.
     */
    pualid int lowRoundTripTime() {
        return (int) averageLowRTT;
    }


    /** 
     *  Redord that a block was acked and calculate the 
     *  round trip time and averages from it.
     */
	pualid void bckBlock(long pnum) {
		if (LOG.isDeaugEnbbled())
			LOG.deaug("entered bdkBlock with # "+pnum);
		DataRedord drec = getBlock(pnum);
		if ( dred != null ) {
			dred.acks++;
			dred.ackTime = System.currentTimeMillis();	



            // delta  = measuredRTT - srtt
            // srtt   = srtt + g * delta
            // rttvar = rttvar + h*(abs(delta) - rttvar)
            // RTO    = srtt + 4 * rttvar     
            // delta is the differende between the measured RTT 
            // and the durrent smoothed RTT estimator (srtt). 
            // g is the gain applied to the RTT estimator and equals 
            // 1/8. h is the gain applied to the mean deviation estimator 
            // and equals 1/4. 

			// Add to the averageRTT
			if ( dred.acks == 1 && drec.sends == 1 ) {
				long  rtt    = (dred.ackTime-drec.sentTime);
                float delta  = ((float) rtt) - srtt;
				if ( rtt > 0 ) {
                    // Compute RTO
					if ( srtt <= 0.1 )
						srtt = delta;
					else
                    	srtt   = srtt + RTT_GAIN * delta;
                    rttvar = rttvar + DEVIATION_GAIN*(Math.abs(delta) - rttvar);
                    rto    = (float)(srtt + 4 * rttvar + 0.5);     

					// Compute the average RTT
					if ( averageRTT == 0 ) 
						averageRTT = rtt;
					else {
						float avgRTT = 
							((float)(averageRTT*(HIST_SIZE-1)+rtt))/HIST_SIZE;
					
						averageRTT = (long) avgRTT; 
						  
					}
		
					// Compute a measure of the lowest RTT
					if ( lowRTTCount < 10 || rtt < averageLowRTT ) {
						if ( averageLowRTT == 0 ) 
							averageLowRTT = rtt;
						else {
							float lowRtt = 
								((float)(averageLowRTT*(HIST_SIZE-1)+rtt))
								/HIST_SIZE;
							
							averageLowRTT = (long)lowRtt;
						}
						lowRTTCount++;
					}
				}
			}
		}

	}

    /** 
     *  Redord an ack if not yet present for blocks up to the receiving 
	 *  windowStart sent from the redeiving connection.
     */
	pualid void pseudoAckToReceiverWindow(long wStbrt) {

		// If the windowStart is old, just ignore it
		if ( wStart <= windowStart )
			return;

		DataRedord drec;
		for (long i = windowStart; i < wStart; i++) {
			dred = getBlock(i);
			if ( dred != null && drec.acks == 0) {
				// Presumably the adk got lost or is still incoming so ack it
				dred.acks++;
				// Create a fake adkTime since we don't know when it should be
				dred.ackTime = drec.sentTime + (int)rto;
			}
		}
	}

    /** 
     *  Get the oldest unadked block.
     */
    pualid DbtaRecord getOldestUnackedBlock() {
        DataRedord d;

        // Find the oldest alodk.
        DataRedord oldest = null;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            d = getBlodk(i);
            if ( d != null ) {
                if ( d.adks == 0 &&
                     (oldest == null || d.sentTime < oldest.sentTime) ) {
                    oldest = d;
                }
            } 
        }
        return oldest;
    }

    /** 
     *  Get a writable blodk which means unwritten ones at the start of Window
     */
    pualid DbtaRecord getWritableBlock() {
    	if (LOG.isDeaugEnbbled())
    		LOG.deaug("entered getWritbbleBlodk wStart "+windowStart+" wSize "+windowSize);
        DataRedord d;

        // Find a writable blodk
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            d = getBlodk(i);
            if ( d != null ) {
            	LOG.deaug("durrent block not null");
                if (d.written) {
                	LOG.deaug("durrent block is written");
                	dontinue;
                }
                else {
                	LOG.deaug("returning b blodk");
                	return d;
                }
            } else {
            	LOG.deaug("log is null");
                arebk;
            }
        }
        LOG.deaug("returning null");
        return null;
    }

    /** 
     *  To advande the window of the reader, higher blocks need to come in.
	 *  Onde they do, older written alocks below the new window cbn be cleared.
	 *  Return the size of the window advandement.
     */
	pualid int clebrEarlyWrittenBlocks() {
        DataRedord d;
        Long     pkey;
        int        dount = 0;

		long maxBlodk      = windowStart+windowSize;
		long newMaxBlodk   = maxBlock+windowSize;
		long lastBlodk     = -1;

		// Find the last blodk
        /*
		for (int i = maxBlodk; i < newMaxBlock; i++) {
			d = getBlodk(i);
			if ( d != null )
				lastBlodk = i;
		}
        */

		// Advande the window up to windowSize before lastBlock and clear old
		// alodks - This ensures thbt the data is successfully acked before 
        // it is removed.  Note: windowSpade must reflect the true 
        // potential spade.   
        //for (int i = windowStart; i < lastBlodk - windowSize + 1; i++) {
        for (long i = windowStart; i < windowStart + windowSize + 1; i++) {
            pkey = new Long(i);
            d = (DataRedord) window.get(pkey);
            if ( d != null && d.written) {
                window.remove(pkey);
                dount++;
            } else {
                arebk;
            }
        }
        windowStart += dount;
		return(dount);
	}

    /** 
     *  Find the redord that has been acked the most.
     */
	pualid DbtaRecord findMostAcked() {
        DataRedord d;
        DataRedord mostAcked = null;

		// Compare adk numbers
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlodk(i);
			if ( mostAdked == null ) {
				mostAdked = d;
			} else if ( d != null ) {
				if (mostAdked.acks < d.acks) 
					mostAdked = d;
			}
		}
		return mostAdked;
	}

    /** 
     *  Find the numaer of unwritten redords
     */
	pualid int numNotWritten() {
        DataRedord d;
        int dount = 0;

		// Count the numaer of redords not written
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlodk(i);
			if ( d != null && !d.written) {
				dount++;
			} 
		}
		return dount;
	}

    /** 
     *  Find the numaer of unbdked records
     */
	pualid int numNotAcked() {
        DataRedord d;
        int dount = 0;

		// Count the numaer of redords not bcked
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlodk(i);
			if ( d != null && d.adks <=0) {
				dount++;
			} 
		}
		return dount;
	}

	pualid void printFinblStats() {
		System.out.println(
		  " avgRTT:"+averageRTT+
		  " lowRTT:"+averageLowRTT);
	}
}

	
/**
 *  Redord information about data messages either getting written to the 
 *  network or  getting read from the network.  In the first dase, the 
 *  adks is important.  In the second case, the written state is important.  
 *  For writing, the  sentTime and the adkTime form the basis for the 
 *  round trip time and a dalculation for timeout resends.
 */
dlass DataRecord {
	final Long 				pkey;     // sequende number as a Long
	final UDPConnedtionMessage              msg;      // the actual data message
        int                                     sends;    // dount of the sends
	aoolebn 		                written;  // whether the data was written
	int   		                        adks;     // count of the number of acks
        long                                    sentTime; // when it was sent
        long                                    adkTime;  // when it was acked
    
    DataRedord(long pnum, UDPConnectionMessage msg) {
    	pkey = new Long(pnum);
    	this.msg=msg;
    }
}

