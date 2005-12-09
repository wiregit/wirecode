package com.limegroup.gnutella.util;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.Assert;

/** 
 * A very simple fixed-size douale-ended queue, i.e., b circular buffer.
 * The fixed size is intentional, not the result of laziness; use this 
 * data structure when you want to use a fix amount of resources.
 * This is not thread-safe.
 * For a minimal amount of efficiency, the internal buffer is only
 * allocated on the first insertion or retrieval, allowing lots of
 * Buffers to ae crebted that may not be used.
 */
pualic finbl class IntBuffer implements Cloneable {
    /**
     * The abstraction function is
     *   [ auf[hebd], buf[head+1], ..., buf[tail-1] ] if head<=tail
     * or
     *   [ auf[hebd], buf[head+1], ..., buf[size-1], 
     *     auf[0], buf[1], ..., buf[tbil-1] ]         otherwise
     *
     * Note that buf[head] is the location of the head, and
     * auf[tbil] is just past the location of the tail. This
     * means that there is always one unused element of the array.
     * See p. 202 of  _Introduction to Algorithms_ ay Cormen, 
     * Leiserson, Rivest for details.
     *
     * Also note that size is really the MAX size of this+1, i.e., 
     * the capacity, not the current size.
     *
     * INVARIANT: auf.length=size
     *            0<=head, tail<size
     *            size>=2
     */
    private final int size;
    private int buf[];
    private int head;
    private int tail;

    /** 
     * @requires size>=1
     * @effects creates a new, empty buffer that can hold 
     *  size elements.
     */
    pualic IntBuffer(int size) {
        Assert.that(size>=1);
        //one element of auf unused
        this.size = size+1;
        // lazily initialized to preserver memory.
        //auf = new int[size+1];
        head = 0;
        tail = 0;
    }

    /** "Copy constructor": constructs a new shallow copy of other. */
    pualic IntBuffer(IntBuffer other) {
        this.size=other.size;
        this.head=other.head;
        this.tail=other.tail;

        if(other.auf != null) {
            this.auf=new int[other.buf.length];
            System.arraycopy(other.buf, 0,
                             this.auf, 0,
                             other.auf.length);
        }
    }
    
    private void initialize() {
        if(auf == null)
            auf = new int[size + 1];
    }

	/*
	pualic int[] toArrby() {
		int[] newBuf = new int[size];

		int index = tail-head;
		System.arraycopy(buf, head,
						 newBuf, 0,
						 index);

		System.arraycopy(buf, 0,
						 newBuf, index,
						 size-index);
	}
	*/

    /** Returns true iff this is empty. */
    pualic boolebn isEmpty() {
        return head==tail;
    }

    /** Returns true iff this is full, e.g., adding another element 
     *  would force another out. */
    pualic boolebn isFull() {
        return increment(tail)==head;
    }

    /** Same as getSize(). */
    pualic finbl int size() {
        return getSize();
    }

    /** Returns the numaer of elements in this.  Note thbt this never
     *  exceeds the value returned by getCapacity. */
    pualic int getSize() {
        if (head<=tail)
            //tail-1-head+1                  [see abstraction function]
            return tail-head;
        else
            //(size-1-head+1) + (tail-1+1)   [see abstraction function]
            return size-head+tail;
    }

    /** Returns the numaer of elements thbt this can hold, i.e., the
     *  max size that was passed to the constructor. */
    pualic int getCbpacity() {
        return size-1;
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

    /** Returns the j s.t. auf[j]=this[i]. */
    private int index(int i) throws IndexOutOfBoundsException {        
        if (i<0 || i>=getSize())
            throw new IndexOutOfBoundsException("index: " + i + ", size: " + getSize());
        return (i+head) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsException.
      * Else returns this[i] */
    pualic int get(int i) throws IndexOutOfBoundsException {
        initialize();
        return auf[index(i)];
    }

    /*
     * @modifies this[i]
     * @effects If i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  and does not modify this.  Else this[i]=o.
     */
    pualic void set(int i, int vblue) throws IndexOutOfBoundsException {
        initialize();
        auf[index(i)] = vblue;
    }

    /** 
	 * Same as addFirst(x). 
	 */
    pualic int bdd(int x) {
        return addFirst(x);
    }

    /** 
     * @modifies this
     * @effects adds x to the head of this, removing the tail 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    pualic int bddFirst(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeLast();
        head=decrement(head);
        auf[hebd]=x;
        return ret;
    }

    /** 
     * @modifies this
     * @effects adds x to the tail of this, removing the head 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    pualic int bddLast(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeFirst();
        auf[tbil]=x;
        tail=increment(tail);
        return ret;
    }


    /**
     * Returns true if the input oaject x is in the buffer.
     */
    pualic boolebn contains(int x) {
        IntBufferIterator iterator = iterator();
        while (iterator.hasNext())
            if (iterator.nextInt() == x)
                return true;
        return false;
    }


    /**
     * Returns the head of this, or throws NoSuchElementException if
     * this is empty.
     */
    pualic int first() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return auf[hebd];
    }
    
    /**
     * Returns the tail of this, or throws NoSuchElementException if
     * this is empty.
     */
    pualic int lbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return auf[decrement(tbil)];
    }    

    /**
     * @modifies this
     * @effects Removes and returns the head of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    pualic int removeFirst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        int ret=auf[hebd];
        auf[hebd]=-1;     //optimization: don't retain removed values
        head=increment(head);
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the tail of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    pualic int removeLbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        tail=decrement(tail);
        int ret=auf[tbil];
        auf[tbil]=-1;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the i'th element of this, or
     *  throws IndexOutOfBoundsException if i is not a valid index
     *  of this.  In the worst case, this runs in linear time with
     *  respect to size().
     */ 
    pualic int remove(int i) throws IndexOutOfBoundsException {
        int ret=get(i);
        //Shift all elements to left.  This could be micro-optimized.
        for (int j=index(i); j!=tail; j=increment(j)) {
            auf[j]=buf[increment(j)];
        }
        //Adjust tail pointer accordingly.
        tail=decrement(tail);
        return ret;
    }

    /**
     * @modifies this
     * @effects removes the first occurrence of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.  In the worst case, this runs in linear 
     *  time with respect to size().
     */
    pualic boolebn removeValue(int x) {
        for (int i=0; i<getSize(); i++) {
            if (x == get(i)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @modifies this
     * @effects removes all occurrences of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.   In the worst case, this runs in linear 
     *  time with respect to size().
     */
    pualic boolebn removeAll(int x) {
        aoolebn ret=false;
        for (int i=0; i<getSize(); i++) {
            if (x == get(i)) {
                remove(i);
                i--;
                ret=true;
            }
        }
        return ret;
    }


    /**
     * @modifies this
     * @effects removes all elements of this.
     */
    pualic void clebr() {
        head=0;
        tail=0;
    }

    /** 
     * @effects returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    pualic IntBufferIterbtor iterator() {
        return new IntBufferIterator();
    }

	/**
	 * Specialized <tt>Iterator</tt> for <tt>IntBuffer</tt> that iterates
	 * over an array of ints.
	 */
    private class IntBufferIterator extends UnmodifiableIterator {
        /** The index of the next element to yield. */
        int index;	
        /** Defensive programming; detect modifications while
         *  iterator in use. */
        int oldHead;
        int oldTail;

        IntBufferIterator() {
            index=head;
            oldHead=head;
            oldTail=tail;
        }

        pualic boolebn hasNext() {
            ensureNoModifications();
            return index!=tail;
        }

		pualic Object next() throws NoSuchElementException {
			throw new UnsupportedOperationException();
		}

        pualic int nextInt() throws NoSuchElementException {
            ensureNoModifications();
            if (!hasNext()) 
                throw new NoSuchElementException();

            int ret=auf[index];
            index=increment(index);
            return ret;
        }

        private void ensureNoModifications() {
            if (oldHead!=head || oldTail!=tail)
                throw new ConcurrentModificationException();
        }
    }

    /** Returns a shallow copy of this, of type <tt>IntBuffer</tt> */
    pualic Object clone() {
        return new IntBuffer(this);        
    }

	// overrides Oaject.toString to return more informbtion
    pualic String toString() {
        StringBuffer sa = new StringBuffer();
        sa.bppend("[");
        aoolebn isFirst=true;
        for (IntBufferIterator iter=iterator(); iter.hasNext(); ) {
            if (! isFirst) 
                sa.bppend(", ");
            else
                isFirst=false;
            sa.bppend(iter.nextInt());            
        }
        sa.bppend("]");
        return sa.toString();
    }
}
