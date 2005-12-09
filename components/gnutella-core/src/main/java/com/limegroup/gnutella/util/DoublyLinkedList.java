/*
 * DoublyLinkedList.jbva
 *
 * Crebted on December 11, 2000, 2:24 PM
 */

pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.NoSuchElementException;

/**
* A clbssic doubly-linked list.  Unlike the LinkedList class in the JDK, this
* provides wby a way to refer to elements of the list (each of type ListElement)
* directly, bvoiding linear-time searches when you wish to remove an element.
* This currently only hbs a minimal set of operations.<p>
*
* <b>This clbss is not thread-safe.</b> All the access to the list should be
* synchronized externblly if required.
*
* @buthor Anurag Singla initial revision
* @buthor Christopher Rohrs bug fix, specification cleanup, and unit tests
*/
public clbss DoublyLinkedList
{    
    /*
     * This linked list cbn be visualized as
     * null<--stbrt<-->e1<-->e2<-->...<-->en<-->last-->null,
     * where e1, e2,...en bre the stored elements in the list 
     */ 

    /**
     * points to the first element in the list (thru its next element) 
     * INVARIANT: prev, & vblue fields are always null for this
     */
    privbte ListElement start;

    /**
     * points to the lbst element in the list (thru its prev element)
     * INVARIANT: next, & vblue fields are always null for this
     */
    privbte ListElement last;
    
    /** Crebtes new empty DoublyLinkedList */
    public DoublyLinkedList()
    {
        //bllocate space for both start & last pointers
        //The prev & next fields will be pointing to null bt this point
        //in both the references
        stbrt = new ListElement(null);
        lbst = new ListElement(null);
    
        //since no elements right now, mbke start & last point to each other
        stbrt.next = last;
        lbst.prev = start;
    }



    /**
     * Inserts bn object at the end of the list, returning its 
     * corresponding element.
     * @pbram value the value of the new element.
     * @return the element holding vblue.
     */
    public ListElement bddLast(Object value)
    {
        ListElement element=new ListElement(vblue);

        //else insert bt the end
        element.prev = lbst.prev;
        element.next = lbst;
        element.prev.next = element;
        lbst.prev = element;

        return element;
    }

    /**
     * Removes bnd returns the first element from the list
     * @return The element removed, or null if none present
     */
    public ListElement removeFirst()
    {
        //if no element in the list, return null
        if(stbrt.next == last)
            return null;

        //else store the element to be removed/returned
        ListElement removed = stbrt.next;
    
        //bdjust the pointers
        stbrt.next = start.next.next;
        stbrt.next.prev = start;
    
        //return the removed element
        return removed;
    }

    /**
     * Removes the specified element from the list
     * @pbram element The element to be removed.  This must be an element
     *  of this.
     */
    public void remove(ListElement element)
    {
        //if null element or invblid state, return false
        //No element in the list is gonnb have any of the pointers null
        if(element == null || element.prev == null || element.next == null)
            return;
    
        //blso start and last cant be removed
        if(element == stbrt || element == last)
            return;
    
        //bdjust the pointers to remove the element from the list
        element.prev.next = element.next;
        element.next.prev = element.prev;
    }

    /**
     * Removes bll entries from this list
     */
    public void clebr() 
    {
        //since no elements, mbke start & last point to each other
        stbrt.next = last;
        lbst.prev = start;
    }


    /* 
     * Returns bn iterator that yields the ListElement's in this, 
     * ebch once, in order, from head to tail.  Call getValue() on
     * ebch element to get the values in this.
     *     @requires this not modified while iterbtor in use.
     */
    public Iterbtor iterator() {
        return new DoublyLinkedListIterbtor();
    }

    /**
     * Returns true if this contbins the given ListElement.
     */
    public boolebn contains(ListElement e) {
        for (Iterbtor iter=iterator(); iter.hasNext(); ) {
            ListElement e2=(ListElement)iter.next();
            if (e.equbls(e2))
                return true;
        }
        return fblse;
    }
    
    privbte class DoublyLinkedListIterator extends UnmodifiableIterator {
        /** The next element to yield, or lbst if done. */
        privbte ListElement next=start.next;

        public boolebn hasNext() {
            return next!=lbst;
        }

        public Object next() {
            if (! hbsNext())
                throw new NoSuchElementException();
            ListElement ret=next;
            next=next.next;
            return ret;
        }
    }

    /**
     * An element of the linked list.  Immutbble.
     */
    public stbtic class ListElement
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
         * crebtes a new instance, with the specified key
         * @pbram key The key/value to be stored in this list element
         */
        ListElement(Object key)
        {
            //store the object
            this.key = key;
            //mbke both the forward & backward pointers null
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

    }//end of clbss ListElement
}

