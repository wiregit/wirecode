package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import java.util.*;

/** 
 * A very simple fixed-size double-ended queue.
 * (The fixed size is intentional, not the result of laziness!)
 * This is not thread-safe.
 */
public class Buffer {
    /**
     * The abstraction function is
     *   [ buf[next-1], buf[next-2], ..., buf[0],
     *     buf[size-1], buf[size-2]. ..., buf[next] ]
     * if buf[next]!=null.  Otherwise it is
     *   [ buf[next-1], buf[next-2], ..., buf[0] ]
     * Note that buf[next] is the place to put the next element.  
     *
     * INVARIANT: buf.length=size 
     *            0<=next<size
     *            (buf[size-1]==null)==(buf[size-2]==null)==...==(buf[next]==null)
     */
    private int size;
    private Object buf[];
    private int next;

    /** 
     * @requires this
     * @effects creates a new, empty buffer of the given size. 
     */
    public Buffer(int size) {
	this.size=size;
	buf=new Object[size];
	next=0;
    }

    /** 
     * @modifies this
     * @requires x!=null
     * @effects adds x to the head of this, removing the tail 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    public Object add(Object x) {
	Object old=buf[next];
	buf[next]=x;
	next=(next+1) % size;
	return old;
    }

    /** 
     * @effects returns an iterator that yields the elements of this, from
     *  youngest (mostly recently added) to oldest.
     * @requires this not modified will iterator in use.
     */
    public Iterator iterator() {
	return new BufferIterator();
    }

    private class BufferIterator extends UnmodifiableIterator {
	/** The index of the next element to yield. */
	int i;
	/** True if I just yielded buf[next].
	 *  Also true if the next element is null. 
	 *  In either case, I'm done. */
	boolean done;

	BufferIterator() {
	    if (next==0) {
		i=size-1;
		done=(buf[i]==null); //special case: we're already done
	    } else
		i=next-1;
	}

	public boolean hasNext() {
	    return !done;
	}

	public Object next() throws NoSuchElementException {
	    if (done) throw new NoSuchElementException();
	    Object ret=buf[i];

	    if (i==next) //we just yielded the oldest element in this
		done=true;

	    //Move i to the left, wrapping around if necessary.
	    if (i==0) {
		i=size-1;         //wrap around
		if (buf[i]==null) //if last element is null, we're done.
		    done=true;
	    } else {
		i--;
	    }
	    return ret;
	}
    }

//      public static void main(String args[]) {
//  	Buffer buf=new Buffer(4);
//  	Iterator iter=null;

//  	iter=buf.iterator();
//  	Assert.that(!iter.hasNext());
//  	try {
//  	    iter.next();
//  	    Assert.that(false);
//  	} catch (NoSuchElementException e) {
//  	    Assert.that(true);
//  	}

//  	buf.add("test");
//  	iter=buf.iterator();
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test"));
//  	Assert.that(!iter.hasNext());
//  	try {
//  	    iter.next();
//  	    Assert.that(false);
//  	} catch (NoSuchElementException e) {
//  	    Assert.that(true);
//  	}

//  	buf.add("test2");
//  	buf.add("test3");
//  	iter=buf.iterator();
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test3"));
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test2"));
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test"));
//  	Assert.that(!iter.hasNext());

//  	buf.add("test4");
//  	buf.add("test5");
//  	iter=buf.iterator();
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test5"));
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test4"));
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test3"));
//  	Assert.that(iter.hasNext());
//  	Assert.that(iter.next().equals("test2"));
//  	Assert.that(!iter.hasNext());	    
//      }
}
