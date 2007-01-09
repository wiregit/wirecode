package com.limegroup.gnutella.udpconnect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.LongHashMap;


/**
 *  This class defines a DataWindow for sending or receiving data 
 *  using UDP with possible out of order data.  Within a certain window 
 *  size, this data will be accepted.  Data that has not been acknowledged,
 *  will remain.  For readers, the data can be passed on once any holes are 
 *  received. For the writer, if the round trip time for acks of the older data 
 *  is greatly exceeded, the data can be resent to try and receive an ack.
 *
 *  All methods in this class rely on external synchronization of access.
 * 
 *  TODO: DataMessage timing still requires work.
 */
public class DataWindow
{
	private static final Log LOG = LogFactory.getLog(DataWindow.class);
    
    public  static final int   MAX_SEQUENCE_NUMBER = 0xFFFF;
    private static final int   HIST_SIZE           = 4;
    private static final float RTT_GAIN            = 1.0f / 8.0f;
    private static final float DEVIATION_GAIN      = 1.0f / 4.0f;

	private final LongHashMap<DataRecord> window;
	private long    windowStart;
	private int     windowSize;
	private long    averageRTT;
	private long    averageLowRTT;
	private int     lowRTTCount;
    private float   srtt;
    private float   rttvar;
    private float   rto;
    
    /**
     * optimization: use this instead of iterating through data to see
     * if there is any readable data.
     * must be set/unset when data is added/removed.
     */
    private boolean readableData;

    /*
     *  Define a data window for sending or receiving multiple udp packets
     *  The size defines how much look ahead there is.  Start is normally zero
     *  or one.
     */
	public DataWindow(int size, long start) {
		windowStart = start;
		windowSize  = size;
		window      = new LongHashMap<DataRecord>(size+2);
	}

    /*
     *  Add a new message to the window.  
     */
	public DataRecord addData(DataMessage msg) {
		if (LOG.isDebugEnabled())
			LOG.debug("adding message seq "+msg.getSequenceNumber()+ " window start "+windowStart);

        long seqNo = msg.getSequenceNumber();
		DataRecord d = new DataRecord(msg);
        if(seqNo == windowStart)
            readableData = true;
        
		window.put(seqNo, d);

        return d;
	}

    /** 
     *  Get the block based on the sequenceNumber.
     */
	public DataRecord getBlock(long pnum) {
		return window.get(pnum);
	}

    /** 
     *  Get the start of the data window. The start will generally be the
     *  sequence number of the lowest unacked message.
     */
    public long getWindowStart() {
        return windowStart;
    }

    /** 
     *  Get the size of the data window.
     */
	public int getWindowSize() {
		return windowSize;
	}

    /** 
     *  Get the number of slots in use.  This excludes read data.
     */
    public int getUsedSpots() {
        DataRecord d;
        int        count = 0;
        for (long i = windowStart; i < windowStart+windowSize+3; i++) {
            // Count the spots that are full and not written
            if ( (d = window.get(i)) != null &&
                  (!d.read || i != windowStart))
                count++;
        }
        return(count);
    }

    /** 
     *  Get the number of slots available to be used.
     */
    public int getWindowSpace() {
        return(windowSize - getUsedSpots());
    }

    /** 
     *  Calculate the average wait time of the N lowest unresponded to 
     *  blocks
     */
	public int calculateWaitTime(long time, int n) {
        DataRecord d;
        int        count = 0;
		long       totalDelta = 0;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            d = window.get(i);
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
	public int clearLowAckedBlocks(ChunkReleaser releaser) {
        DataRecord d;
        int        count = 0;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            d = window.get(i);
            if ( d != null && d.acks > 0 ) {
                window.remove(i);
                count++;
                
                if(releaser != null)
                    releaser.releaseChunk(d.msg.getChunk());
            } else {
                break;
            }
        }
        windowStart += count;
		return count;
	}

    /** 
     *  From the window, find the number for the next block. 
     *  i.e. sequenceNumber
     */
    public long getLowestUnsentBlock() {
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            if (window.get(i) == null)
                return(i);
        }
        return -1;
    }

    /** 
     *  Count the number of acks from higher number blocks.
     *  This should give you a hint that a block went missing.
     *  Note that this assumes that the low block isn't acked since
     *  it would get cleared if it was acked.
     */
    public int countHigherAckBlocks() {
        DataRecord d;
        int        count = 0;
        for (long i = windowStart+1; i < windowStart+windowSize+1; i++) {
            d = window.get(i);
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
    public boolean acksAppearToBeMissing(long time, int multiple) {
		int irto = (int)rto;
		// Check for first record being old
		DataRecord drec = getBlock(windowStart);
		if ( irto > 0 &&
			 drec != null   &&
			 drec.acks < 1  &&
		     drec.sentTime + (multiple * irto) < time ) {
			return true;
		}

		return false;
    }

    /** 
     *  Return the RTO based on window data and acks.
     */
    public int getRTO() {
        return (int)rto;
    }

    /** 
     *  Return the rttvar which is a measure of the range of rtt values
     */
    public float getRTTVar() {
        return rttvar;
    }

    /** 
     *  Return the srtt estimate
     */
    public float getSRTT() {
        return srtt;
    }


    /** 
     *  Return the current measure of low round trip time.
     */
    public int lowRoundTripTime() {
        return (int) averageLowRTT;
    }


    /** 
     *  Record that a block was acked and calculate the 
     *  round trip time and averages from it.
     */
	public void ackBlock(long pnum) {
		if (LOG.isDebugEnabled())
			LOG.debug("entered ackBlock with # "+pnum);
		DataRecord drec = getBlock(pnum);
		if ( drec != null ) {
			drec.acks++;
			drec.ackTime = System.currentTimeMillis();	



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
                float delta  = rtt - srtt;
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
     *  Record an ack if not yet present for blocks up to the receiving 
	 *  windowStart sent from the receiving connection.
     */
	public void pseudoAckToReceiverWindow(long wStart) {

		// If the windowStart is old, just ignore it
		if ( wStart <= windowStart )
			return;

		DataRecord drec;
		for (long i = windowStart; i < wStart; i++) {
			drec = getBlock(i);
			if ( drec != null && drec.acks == 0) {
				// Presumably the ack got lost or is still incoming so ack it
				drec.acks++;
				// Create a fake ackTime since we don't know when it should be
				drec.ackTime = drec.sentTime + (int)rto;
			}
		}
	}

    /** 
     *  Get the oldest unacked block.
     */
    public DataRecord getOldestUnackedBlock() {
        DataRecord d;

        // Find the oldest block.
        DataRecord oldest = null;
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
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
     * Checks if we have atleast one block that can be read (in order).
     * Specifically, this will return false if there is atleast one null or
     * already-read block nearer to the windowStart than the first non-null non-read
     * block.
     */
    public boolean hasReadableData() {
        return readableData;
    }

    /** 
     *  Get a readable block.  This will return the first unread block starting from
     *  windowStart.  
     */
    public DataRecord getReadableBlock() {
    	if (LOG.isDebugEnabled())
            LOG.debug("wStart " + windowStart+" wSize "+windowSize);
        DataRecord d;

        // Find a readable block
        for (long i = windowStart; i < windowStart+windowSize+1; i++) {
            d = getBlock(i);
            if ( d != null ) {
                if (d.read)
                	continue;
                else
                	return d;
            } else {
                break;
            }
        }
        return null;
    }

    /** 
     *  To advance the window of the reader, higher blocks need to come in.
	 *  Once they do, older read blocks below the new window can be cleared.
	 *  Return the size of the window advancement.
     */
	public int clearEarlyReadBlocks() {
        DataRecord d;
        int        count = 0;

	//	long maxBlock      = windowStart+windowSize;
	//	long newMaxBlock   = maxBlock+windowSize;
	//	long lastBlock     = -1;

		// Find the last block
        /*
		for (int i = maxBlock; i < newMaxBlock; i++) {
			d = getBlock(i);
			if ( d != null )
				lastBlock = i;
		}
        */

		// Advance the window up to windowSize before lastBlock and clear old
		// blocks - This ensures that the data is successfully acked before 
        // it is removed.  Note: windowSpace must reflect the true 
        // potential space.   
        //for (int i = windowStart; i < lastBlock - windowSize + 1; i++) {
        for (long i = windowStart; i < windowStart + windowSize + 1; i++) {
            d = window.get(i);
            if ( d != null && d.read) {
                window.remove(i);
                count++;
            } else {
                if(d == null)
                    readableData = false;
                else
                    readableData = true;
                
                break;
            }
        }
        windowStart += count;
		return(count);
	}

    /** 
     *  Find the record that has been acked the most.
     */
	public DataRecord findMostAcked() {
        DataRecord d;
        DataRecord mostAcked = null;

		// Compare ack numbers
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
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
     *  Find the number of unread records
     */
	public int numNotRead() {
        DataRecord d;
        int count = 0;

		// Count the number of records not written
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && !d.read) {
				count++;
			} 
		}
		return count;
	}

    /** 
     *  Find the number of unacked records
     */
	public int numNotAcked() {
        DataRecord d;
        int count = 0;

		// Count the number of records not acked
		for (long i = windowStart; i < windowStart+windowSize+1; i++) {
			d = getBlock(i);
			if ( d != null && d.acks <=0) {
				count++;
			} 
		}
		return count;
	}

	public void printFinalStats() {
		System.out.println(
		  " avgRTT:"+averageRTT+
		  " lowRTT:"+averageLowRTT);
	}
}

	
/**
 *  Record information about data messages either getting written to the 
 *  network or  getting read from the network.  In the first case, the 
 *  acks is important.  In the second case, the read state is important.  
 *  For writing, the  sentTime and the ackTime form the basis for the 
 *  round trip time and a calculation for timeout resends.
 */
class DataRecord {
	final DataMessage  msg;      // the actual data message
    int                         sends;    // count of the sends
	boolean 		            read;     // whether the data was read
	int   		                acks;     // count of the number of acks
    long                        sentTime; // when it was sent
    long                        ackTime;  // when it was acked
    
    DataRecord(DataMessage msg) {
    	this.msg=msg;
    }
}

