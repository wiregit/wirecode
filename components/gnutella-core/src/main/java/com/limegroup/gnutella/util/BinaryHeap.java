package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.limewire.collection.UnmodifiableIterator;


/** 
 * A class for maintaining the objects in a binary heap form, i.e., a classic
 * fixed-size priority queue.  Its a MAX heap, i.e., the root of the heap is the
 * element with max value.  The objects to be inserted into the heap must
 * implement java.lang.Comparable interface, as that is what is used for
 * comparison purposes so as to order the objects in the heap form.  While in
 * the heap, these objects must not be mutated in a way that affects compareTo.
 * <b>This class is not synchronized; that is up to the user.</b><p>
 *
 * BinaryHeap now contains a constructor to allow dynamic resizing as well.<p>
 *
 * @see FixedsizePriorityQueueTest
 */
public class BinaryHeap<T extends Comparable<T>> implements Iterable<T> {
    /**
     * number of elements currently present in the heap
     */
    private int currentSize;

    /**
     * The array to keep the elements of the heap
     */
    private T[] array;

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
    @SuppressWarnings("unchecked")
    public BinaryHeap(int maxSize, boolean resizable)
    {
        this.resizable=resizable;
        currentSize = 0;
        this.maxSize = maxSize;
        array = (T[])new Comparable[maxSize + 1];
    }

    /**
     * @modifes this
     * @effects removes all elements from this
     */
    public void clear()
    {
        while(currentSize > 0) 
        	array[currentSize--] = null;
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
    public BinaryHeap(T ... array)
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
    @SuppressWarnings("unchecked")
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
        this.array = (T[])newArray;
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
        T temp = array[i];
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
    public T insert(T x)
    {
        resize();

        T ret=null;
        //Normal case
        if (currentSize<maxSize) {
            currentSize++;
        } 
        //Overflow
        else {
        	if (array[currentSize].compareTo(x) > 0) {
        		return x;
        	}
        	else {
        		ret=array[currentSize];
        	}
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
    public T getMax() throws NoSuchElementException
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
    public T extractMax() throws NoSuchElementException
    {

        //check if there is atleast one element in the heap
        if(currentSize < 1)
            throw new NoSuchElementException();


        //first element (root) is the max
        //save it, swap first and last element, decrease the size of heap by one
        //and heapify it from the root
        T max = array[1];
        array[1] = array[currentSize];
        array[currentSize] = null; // allow GC to clean the object later on.
        currentSize--;
        heapify(1);

        //return the max element
        return max;
    }

    /** 
     * @requires this not modified while iterator in use 
     * @effects returns an iterator that yields the max element first, then the
     *   rest of the elements in any order.  
     */
    public Iterator<T> iterator()
    {
        //TODO1: test me!
        return new BinaryHeapIterator();
    }

    class BinaryHeapIterator extends UnmodifiableIterator<T> {
        int next=1;

        public boolean hasNext() {
            return next<=currentSize;
        }

        public T next() throws NoSuchElementException {
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

    public String toString()
    {
        StringBuilder ret=new StringBuilder("[");
        for(T t : this) {
            ret.append(t.toString()+ ", ");
        }
        ret.append("]");
        return ret.toString();
    }
}
