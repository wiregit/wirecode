
package com.limegroup.gnutella.util;

import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.NoSuchElementException;


/** A class for maintaining the objects in a binary heap form. Its a MAX heap,
 * ie the root of the heap is the element with max value.
 * The objects to be inserted into the heap must implement java.lang.Comparable
 * interface, as that is what is used for comparison purposes so as to order
 * the objects in the heap form
 * @author Anurag Singla
 */

public class BinaryHeap
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
     * The maximum number of elements that can be put in the heap
     */
    private int maxSize;

    /**
     * Constructor that initializes the size, and allocates sufficient memory
     * to keep the elements (memory allocated for size+1 elements, as zeroth element
     * is not used in the array, for convenience in heap operations)
     * @param size The maximum size of the heap
     */
    public BinaryHeap(int maxSize)
    {
        currentSize = 0;
        this.maxSize = maxSize;
        array = new Comparable[maxSize + 1];
    }

    /**
     * Initializes the array with the passed array
     * Also takes the length of the array and sets it as the currentSize as well as
     * maxSize for the heap, and makes heap out of that. The first element in the array
     * (at location 0) shouldn't contain any data, as it is discraded. The array is assumed
     * to be having values starting from location 1.
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
     * Used to maintain the heap property
     * When heapify is called, it is assumed that the binary trees rooted at
     * array[2i] (left child), and array[2i+1] (right child) are heaps,
     * but array[i] may be smaller than its children, thus violating the
     * heap property. The function of heapify is to let the value at array[i]
     * float down in the heap so that the subtree rooted at index i becomes a heap.
     */
    private void heapify(int i)

    {
        int l = 2 * i;
        int r = 2 * i + 1;

        int largest;

        //compare array[i] with the left child to see if it is bigger than array[i]
        //set the largest as the larger of the two
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
     * Makes the heap out of the elements in the array (may be in jumbled form initially).
     * After this method finishes, the array elements are in th eform of heap structure
     * This operation is O(n), where n is the number of elements present in the array
     * @see BinaryHeap#currentSize
     */
    public void buildHeap()
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
     * Inserts the given object into the heap
     * @param x The object to be inserted
     */
    public void insert(Comparable x)
    {
        int i;

        //Assume that the object is placed in the currentSize+1 location
        //Compare x with its parent. If x is larger than the parent, swap the parent
        //and x. Now again repeat the steps now that the x is in the new
        //swapped position

        for(i = ++currentSize; (i > 1) && (x.compareTo(array[i/2]) > 0); i = i/2)
        {
            array[i] = array[i/2];
        }

        array[i] = x;

    }//end of insert


    /**
     * Gives the maximum element in the heap
     * @return the max element
     * @throws NoSuchElementException
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


}//end of class BinaryHeap
