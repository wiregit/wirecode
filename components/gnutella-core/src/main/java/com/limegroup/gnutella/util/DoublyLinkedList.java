/*
 * DoublyLinkedList.java
 *
 * Created on December 11, 2000, 2:24 PM
 */

package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
* A classic doubly-linked list.  Unlike the LinkedList class in the JDK, this
* provides way a way to refer to elements of the list (each of type ListElement)
* directly, avoiding linear-time searches when you wish to remove an element.
* This currently only has a minimal set of operations.<p>
*
* <b>This class is not thread-safe.</b> All the access to the list should be
* synchronized externally if required.
*
* @author Anurag Singla initial revision
* @author Christopher Rohrs bug fix, specification cleanup, and unit tests
*/
public class DoublyLinkedList
{    
    /*
     * This linked list can be visualized as
     * null<--start<-->e1<-->e2<-->...<-->en<-->last-->null,
     * where e1, e2,...en are the stored elements in the list 
     */ 

    /**
     * points to the first element in the list (thru its next element) 
     * INVARIANT: prev, & value fields are always null for this
     */
    private ListElement start;

    /**
     * points to the last element in the list (thru its prev element)
     * INVARIANT: next, & value fields are always null for this
     */
    private ListElement last;
    
    /** Creates new empty DoublyLinkedList */
    public DoublyLinkedList()
    {
        //allocate space for both start & last pointers
        //The prev & next fields will be pointing to null at this point
        //in both the references
        start = new ListElement(null);
        last = new ListElement(null);
    
        //since no elements right now, make start & last point to each other
        start.next = last;
        last.prev = start;
    }



    /**
     * Inserts an object at the end of the list, returning its 
     * corresponding element.
     * @param value the value of the new element.
     * @return the element holding value.
     */
    public ListElement addLast(Object value)
    {
        ListElement element=new ListElement(value);

        //else insert at the end
        element.prev = last.prev;
        element.next = last;
        element.prev.next = element;
        last.prev = element;

        return element;
    }

    /**
     * Removes and returns the first element from the list
     * @return The element removed, or null if none present
     */
    public ListElement removeFirst()
    {
        //if no element in the list, return null
        if(start.next == last)
            return null;

        //else store the element to be removed/returned
        ListElement removed = start.next;
    
        //adjust the pointers
        start.next = start.next.next;
        start.next.prev = start;
    
        //return the removed element
        return removed;
    }

    /**
     * Removes the specified element from the list
     * @param element The element to be removed.  This must be an element
     *  of this.
     */
    public void remove(ListElement element)
    {
        //if null element or invalid state, return false
        //No element in the list is gonna have any of the pointers null
        if(element == null || element.prev == null || element.next == null)
            return;
    
        //also start and last cant be removed
        if(element == start || element == last)
            return;
    
        //adjust the pointers to remove the element from the list
        element.prev.next = element.next;
        element.next.prev = element.prev;
    }

    /**
     * Removes all entries from this list
     */
    public void clear() 
    {
        //since no elements, make start & last point to each other
        start.next = last;
        last.prev = start;
    }


    /* 
     * Returns an iterator that yields the ListElement's in this, 
     * each once, in order, from head to tail.  Call getValue() on
     * each element to get the values in this.
     *     @requires this not modified while iterator in use.
     */
    public Iterator iterator() {
        return new DoublyLinkedListIterator();
    }

    /**
     * Returns true if this contains the given ListElement.
     */
    public boolean contains(ListElement e) {
        for (Iterator iter=iterator(); iter.hasNext(); ) {
            ListElement e2=(ListElement)iter.next();
            if (e.equals(e2))
                return true;
        }
        return false;
    }
    
    private class DoublyLinkedListIterator extends UnmodifiableIterator {
        /** The next element to yield, or last if done. */
        private ListElement next=start.next;

        public boolean hasNext() {
            return next!=last;
        }

        public Object next() {
            if (! hasNext())
                throw new NoSuchElementException();
            ListElement ret=next;
            next=next.next;
            return ret;
        }
    }

    /**
     * An element of the linked list.  Immutable.
     */
    public static class ListElement
    {
        /**
         * The key/object it stores
         */
        Object key;
    
        /**
         * Refernce to the previous element in the list
         */
        ListElement prev;
    
        /**
         * Refernce to the next element in the list
         */
        ListElement next;
    
        /**
         * creates a new instance, with the specified key
         * @param key The key/value to be stored in this list element
         */
        ListElement(Object key)
        {
            //store the object
            this.key = key;
            //make both the forward & backward pointers null
            prev = null;
            next = null;
        }
    
        /**
         * returns the key stored in this element
         * @return the key stored in this element
         */
        public Object getKey()
        {
            return key;
        }

    }//end of class ListElement
}

