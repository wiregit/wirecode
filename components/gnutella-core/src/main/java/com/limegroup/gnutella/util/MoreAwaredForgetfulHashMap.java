package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;

/**
* This is similar in functionality to ForgetfulHashMap, but is more awared,
* as it returns the Key-Value Pair removed when there's no space to add a 
* new element, and also lets the user verify the size among other things
* @see ForgetfulHashMap for more details on implementation.
*/
public class MoreAwaredForgetfulHashMap extends HashMap {

    
    private Object[] queue;
    private int next;
    private int n;
    
    /** 
     * Create a new RouteTable that holds only the last "size" entries.
     *
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public MoreAwaredForgetfulHashMap(int size) {
        if (size < 1)
            throw new IllegalArgumentException();
        queue=new Object[size];
        next=0;
        n=size;
    }

    /**
     * @modifies this
     * @effects Maps the given key to the given value, ensuring that
     * (key, value) is the newest pair in this.  If adding the key
     * would make this contain more elements than the size given at
     * construction, removes the oldest key-value pair and returns it;
     * otherwise returns null.  
     */
    public Object put(Object key, Object value) {
        Object ret=super.put(key,value);
        if (ret!=null) {
            return null;
        }

        //declare an object to store the returnValuePair
        //in case we delete something from the map
        Map.Entry returnValue = null;
        
        //Purge oldest entry if we're all full, or if we'll become full
        //after adding this entry.  It's ok if queue[next] is no longer
        //in the map.
        Object oldestKey=queue[next];
        if (queue[next]!=null) {
            //get the old value
            returnValue = new KeyValue(oldestKey, super.get(oldestKey));
            super.remove(oldestKey);
        }
        //And make (key,value) the newest entry.  It is ok
        //if key is already in queue.
        queue[next]=key;
        next++;
        if (next>=n) {
            next=0;
        }
        return returnValue;
    }

    public void putAll(Map t) {
        Iterator iter=t.keySet().iterator();
        while (iter.hasNext()) {
            Object key=iter.next();
            put(key,t.get(key));
        }
        
    }

    /**
    * checks if the hash Map is full or not (ie if for adding new item we need
    * to remove some item
    * @return true if the map is full, false otherwise
    */
    public boolean isFull()
    {
        return queue[next] != null ;
    }

}

