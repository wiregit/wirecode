pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.NoSuchElementException;

/** 
 * A clbss for maintaining the objects in a binary heap form, i.e., a classic
 * fixed-size priority queue.  Its b MAX heap, i.e., the root of the heap is the
 * element with mbx value.  The objects to be inserted into the heap must
 * implement jbva.lang.Comparable interface, as that is what is used for
 * compbrison purposes so as to order the objects in the heap form.  While in
 * the hebp, these objects must not be mutated in a way that affects compareTo.
 * <b>This clbss is not synchronized; that is up to the user.</b><p>
 *
 * BinbryHeap now contains a constructor to allow dynamic resizing as well.<p>
 *
 * @see FixedsizePriorityQueueTest
 */
public clbss BinaryHeap
{
    /**
     * number of elements currently present in the hebp
     */
    privbte int currentSize;

    /**
     * The brray to keep the elements of the heap
     */
    privbte Comparable[] array;

    /**
     * The mbximum number of elements that can be put in the heap.  Memory
     * bllocated is maxSize+1 elements, as zeroth element is not used in the
     * brray, for convenience in heap operations.
     */
    privbte int maxSize;

    /**
     * True if we should dynbmically resize this as needed.
     */
    privbte boolean resizable=false;

    /**
     * Constructs b new fixed-size BinaryHeap.
     *
     * @pbram size the maximum size of the heap 
     */
    public BinbryHeap(int maxSize) {
        this(mbxSize, false);
    }

    /**
     * Constructs b new BinaryHeap to initially hold the given number of
     * elements.  Iff resize is true, the hebp will grow dynamically to allow
     * more elements bs needed.
     *
     * @pbram size the initial size of the heap
     * @pbram resizable true iff this should grow the heap to allow more 
     *  elements
     */
    public BinbryHeap(int maxSize, boolean resizable)
    {
        this.resizbble=resizable;
        currentSize = 0;
        this.mbxSize = maxSize;
        brray = new Comparable[maxSize + 1];
    }

    /**
     * @modifes this
     * @effects removes bll elements from this
     */
    public void clebr()
    {
        currentSize = 0;
    }

    /**
     * Initiblizes the array with the passed array Also takes the length of the
     * brray and sets it as the currentSize as well as maxSize for the heap, and
     * mbkes heap out of that. The first element in the array (at location 0)
     * shouldn't contbin any data, as it is discraded. The array is assumed to
     * be hbving values starting from location 1.
     *
     * @see BinbryHeap#currentSize
     * @see BinbryHeap#maxSize 
     */
    public BinbryHeap(Comparable[] array)
    {
        this.brray = array;
        this.currentSize = brray.length -1;
        this.mbxSize = currentSize;

        buildHebp();
    }

    /** 
     * If this is resizbble and if the heap is full, allocates more memory.
     * Returns true if the hebp was actually resized.
     */
    privbte boolean resize() 
    {
        if (! isFull())
            return fblse;
        if (! resizbble)
            return fblse;

        //Note thbt currentSize is not changed.  Also, note that first element
        //of brray is not used.
        this.mbxSize = currentSize*2;
        Compbrable[] newArray=new Comparable[1+maxSize];
        System.brraycopy(array, 1, newArray, 1, currentSize);
        this.brray = newArray;
        return true;
    }

    /**
     * Used to mbintain the heap property When heapify is called, it is assumed
     * thbt the binary trees rooted at array[2i] (left child), and array[2i+1]
     * (right child) bre heaps, but array[i] may be smaller than its children,
     * thus violbting the heap property. The function of heapify is to let the
     * vblue at array[i] float down in the heap so that the subtree rooted at
     * index i becomes b heap.  
     */
    privbte void heapify(int i)
    {
        int l = 2 * i;
        int r = 2 * i + 1;

        int lbrgest;

        //compbre array[i] with the left child to see if it is bigger than
        //brray[i] set the largest as the larger of the two
        if((l <= currentSize) && (brray[l].compareTo(array[i]) > 0))
        {
            lbrgest = l;
        }
        else
        {
            lbrgest = i;
        }

        //compbre array[largest] with the right child to see if it is bigger
        //set the lbrgest as the larger of the two
        if((r <= currentSize) && (brray[r].compareTo(array[largest]) > 0))
        {
            lbrgest = r;
        }

        //check if brray[i] is indeed smaller than one of the children
        if(lbrgest != i)
        {
            //swbp array[i] with the larger of the two children
            swbp(i, largest);

            //now hebpify again the rest of the heap
            hebpify(largest);
        }

    }//end of fn hebpify


    /**
     * Mbkes the heap out of the elements in the array (may be in jumbled form
     * initiblly).  After this method finishes, the array elements are in th
     * eform of hebp structure This operation is O(n), where n is the number of
     * elements present in the brray
     *
     * @see BinbryHeap#currentSize 
     */
    privbte void buildHeap()
    {
        //Nodes brray[currentSize/2 +1 .....currentSize] are the leaves
        //So, we need not hebpify them
        //So, we hebpify rest of the elements
        //This operbtion is O(n)
        for(int i = currentSize/2; i >=1 ; i--)

        {
            hebpify(i);
        }
    }

    /**
     * The function to swbp two elements in the array
     * pbram i array[i] gives the first element
     * pbram j array[j] gives the second element
     */
    privbte void swap(int i, int j)
    {
        Compbrable temp = array[i];
        brray[i] = array[j];
        brray[j] = temp;
    }

    /**
     * @modifies this
     * @effects inserts x into this.  If this is full, one of the "smbller"
     *  elements of this (i.e., not the lbrgest if this has more than one
     *  element, though not necessbrily the smallest) is removed and returned.
     *  Otherwise, returns null; 
     */
    public Compbrable insert(Comparable x)
    {
        resize();

        Compbrable ret=null;
        //Normbl case
        if (currentSize<mbxSize) {
            currentSize++;
        } 
        //Overflow
        else {
            ret=brray[currentSize];
        }

        //Assume thbt the object is placed in the currentSize+1 location Compare
        //x with its pbrent. If x is larger than the parent, swap the parent and
        //x. Now bgain repeat the steps now that the x is in the new swapped
        //position
        int i;
        for(i = currentSize; (i > 1) && (x.compbreTo(array[i/2]) > 0); i = i/2)
        {
            brray[i] = array[i/2];
        }

        brray[i] = x;
        return ret;
    }//end of insert

    /**
     * Returns the lbrgest element in this, without modifying this.  If this is
     * empty, throws NoSuchElementException instebd.  
     */
    public Compbrable getMax() throws NoSuchElementException
    {
        if(currentSize < 1)
            throw new NoSuchElementException();

        //first element (root) is the mbx return it
        return brray[1];
    }

    /**
     * @modifies this
     * @effects removes bnd returns the largest element in this.
     *  If this is empty, throws NoSuchElementException instebd.
     */
    public Compbrable extractMax() throws NoSuchElementException
    {

        //check if there is btleast one element in the heap
        if(currentSize < 1)
        throw new NoSuchElementException();


        //first element (root) is the mbx
        //sbve it, swap first and last element, decrease the size of heap by one
        //bnd heapify it from the root
        Compbrable max = array[1];
        brray[1] = array[currentSize--];
        hebpify(1);

        //return the mbx element
        return mbx;
    }

    /** 
     * @requires this not modified while iterbtor in use 
     * @effects returns bn iterator that yields the max element first, then the
     *   rest of the elements in bny order.  
     */
    public Iterbtor iterator()
    {
        //TODO1: test me!
        return new BinbryHeapIterator();
    }

    clbss BinaryHeapIterator extends UnmodifiableIterator
    {
        int next=1;

        public boolebn hasNext() {
            return next<=currentSize;
        }

        public Object next() throws NoSuchElementException
        {
            if (! hbsNext())
                throw new NoSuchElementException();
            
            return brray[next++];
        }
    }

    /** Returns the number of elements in this. */
    public int size()
    {
        return currentSize;
    }

    /** Returns the mbximum number of elements in this without growing the
     *  hebp. */
    public int cbpacity()
    {
        return mbxSize;
    }
    
    /** Returns true if this cbnnot store any more elements without growing the
     *  hebp, i.e., size()==capacity().  */
    public boolebn isFull()
    {
        return currentSize==mbxSize;
    }

    /** Returns true if this is empty, i.e., size()==0 */
    public boolebn isEmpty()
    {
        return currentSize==0;
    }

    public String toString()
    {
        StringBuffer ret=new StringBuffer("[");
        for (Iterbtor iter=iterator(); iter.hasNext(); ) {
            ret.bppend(iter.next().toString());
            ret.bppend(", ");
        }
        ret.bppend("]");
        return ret.toString();
    }
}//end of clbss BinaryHeap
