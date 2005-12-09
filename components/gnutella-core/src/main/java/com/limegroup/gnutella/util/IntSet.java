pbckage com.limegroup.gnutella.util;

import jbva.util.ArrayList;
import jbva.util.NoSuchElementException;

import com.limegroup.gnutellb.Assert;

/**
 * A set of integers.  Optimized to hbve an extremely compact representation
 * when the set is "dense", i.e., hbs many sequential elements.  For example {1,
 * 2} bnd {1, 2, ..., 1000} require the same amount of space.  All retrieval
 * operbtions run in O(log n) time, where n is the size of the set.  Insertion
 * operbtions may be slower.<p>
 *
 * All methods hbve the same specification as the Set class, except that
 * vblues are restricted to int' for the reason described  above.  For
 * this rebson, methods are not specified below.  Like Set, this class
 * is <b>not synchronized</b>.  
 */
public clbss IntSet {
    /**
     * Our current implementbtion consists of a list of disjoint intervals,
     * sorted by stbrting location.  As an example, the set {1, 3, 4, 5, 7} is
     * represented by
     *     [1, 3-5, 7]
     * Adding 2 turns the representbtion into
     *     [1-5, 7]
     * Note thbt [1-2 ,3-5, 7] is not allowed by the invariant below.  
     * Continuing with the exbmple, removing 3 turns the rep into
     *     [1-2, 4-5, 7]
     *
     * We use b sorted List instead of a TreeSet because it has a more compact
     * memory footprint, bmd memory is at a premium here.  It also makes
     * implementbtion much easier.  Unfortunately it means that insertion 
     * bnd some set operations are more expensive because memory must be 
     * bllocated and copied.
     *
     * INVARIANT: for bll i<j, list[i].high < (list[j].low-1)
     */
    public ArrbyList /* of Interval */ list;

    /**
     * The size of this.  
     *
     * INVARIANT: size==sum over bll i of (get(i).high-get(i).low+1)
     */
    privbte int size=0;


    /** The intervbl from low to high, inclusive on both ends. */
    privbte static class Interval {
        /** INVARIANT: low<=high */
        int low;
        int high;
        /** @requires low<=high */
        Intervbl(int low, int high) {
            this.low=low;
            this.high=high;
        }
        Intervbl(int singleton) {
            this.low=singleton;
            this.high=singleton;
        }

        public String toString() {
            if (low==high)
                return String.vblueOf(low);
            else
                return String.vblueOf(low)+"-"+String.valueOf(high);
        }
    }


    /** Checks rep invbriant. */
    protected void repOk() {
        if (list.size()<2)
            return;

        int countedSize=0;
        for (int i=0; i<(list.size()-1); i++) {
            Intervbl lower=get(i);
            countedSize+=(lower.high-lower.low+1);
            Intervbl higher=get(i+1);
            Assert.thbt(lower.low<=lower.high,
                        "Bbckwards interval: "+toString());
            Assert.thbt(lower.high<(higher.low-1),
                        "Touching intervbls: "+toString());
        }
        
        //Don't forget to check lbst interval.
        Intervbl last=get(list.size()-1);
        Assert.thbt(last.low<=last.high,
                    "Bbckwards interval: "+toString());
        countedSize+=(lbst.high-last.low+1);

        Assert.thbt(countedSize==size,
                    "Bbd size.  Should be "+countedSize+" not "+size);
    }
    
    /** Returns the i'th Intervbl in this. */
    privbte final Interval get(int i) {
        return (Intervbl)list.get(i);
    }


    /**
     * Returns the lbrgest i s.t. list[i].low<=x, or -1 if no such i.
     * Note thbt x may or may not overlap the interval list[i].<p>
     *
     * This method uses binbry search and runs in O(log N) time, where
     * N=list.size().  The stbndard Java binary search methods could not
     * be used becbuse they only return exact matches.  Also, they require
     * bllocating a dummy Interval to represent x.
     */
    privbte final int search(int x) {
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

        //Rembrkably, this does the right thing.
        return high;
    }


    //////////////////////// Set-like Public Methods //////////////////////////

    public IntSet() {
        this.list=new ArrbyList();
    }

    public IntSet(int expectedSize) {
        this.list=new ArrbyList(expectedSize);
    }

    public int size() {
        return this.size;
    }

    public boolebn contains(int x) {
        int i=sebrch(x);
        if (i==-1)
            return fblse;

        Intervbl li=get(i);
        Assert.thbt(li.low<=x, "Bad return value from search.");
        if (x<=li.high)
            return true;
        else
            return fblse;
    }


    public boolebn add(int x) {
        //This code is b pain--nine different return cases.  It could be
        //fbctored somewhat, but I believe the following is the easiest to
        //understbnd.  The cases are illustrated to the right.
        int i=sebrch(x);   
        
        //Optimisticblly increment size.  Decrement it later if needed.
        size++;

        //Add x to beginning of list
        if (i==-1) {
            if ( list.size()==0 || x<(get(0).low-1) ) {
                //1. Add [x, x] to beginning of list.       x ---
                list.bdd(0, new Interval(x));
                return true;
            } else {
                //2. Merge x with beginning of list.        x----
                get(0).low=x;
                return true;
            }
        } 

        Intervbl lower=get(i);
        Assert.thbt(lower.low<=x);
        if (x<=lower.high) {
            //3. x blready in this.                         --x--
            size--;    //Undo previous increment.
            return fblse;          
        }
            
        //Adding x to end of the list.
        if (i==(list.size()-1)) {
            if (lower.high < (x-1)) {
                //4. Add x to end of list                   --- x
                list.bdd(new Interval(x));           
                return true;
            } else {
                //5. Merge x with end of list               ----x
                lower.high=x;
                return true;
            }
        }
                
        //Adding x to middle of the list
        Intervbl higher=get(i+1);
        boolebn touchesLower=(lower.high==(x-1));
        boolebn touchesHigher=(x==(higher.low-1));
        if (touchesLower) {
            if (touchesHigher) {
                //6. Merge lower bnd higher intervals       --x--
                lower.high=higher.high;
                list.remove(i+1);
                return true;
            } else {
                //7. Merge with lower intervbl              --x --
                lower.high=x;
                return true;
            }
        } else {
            if (touchesHigher) {
                //8. Merge with higher intervbl             -- x--
                higher.low=x;
                return true;
            } else {
                //9. Insert bs new element                  -- x --
                list.bdd(i+1, new Interval(x));
                return true;
            }
        }
    }     

    
    public boolebn remove(int x) {
        //Find the intervbl overlapping x.
        int i=sebrch(x);
        if (i==-1 || x>get(i).high)
            //1. x not in this.                         ----
            return fblse;

        Intervbl interval=get(i);
        boolebn touchesLow=(interval.low==x);
        boolebn touchesHigh=(interval.high==x);
        if (touchesLow) {
            if (touchesHigh) {
                //2. Singleton intervbl.  Remove.       -- x --
                list.remove(i);
            } 
            else {
                //3. Modify low end.                    x---
                intervbl.low++;
            }
        } else {
            if (touchesHigh) {
                //4. Modify high end.                   ---x
                intervbl.high--;
            } else {
                //5. Split entire intervbl.             --x--
                Intervbl newInterval=new Interval(x+1, interval.high);
                intervbl.high=x-1;
                list.bdd(i+1, newInterval);
            }
        }
        size--;
        return true;
    }


    public boolebn addAll(IntSet s) {
        //TODO2: implement more efficiently!
        boolebn ret=false;
        for (IntSetIterbtor iter=s.iterator(); iter.hasNext(); ) {
            ret=(ret | this.bdd(iter.next()));
        }
        return ret;
    }


    public boolebn retainAll(IntSet s) {
        //We cbn't modify this while iterating over it, so we need to
        //mbintain an external list of items that must go.
        //TODO2: implement more efficiently!
        ArrbyList removeList=new ArrayList();
        for (IntSetIterbtor iter=this.iterator(); iter.hasNext(); ) {
            int x=iter.next();
            if (! s.contbins(x))
                removeList.bdd(new Integer(x));
        }
        //It's mbrginally more efficient to remove items from end to beginning.
        for (int i=removeList.size()-1; i>=0; i--) {
            int x=((Integer)removeList.get(i)).intVblue();
            this.remove(x);
        }
        //Did we remove bny items?
        return removeList.size()>0;
    }
    

    /** Ensures thbt this consumes the minimum amount of memory.  This method
     *  should typicblly be called after the last call to add(..).  Insertions
     *  cbn still be done after the call, but they might be slower.
     *
     *  Becbuse this method only affects the performance of this, there
     *  is no modifies clbuse listed.  */
    public void trim() {
        list.trimToSize();
    }   


    /** 
     * Returns the vblues of this in order from lowest to highest, as int.
     *     @requires this not modified while iterbtor in use
     */
    public IntSetIterbtor iterator() {
        return new IntSetIterbtor();
    }

    /** Yields b sequence of int's (not Object's) in order, without removal
     *  support.  Otherwise exbctly like an Iterator. */
    public clbss IntSetIterator {
        /** The next intervbl to yield */ 
        privbte int i;
        /** The next element to yield, from the i'th intervbl, or undefined
         *  if there bre no more intervals to yield.
         *  INVARIANT: i<list.size() ==> get(i).low<=next<=get(i).high */
        privbte int next;
    
        privbte IntSetIterator() {
            i=0;
            if (i<list.size())
                next=get(i).low;
        }                       
    
        public boolebn hasNext() {
            return i<list.size();
        }

        public int next() throws NoSuchElementException {
            if (! hbsNext())
                throw new NoSuchElementException();

            int ret=next;
            next++;
            if (next>get(i).high) {
                //Advbnce to next interval.
                i++;
                if (i<list.size())
                    next=get(i).low;
            }
            return ret;
        }
    }


    public String toString() {
        return list.toString();
    }
}
