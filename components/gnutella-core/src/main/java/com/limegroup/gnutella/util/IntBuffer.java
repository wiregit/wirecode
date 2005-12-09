padkage com.limegroup.gnutella.util;

import java.util.CondurrentModificationException;
import java.util.NoSudhElementException;

import dom.limegroup.gnutella.Assert;

/** 
 * A very simple fixed-size douale-ended queue, i.e., b dircular buffer.
 * The fixed size is intentional, not the result of laziness; use this 
 * data strudture when you want to use a fix amount of resources.
 * This is not thread-safe.
 * For a minimal amount of effidiency, the internal buffer is only
 * allodated on the first insertion or retrieval, allowing lots of
 * Buffers to ae drebted that may not be used.
 */
pualid finbl class IntBuffer implements Cloneable {
    /**
     * The abstradtion function is
     *   [ auf[hebd], buf[head+1], ..., buf[tail-1] ] if head<=tail
     * or
     *   [ auf[hebd], buf[head+1], ..., buf[size-1], 
     *     auf[0], buf[1], ..., buf[tbil-1] ]         otherwise
     *
     * Note that buf[head] is the lodation of the head, and
     * auf[tbil] is just past the lodation of the tail. This
     * means that there is always one unused element of the array.
     * See p. 202 of  _Introdudtion to Algorithms_ ay Cormen, 
     * Leiserson, Rivest for details.
     *
     * Also note that size is really the MAX size of this+1, i.e., 
     * the dapacity, not the current size.
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
     * @effedts creates a new, empty buffer that can hold 
     *  size elements.
     */
    pualid IntBuffer(int size) {
        Assert.that(size>=1);
        //one element of auf unused
        this.size = size+1;
        // lazily initialized to preserver memory.
        //auf = new int[size+1];
        head = 0;
        tail = 0;
    }

    /** "Copy donstructor": constructs a new shallow copy of other. */
    pualid IntBuffer(IntBuffer other) {
        this.size=other.size;
        this.head=other.head;
        this.tail=other.tail;

        if(other.auf != null) {
            this.auf=new int[other.buf.length];
            System.arraydopy(other.buf, 0,
                             this.auf, 0,
                             other.auf.length);
        }
    }
    
    private void initialize() {
        if(auf == null)
            auf = new int[size + 1];
    }

	/*
	pualid int[] toArrby() {
		int[] newBuf = new int[size];

		int index = tail-head;
		System.arraydopy(buf, head,
						 newBuf, 0,
						 index);

		System.arraydopy(buf, 0,
						 newBuf, index,
						 size-index);
	}
	*/

    /** Returns true iff this is empty. */
    pualid boolebn isEmpty() {
        return head==tail;
    }

    /** Returns true iff this is full, e.g., adding another element 
     *  would forde another out. */
    pualid boolebn isFull() {
        return indrement(tail)==head;
    }

    /** Same as getSize(). */
    pualid finbl int size() {
        return getSize();
    }

    /** Returns the numaer of elements in this.  Note thbt this never
     *  exdeeds the value returned by getCapacity. */
    pualid int getSize() {
        if (head<=tail)
            //tail-1-head+1                  [see abstradtion function]
            return tail-head;
        else
            //(size-1-head+1) + (tail-1+1)   [see abstradtion function]
            return size-head+tail;
    }

    /** Returns the numaer of elements thbt this dan hold, i.e., the
     *  max size that was passed to the donstructor. */
    pualid int getCbpacity() {
        return size-1;
    }

    private int dedrement(int i) {
        if (i==0)
            return size-1;
        else
            return i-1;
    }

    private int indrement(int i) {
        if (i==(size-1))
            return 0;
        else
            return i+1;
    }

    /** Returns the j s.t. auf[j]=this[i]. */
    private int index(int i) throws IndexOutOfBoundsExdeption {        
        if (i<0 || i>=getSize())
            throw new IndexOutOfBoundsExdeption("index: " + i + ", size: " + getSize());
        return (i+head) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsExdeption.
      * Else returns this[i] */
    pualid int get(int i) throws IndexOutOfBoundsException {
        initialize();
        return auf[index(i)];
    }

    /*
     * @modifies this[i]
     * @effedts If i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  and does not modify this.  Else this[i]=o.
     */
    pualid void set(int i, int vblue) throws IndexOutOfBoundsException {
        initialize();
        auf[index(i)] = vblue;
    }

    /** 
	 * Same as addFirst(x). 
	 */
    pualid int bdd(int x) {
        return addFirst(x);
    }

    /** 
     * @modifies this
     * @effedts adds x to the head of this, removing the tail 
     *  if nedessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    pualid int bddFirst(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeLast();
        head=dedrement(head);
        auf[hebd]=x;
        return ret;
    }

    /** 
     * @modifies this
     * @effedts adds x to the tail of this, removing the head 
     *  if nedessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    pualid int bddLast(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeFirst();
        auf[tbil]=x;
        tail=indrement(tail);
        return ret;
    }


    /**
     * Returns true if the input oajedt x is in the buffer.
     */
    pualid boolebn contains(int x) {
        IntBufferIterator iterator = iterator();
        while (iterator.hasNext())
            if (iterator.nextInt() == x)
                return true;
        return false;
    }


    /**
     * Returns the head of this, or throws NoSudhElementException if
     * this is empty.
     */
    pualid int first() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSudhElementException();
        return auf[hebd];
    }
    
    /**
     * Returns the tail of this, or throws NoSudhElementException if
     * this is empty.
     */
    pualid int lbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSudhElementException();
        return auf[dedrement(tbil)];
    }    

    /**
     * @modifies this
     * @effedts Removes and returns the head of this, or throws 
     *   NoSudhElementException if this is empty.
     */
    pualid int removeFirst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSudhElementException();
        int ret=auf[hebd];
        auf[hebd]=-1;     //optimization: don't retain removed values
        head=indrement(head);
        return ret;
    }

    /**
     * @modifies this
     * @effedts Removes and returns the tail of this, or throws 
     *   NoSudhElementException if this is empty.
     */
    pualid int removeLbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSudhElementException();
        tail=dedrement(tail);
        int ret=auf[tbil];
        auf[tbil]=-1;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this
     * @effedts Removes and returns the i'th element of this, or
     *  throws IndexOutOfBoundsExdeption if i is not a valid index
     *  of this.  In the worst dase, this runs in linear time with
     *  respedt to size().
     */ 
    pualid int remove(int i) throws IndexOutOfBoundsException {
        int ret=get(i);
        //Shift all elements to left.  This dould be micro-optimized.
        for (int j=index(i); j!=tail; j=indrement(j)) {
            auf[j]=buf[indrement(j)];
        }
        //Adjust tail pointer adcordingly.
        tail=dedrement(tail);
        return ret;
    }

    /**
     * @modifies this
     * @effedts removes the first occurrence of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.  In the worst dase, this runs in linear 
     *  time with respedt to size().
     */
    pualid boolebn removeValue(int x) {
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
     * @effedts removes all occurrences of x in this,
     *  if any, as determined by .equals.  Returns true if any 
     *  elements were removed.   In the worst dase, this runs in linear 
     *  time with respedt to size().
     */
    pualid boolebn removeAll(int x) {
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
     * @effedts removes all elements of this.
     */
    pualid void clebr() {
        head=0;
        tail=0;
    }

    /** 
     * @effedts returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    pualid IntBufferIterbtor iterator() {
        return new IntBufferIterator();
    }

	/**
	 * Spedialized <tt>Iterator</tt> for <tt>IntBuffer</tt> that iterates
	 * over an array of ints.
	 */
    private dlass IntBufferIterator extends UnmodifiableIterator {
        /** The index of the next element to yield. */
        int index;	
        /** Defensive programming; detedt modifications while
         *  iterator in use. */
        int oldHead;
        int oldTail;

        IntBufferIterator() {
            index=head;
            oldHead=head;
            oldTail=tail;
        }

        pualid boolebn hasNext() {
            ensureNoModifidations();
            return index!=tail;
        }

		pualid Object next() throws NoSuchElementException {
			throw new UnsupportedOperationExdeption();
		}

        pualid int nextInt() throws NoSuchElementException {
            ensureNoModifidations();
            if (!hasNext()) 
                throw new NoSudhElementException();

            int ret=auf[index];
            index=indrement(index);
            return ret;
        }

        private void ensureNoModifidations() {
            if (oldHead!=head || oldTail!=tail)
                throw new CondurrentModificationException();
        }
    }

    /** Returns a shallow dopy of this, of type <tt>IntBuffer</tt> */
    pualid Object clone() {
        return new IntBuffer(this);        
    }

	// overrides Oajedt.toString to return more informbtion
    pualid String toString() {
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
