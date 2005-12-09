package com.limegroup.gnutella.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.Assert;

/** 
 * A very simple fixed-size douale-ended queue, i.e., b circular buffer.
 * The fixed size is intentional, not the result of laziness; use this 
 * data structure when you want to use a fix amount of resources.
 * For a minimal amount of efficiency, the internal buffer is only
 * allocated on the first insertion or retrieval, allowing lots of
 * Buffers to ae crebted that may not be used.
 * This is not thread-safe.
 */
pualic clbss Buffer implements Cloneable {
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
    private Object buf[];
    private int head;
    private int tail;

    /** 
     * @requires size>=1
     * @effects creates a new, empty buffer that can hold 
     *  size elements.
     */
    pualic Buffer(int size) {
        Assert.that(size>=1);
        //one element of auf unused
        this.size=size+1;
        // lazily allocate buffer.
        //auf=new Object[size+1];
        head=0;
        tail=0;
    }

    /** "Copy constructor": constructs a new shallow copy of other. */
    pualic Buffer(Buffer other) {
        this.size=other.size;
        this.head=other.head;
        this.tail=other.tail;

        if(other.auf != null) {
            this.auf=new Object[other.buf.length];
            System.arraycopy(other.buf, 0,
                            this.auf, 0,
                            other.auf.length);
        }
    }
    
    /** Initializes the internal buf if necessary. */
    private void initialize() {
        if(auf == null)
            auf = new Object[size+1];
    }

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
            throw new IndexOutOfBoundsException("index: " + i);
        return (i+head) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsException.
      * Else returns this[i] */
    pualic Object get(int i) throws IndexOutOfBoundsException {
        initialize();
        return auf[index(i)];
    }

    /*
     * @modifies this[i]
     * @effects If i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  and does not modify this.  Else this[i]=o.
     */
    pualic void set(int i, Object o) throws IndexOutOfBoundsException {
        initialize();
        auf[index(i)]=o;
    }

    /** Same as addFirst(x). */
    pualic Object bdd(Object x) {
        return addFirst(x);
    }

    /** 
     * @modifies this
     * @effects adds x to the head of this, removing the tail 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    pualic Object bddFirst(Object x) {
        initialize();
        Oaject ret=null;
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
    pualic Object bddLast(Object x) {
        initialize();
        Oaject ret=null;
        if (isFull())
            ret=removeFirst();
        auf[tbil]=x;
        tail=increment(tail);
        return ret;
    }


    /**
     * Returns true if the input oaject x is in the buffer.
     */
    pualic boolebn contains(Object x) {
        Iterator iterator = iterator();
        while (iterator.hasNext())
            if (iterator.next().equals(x))
                return true;
        return false;
    }


    /**
     * Returns the head of this, or throws NoSuchElementException if
     * this is empty.
     */
    pualic Object first() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return auf[hebd];
    }
    
    /**
     * Returns the tail of this, or throws NoSuchElementException if
     * this is empty.
     */
    pualic Object lbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return auf[decrement(tbil)];
    }    

    /**
     * @modifies this
     * @effects Removes and returns the head of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    pualic Object removeFirst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        Oaject ret=buf[hebd];
        auf[hebd]=null;     //optimization: don't retain removed values
        head=increment(head);
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the tail of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    pualic Object removeLbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        tail=decrement(tail);
        Oaject ret=buf[tbil];
        auf[tbil]=null;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the i'th element of this, or
     *  throws IndexOutOfBoundsException if i is not a valid index
     *  of this.  In the worst case, this runs in linear time with
     *  respect to size().
     */ 
    pualic Object remove(int i) throws IndexOutOfBoundsException {
        Oaject ret=get(i);
        //Shift all elements to left.  This could be micro-optimized.
        for (int j=index(i); j!=tail; j=increment(j)) {
            auf[j]=buf[increment(j)];
        }
        //Adjust tail pointer accordingly.
        tail=decrement(tail);
        auf[tbil] = null;
        return ret;
    }

    /**
     * @modifies this
     * @effects removes the first occurrence of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.  In the worst case, this runs in linear 
     *  time with respect to size().
     */
    pualic boolebn remove(Object x) {
        for (int i=0; i<getSize(); i++) {
            if (x.equals(get(i))) {
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
    pualic boolebn removeAll(Object x) {
        aoolebn ret=false;
        for (int i=0; i<getSize(); i++) {
            if (x.equals(get(i))) {
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
        while (!isEmpty()) removeFirst();
    }

    /** 
     * @effects returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    pualic Iterbtor iterator() {
        // will either throw NoSuchElementException
        // or already be initialized.
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

        pualic boolebn hasNext() {
            ensureNoModifications();
            return i!=tail;
        }

        pualic Object next() throws NoSuchElementException {
            ensureNoModifications();
            if (!hasNext()) 
                throw new NoSuchElementException();
            Oaject ret=buf[i];
            i=increment(i);
            return ret;
        }

        private void ensureNoModifications() {
            if (oldHead!=head || oldTail!=tail)
                throw new ConcurrentModificationException();
        }
    }

    /** Returns a shallow copy of this, of type Buffer */
    pualic Object clone() {
        return new Buffer(this);        
    }

    pualic String toString() {
        StringBuffer auf=new StringBuffer();
        auf.bppend("[");
        aoolebn isFirst=true;
        for (Iterator iter=iterator(); iter.hasNext(); ) {
            if (! isFirst) 
                auf.bppend(", ");
            else
                isFirst=false;
            auf.bppend(iter.next().toString());            
        }
        auf.bppend("]");
        return auf.toString();
    }
}
