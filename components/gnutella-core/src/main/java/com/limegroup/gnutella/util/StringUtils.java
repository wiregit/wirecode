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
     *  </pre> 
     */
    public static final boolean contains(String input, String pattern) {
        return contains(input, pattern, false);
    }

    /** Exactly like contains(input, pattern), but case is ignored if
     *  ignoreCase==true. */
    public static final boolean contains(String input, String pattern,
                                         boolean ignoreCase) {
        //More efficient algorithms are possible, e.g. a modified version of the
        //Rabin-Karp algorithm, but they are unlikely to be faster with such
        //short strings.  Also, some contant time factors could be shaved by
        //combining the second FOR loop below with the subset(..) call, but that
        //just isn't important.  The important thing is to avoid needless
        //allocations.

        final int n=pattern.length();
        //Where to resume searching after last wildcard, e.g., just past
        //the last match in input.
        int last=0;
        //For each token in pattern starting at i...
        for (int i=0; i<n; ) {
            //1. Find the smallest j>i s.t. pattern[j] is space, *, or +.
            char c=' ';
            int j=i;
            for ( ; j<n; j++) {
                char c2=pattern.charAt(j);
                if (c2==' ' || c2=='+' || c2=='*') {
                    c=c2;
                    break;
                }
            }

            //2. Match pattern[i..j-1] against input[last...].
            int k=subset(pattern, i, j,
                         input, last,
                         ignoreCase);
            if (k<0)
                return false;

            //3. Reset the starting search index if got ' ' or '+'.
            //Otherwise increment past the match in input.
            if (c==' ' || c=='+') 
                last=0;
            else if (c=='*')
                last=k+j-i;
            i=j+1;
        }
        return true;            
    }

    /** 
     * @requires TODO3: fill this in
     * @effects returns the the smallest i>=bigStart
     *  s.t. little[littleStart...littleStop-1] is a prefix of big[i...] 
     *  or -1 if no such i exists.  If ignoreCase==false, case doesn't matter
     *  when comparing characters.
     */
    private static final int subset(String little, int littleStart, int littleStop,
                                    String big, int bigStart,
                                    boolean ignoreCase) {
        //Equivalent to
        // return big.indexOf(little.substring(littleStart, littleStop), bigStart);
        //but without an allocation.
        //Note special case for ignoreCase below.
        
        if (ignoreCase) {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStart; i<n; i++) {
                //Check if little[littleStart...littleStop-1] matches with shift i
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    char c1=big.charAt(i+j); 
                    char c2=little.charAt(littleStart+j);
                    if (c1!=c2 && c1!=toOtherCase(c2))  //Ignore case. See below.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        } else {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStart; i<n; i++) {
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    char c1=big.charAt(i+j); 
                    char c2=little.charAt(littleStart+j);
                    if (c1!=c2)                        //Consider case.  See above.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        }
    }

    /** If c is a lower case ASCII character, returns Character.toUpperCase(c).
     *  Else if c is an upper case ASCII character, returns Character.toLowerCase(c),
     *  Else returns c.
     *  Note that this is <b>not internationalized</b>; but it is fast.
     */
    public static final char toOtherCase(char c) {
        int i=(int)c; 
        final int A=(int)'A';   //65
        final int Z=(int)'Z';   //90
        final int a=(int)'a';   //97
        final int z=(int)'z';   //122
        final int SHIFT=a-A;

        if (i<A)          //non alphabetic
            return c;
        else if (i<=Z)    //upper-case
            return (char)(i+SHIFT);
        else if (i<a)     //non alphabetic
            return c;
        else if (i<=z)    //lower-case
            return (char)(i-SHIFT);
        else              //non alphabetic
            return c;            
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
        //Case methods.  Test all boundary conditions.
        //See ASCII table for further justification.
        Assert.that(toOtherCase('\0')=='\0');
        Assert.that(toOtherCase('0')=='0');
        Assert.that(toOtherCase('@')=='@');
        Assert.that(toOtherCase('A')=='a');
        Assert.that(toOtherCase('H')=='h');
        Assert.that(toOtherCase('Z')=='z');
        Assert.that(toOtherCase('[')=='[');
        Assert.that(toOtherCase('`')=='`');
        Assert.that(toOtherCase('a')=='A');
        Assert.that(toOtherCase('h')=='H');
        Assert.that(toOtherCase('z')=='Z');
        Assert.that(toOtherCase('{')=='{');        

        //Wildcards
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

        //Spaces and wildcards
        Assert.that(StringUtils.contains("abcd", "x") == false);
        Assert.that(StringUtils.contains("abcd", "a b") == true);
        Assert.that(StringUtils.contains("abcd", "a x") == false);
        Assert.that(StringUtils.contains("abcd", "a c") == true);
        Assert.that(StringUtils.contains("abcd", "a+c") == true);
        Assert.that(StringUtils.contains("abcd", "d a") == true);
        Assert.that(StringUtils.contains("abcd", "a d+c") == true);
        Assert.that(StringUtils.contains("abcd", "a dc") == false);
        Assert.that(StringUtils.contains("abcd", "a b*c") == true);
        Assert.that(StringUtils.contains("abcd", "a c*b") == false);
        Assert.that(StringUtils.contains("abcd", " ab+") == true);
        Assert.that(StringUtils.contains("abcd", "+x+") == false);
        Assert.that(StringUtils.contains("abcde", "ab bcd") == true);
        Assert.that(StringUtils.contains("abcde", "ab bd") == false);
        Assert.that(StringUtils.contains("abcdefghj",
                                         "+*+*ab*d+def*g c ") == true);  

        //Cases 
        Assert.that(StringUtils.contains("aBcDd", "bCD", true) == true);
        Assert.that(StringUtils.contains("aBcDd", "bCD", false) == false);
        Assert.that(StringUtils.contains("....", "..", true) == true);
        Assert.that(StringUtils.contains("....", "..", false) == true);

        //Clip2 compatibility      
        Assert.that(StringUtils.contains("abcd", " ") == true);
        Assert.that(StringUtils.contains("abcd", "    ") == true);
                                         
        //Unit tests for split in HTTPUtil.
    }
    */
}
