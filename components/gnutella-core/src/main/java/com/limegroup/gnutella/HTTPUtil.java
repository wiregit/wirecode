/**
 * auth: rsoule
 * file: HTTPUtil.java
 * desc: This file will have common utilities that might be useful
 *       for the HTTP client/server.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import com.sun.java.util.collections.*;

public class HTTPUtil {

    /** Returns the tokens of s delimited by the given delimeter,
     *  without returning the delimeter.  Examples:
     *  <pre>
     *    stringSplit("a//b/ c /","/")=={"a","b"," c "}
     *    stringSplit("a b", "/")=={"a b"}.
     *    stringSplit("///", "/")=={}.
     *  </pre>
     *
     * <b>Note that whitespace is preserved if it is not part of the delimeter.</b>
     * An older version of this trim()'ed each token of whitespace.
     */
    public static String[] stringSplit(String s, char delimeter) {
	return split(s, delimeter+"");
//             	                         /* given a character delimeter */ 
//  	s = s.trim();                    /* stringSplit will return an */
//  	                                 /* array of the Strings separated */
//  	int n = s.length();              /* by that delimeter */ 
	
//  	if (n==0) return new String[0];
	
//  	Vector buf=new Vector();         /* are vectors bad? inneficient? */
//  	                                 /* s[i] is the start of the word to */
//  	for (int i=0; i<n; ) {           /* add to buf */
//          	                         /* s[j] is just past the end */
//  	    if (s.charAt(i)== delimeter) i++;
	
//  	    int j=s.indexOf(delimeter,i+1);
	    
//  	    if (j==-1)
//  		j=n;

//  	    buf.add(s.substring(i,j));

//  	    for (i=j+1; j<n-1 ; ) {       /* Ignore white space after s[j]*/
//    		if (s.charAt(i)!=' ')
//    		    break;
//    		i++;
//    	    }			

//  	}

//  	String[] ret=new String[buf.size()];

//  	for (int i=0; i<ret.length; i++)
//  	    ret[i]=(String)buf.get(i);

//  	return ret;

    }

    public static String[] split(String str, String delimeter) {
	
	StringTokenizer tokenizer = new StringTokenizer(str, delimeter);

	Vector buf = new Vector();
	
	String s;
	
	while (true) {

	    try {
		s = tokenizer.nextToken();
	    }
	    catch (NoSuchElementException e) {
		break;
	    }
	    
	    buf.add(s);
	}
	
	int size = buf.size();
	
	String[] ret = new String[size];
	
	for(int i= 0; i < size; i++) 
	    ret[i] = (String)buf.get(i);
	
	return ret;
	
    } 

//      public static void main(String args[]) {
//  	String in;
//  	String[] expected;
//  	String[] result;

//  	in="a//b/ c /"; expected=new String[] {"a","b"," c "};
//  	result=stringSplit(in, '/');
//  	Assert.that(Arrays.equals(expected, result));
	
//  	in="a b";       expected=new String[] {"a b"};
//  	result=stringSplit(in, '/');
//  	Assert.that(Arrays.equals(expected, result));

//  	in="///";       expected=new String[] {};
//  	result=stringSplit(in, '/');
//  	Assert.that(Arrays.equals(expected, result));
//      }
}











