package com.limegroup.gnutella.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.Assert;

/** 
 * A very simple fixed-size double-ended queue, i.e., a circular buffer.
 * The fixed size is intentional, not the result of laziness; use this 
 * data structure when you want to use a fix amount of resources.
 * For a minimal amount of efficiency, the internal buffer is only
 * allocated on the first insertion or retrieval, allowing lots of
 * Buffers to be created that may not be used.
 * This is not thread-safe.
 */
public class Buffer implements Cloneable {
    /**
     * The abstraction function is
     *   [ buf[head], buf[head+1], ..., buf[tail-1] ] if head<=tail
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
     * Also note that size is really the MAX size of this+1, i.e., 
     * the capacity, not the current size.
     *
     * INVARIANT: buf.length=size
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
    public Buffer(int size) {
        Assert.that(size>=1);
        //one element of buf unused
        this.size=size+1;
        // lazily allocate buffer.
        //buf=new Object[size+1];
        head=0;
        tail=0;
    }

    /** "Copy constructor": constructs a new shallow copy of other. */
    public Buffer(Buffer other) {
        this.size=other.size;
        this.head=other.head;
        this.tail=other.tail;

        if(other.buf != null) {
            this.buf=new Object[other.buf.length];
            System.arraycopy(other.buf, 0,
                            this.buf, 0,
                            other.buf.length);
        }
    }
    
    /** Initializes the internal buf if necessary. */
    private void initialize() {
        if(buf == null)
            buf = new Object[size+1];
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

    /** Same as getSize(). */
    public final int size() {
        return getSize();
    }

    /** Returns the number of elements in this.  Note that this never
     *  exceeds the value returned by getCapacity. */
    public int getSize() {
        if (head<=tail)
            //tail-1-head+1                  [see abstraction function]
            return tail-head;
        else
            //(size-1-head+1) + (tail-1+1)   [see abstraction function]
            return size-head+tail;
    }

    /** Returns the number of elements that this can hold, i.e., the
     *  max size that was passed to the constructor. */
    public int getCapacity() {
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

    /** Returns the j s.t. buf[j]=this[i]. */
    private int index(int i) throws IndexOutOfBoundsException {        
        if (i<0 || i>=getSize())
            throw new IndexOutOfBoundsException("index: " + i);
        return (i+head) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsException.
      * Else returns this[i] */
    public Object get(int i) throws IndexOutOfBoundsException {
        initialize();
        return buf[index(i)];
    }

    /*
     * @modifies this[i]
     * @effects If i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  and does not modify this.  Else this[i]=o.
     */
    public void set(int i, Object o) throws IndexOutOfBoundsException {
        initialize();
        buf[index(i)]=o;
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
        initialize();
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
        initialize();
        Object ret=null;
        if (isFull())
            ret=removeFirst();
        buf[tail]=x;
        tail=increment(tail);
        return ret;
    }


    /**
     * Returns true if the input object x is in the buffer.
     */
    public boolean contains(Object x) {
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
        buf[head]=null;     //optimization: don't retain removed values
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
        Object ret=buf[tail];
        buf[tail]=null;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes and returns the i'th element of this, or
     *  throws IndexOutOfBoundsException if i is not a valid index
     *  of this.  In the worst case, this runs in linear time with
     *  respect to size().
     */ 
    public Object remove(int i) throws IndexOutOfBoundsException {
        Object ret=get(i);
        //Shift all elements to left.  This could be micro-optimized.
        for (int j=index(i); j!=tail; j=increment(j)) {
            buf[j]=buf[increment(j)];
        }
        //Adjust tail pointer accordingly.
        tail=decrement(tail);
        buf[tail] = null;
        return ret;
    }

    /**
     * @modifies this
     * @effects removes the first occurrence of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.  In the worst case, this runs in linear 
     *  time with respect to size().
     */
    public boolean remove(Object x) {
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
    public boolean removeAll(Object x) {
        boolean ret=false;
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
    public void clear() {
        while (!isEmpty()) removeFirst();
    }

    /** 
     * @effects returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    public Iterator iterator() {
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

    /** Returns a shallow copy of this, of type Buffer */
    public Object clone() {
        return new Buffer(this);        
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("[");
        boolean isFirst=true;
        for (Iterator iter=iterator(); iter.hasNext(); ) {
            if (! isFirst) 
                buf.append(", ");
            else
                isFirst=false;
            buf.append(iter.next().toString());            
        }
        buf.append("]");
        return buf.toString();
    }
}
