package com.limegroup.gnutella.util;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Comparator;

/** 
 * Coomon Array manipulation routines
 */
public class ArrayListUtil
{

    /** 
     *  Build an ArrayList of Integer from and array of int.
     */
    public static ArrayList buildArray(int [] list) 
    {
	ArrayList nlist = new ArrayList();
	for ( int i = 0; i < list.length; i++ )
	{
	    Integer val = new Integer(list[i]);
	    nlist.add(val);
	}
	return(nlist);
    }

    /** 
     *  Creates an Integer comparator 
     */
    public static Comparator integerComparator() 
    {

	class IntegerComparator implements Comparator 
	{
	    public int compare(Object a, Object b) {
		Integer aint=(Integer)a;
		Integer bint=(Integer)b;
		return (aint.intValue() - bint.intValue());
	    }

	    public boolean equals(Object o) {
		return false;
	    }
	}
	return new IntegerComparator();
    }
    

}
