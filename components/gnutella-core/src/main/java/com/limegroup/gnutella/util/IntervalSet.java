package com.limegroup.gnutella.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.downloader.Interval;

/**
 * A "range" version of IntSet. This is a first cut of the class and does
 * not support all the operations IntSet does, just the ones we need for now.
 * <p>
 * Important Note: This class uses Interval from the download package. Ideally,
 * classes in the util package should be stand alone, but we need to have 
 * Interval stay in downloads for reasons of backward compatibility.
 */
public class IntervalSet implements Iterable<Interval>{
    
    /**
     * The sorted set of intervals this contains.
     */
    private final List<Interval> intervals;
    
    //constructor.
    public IntervalSet() {
        intervals = new ArrayList<Interval>();
    }

    /**
     * Creates an interval set representing a single Interval.
     * 
     * @param lowBound the lower bound of the represented Interval
     * @param highBound the upper bound of the represented Interval
     * @return an IntervalSet representing the range lowBound to highBound, inclusive.
     */
    public static IntervalSet createSingletonSet(long lowBound, long highBound) {
        IntervalSet ret = new IntervalSet();
        ret.add(new Interval(lowBound, highBound));
        return ret;
    }
    
    public void add(Interval addInterval) {
        final int low = addInterval.low;
        final int high = addInterval.high;
        Interval lower=null;
        Interval higher=null;
        for(Iterator<Interval> iter = intervals.iterator(); iter.hasNext(); ) {
            Interval interval = iter.next();
            if (low<=interval.low && interval.high<=high) {//  <low-------high>
                iter.remove();                             //      interval
                continue;
            }

            if (low >= interval.low && interval.high >= high) //   <low, high>
                return;                                       // ....interval....
            
            if (low<=interval.high + 1 && interval.low < low)    //     <low, high>
                lower=interval;                                  //  interval........

            if (interval.low - 1 <=high && interval.high > high)  //     <low, high>
                higher=interval;                                  //  .........interval
            
            // if high < interval.low we must have found all overlaps since
            // intervals is sorted.
            if (higher != null || interval.low > high)
                break;
        }

        //Add block.  Note that remove(..) is linear time.  That's not an issue
        //because there are typically few blocks.
        if (lower==null && higher==null) {
            //a) Doesn't overlap
            addImpl(new Interval(low, high));
        } else if (lower!=null && higher!=null) {
            //b) Join two blocks
            removeImpl(higher);
            removeImpl(lower);
            addImpl(new Interval(lower.low, higher.high));
        } else if (higher!=null) {
            //c) Join with higher
            removeImpl(higher);
            addImpl(new Interval(low, higher.high));
        } else /*if (lower!=null)*/ {
            //d) Join with lower
            removeImpl(lower);
            addImpl(new Interval(lower.low, high));
        }   
    }
    
    /**
     * Adds a whole IntervalSet into this IntervalSet.
     * @param set
     */
    public void add(IntervalSet set) {
        for(Interval interval : set)
            add(interval);
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
        for (Iterator<Interval> iter = intervals.iterator(); iter.hasNext();) {
            Interval interval = iter.next();
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
     * Deletes all intervals in the specified set
     * from this set.
     */
    public void delete(IntervalSet set) {
        for(Interval interval : set)
            delete(interval);
    }
    
    /**
     * Returns the first element without modifying this IntervalSet.
     * @throws NoSuchElementException if no intervals exist.
     */
    public Interval getFirst() throws NoSuchElementException {
        if(intervals.isEmpty())
            throw new NoSuchElementException();
        
        return intervals.get(0);
    }
    
    /**
     * Returns the last element without modifying this IntervalSet.
     * @throws NoSuchElementException if no intervals exist.
     */
    public Interval getLast() throws NoSuchElementException {
        if(intervals.isEmpty())
            throw new NoSuchElementException();
        
        Interval ret = intervals.get(intervals.size()-1);
        return ret;
    }
    
    /** @return the number of Intervals in this IntervalSet */
    public int getNumberOfIntervals() {
        return intervals.size();
    }

	/**
	 * @return whether this interval set contains fully the given interval
	 */
	public boolean contains(Interval i) {
        for(Interval ours : this) {
            if (ours.low <= i.low && ours.high >= i.high)
                return true;
        }
        return false;        
    }
    
    /**
     * @return whether this interval set contains any part of the given interval
     */
    public boolean containsAny(Interval i) {
        int low = i.low;
        int high = i.high;
        for(Interval interval : this) {
            if (low<=interval.low && interval.high<=high)  //  <low-------high>
                return true;                               //      interval

            if (low >= interval.low && interval.high >= high) //   <low, high>
                return true;                                  // ....interval....
            
            if (low<=interval.high + 1 && interval.low < low)    //     <low, high>
                return true;                                     //  interval........

            if (interval.low - 1 <=high && interval.high > high)  //     <low, high>
                return true;                                     //  .........interval
        }
        
        return false;
    }
	
    /**
     *@return a List of intervals that overlap checkInterval. For example
     * if Intervals contains{[1-4],[6-10]} and checkInterval is [3-8],
     * this method should return a list of 2 intervals {[3-4],[6-8]}
     * If there are no overlaps, this method returns an empty List.
     */
    public List<Interval> getOverlapIntervals(Interval checkInterval) {
        List<Interval> overlapBlocks = new ArrayList<Interval>(); //initialize for this write
        long high =checkInterval.high;
        long low = checkInterval.low;
        if (low > high)
            return overlapBlocks;
        
        //TODO2:For now we iterate over each of the inervals we have, 
        //but there should be a faster way of finding which intrevals we 
        //can overlap, Actually there is a max of  two intervals we can overlap
        //one on the top end and one on the bottom end. We need to make this 
        //more efficient
        for(Interval interval : intervals) {
            //case a:
            if(low <= interval.low && interval.high <= high) {
                //Need to check the whole iterval, starting point=interval.low
                overlapBlocks.add(interval);
                continue;
            }
            //case b:
            if(low<=interval.high && interval.low < low) {
                overlapBlocks.add(new Interval(low,
                                           Math.min(high,interval.high)));
            }
            //case c:
            if(interval.low <= high && interval.high > high) {
                overlapBlocks.add(new Interval(Math.max(interval.low,low),
                                               high));
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

    public Iterator<Interval> getAllIntervals() {
        return intervals.iterator();
    }
    
    public Iterator<Interval> iterator() {
        return intervals.iterator();
    }

    public List<Interval> getAllIntervalsAsList() {
        return new ArrayList<Interval>(intervals);
    }

    public int getSize() {
        int sum=0;
        for(Interval block : intervals) {
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
        boolean fixed = false;
        for (Iterator<Interval> iter=intervals.iterator(); iter.hasNext(); ) {
            interval = iter.next();
            if (interval.low!=0 && low<interval.low) {//needed for first interval
                if (low+1 > interval.low-1) {
                    if(!fixed) {
                        fixed = true;
                        fix();
                        iter = intervals.iterator();
                        low = -1;
                        interval = null;
                        continue;
                    } else {
                        throw new IllegalArgumentException("constructing invalid interval "+
                                " while trying to invert \n"+toString()+
                                " \n with size "+maxSize+
                                " low:"+low+" interval.low:"+interval.low);
                    }
                }
                ret.add(new Interval(low+1, interval.low-1));
            }
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
    public Iterator<Interval> getNeededIntervals(int maxSize) {
        return this.invert(maxSize).getAllIntervals();
    }

    /**
     * Clones the IntervalSet.  The underlying intervals are the same
     * (so they should never be modified), but the TreeSet this is
     * backed off of is new.
     */
    public IntervalSet clone() {
        IntervalSet ret = new IntervalSet();
        for(Interval interval : this)
            // access the internal TreeSet directly, - it's faster that way.
            ret.intervals.add(interval);
        return ret;
    }
    
    /**
     * Adds into the list, in order.
     */
    private void addImpl(Interval i) {
        int point = Collections.binarySearch(intervals, i, IntervalComparator.INSTANCE);
        if(point >= 0)
            throw new IllegalStateException("interval (" + i + ") already in list: " + intervals);
        point = -(point + 1);
        intervals.add(point, i);
    }
    
    /**
     * Removes from the list, quickly.
     */
    private void removeImpl(Interval i) {
        int point = Collections.binarySearch(intervals, i, IntervalComparator.INSTANCE);
        if(point < 0)
            throw new IllegalStateException("interval (" + i + ") doesn't exist in list: " + intervals);
        intervals.remove(point);
    }

    /**
     * Comparator for intervals.
     */
    private static class IntervalComparator implements Comparator<Interval> {
        private static final IntervalComparator INSTANCE = new IntervalComparator();
        public int compare(Interval ia, Interval ib) {
            if ( ia.low > ib.low ) 
                return 1;
            else if (ia.low < ib.low )
                return -1;
            else
                return 0;
                
           // return ia.low-ib.low;
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
        for(Interval current : intervals) {
    		current.toBytes(ret,pos);
    		pos+=8;
    	}
    	return ret;
    }
    
    /**
     * parses an IntervalSet from a byte array.  
     */
    public static IntervalSet parseBytes(byte [] data) throws IOException {
        if (data.length % 8 != 0) 
            throw new IOException();
        
    	IntervalSet ret = new IntervalSet();
    	for (int i =0; i< data.length/8;i++) {
    		int low = (int)ByteOrder.uint2long(ByteOrder.beb2int(data,i*8));
    		int high = (int)ByteOrder.uint2long(ByteOrder.beb2int(data,i*8+4));
            if (high < low || high < 0 || low < 0)
                throw new IOException();
    		ret.add(new Interval(low,high));
    	}
    	return ret;
    }
    
    /**
     * Recomposes intervals to ensure that invariants are met.
     */
    private void fix() {
        String preIntervals = intervals.toString();
        
        List<Interval> oldIntervals = new ArrayList<Interval>(intervals);
        intervals.clear();
        for(Iterator<Interval> i = oldIntervals.iterator(); i.hasNext(); )
            add(i.next());
        
        String postIntervals = intervals.toString();
        
        Assert.silent(false, 
            "IntervalSet invariants broken.\n" + 
            "Pre  Fixing: " + preIntervals + "\n" +
            "Post Fixing: " + postIntervals);
    }
}
