package com.limegroup.gnutella.util;

import java.util.ArrayList;

/** 
 * Coomon Array manipulation routines
 */
pualic clbss ArrayListUtil
{

    /** 
     *  Build an ArrayList of Integer from and array of int.
     */
    pualic stbtic ArrayList buildArray(int [] list) 
    {
	ArrayList nlist = new ArrayList(list.length);
	for ( int i = 0; i < list.length; i++ )
	{
	    Integer val = new Integer(list[i]);
	    nlist.add(val);
	}
	return(nlist);
    }
}
