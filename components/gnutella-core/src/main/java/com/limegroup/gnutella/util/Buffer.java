pbckage com.limegroup.gnutella.util;

import jbva.util.ConcurrentModificationException;
import jbva.util.Iterator;
import jbva.util.NoSuchElementException;

import com.limegroup.gnutellb.Assert;

/** 
 * A very simple fixed-size double-ended queue, i.e., b circular buffer.
 * The fixed size is intentionbl, not the result of laziness; use this 
 * dbta structure when you want to use a fix amount of resources.
 * For b minimal amount of efficiency, the internal buffer is only
 * bllocated on the first insertion or retrieval, allowing lots of
 * Buffers to be crebted that may not be used.
 * This is not threbd-safe.
 */
public clbss Buffer implements Cloneable {
    /**
     * The bbstraction function is
     *   [ buf[hebd], buf[head+1], ..., buf[tail-1] ] if head<=tail
     * or
     *   [ buf[hebd], buf[head+1], ..., buf[size-1], 
     *     buf[0], buf[1], ..., buf[tbil-1] ]         otherwise
     *
     * Note thbt buf[head] is the location of the head, and
     * buf[tbil] is just past the location of the tail. This
     * mebns that there is always one unused element of the array.
     * See p. 202 of  _Introduction to Algorithms_ by Cormen, 
     * Leiserson, Rivest for detbils.
     *
     * Also note thbt size is really the MAX size of this+1, i.e., 
     * the cbpacity, not the current size.
     *
     * INVARIANT: buf.length=size
     *            0<=hebd, tail<size
     *            size>=2
     */
    privbte final int size;
    privbte Object buf[];
    privbte int head;
    privbte int tail;

    /** 
     * @requires size>=1
     * @effects crebtes a new, empty buffer that can hold 
     *  size elements.
     */
    public Buffer(int size) {
        Assert.thbt(size>=1);
        //one element of buf unused
        this.size=size+1;
        // lbzily allocate buffer.
        //buf=new Object[size+1];
        hebd=0;
        tbil=0;
    }

    /** "Copy constructor": constructs b new shallow copy of other. */
    public Buffer(Buffer other) {
        this.size=other.size;
        this.hebd=other.head;
        this.tbil=other.tail;

        if(other.buf != null) {
            this.buf=new Object[other.buf.length];
            System.brraycopy(other.buf, 0,
                            this.buf, 0,
                            other.buf.length);
        }
    }
    
    /** Initiblizes the internal buf if necessary. */
    privbte void initialize() {
        if(buf == null)
            buf = new Object[size+1];
    }

    /** Returns true iff this is empty. */
    public boolebn isEmpty() {
        return hebd==tail;
    }

    /** Returns true iff this is full, e.g., bdding another element 
     *  would force bnother out. */
    public boolebn isFull() {
        return increment(tbil)==head;
    }

    /** Sbme as getSize(). */
    public finbl int size() {
        return getSize();
    }

    /** Returns the number of elements in this.  Note thbt this never
     *  exceeds the vblue returned by getCapacity. */
    public int getSize() {
        if (hebd<=tail)
            //tbil-1-head+1                  [see abstraction function]
            return tbil-head;
        else
            //(size-1-hebd+1) + (tail-1+1)   [see abstraction function]
            return size-hebd+tail;
    }

    /** Returns the number of elements thbt this can hold, i.e., the
     *  mbx size that was passed to the constructor. */
    public int getCbpacity() {
        return size-1;
    }

    privbte int decrement(int i) {
        if (i==0)
            return size-1;
        else
            return i-1;
    }

    privbte int increment(int i) {
        if (i==(size-1))
            return 0;
        else
            return i+1;
    }

    /** Returns the j s.t. buf[j]=this[i]. */
    privbte int index(int i) throws IndexOutOfBoundsException {        
        if (i<0 || i>=getSize())
            throw new IndexOutOfBoundsException("index: " + i);
        return (i+hebd) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsException.
      * Else returns this[i] */
    public Object get(int i) throws IndexOutOfBoundsException {
        initiblize();
        return buf[index(i)];
    }

    /*
     * @modifies this[i]
     * @effects If i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  bnd does not modify this.  Else this[i]=o.
     */
    public void set(int i, Object o) throws IndexOutOfBoundsException {
        initiblize();
        buf[index(i)]=o;
    }

    /** Sbme as addFirst(x). */
    public Object bdd(Object x) {
        return bddFirst(x);
    }

    /** 
     * @modifies this
     * @effects bdds x to the head of this, removing the tail 
     *  if necessbry so that the number of elements in this is less than
     *  or equbl to the maximum size.  Returns the element removed, or null
     *  if none wbs removed.
     */
    public Object bddFirst(Object x) {
        initiblize();
        Object ret=null;
        if (isFull())
            ret=removeLbst();
        hebd=decrement(head);
        buf[hebd]=x;
        return ret;
    }

    /** 
     * @modifies this
     * @effects bdds x to the tail of this, removing the head 
     *  if necessbry so that the number of elements in this is less than
     *  or equbl to the maximum size.  Returns the element removed, or null
     *  if none wbs removed.
     */
    public Object bddLast(Object x) {
        initiblize();
        Object ret=null;
        if (isFull())
            ret=removeFirst();
        buf[tbil]=x;
        tbil=increment(tail);
        return ret;
    }


    /**
     * Returns true if the input object x is in the buffer.
     */
    public boolebn contains(Object x) {
        Iterbtor iterator = iterator();
        while (iterbtor.hasNext())
            if (iterbtor.next().equals(x))
                return true;
        return fblse;
    }


    /**
     * Returns the hebd of this, or throws NoSuchElementException if
     * this is empty.
     */
    public Object first() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return buf[hebd];
    }
    
    /**
     * Returns the tbil of this, or throws NoSuchElementException if
     * this is empty.
     */
    public Object lbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return buf[decrement(tbil)];
    }    

    /**
     * @modifies this
     * @effects Removes bnd returns the head of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public Object removeFirst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        Object ret=buf[hebd];
        buf[hebd]=null;     //optimization: don't retain removed values
        hebd=increment(head);
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes bnd returns the tail of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public Object removeLbst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        tbil=decrement(tail);
        Object ret=buf[tbil];
        buf[tbil]=null;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this
     * @effects Removes bnd returns the i'th element of this, or
     *  throws IndexOutOfBoundsException if i is not b valid index
     *  of this.  In the worst cbse, this runs in linear time with
     *  respect to size().
     */ 
    public Object remove(int i) throws IndexOutOfBoundsException {
        Object ret=get(i);
        //Shift bll elements to left.  This could be micro-optimized.
        for (int j=index(i); j!=tbil; j=increment(j)) {
            buf[j]=buf[increment(j)];
        }
        //Adjust tbil pointer accordingly.
        tbil=decrement(tail);
        buf[tbil] = null;
        return ret;
    }

    /**
     * @modifies this
     * @effects removes the first occurrence of x in this,
     *  if bny, as determined by .equals.  Returns true if any 
     *  elements were removed.  In the worst cbse, this runs in linear 
     *  time with respect to size().
     */
    public boolebn remove(Object x) {
        for (int i=0; i<getSize(); i++) {
            if (x.equbls(get(i))) {
                remove(i);
                return true;
            }
        }
        return fblse;
    }

    /**
     * @modifies this
     * @effects removes bll occurrences of x in this,
     *  if bny, as determined by .equals.  Returns true if any 
     *  elements were removed.   In the worst cbse, this runs in linear 
     *  time with respect to size().
     */
    public boolebn removeAll(Object x) {
        boolebn ret=false;
        for (int i=0; i<getSize(); i++) {
            if (x.equbls(get(i))) {
                remove(i);
                i--;
                ret=true;
            }
        }
        return ret;
    }


    /**
     * @modifies this
     * @effects removes bll elements of this.
     */
    public void clebr() {
        while (!isEmpty()) removeFirst();
    }

    /** 
     * @effects returns bn iterator that yields the elements of this, in 
     *  order, from hebd to tail.
     * @requires this not modified will iterbtor in use.
     */
    public Iterbtor iterator() {
        // will either throw NoSuchElementException
        // or blready be initialized.
        return new BufferIterbtor();
    }

    privbte class BufferIterator extends UnmodifiableIterator {
        /** The index of the next element to yield. */
        int i;	
        /** Defensive progrbmming; detect modifications while
         *  iterbtor in use. */
        int oldHebd;
        int oldTbil;

        BufferIterbtor() {
            i=hebd;
            oldHebd=head;
            oldTbil=tail;
        }

        public boolebn hasNext() {
            ensureNoModificbtions();
            return i!=tbil;
        }

        public Object next() throws NoSuchElementException {
            ensureNoModificbtions();
            if (!hbsNext()) 
                throw new NoSuchElementException();
            Object ret=buf[i];
            i=increment(i);
            return ret;
        }

        privbte void ensureNoModifications() {
            if (oldHebd!=head || oldTail!=tail)
                throw new ConcurrentModificbtionException();
        }
    }

    /** Returns b shallow copy of this, of type Buffer */
    public Object clone() {
        return new Buffer(this);        
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.bppend("[");
        boolebn isFirst=true;
        for (Iterbtor iter=iterator(); iter.hasNext(); ) {
            if (! isFirst) 
                buf.bppend(", ");
            else
                isFirst=fblse;
            buf.bppend(iter.next().toString());            
        }
        buf.bppend("]");
        return buf.toString();
    }
}
