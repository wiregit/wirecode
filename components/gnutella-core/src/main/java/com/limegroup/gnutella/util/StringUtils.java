package com.limegroup.gnutella.util;

import com.limegroup.gnutella.HTTPUtil;
import com.limegroup.gnutella.Assert;

/** Various static routines for manipulating strings.*/
public class StringUtils {
    
    /** Returns true if input contains the given pattern, which may contain the
     *  wildcard character '*'.  TODO: need more formal definition.  Examples:
     *
     *  <pre>
     *  StringUtils.contains("", "") ==> true
     *  StringUtils.contains("abc", "") ==> true
     *  StringUtils.contains("abc", "b") ==> true
     *  StringUtils.contains("abc", "d") ==> false
     *  StringUtils.contains("abcd", "a*d") ==> true
     *  StringUtils.contains("abcd", "*a**d*") ==> true
     *  StringUtils.contains("abcd", "d*a") ==> false
     *  </pre> */
    public static boolean contains(String input, String pattern) {
        //TODO2: more efficient algorithms are possible that use fewer
        //allocations, e.g. a modified version of the Rabin-Karp algorithm.
        String words[]=split(pattern, '*');
        int fromIndex=0;
        for (int i=0; i<words.length; i++) {
            String word=words[i];
            //Try to find word i after word i+1
            int j=input.indexOf(word, fromIndex);
            if (j==-1)
                return false;
            fromIndex=j+word.length();
        }
        return true;
    }


    /** Returns the tokens of s delimited by the given delimeter,
     *  without returning the delimeter.  Examples:
     *  <pre>
     *    split("a//b/ c /","/")=={"a","b"," c "}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={}.
     *  </pre>
     *
     * <b>Note that whitespace is preserved if it is not part of the delimeter.</b>
     * An older version of this trim()'ed each token of whitespace.
     */
    public static String[] split(String s, char delimeter) {
        return HTTPUtil.stringSplit(s, delimeter);
    }

    /** Exactly like split(String,char), except ANY of the delimeters in
     *  "delimeters" can be used to split s. */
    public static String[] split(String s, String delimeters) {
        return HTTPUtil.split(s, delimeters);
    }

    /*
    public static void main(String args[]) {
        Assert.that(StringUtils.contains("", "") == true);
        Assert.that(StringUtils.contains("abc", "") == true);
        Assert.that(StringUtils.contains("abc", "b") == true);
        Assert.that(StringUtils.contains("abc", "d") == false);
        Assert.that(StringUtils.contains("abcd", "a*d") == true);
        Assert.that(StringUtils.contains("abcd", "*a**d*") == true);
        Assert.that(StringUtils.contains("abcd", "d*a") == false);
        Assert.that(StringUtils.contains("abcd", "*.*") == false);
        Assert.that(StringUtils.contains("abc.d", "*.*") == true);
        Assert.that(StringUtils.contains("abc.", "*.*") == true);
        
        //Unit tests for split in HTTPUtil.
    }
    */   
}
