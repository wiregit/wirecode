package com.limegroup.gnutella.util;

import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.Iterator;

/**
 * Provides utility methods like checking set intersection etc.
 * @author Anurag Singla
 */
public class Utilities 
{
    /**
     * Determines if two sets have non-void intersection
     * @param set1 First set
     * @param set2 Second set
     * @return true, if two sets have non-void intersection, false otherwise
     */
    public static boolean hasIntersection(Set set1, Set set2)
    {
        //Iterate over the first set, and check the value in the second set
        for(Iterator iterator = set1.iterator(); iterator.hasNext();)
        {
            //if second set contains the entry, return true
            if(set2.contains(iterator.next()))
                return true;
        }
        //if no match found, return true
        return false;
    }
 
}
