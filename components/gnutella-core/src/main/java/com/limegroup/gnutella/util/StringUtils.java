package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Constants;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;


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

    /**
     * Exactly like split(s, Character.toString(delimeter))
     */
    public static String[] split(String s, char delimeter) {
        //Character.toString only available in Java 1.4+
        return split(s, delimeter+"");
    }

    /** 
     *  Returns the tokens of s delimited by the given delimeter, without
     *  returning the delimeter.  Repeated sequences of delimeters are treated
     *  as one. Examples:
     *  <pre>
     *    split("a//b/ c /","/")=={"a","b"," c "}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={}.
     *  </pre>
     *
     * <b>Note that whitespace is preserved if it is not part of the delimeter.</b>
     * An older version of this trim()'ed each token of whitespace.  
     */
    public static String[] split(String s, String delimeters) {
        //Tokenize s based on delimeters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimeters);
        Vector buf = new Vector();        
        while (tokenizer.hasMoreTokens())
            buf.add(tokenizer.nextToken());

        //Copy from buffer to array.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = (String)buf.get(i);

        return ret;
    }

    /**
     * Exactly like splitNoCoalesce(s, Character.toString(delimeter))
     */
    public static String[] splitNoCoalesce(String s, char delimeter) {
        //Character.toString only available in Java 1.4+
        return splitNoCoalesce(s, delimeter+"");
    }

    /**
     * Similar to split(s, delimeters) except that subsequent delimeters are not
     * coalesced, so the returned array may contain empty strings.  If s starts
     * (ends) with a delimeter, the returned array starts (ends) with an empty
     * strings.  If s contains N delimeters, N+1 strings are always returned.
     * Examples:
     *
    *  <pre>
     *    split("a//b/ c /","/")=={"a","","b"," c ", ""}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={"","","",""}.
     *  </pre>
     *
     * @return an array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where 
     *  for all dI, dI.size()==1 && delimeters.indexOf(dI)>=0; and for
     *  all c in A[i], delimeters.indexOf(c)<0
     */
    public static String[] splitNoCoalesce(String s, String delimeters) {
        //Tokenize s based on delimeters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimeters, true);
        Vector buf = new Vector(); 
        //True if last token was a delimeter.  Initialized to true to force
        //an empty string if s starts with a delimeter.
        boolean gotDelimeter=true; 
        while (tokenizer.hasMoreTokens()) {
            String token=tokenizer.nextToken();
            //Is token a delimeter?
            if (token.length()==1 && delimeters.indexOf(token)>=0) {
                //If so, add blank only if last token was a delimeter.
                if (gotDelimeter)
                    buf.add("");
                gotDelimeter=true;
            } else {
                //If not, add "real" token.
                buf.add(token);
                gotDelimeter=false;
            }            
        }
        //Add trailing empty string UNLESS s is the empty string.
        if (gotDelimeter && !buf.isEmpty())
            buf.add("");

        //Copy from buffer to array.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = (String)buf.get(i);

        return ret;
    }

    /** Exactly the same as a.compareToIgnoreCase(b), which unfortunately
     *  doesn't exist in Java 1.1.8. */
    public static int compareIgnoreCase(String a, String b) {
        //Check out String.compareTo(String) for a description of the basic
        //algorithm.  The ignore case extension is trivial.
        for (int i=0; i<Math.min(a.length(), b.length()); i++) {
            char ac=Character.toLowerCase(a.charAt(i));
            char bc=Character.toLowerCase(b.charAt(i));
            int diff=ac-bc;
            if (diff!=0)
                return diff;
        }
        return a.length()-b.length();
    }

    /**
     * Returns the entries in the set in a string form, that can be used
     * in HTTP headers (among other purposes)
     * @param set The set whose entries are to be convereted to string form
     * @return the entries in the set in a string form. 
     * e.g. For a collection with entries ("a", "b"), the string returned will
     * be "a,b"
     */
    public static String getEntriesAsString(Collection collection){
        StringBuffer buffer = new StringBuffer();
        boolean isFirstEntry = true;
        //get the connected supernodes and pass them
        for(Iterator iter = collection.iterator();iter.hasNext();){
            //get the next entry
            Object entry = iter.next();
            //if the first entry that we are adding
            if(!isFirstEntry){
                //append separator to separate the entries
                buffer.append(Constants.ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstEntry = false;
            }
            //append the entry
            buffer.append(entry.toString());
        }
        return buffer.toString();
    }
    
    /**
     * Returns the entries passed in the string form as a Set fo strings
     * @param values The string representation of entries to be split.
     * The entries in the string are separated by Constants.ENTRY_SEPARATOR
     * @return the entries in the set form. 
     * e.g. For string "a,b", the Set returned will have 2 entries:
     * "a" & "b"
     */
    public static Set getSetofValues(String values){
        Set valueSet = new HashSet();
        //tokenize the values
        StringTokenizer st = new StringTokenizer(values,
            Constants.ENTRY_SEPARATOR);
        //add the values to the set
        while(st.hasMoreTokens()){
            valueSet.add(st.nextToken());
        }
        //return the set
        return valueSet;
    }
    
    //Unit tests: tests/com/limegroup/gnutella/util/StringUtils
}
