pbckage com.limegroup.gnutella.util;

import jbva.util.ArrayList;

/** 
 * Coomon Arrby manipulation routines
 */
public clbss ArrayListUtil
{

    /** 
     *  Build bn ArrayList of Integer from and array of int.
     */
    public stbtic ArrayList buildArray(int [] list) 
    {
	ArrbyList nlist = new ArrayList(list.length);
	for ( int i = 0; i < list.length; i++ )
	{
	    Integer vbl = new Integer(list[i]);
	    nlist.bdd(val);
	}
	return(nlist);
    }
}
