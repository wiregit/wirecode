/**
 * auth: rsoule
 * file: HTTPUtil.java
 * desc: This file will have common utilities that might be useful
 *       for the HTTP client/server.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.util.*;

public class HTTPUtil {

    public static String[] stringSplit(String s, char delimeter) {
           	                         /* given a character delimeter */ 
	s = s.trim();                    /* stringSplit will return an */
	                                 /* array of the Strings separated */
	int n = s.length();              /* by that delimeter */ 
	
	if (n==0) return new String[0];
	
	Vector buf=new Vector();         /* are vectors bad? inneficient? */
	                                 /* s[i] is the start of the word to */
	for (int i=0; i<n; ) {           /* add to buf */
        	                         /* s[j] is just past the end */
	    if (s.charAt(i)== delimeter) i++;
	
	    int j=s.indexOf(delimeter,i+1);
	    
	    if (j==-1)
		j=n;

	    buf.add(s.substring(i,j));

	    for (i=j+1; j<n ; ) {       /* Ignore white space after s[j]*/
		if (s.charAt(i)!=' ')
		    break;
		i++;
	    }			

	}

	String[] ret=new String[buf.size()];

	for (int i=0; i<ret.length; i++)
	    ret[i]=(String)buf.get(i);

	return ret;

    }

  

}











