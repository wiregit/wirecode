package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;

/**
 * A "range" version of IntSet. This is a first cut of the class and does
 * not support all the operations IntSet does, just the ones we need for now.
 * <p>
 * Important Note: This class uses Interval from the download package. Ideally,
 * classes in the util package should be stand alone, but we need to have 
 * Interval stay in downloads for reasons of backward compatibility.
 */
public class IntervalSet {
    
    /**
     * The sorted set of intervals this contains.
     */
    private final SortedSet /*of Interval*/ intervals;
    
    //constructor.
    public IntervalSet() {
        intervals = new TreeSet(new IntervalComparator());
    }

    public void add(Interval addInterval) {
        int low = addInterval.low;
        int high = addInterval.high;
        Interval lower=null;
        Interval higher=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            Interval interval=(Interval)iter.next();
            if (low<=interval.low && interval.high<=high) {//  <low-------high>
                iter.remove();                             //      interval
                continue;
            }

            if (low<=interval.high && interval.low<low)    //     <low, high>
                lower=interval;                            //  interval........

            if (interval.high == low-1)             //             <low,high>
                lower=interval;                     // ....interval

            if (interval.low<=high && interval.high>high)  //     <low, high>
                higher=interval;                           //  .........interval

            if (high == interval.low-1)              //  <low, high>
                higher = interval;                   //             ...interval
            
            // if high < interval.low we must have found all overlaps since
            // intervals is sorted.
            if (higher != null || interval.low > high)
                break;
        }

        //Add block.  Note that remove(..) is linear time.  That's not an issue
        //because there are typically few blocks.
        if (lower==null && higher==null) {
            //a) Doesn't overlap
            intervals.add(new Interval(low, high));
        } else if (lower!=null && higher!=null) {
            //b) Join two blocks
            intervals.remove(higher);
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, higher.high));
        } else if (higher!=null) {
            //c) Join with higher
            intervals.remove(higher);
            intervals.add(new Interval(low, higher.high));
        } else /*if (lower!=null)*/ {
            //d) Join with lower
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, high));
        }   
    }
    
    /**
     * Deletes any overlap of existing intervals with the Interval to delete.
     * @param deleteMe the Interval that should be deleted.
     */
    public void delete(Interval deleteMe) {
        int low = deleteMe.low;
        int high = deleteMe.high;
        Interval lower = null;
        Interval higher = null;
        for (Iterator iter = intervals.iterator(); iter.hasNext();) {
            Interval interval = (Interval) iter.next();
            if (interval.high >= low && interval.low <= high) { //found
                iter.remove();                                  // overlap
                if (interval.high <= high) {
                    if (interval.low < low)
                        // interval.low < low <= interval.high <= high
                        lower = new Interval(interval.low, low - 1);
                    // else 
                    // low <= interval.low <= interval.high <= high
                    // do nothing, the interval has already been removed
                        
                } else if (interval.low >= low) {
                    // low <= interval.low <= high < interval.high
                    higher = new Interval(high + 1, interval.high);
                    // safe to break here because intervals is sorted.
                    break;
                } else {
                    // interval.low < low <= high < interval.high
                    lower = new Interval(interval.low, low - 1);
                    higher = new Interval(high + 1, interval.high);
                    // we can break here because no other intervals will
                    // overlap with deleteMe
                    break;
                }
            }
            // stop here because intervals is sorted and all following 
            // intervals will be out of range:
            // low <= high < interval.low <= interval.high
            else if (interval.low >= high)
                break;
        }
        if (lower != null)
            add(lower);
        if (higher != null)
            add(higher);
    }
    
    /**
     * Deletes the specified all intervals in the specified set
     * from this set.
     */
    public void delete(IntervalSet set) {
        for (Iterator iter = set.getAllIntervals(); iter.hasNext(); )
            delete((Interval)iter.next());
    }
    
    /**
     * Removes the first element.  Throws NoSuchElementException
     * if no intervals exist.
     */
    public Interval removeFirst() throws NoSuchElementException {
        Interval ret = (Interval)intervals.first();
        intervals.remove(ret);
        return ret;
    }

    /**
     *@return a List of intervals that overlap checkInterval. For example
     * if Intervals contains{[1-4],[6-10]} and checkInterval is [3-8],
     * this method should return a list of 2 intervals {[3-4],[6-8]}
     * If there are no overlaps, this method returns an empty List.
     */
    public List getOverlapIntervals(Interval checkInterval) {
        List overlapBlocks = new ArrayList(); //initialize for this write
        long high =checkInterval.high;
        long low = checkInterval.low;
        if (low >= high)
            return overlapBlocks;
        
        //TODO2:For now we iterate over each of the inervals we have, 
        //but there should be a faster way of finding which intrevals we 
        //can overlap, Actually there is a max of  two intervals we can overlap
        //one on the top end and one on the bottom end. We need to make this 
        //more efficient
        for(Iterator iter = intervals.iterator(); iter.hasNext(); ) {
            Interval interval = (Interval)iter.next();
            //case a:
            if(low <= interval.low && interval.high <= high) {
                //Need to check the whole iterval, starting point=interval.low
                overlapBlocks.add(interval);
                continue;
            }
            //case b:
            if(low<=interval.high && interval.low < low) {
                overlapBlocks.add(new Interval((int)low,
                                           Math.min((int)high,interval.high)));
            }
            //case c:
            if(interval.low <= high && interval.high > high) {
                overlapBlocks.add(new Interval(Math.max(interval.low,(int)low),
                                               (int)high));
            }
            //Note: There is one condition under which case b and c are both
            //true. In this case the same interval will be added twice. The
            //effect of this is that we will check the same overlap interval 
            //2 times. We are still doing it this way, beacuse this conditon
            //will not happen in practice, and the code looks better this way, 
            //and finally, it cannot do any harm - the worst that can happen is
            //that we check the exact same interval twice.
        }
        return overlapBlocks;
    }

    public Iterator getAllIntervals() {
        return intervals.iterator();
    }

    public List getAllIntervalsAsList() {
        return new ArrayList(intervals);
    }

    public int getSize() {
        int sum=0;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            Interval block=(Interval)iter.next();
            sum+=block.high-block.low+1;
        }
        return sum;
    }
    
    public boolean isEmpty() {
        return intervals.isEmpty();
    }
    
    public void clear() {
        intervals.clear();
    }

    /**
     * This method creates an IntervalSet that is the negative to this 
     * IntervalSet
     * @return IntervalSet containing all ranges not contained in this
     */
    public IntervalSet invert(int maxSize) {
        IntervalSet ret = new IntervalSet();
        if(maxSize < 1) 
            return ret; //return an empty IntervalSet
        if (intervals.size()==0) {//Nothing recorded?
            Interval block=new Interval(0, maxSize-1);
            ret.add(block);
            return ret;
        }
            
        //Now step through list one element at a time, putting gaps into buf.
        //We take advantage of the fact that intervals are disjoint.  Treat
        //beginning specially.  
        //LOOP INVARIANT: interval!=null ==> low==interval.high
        int low=-1;
        Interval interval=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            interval=(Interval)iter.next();
            if (interval.low!=0 && low<interval.low)//needed for first interval
                ret.add(new Interval(low+1, interval.low-1));
            low=interval.high;
        }
        //Special case space between last block and end of file.
        Assert.that(interval!=null, "Null interval in getFreeBlocks");
        if (interval.high < maxSize-1)
            ret.add(new Interval(interval.high+1, maxSize-1));
        return ret;
    }
        
    /**
     * @return an iterator or intervals needed to fill in the holes in this
     * IntervalSet. Note that the IntervalSet does not know the maximum value of
     * all the intervals.
     */
    public Iterator getNeededIntervals(int maxSize) {
        return this.invert(maxSize).getAllIntervals();
    }

    /**
     * Clones the IntervalSet.  The underlying intervals are the same
     * (so they should never be modified), but the TreeSet this is
     * backed off of is new.
     */
    public Object clone() {
        IntervalSet ret = new IntervalSet();
        for (Iterator iter = getAllIntervals(); iter.hasNext(); )
            // access the internal TreeSet directly, - it's faster that way.
            ret.intervals.add(iter.next());
        return ret;
    }


    /**
     * Comparator for intervals.
     */
    private class IntervalComparator implements Comparator {
        public int compare(Object a, Object b) {
            Interval ia=(Interval)a;
            Interval ib=(Interval)b;
            return ia.low-ib.low;
        }
    }
    
    /**
     * Lists the contained intervals.
     */
    public String toString() {
        return intervals.toString();
    }
    
    /**
     *
     * @return packed representation of the intervals.
     */
    public byte [] toBytes() {
    	byte [] ret = new byte[intervals.size()*8];
    	int pos = 0;
    	for (Iterator iter = intervals.iterator();iter.hasNext();) {
    		Interval current = (Interval) iter.next();
    		System.arraycopy(current.toBytes(),0,ret,pos,8);
    		pos+=8;
    	}
    	return ret;
    }
    
    /**
     * parses an IntervalSet from a byte array.  Each interval is 8 bytes; so if
     * the size of the array is not divisible by 8 the last remaining bytes are discarded.
     * @return
     */
    public static IntervalSet parseBytes(byte [] data) {
    	IntervalSet ret = new IntervalSet();
    	for (int i =0; i< data.length/8;i++) {
    		int low = ByteOrder.beb2int(data,i*8);
    		int high = ByteOrder.beb2int(data,i*8+4);
    		ret.add(new Interval(low,high));
    	}
    	return ret;
    }
    /**
     * Parses X-Available-Ranges header and returns an IntervalSet.
     * 
     * @param line the X-Available-Ranges header line which should look like:
     *         "X-Available-Ranges: bytes A-B, C-D, E-F"
     *         "X-Available-Ranges:bytes A-B"
     * @param rfd the RemoteFileDesc2 for the location we are trying to download
     *         from. We need this to check buggy clients that send available ranges
     * 		past the end of the file.  If this is null, we do not perform the check.. 
     * @exception ProblemReadingHeaderException when we could not parse the 
     *         header line.
     */
    public static IntervalSet parseFromHttpHeader(String line, RemoteFileDesc rfd) 
    	throws ProblemReadingHeaderException{
    	IntervalSet availableRanges = new IntervalSet();
        
        line = line.toLowerCase();
        // start parsing after the word "bytes"
        int start = line.indexOf("bytes") + 6;
        // if start == -1 the word bytes has not been found
        // if start >= line.length we are at the end of the 
        // header line
        while (start != -1 && start < line.length()) {
            // try to parse the number before the dash
            int stop = line.indexOf('-', start);
            // test if this is a valid interval
            if ( stop == -1 )
                break; 

            // this is the interval to store the available 
            // range we are parsing in.
            Interval interval = null;
    
            try {
                // read number before dash
                // bytes A-B, C-D
                //       ^
                int low = Integer.parseInt(line.substring(start, stop).trim());
                
                // now moving the start index to the 
                // character after the dash:
                // bytes A-B, C-D
                //         ^
                start = stop + 1;
                // we are parsing the number before the comma
                stop = line.indexOf(',', start);
                
                // If we are at the end of the header line, there is no comma 
                // following.
                if ( stop == -1 )
                    stop = line.length();
                
                // read number after dash
                // bytes A-B, C-D
                //         ^
                int high = Integer.parseInt(line.substring(start, stop).trim());
                
                if (rfd!=null &&
                		high >= rfd.getSize())
                    high = rfd.getSize()-1;

                if(low > high)//interval read off network is bad, try next one
                    continue;

                // this interval should be inclusive at both ends
                interval = new Interval( low, high );
                
                // start parsing after the next comma. If we are at the
                // end of the header line start will be set to 
                // line.length() +1
                start = stop + 1;
                
            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException(e);
            }
            availableRanges.add(interval);
        }
        return availableRanges;
    }
}
