/*
 * DoublyLinkedList.java
 *
 * Created on December 11, 2000, 2:24 PM
 */

package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

/**
* Its a doubly linked list 
* <br>
* Note: All the access to the list should be synchronized externally(if
* required)
* @author  Anurag Singla
* @version
*/
public class DoublyLinkedList
{
    
/**
* This linked list can be visualized as
* null<--start<-->e1<-->e2<-->...<-->en<-->last-->null,
* where e1, e2,...en are the stored elements in the list 
*/ 

/**
* points to the first element in the list (thru its next element) 
* prev, & value fields are always null for this
*/
private ListElement start;

/**
* points to the last element in the list (thru its prev element)
* next, & value fields are always null for this
*/
private ListElement last;
    
/** Creates new DoublyLinkedList */
public DoublyLinkedList()
{
    //allocate space for both start & last pointers
    //The prev & next fields will be pointing to null at this point
    //in both the references
    start = new ListElement();
    last = new ListElement();
    
    //since no elements right now, make start & last point to each other
    start.next = last;
    last.prev = start;
}



/**
* inserts the passed element at the end of the list
* @param element The element to be inserted
*/
public void addLast(ListElement element)
{
    //if null element do nothing
    if(element == null)
        return;
    
    //else insert at the end
    element.prev = last.prev;
    element.next = last;
    element.prev.next = element;
    last.prev = element;
}

/**
* removes the first element from the list
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
    start.next.next.prev = start;
    
    //return the removed element
    return removed;
}

/**
* Removes the specified element from the list
* @param element The element to be removed
* @return true, if the element actually removed, false otherwise(may be 
* because the element was not present or was null)
*/
public boolean remove(ListElement element)
{
    //if null element or invalid state, return false
    //No element in the list is gonna have any of the pointers null
    if(element == null || element.prev == null || element.next == null)
        return false;
    
    //also start and last cant be removed
    if(element == start || element == last)
        return false;
    
    //adjust the pointers to remove the element from the list
    element.prev.next = element.next;
    element.next.prev = element.prev;
    
    return true;
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


/**
* Returns a new instance for the inner class
* @param key The key to be stored in the new instance
* @return the new instance of ListElement (inner class)
*/
public static DoublyLinkedList.ListElement getANewListElement(Object key)
{
    //create and return a new instance
    return new ListElement(key);
}

/**
* An element of the linked list
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
    * creates a new instance
    */
    public ListElement()
    {
        //make both the forward & backward pointers null
        prev = null;
        next = null;
    }
    
    /**
    * creates a new instance, with the specified key
    * @param key The key/value to be stored in this list element
    */
    public ListElement(Object key)
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