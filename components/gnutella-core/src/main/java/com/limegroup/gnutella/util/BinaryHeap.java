package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.NoSuchElementException;
import com.sun.java.util.collections.Iterator;

/** 
 * A class for maintaining the objects in a binary heap form, i.e., a classic
 * fixed-size priority queue.  Its a MAX heap, i.e., the root of the heap is the
 * element with max value.  The objects to be inserted into the heap must
 * implement java.lang.Comparable interface, as that is what is used for
 * comparison purposes so as to order the objects in the heap form.  While in
 * the heap, these objects must not be mutated in a way that affects compareTo.
 * <b>This class is not synchronized; that is up to the user.</b><p>
 *
 * BinaryHeap now contains a constructor to allow dynamic resizing as well.
 */
public class BinaryHeap implements Cloneable
{
    /**
     * number of elements currently present in the heap
     */
    private int currentSize;

    /**
     * The array to keep the elements of the heap
     */
    private Comparable[] array;

    /**
     * The maximum number of elements that can be put in the heap.  Memory
     * allocated is maxSize+1 elements, as zeroth element is not used in the
     * array, for convenience in heap operations.
     */
    private int maxSize;

    /**
     * True if we should dynamically resize this as needed.
     */
    private boolean resizable=false;

    /**
     * Constructs a new fixed-size BinaryHeap.
     *
     * @param size the maximum size of the heap 
     */
    public BinaryHeap(int maxSize) {
        this(maxSize, false);
    }

    /**
     * Constructs a new BinaryHeap to initially hold the given number of
     * elements.  Iff resize is true, the heap will grow dynamically to allow
     * more elements as needed.
     *
     * @param size the initial size of the heap
     * @param resizable true iff this should grow the heap to allow more 
     *  elements
     */
    public BinaryHeap(int maxSize, boolean resizable)
    {
        this.resizable=resizable;
        currentSize = 0;
        this.maxSize = maxSize;
        array = new Comparable[maxSize + 1];
    }

    /**
     * Copy constructor.  Creates a shallow copy of other.
     */
    public BinaryHeap(BinaryHeap other)
    {
        this.currentSize=other.currentSize;
        this.array=new Comparable[other.array.length];
        System.arraycopy(other.array, 0, this.array, 0, other.array.length);
        this.maxSize=other.maxSize;
        this.resizable=other.resizable;   
    }

    /**
     * @modifes this
     * @effects removes all elements from this
     */
    public void clear()
    {
        currentSize = 0;
    }

    /**
     * Initializes the array with the passed array Also takes the length of the
     * array and sets it as the currentSize as well as maxSize for the heap, and
     * makes heap out of that. The first element in the array (at location 0)
     * shouldn't contain any data, as it is discraded. The array is assumed to
     * be having values starting from location 1.
     *
     * @see BinaryHeap#currentSize
     * @see BinaryHeap#maxSize 
     */
    public BinaryHeap(Comparable[] array)
    {
        this.array = array;
        this.currentSize = array.length -1;
        this.maxSize = currentSize;

        buildHeap();
    }

    /** 
     * If this is resizable and if the heap is full, allocates more memory.
     * Returns true if the heap was actually resized.
     */
    private boolean resize() 
    {
        if (! isFull())
            return false;
        if (! resizable)
            return false;

        //Note that currentSize is not changed.  Also, note that first element
        //of array is not used.
        this.maxSize = currentSize*2;
        Comparable[] newArray=new Comparable[1+maxSize];
        System.arraycopy(array, 1, newArray, 1, currentSize);
        this.array = newArray;
        return true;
    }

    /**
     * Used to maintain the heap property When heapify is called, it is assumed
     * that the binary trees rooted at array[2i] (left child), and array[2i+1]
     * (right child) are heaps, but array[i] may be smaller than its children,
     * thus violating the heap property. The function of heapify is to let the
     * value at array[i] float down in the heap so that the subtree rooted at
     * index i becomes a heap.  
     */
    private void heapify(int i)
    {
        int l = 2 * i;
        int r = 2 * i + 1;

        int largest;

        //compare array[i] with the left child to see if it is bigger than
        //array[i] set the largest as the larger of the two
        if((l <= currentSize) && (array[l].compareTo(array[i]) > 0))
        {
            largest = l;
        }
        else
        {
            largest = i;
        }

        //compare array[largest] with the right child to see if it is bigger
        //set the largest as the larger of the two
        if((r <= currentSize) && (array[r].compareTo(array[largest]) > 0))
        {
            largest = r;
        }

        //check if array[i] is indeed smaller than one of the children
        if(largest != i)
        {
            //swap array[i] with the larger of the two children
            swap(i, largest);

            //now heapify again the rest of the heap
            heapify(largest);
        }

    }//end of fn heapify


    /**
     * Makes the heap out of the elements in the array (may be in jumbled form
     * initially).  After this method finishes, the array elements are in th
     * eform of heap structure This operation is O(n), where n is the number of
     * elements present in the array
     *
     * @see BinaryHeap#currentSize 
     */
    private void buildHeap()
    {
        //Nodes array[currentSize/2 +1 .....currentSize] are the leaves
        //So, we need not heapify them
        //So, we heapify rest of the elements
        //This operation is O(n)
        for(int i = currentSize/2; i >=1 ; i--)

        {
            heapify(i);
        }
    }

    /**
     * The function to swap two elements in the array
     * param i array[i] gives the first element
     * param j array[j] gives the second element
     */
    private void swap(int i, int j)
    {
        Comparable temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * @modifies this
     * @effects inserts x into this.  If this is full, one of the "smaller"
     *  elements of this (i.e., not the largest if this has more than one
     *  element, though not necessarily the smallest) is removed and returned.
     *  Otherwise, returns null; 
     */
    public Comparable insert(Comparable x)
    {
        resize();

        Comparable ret=null;
        //Normal case
        if (currentSize<maxSize) {
            currentSize++;
        } 
        //Overflow
        else {
            ret=array[currentSize];
        }

        //Assume that the object is placed in the currentSize+1 location Compare
        //x with its parent. If x is larger than the parent, swap the parent and
        //x. Now again repeat the steps now that the x is in the new swapped
        //position
        int i;
        for(i = currentSize; (i > 1) && (x.compareTo(array[i/2]) > 0); i = i/2)
        {
            array[i] = array[i/2];
        }

        array[i] = x;
        return ret;
    }//end of insert

    /**
     * Returns the largest element in this, without modifying this.  If this is
     * empty, throws NoSuchElementException instead.  
     */
    public Comparable getMax() throws NoSuchElementException
    {
        if(currentSize < 1)
            throw new NoSuchElementException();

        //first element (root) is the max return it
        return array[1];
    }

    /**
     * @modifies this
     * @effects removes and returns the largest element in this.
     *  If this is empty, throws NoSuchElementException instead.
     */
    public Comparable extractMax() throws NoSuchElementException
    {

        //check if there is atleast one element in the heap
        if(currentSize < 1)
        throw new NoSuchElementException();


        //first element (root) is the max
        //save it, swap first and last element, decrease the size of heap by one
        //and heapify it from the root
        Comparable max = array[1];
        array[1] = array[currentSize--];
        heapify(1);

        //return the max element
        return max;
    }

    /** 
     * @requires this not modified while iterator in use 
     * @effects returns an iterator that yields the max element first, then the
     *   rest of the elements in any order.  
     */
    public Iterator iterator()
    {
        //TODO1: test me!
        return new BinaryHeapIterator();
    }

    class BinaryHeapIterator extends UnmodifiableIterator
    {
        int next=1;

        public boolean hasNext() {
            return next<=currentSize;
        }

        public Object next() throws NoSuchElementException
        {
            if (! hasNext())
                throw new NoSuchElementException();
            
            return array[next++];
        }
    }

    /** Returns the number of elements in this. */
    public int size()
    {
        return currentSize;
    }

    /** Returns the maximum number of elements in this without growing the
     *  heap. */
    public int capacity()
    {
        return maxSize;
    }
    
    /** Returns true if this cannot store any more elements without growing the
     *  heap, i.e., size()==capacity().  */
    public boolean isFull()
    {
        return currentSize==maxSize;
    }

    /** Returns true if this is empty, i.e., size()==0 */
    public boolean isEmpty()
    {
        return currentSize==0;
    }

    public Object clone() 
    {
        return new BinaryHeap(this);
    }

    public String toString()
    {
        StringBuffer ret=new StringBuffer("[");
        for (Iterator iter=iterator(); iter.hasNext(); ) {
            ret.append(iter.next().toString());
            ret.append(", ");
        }
        ret.append("]");
        return ret.toString();
    }
    
    /** Unit test */
    /*
    public static void main(String args[]) 
    {
        BinaryHeap q=new BinaryHeap(4);
        MyInteger one=new MyInteger(1);
        MyInteger two=new MyInteger(2);
        MyInteger three=new MyInteger(3);
        MyInteger four=new MyInteger(4);
        MyInteger five=new MyInteger(5);

        Assert.that(q.isEmpty());
        Assert.that(q.capacity()==4);
        Assert.that(q.size()==0);

        q.insert(two);
        Assert.that(q.size()==1);
        q.insert(three);
        q.insert(four);
        q.insert(one);

        Assert.that(q.isFull());
        Assert.that(q.size()==4);

        Assert.that(q.getMax().equals(four));
        Assert.that(q.extractMax().equals(four));
        Assert.that(q.getMax().equals(three));
        Assert.that(q.extractMax().equals(three));
        q.insert(two);
        Assert.that(q.extractMax().equals(two));
        Assert.that(q.extractMax().equals(two));
        Assert.that(q.extractMax().equals(one));
        
        try {
            q.extractMax();
            Assert.that(false);
        } catch (NoSuchElementException e) { }

        //Iterator
        q=new BinaryHeap(2);
        Assert.that(! q.iterator().hasNext());
        q.insert(one);
        q.insert(two);
        Iterator iter=q.iterator();
        Assert.that(iter.hasNext());
        //first element is max
        Assert.that(iter.next().equals(two));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(one));
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) {
        } catch (Exception e) {
            Assert.that(false);
        }        

        //try inserting when overfilled
        q=new BinaryHeap(4);
        Assert.that(q.insert(one)==null);
        Assert.that(q.insert(four)==null);
        Assert.that(q.insert(three)==null);
        Assert.that(! q.isFull());
        Assert.that(q.insert(two)==null);
        Assert.that(q.isFull());
        System.out.println("The following tests are STRONGER than required"
                           +" the specification of insert.");
        System.out.println("(The spec does not say that the smallest"
                           +" element is removed on overflow.)");
        Assert.that(q.insert(five)!=null);
        Assert.that(q.insert(five)!=null);
        Assert.that(q.extractMax().equals(five));      
        Assert.that(q.extractMax().equals(five));      
        Assert.that(q.extractMax().equals(four));
        Assert.that(q.extractMax().equals(three));
        Assert.that(q.isEmpty());

        testResize();
        testClone();
    }
    
    static void testResize() {
        MyInteger one=new MyInteger(1);
        MyInteger two=new MyInteger(2);
        MyInteger three=new MyInteger(3);
        MyInteger four=new MyInteger(4);
        MyInteger five=new MyInteger(5);
        BinaryHeap q=new BinaryHeap(2, true);

        Assert.that(q.insert(one)==null);
        Assert.that(! q.isFull());
        Assert.that(q.insert(four)==null);
        Assert.that(q.capacity()==2);
        Assert.that(q.isFull());

        Assert.that(q.insert(three)==null);
        Assert.that(q.capacity()==4);
        Assert.that(! q.isFull());
        Assert.that(q.size()==3);
        Assert.that(q.array.length==5);
        Assert.that(q.array[0]==null);

        Assert.that(q.insert(two)==null);
        Assert.that(q.capacity()==4);
        Assert.that(q.isFull());
        Assert.that(q.size()==4);

        Assert.that(q.insert(five)==null);
        Assert.that(q.capacity()==8);
        Assert.that(! q.isFull());
        Assert.that(q.size()==5);

        Assert.that(q.extractMax()==five);
        Assert.that(q.extractMax()==four);
        Assert.that(q.extractMax()==three);
        Assert.that(q.extractMax()==two);;
        Assert.that(q.extractMax()==one);
        Assert.that(q.isEmpty());
        Assert.that(q.capacity()==8);
    }
    
    private static void testClone() {
        BinaryHeap h1=new BinaryHeap(100);
        h1.insert(new MyInteger(3));
        h1.insert(new MyInteger(5));
        h1.insert(new MyInteger(2));
        BinaryHeap h2=new BinaryHeap(h1);
        Assert.that(h1.size()==h2.size());
        Assert.that(h1.capacity()==h2.capacity());
        Assert.that(h1.extractMax()==h2.extractMax());  //not .equals!
        Assert.that(h1.extractMax()==h2.extractMax());  //not .equals!
        Assert.that(h1.extractMax()==h2.extractMax());  //not .equals!
        Assert.that(h2.isEmpty());
    }

    //For testing with Java 1.1.8
    static class MyInteger implements Comparable
    {
        private int val;
        public MyInteger(int val)
        {
            this.val=val;
        }

        public int compareTo(Object other)
        {
            int val2=((MyInteger)other).val;
            if (val<val2)
                return -1;
            else if (val>val2)
                return 1;
            else
                return 0;
        }

        public String toString()
        {
            return String.valueOf(val);
        }
    }
    */
}//end of class BinaryHeap
