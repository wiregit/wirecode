package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import java.util.*;

/** 
 * A very simple fixed-size double-ended queue, i.e., a circular buffer.
 * The fixed size is intentional, not the result of laziness; use this 
 * data structure when you want to use a fix amount of resources.
 * This is not thread-safe.
 */
public class Buffer {
    /**
     * The abstraction function is
     *   [ buf[head], buf[head+1], ..., buf[tail-1] ] if head<tail
     * or
     *   [ buf[head], buf[head+1], ..., buf[size-1], 
     *     buf[0], buf[1], ..., buf[tail-1] ]         otherwise
     *
     * Note that buf[head] is the location of the head, and
     * buf[tail] is just past the location of the tail. This
     * means that there is always one unused element of the array.
     * See p. 202 of  _Introduction to Algorithms_ by Cormen, 
     * Leiserson, Rivest for details.
     *
     * INVARIANT: buf.length=size 
     *            0<=head, tail<size
     *            size>=2
     */
    private int size;
    private Object buf[];
    private int head;
    private int tail;

    /** 
     * @requires size>=1
     * @effects creates a new, empty buffer that can hold 
     *  size elements.
     */
    public Buffer(int size) {
	Assert.that(size>=1);
	//one element of buf unused
	this.size=size+1;
	buf=new Object[size+1];
	head=0;
	tail=0;
    }

    /** Returns true iff this is empty. */
    public boolean isEmpty() {
	return head==tail;
    }

    /** Returns true iff this is full, e.g., adding another element 
     *  would force another out. */
    public boolean isFull() {
	return increment(tail)==head;
    }

    private int decrement(int i) {
	if (i==0)
	    return size-1;
	else
	    return i-1;
    }

    private int increment(int i) {
	if (i==(size-1))
	    return 0;
	else
	    return i+1;
    }

    /** Same as addFirst(x). */
    public Object add(Object x) {
	return addFirst(x);
    }

    /** 
     * @modifies this
     * @effects adds x to the head of this, removing the tail 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    public Object addFirst(Object x) {
	Object ret=null;
	if (isFull())
	    ret=removeLast();
	head=decrement(head);
	buf[head]=x;
	return ret;
    }

    /** 
     * @modifies this
     * @effects adds x to the tail of this, removing the head 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    public Object addLast(Object x) {
	Object ret=null;
	if (isFull())
	    ret=removeFirst();
	buf[tail]=x;
	tail=increment(tail);
	return ret;
    }

    /**
     * Returns the head of this, or throws NoSuchElementException if
     * this is empty.
     */
    public Object first() throws NoSuchElementException {
	if (isEmpty())
	    throw new NoSuchElementException();
	return buf[head];
    }
    
    /**
     * Returns the tail of this, or throws NoSuchElementException if
     * this is empty.
     */
    public Object last() throws NoSuchElementException {
	if (isEmpty())
	    throw new NoSuchElementException();
	return buf[decrement(tail)];
    }    

    /**
     * @modifies this
     * @effects Removes and returns the head of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public Object removeFirst() throws NoSuchElementException {
	if (isEmpty())
	    throw new NoSuchElementException();
	Object ret=buf[head];
	head=increment(head);
	return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the tail of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public Object removeLast() throws NoSuchElementException {
	if (isEmpty())
	    throw new NoSuchElementException();
	tail=decrement(tail);
	return buf[tail];
    }

    /** 
     * @effects returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    public Iterator iterator() {
	return new BufferIterator();
    }

    private class BufferIterator extends UnmodifiableIterator {
	/** The index of the next element to yield. */
	int i;	
	/** Defensive programming; detect modifications while
	 *  iterator in use. */
	int oldHead;
	int oldTail;

	BufferIterator() {
	    i=head;
	    oldHead=head;
	    oldTail=tail;
	}

	public boolean hasNext() {
	    ensureNoModifications();
	    return i!=tail;
	}

	public Object next() throws NoSuchElementException {
	    ensureNoModifications();
	    if (!hasNext()) 
		throw new NoSuchElementException();

	    Object ret=buf[i];
	    i=increment(i);
	    return ret;
	}

	private void ensureNoModifications() {
	    if (oldHead!=head || oldTail!=tail)
		throw new ConcurrentModificationException();
	}
    }

//      public static void main(String args[]) {
//  	//1. Tests of old methods.
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

//  	//2.  Tests of new methods.  These are definitely not sufficient.
//  	buf=new Buffer(4);
//  	Assert.that(buf.addLast("a")==null);
//  	Assert.that(buf.addLast("b")==null);
//  	Assert.that(buf.addLast("c")==null);
//  	Assert.that(buf.addLast("d")==null);
//  	Assert.that(buf.first().equals("a"));
//  	Assert.that(buf.removeFirst().equals("a"));
//  	Assert.that(buf.first().equals("b"));
//  	Assert.that(buf.removeFirst().equals("b"));
//  	Assert.that(buf.addFirst("b")==null);
//  	Assert.that(buf.addFirst("a")==null);
//  	//buf=[a b c d]

//  	Assert.that(buf.addLast("e").equals("a"));
//  	//buf=[b c d e]
//  	Assert.that(buf.last().equals("e"));
//  	Assert.that(buf.first().equals("b"));
//  	Assert.that(buf.removeLast().equals("e"));
//  	Assert.that(buf.removeLast().equals("d"));		

//  	buf=new Buffer(4);
//  	iter=buf.iterator();
//  	buf.addFirst("a");
//  	try {
//  	    iter.hasNext();
//  	    Assert.that(false);
//  	} catch (ConcurrentModificationException e) {
//  	    Assert.that(true);
//  	}
//      }
}
