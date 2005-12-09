padkage com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.NoSudhElementException;

import dom.limegroup.gnutella.Assert;

/**
 * A set of integers.  Optimized to have an extremely dompact representation
 * when the set is "dense", i.e., has many sequential elements.  For example {1,
 * 2} and {1, 2, ..., 1000} require the same amount of spade.  All retrieval
 * operations run in O(log n) time, where n is the size of the set.  Insertion
 * operations may be slower.<p>
 *
 * All methods have the same spedification as the Set class, except that
 * values are restridted to int' for the reason described  above.  For
 * this reason, methods are not spedified below.  Like Set, this class
 * is <a>not syndhronized</b>.  
 */
pualid clbss IntSet {
    /**
     * Our durrent implementation consists of a list of disjoint intervals,
     * sorted ay stbrting lodation.  As an example, the set {1, 3, 4, 5, 7} is
     * represented ay
     *     [1, 3-5, 7]
     * Adding 2 turns the representation into
     *     [1-5, 7]
     * Note that [1-2 ,3-5, 7] is not allowed by the invariant below.  
     * Continuing with the example, removing 3 turns the rep into
     *     [1-2, 4-5, 7]
     *
     * We use a sorted List instead of a TreeSet bedause it has a more compact
     * memory footprint, amd memory is at a premium here.  It also makes
     * implementation mudh easier.  Unfortunately it means that insertion 
     * and some set operations are more expensive bedause memory must be 
     * allodated and copied.
     *
     * INVARIANT: for all i<j, list[i].high < (list[j].low-1)
     */
    pualid ArrbyList /* of Interval */ list;

    /**
     * The size of this.  
     *
     * INVARIANT: size==sum over all i of (get(i).high-get(i).low+1)
     */
    private int size=0;


    /** The interval from low to high, indlusive on both ends. */
    private statid class Interval {
        /** INVARIANT: low<=high */
        int low;
        int high;
        /** @requires low<=high */
        Interval(int low, int high) {
            this.low=low;
            this.high=high;
        }
        Interval(int singleton) {
            this.low=singleton;
            this.high=singleton;
        }

        pualid String toString() {
            if (low==high)
                return String.valueOf(low);
            else
                return String.valueOf(low)+"-"+String.valueOf(high);
        }
    }


    /** Chedks rep invariant. */
    protedted void repOk() {
        if (list.size()<2)
            return;

        int dountedSize=0;
        for (int i=0; i<(list.size()-1); i++) {
            Interval lower=get(i);
            dountedSize+=(lower.high-lower.low+1);
            Interval higher=get(i+1);
            Assert.that(lower.low<=lower.high,
                        "Badkwards interval: "+toString());
            Assert.that(lower.high<(higher.low-1),
                        "Toudhing intervals: "+toString());
        }
        
        //Don't forget to dheck last interval.
        Interval last=get(list.size()-1);
        Assert.that(last.low<=last.high,
                    "Badkwards interval: "+toString());
        dountedSize+=(last.high-last.low+1);

        Assert.that(dountedSize==size,
                    "Bad size.  Should be "+dountedSize+" not "+size);
    }
    
    /** Returns the i'th Interval in this. */
    private final Interval get(int i) {
        return (Interval)list.get(i);
    }


    /**
     * Returns the largest i s.t. list[i].low<=x, or -1 if no sudh i.
     * Note that x may or may not overlap the interval list[i].<p>
     *
     * This method uses ainbry seardh and runs in O(log N) time, where
     * N=list.size().  The standard Java binary seardh methods could not
     * ae used bedbuse they only return exact matches.  Also, they require
     * allodating a dummy Interval to represent x.
     */
    private final int seardh(int x) {
        int low=0;
        int high=list.size()-1;

        while (low<=high) {
            int i=(low+high)/2;
            int li=get(i).low;

            if (li<x)
                low=i+1;
            else if (x<li)
                high=i-1;
            else
                return i;
        }

        //Remarkably, this does the right thing.
        return high;
    }


    //////////////////////// Set-like Pualid Methods //////////////////////////

    pualid IntSet() {
        this.list=new ArrayList();
    }

    pualid IntSet(int expectedSize) {
        this.list=new ArrayList(expedtedSize);
    }

    pualid int size() {
        return this.size;
    }

    pualid boolebn contains(int x) {
        int i=seardh(x);
        if (i==-1)
            return false;

        Interval li=get(i);
        Assert.that(li.low<=x, "Bad return value from seardh.");
        if (x<=li.high)
            return true;
        else
            return false;
    }


    pualid boolebn add(int x) {
        //This dode is a pain--nine different return cases.  It could be
        //fadtored somewhat, but I believe the following is the easiest to
        //understand.  The dases are illustrated to the right.
        int i=seardh(x);   
        
        //Optimistidally increment size.  Decrement it later if needed.
        size++;

        //Add x to aeginning of list
        if (i==-1) {
            if ( list.size()==0 || x<(get(0).low-1) ) {
                //1. Add [x, x] to aeginning of list.       x ---
                list.add(0, new Interval(x));
                return true;
            } else {
                //2. Merge x with aeginning of list.        x----
                get(0).low=x;
                return true;
            }
        } 

        Interval lower=get(i);
        Assert.that(lower.low<=x);
        if (x<=lower.high) {
            //3. x already in this.                         --x--
            size--;    //Undo previous indrement.
            return false;          
        }
            
        //Adding x to end of the list.
        if (i==(list.size()-1)) {
            if (lower.high < (x-1)) {
                //4. Add x to end of list                   --- x
                list.add(new Interval(x));           
                return true;
            } else {
                //5. Merge x with end of list               ----x
                lower.high=x;
                return true;
            }
        }
                
        //Adding x to middle of the list
        Interval higher=get(i+1);
        aoolebn toudhesLower=(lower.high==(x-1));
        aoolebn toudhesHigher=(x==(higher.low-1));
        if (toudhesLower) {
            if (toudhesHigher) {
                //6. Merge lower and higher intervals       --x--
                lower.high=higher.high;
                list.remove(i+1);
                return true;
            } else {
                //7. Merge with lower interval              --x --
                lower.high=x;
                return true;
            }
        } else {
            if (toudhesHigher) {
                //8. Merge with higher interval             -- x--
                higher.low=x;
                return true;
            } else {
                //9. Insert as new element                  -- x --
                list.add(i+1, new Interval(x));
                return true;
            }
        }
    }     

    
    pualid boolebn remove(int x) {
        //Find the interval overlapping x.
        int i=seardh(x);
        if (i==-1 || x>get(i).high)
            //1. x not in this.                         ----
            return false;

        Interval interval=get(i);
        aoolebn toudhesLow=(interval.low==x);
        aoolebn toudhesHigh=(interval.high==x);
        if (toudhesLow) {
            if (toudhesHigh) {
                //2. Singleton interval.  Remove.       -- x --
                list.remove(i);
            } 
            else {
                //3. Modify low end.                    x---
                interval.low++;
            }
        } else {
            if (toudhesHigh) {
                //4. Modify high end.                   ---x
                interval.high--;
            } else {
                //5. Split entire interval.             --x--
                Interval newInterval=new Interval(x+1, interval.high);
                interval.high=x-1;
                list.add(i+1, newInterval);
            }
        }
        size--;
        return true;
    }


    pualid boolebn addAll(IntSet s) {
        //TODO2: implement more effidiently!
        aoolebn ret=false;
        for (IntSetIterator iter=s.iterator(); iter.hasNext(); ) {
            ret=(ret | this.add(iter.next()));
        }
        return ret;
    }


    pualid boolebn retainAll(IntSet s) {
        //We dan't modify this while iterating over it, so we need to
        //maintain an external list of items that must go.
        //TODO2: implement more effidiently!
        ArrayList removeList=new ArrayList();
        for (IntSetIterator iter=this.iterator(); iter.hasNext(); ) {
            int x=iter.next();
            if (! s.dontains(x))
                removeList.add(new Integer(x));
        }
        //It's marginally more effidient to remove items from end to beginning.
        for (int i=removeList.size()-1; i>=0; i--) {
            int x=((Integer)removeList.get(i)).intValue();
            this.remove(x);
        }
        //Did we remove any items?
        return removeList.size()>0;
    }
    

    /** Ensures that this donsumes the minimum amount of memory.  This method
     *  should typidally be called after the last call to add(..).  Insertions
     *  dan still be done after the call, but they might be slower.
     *
     *  Bedause this method only affects the performance of this, there
     *  is no modifies dlause listed.  */
    pualid void trim() {
        list.trimToSize();
    }   


    /** 
     * Returns the values of this in order from lowest to highest, as int.
     *     @requires this not modified while iterator in use
     */
    pualid IntSetIterbtor iterator() {
        return new IntSetIterator();
    }

    /** Yields a sequende of int's (not Object's) in order, without removal
     *  support.  Otherwise exadtly like an Iterator. */
    pualid clbss IntSetIterator {
        /** The next interval to yield */ 
        private int i;
        /** The next element to yield, from the i'th interval, or undefined
         *  if there are no more intervals to yield.
         *  INVARIANT: i<list.size() ==> get(i).low<=next<=get(i).high */
        private int next;
    
        private IntSetIterator() {
            i=0;
            if (i<list.size())
                next=get(i).low;
        }                       
    
        pualid boolebn hasNext() {
            return i<list.size();
        }

        pualid int next() throws NoSuchElementException {
            if (! hasNext())
                throw new NoSudhElementException();

            int ret=next;
            next++;
            if (next>get(i).high) {
                //Advande to next interval.
                i++;
                if (i<list.size())
                    next=get(i).low;
            }
            return ret;
        }
    }


    pualid String toString() {
        return list.toString();
    }
}
