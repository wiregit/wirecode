package com.limegroup.gnutella.udpconnect;

import java.util.HashMap;

/**
 *  This class defines a DataWindow for sending or receiving data 
 *  using UDP with possible out of order data.  Within a certain window 
 *  size, this data will be accepted.  Data that has not been acknowledged,
 *  will remain.  For readers, the data can be passed on once any holes are 
 *  received. For the writer, if the round trip time for acks of the older data 
 *  is greatly exceeded, the data can be resent to try and receive an ack.
 * 
 *  TODO: This class is getting reworked from an improved TFTP experiment into
 *        use with a full streaming connection. The RTT statistics are going 
 *  to be improved for better smoothing.
 */
public class DataWindow
{
    public  static final int   MAX_SEQUENCE_NUMBER = 0xFFFF;
    private static final int   HIST_SIZE           = 4;
    private static final float RTT_GAIN            = 1.0f / 8.0f;
    private static final float DEVIATION_GAIN      = 1.0f / 4.0f;

	private HashMap window;
	private int     windowStart;
	private int     windowSize;
	private int     endBlock;
	private long    averageRTT;
	private long    smoothRTT;
	private long    lowRTT;
	private int     lowRTTCount;
    private float   srtt;
    private float   rttvar;
    private int     rto;

    /*
     *  Define a data window for sending or receiving multiple udp packets
     *  The size defines how much look ahead there is.  Start is normally zero
     *  or one.
     */
	public DataWindow(int size, int start) {
		windowStart = start;
		windowSize  = size;
		window      = new HashMap(size+2);
		endBlock    = Integer.MAX_VALUE;
		averageRTT  = 0;
		lowRTT      = 0;
		lowRTTCount = 0;
	}

    /*
     *  Add a new message to the window.  TODO: Not based on packet.
     */
	public synchronized DataRecord addData(int pnum, UDPConnectionMessage msg) {
		DataRecord d;
		d          = new DataRecord();
		d.pnum     = pnum;
		d.msg      = msg;
		d.pkey     = String.valueOf(pnum);
		d.sends    = 0;
		d.written  = false;
		d.acks     = 0;
        d.sentTime = 0;
        d.ackTime  = 0;
		window.put(d.pkey, d);

        return d;
	}

    /*
     *  Record the end of the transfer location.  Obsolete.
     */
	public void setEndBlock(int blockNum) {
		endBlock = blockNum;
	}

    /*
     *  Get the end of the transfer location.  Obsolete.
     */
	public int getEndBlock() {
		return endBlock;
	}

	public synchronized DataRecord getBlock(int pnum) {
		String     pkey = String.valueOf(pnum);
		DataRecord d    = (DataRecord) window.get(pkey);
		return d;
	}

    /** 
     *  Get the start of the data window. The start will generally be the
     *  sequence number of the lowest unacked message.
     */
    public synchronized int getWindowStart() {
        return windowStart;
    }

    /** 
     *  Get the size of the data window.
     */
	public synchronized int getWindowSize() {
		return windowSize;
	}

    /** 
     *  Get the number of slots in use.
     */
    public synchronized int getUsedSpots() {
        String pkey;
        int    count = 0;
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = String.valueOf(i);
            if (window.get(pkey) != null)
                count++;
        }
        return(count);
    }

    /** 
     *  Get the number of slots available to be used.
     */
    public synchronized int getWindowSpace() {
        return(windowSize - getUsedSpots());
    }

    /** 
     *  Calculate the average wait time of the N lowest unresponded to 
     *  blocks
     */
	public synchronized int calculateWaitTime(long time, int n) {
        DataRecord d;
        String     pkey;
        int        count = 0;
		long       totalDelta = 0;
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = String.valueOf(i);
            d = (DataRecord) window.get(pkey);
            if ( d != null && d.acks == 0 ) {
                count++;
				totalDelta += time - d.sentTime;
				if (count >= n) 
					break;
            } 
        }
		if (count > 0)
			return(((int)totalDelta)/count);
		else
			return 0;
	}

    /** 
     *  Clear out the acknowledged blocks at the beginning and advance the 
     *  window forward.  Return the number of acked blocks.
     */
	public synchronized int clearLowAckedBlocks() {
        DataRecord d;
        String     pkey;
        int        count = 0;
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = String.valueOf(i);
            d = (DataRecord) window.get(pkey);
            if ( d != null && d.acks > 0 ) {
                window.remove(pkey);
                count++;
            } else {
                break;
            }
        }
        windowStart += count;
		return(count);
	}

    /** 
     *  From the window, find the number for the next block. 
     *  i.e. sequenceNumber
     */
    public synchronized int getLowestUnsentBlock() {
        String pkey;
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            pkey = String.valueOf(i);
            if (window.get(pkey) == null)
                return(i);
        }
        return(-1);
    }

    /** 
     *  Count the number of acks from higher number blocks.
     *  This should give you a hint that a block went missing.
     *  Note that this assumes that the low block isn't acked since
     *  it would get cleared if it was acked.
     */
    public synchronized int countHigherAckBlocks() {
        DataRecord d;
        String     pkey;
        int        count = 0;
        for (int i = windowStart+1; i < windowStart+windowSize+1; i++) {
            pkey = String.valueOf(i);
            d = (DataRecord) window.get(pkey);
            if ( d != null && d.acks > 0 ) {
                count++;
            } 
        }
        return(count);
    }

    /** 
     *  If the sent data has not been acked for some multiple of 
     *  the RTO, it looks like a message was lost.
     */
    public synchronized boolean acksAppearToBeMissing(long time, int multiple) {
		// Check for first record being old
		DataRecord drec = getBlock(windowStart);
		if ( rto > 0 &&
			 drec != null   &&
			 drec.acks < 1  &&
		     drec.sentTime + (multiple * smoothRTT) < time ) {
System.out.println("RTO:"+rto);
			return true;
		}

		return false;
    }

    /** 
     *  Return the RTO based on window data and acks.
     */
    public synchronized int getRTO() {
        return rto;
    }

    /** 
     *  Return the current measure of average round trip time.
     */
    public synchronized int averageRoundTripTime() {
		return (int) averageRTT;
	}

    /** 
     *  Return the current measure of low round trip time.
     */
    public synchronized int lowRoundTripTime() {
        return (int) lowRTT;
    }

    /** 
     *  Return a measure of the smoothed round trip time
     */
    public synchronized int smoothRoundTripTime() {
		return (int) smoothRTT;
	}

    /** 
     *  Record that a block was acked and calculate the 
     *  round trip time and averages from it.
     */
	public synchronized DataRecord ackBlock(int pnum) {
		DataRecord drec = getBlock(pnum);
		if ( drec != null ) {
			drec.acks++;
			drec.ackTime = System.currentTimeMillis();	


            // TODO:
            // delta  = measuredRTT - srtt
            // srtt   = srtt + g * delta
            // rttvar = rttvar + h*(abs(delta) - rttvar)
            // RTO    = srtt + 4 * rttvar     
            // delta is the difference between the measured RTT 
            // and the current smoothed RTT estimator (srtt). 
            // g is the gain applied to the RTT estimator and equals 
            // 1/8. h is the gain applied to the mean deviation estimator 
            // and equals 1/4. 

			// Add to the averageRTT
			if ( drec.acks == 1 && drec.sends == 1 ) {
				long  rtt    = (drec.ackTime-drec.sentTime);
				long  adjRTT = rtt + HIST_SIZE/2 + 1;
                float delta  = ((float) rtt) - srtt;
				if ( rtt > 0 ) {
                    // Compute RTO
                    srtt   = srtt + RTT_GAIN * delta;
                    rttvar = rttvar + DEVIATION_GAIN*(Math.abs(delta) - rttvar);
                    rto    = (int)(srtt + 4 * rttvar + 0.5);     

					// Compute the average RTT
					if ( averageRTT == 0 ) 
						averageRTT = rtt;
					else {
						averageRTT = 
						  (averageRTT*(HIST_SIZE-1))/HIST_SIZE +
						   adjRTT/HIST_SIZE;
					}

					// Compute a longer moving average of the RTT
					long adjAvgRTT = averageRTT + HIST_SIZE + 2;
					if ( smoothRTT == 0 ) 
						smoothRTT = rtt*4;
					else 
						smoothRTT = 
						  ((smoothRTT+2)*(2*HIST_SIZE-1))/(2*HIST_SIZE) + 
						   adjAvgRTT/(2*HIST_SIZE);
		
					// Compute a measure of the lowest RTT
					if ( lowRTTCount < 10 || rtt < lowRTT ) {
						if ( lowRTT == 0 ) 
							lowRTT = rtt;
						else 
							lowRTT = 
							  ((lowRTT+1)*(HIST_SIZE-1))/HIST_SIZE + 
							   adjRTT/HIST_SIZE;
						lowRTTCount++;
					}
				}
			}
		}
		return drec;
	}

    /** 
     *  Get the oldest unacked block.
     */
    public synchronized DataRecord getOldestUnackedBlock() {
        DataRecord d;

        // Find the oldest block.
        DataRecord oldest = null;
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            d = getBlock(i);
            if ( d != null ) {
                if ( d.acks == 0 &&
                     (oldest == null || d.sentTime < oldest.sentTime) ) {
                    oldest = d;
                }
            } 
        }
        return oldest;
    }

    /** 
     *  Get a writable block which means unwritten ones at the start of Window
     */
    public synchronized DataRecord getWritableBlock() {
        DataRecord d;

        // Find a writable block
        for (int i = windowStart; i < windowStart+windowSize+1; i++) {
            d = getBlock(i);
            if ( d != null ) {
                if (d.written) continue;
                else return d;
            } else {
                break;
            }
        }
        return null;
    }

    /** 
     *  To advance the window of the reader, higher blocks need to come in.
	 *  Once they do, older written blocks below the new window can be cleared.
	 *  Return the size of the window advancement.
     */
	public synchronized int clearEarlyWrittenBlocks() {
        DataRecord d;
        String     pkey;
        int        count = 0;

		int maxBlock      = windowStart+windowSize;
		int newMaxBlock   = maxBlock+windowSize;
		int lastBlock     = -1;

		// Find the last block
		for (int i = maxBlock; i < newMaxBlock; i++) {
			d = getBlock(i);
			if ( d != null )
				lastBlock = i;
		}

		// Advance the window up to windowSize before lastBlock and clear old
		// blocks
        for (int i = windowStart; i < lastBlock-windowSize+1; i++) {
            pkey = String.valueOf(i);
            d = (DataRecord) window.get(pkey);
            if ( d != null && d.written) {
                window.remove(pkey);
                count++;
            } else {
                break;
            }
        }
        windowStart += count;
		return(count);
	}

    /** 
     *  Find the record that has been acked the most.
     */
	public synchronized DataRecord findMostAcked() {
        DataRecord d;
        DataRecord mostAcked = null;

		// Compare ack numbers
		for (int i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( mostAcked == null ) {
				mostAcked = d;
			} else if ( d != null ) {
				if (mostAcked.acks < d.acks) 
					mostAcked = d;
			}
		}
		return mostAcked;
	}

    /** 
     *  Find the number of unwritten records
     */
	public synchronized int numNotWritten() {
        DataRecord d;
        int count = 0;

		// Count the number of records not written
		for (int i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && !d.written) {
				count++;
			} 
		}
		return count;
	}

    /** 
     *  Find the number of unacked records
     */
	public synchronized int numNotAcked() {
        DataRecord d;
        int count = 0;

		// Count the number of records not acked
		for (int i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && d.acks <=0) {
				count++;
			} 
		}
		return count;
	}

	public void printFinalStats() {
		System.out.println(
		  "smoothRTT:"+smoothRTT+
		  " avgRTT:"+averageRTT+
		  " lowRTT:"+lowRTT);
	}


}


	
class DataRecord {
	public int 			 		pnum;
	public String 				pkey;
	public UDPConnectionMessage msg;
    public int                  sends;
	public boolean 		        written;
	public int   		        acks;
    public long                 sentTime;
    public long                 ackTime;
}

