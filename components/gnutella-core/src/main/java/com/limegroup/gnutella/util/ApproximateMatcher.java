package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;

/**
 * An approximate string matcher.  Two strings are considered
 * "approximately equal" if one can be transformed into the other
 * through some series of inserts, deletes, and substitutions.
 */
public class ApproximateMatcher
{
    /** INVARIANT: |s1|<=|s2| */
    private String s1;
    private String s2;
    private boolean ignoreCase=false;
    private boolean compareBackwards=false;
    private boolean specialUnderscores=false;
    
    /** For avoiding allocations.  This can only be used by one thread at a
     *  time.  INVARIANT: buffer!=null => buffer is a bufSize by bufSize array.
     */
    private static volatile int[][] buffer;
    private static volatile int bufSize=0;
    

    /** 
     * Creates a new matcher to match s1 against s2..  The default
     * matcher is case-sensitive, compares forwards, and treats
     * underscores normally.
     */
    public ApproximateMatcher( String s1, String s2) {
        if (s1.length()<=s2.length()) {
            this.s1=s1;
            this.s2=s2;
        } else {
            this.s2=s1;
            this.s1=s2;
        }
    }

    /**
     * Sets whether this should ignore case.  If ignoreCase is true, case will
     * be ignored in matching; otherwise it will not.  Case comparisons are only
     * defined for Latin characters.
     *
     * @modifies this 
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase=ignoreCase;
    }

    /**
     * Sets whether this should compare strings backwards.  This never affects
     * the returned value of match or matches, but it may affect the efficiency
     * of the matches method.  Specifically, if two strings are more likely to
     * differ at the tail than the head, you should set this true.
     *
     * @modifies this 
     */
    public void setCompareBackwards(boolean compareBackwards) {
        this.compareBackwards=compareBackwards;
    }

    /**
     * If true, underscores and spaces are considered interchangeable when
     * comparing.
     *
     * @modifies this 
     */
    public void setSpecialUnderscores(boolean specialUnderscores) {
        this.specialUnderscores=specialUnderscores;
    }

    /**
     * Allocates a buffer for use in subsequent calls to match and matches.  The
     * strings to be matched must be less than length size for the buffer to be
     * used.  This method is provided solely for efficiency reasons if you must
     * match lots of strings.  <b>Only one thread may be using an
     * ApproximateMatcher after a call to setBuffer.</b> The buffer can be
     * released by releaseBuffer.
     *
     * @modifies all ApproximateMatcher classes, though this isn't observable
     * @requires only one ApproximateMatcher used at a time until releaseBuffer.
     */
    public static void setBuffer(int size) {
        bufSize=size+1;
        buffer=new int[bufSize][bufSize]; //need "margins" of 1 on each side
    }

    /** 
     * Releases any storage allocated by setBuffer.
     *
     * @modifies all ApproximateMatcher classes, though this isn't observable
     */
    public static void releaseBuffer() {
        buffer=null;
    }

    /*
     * Returns the edit distance between s1 and s2.  That is, returns the number
     * of insertions, deletions, or replacements necessary to transform s1 into
     * s2.  A value of 0 means the strings match exactly.  Case is ignored if
     * ignoreCase==true.  
     */
    public final int match() {
        //Let m=s1.length(), n=s2.length(), and k be the edit difference between
        //s1 and s2.  It's possible to reduce the time from O(mn) time to O(kn)
        //time by repeated iterations of the the k-difference algorithm.  But
        //this is a bit complicated.
        return matchInternal(Integer.MAX_VALUE);
    }

    /**
     * Returns true if the edit distance between s1 and s2 is less than or equal
     * to maxOps.  That is, returns true if s1 can be transformed into s2
     * through no more than maxOps insertions, deletions, or replacements.  Case
     * is ignored if ignoreCase==true.  This method is generally more efficient
     * than match(..) if you only care whether two strings approximately match.
     */
    public final boolean matches(int maxOps) {
        return matchInternal(maxOps)<=maxOps;
    }

    /** 
     * Returns true if s1 can be transformed into s2 without changing more than
     * the given fraction of s1's letters.  For example, matches(1.) is the same
     * as an exact comparison, while matches(0.) always returns true as long as
     * |s1|>=|s2|.  matches(0.9) means "s1 and s2 match pretty darn closely".
     *
     * @requires 0.<=match<=1.  
     */
    public final boolean matches(float precision) {
        int s1n=s1.length();
        int n=(int)(precision*((float)s1n));  //number UNchanged
        int maxOps=s1n-n;                     //number changed
        return matches(maxOps);
    }
    

//      /**
//       * If the edit distance between s1 and s2 is less than or equal to maxOps,
//       * returns the edit distance.  Otherwise returns some number greater than
//       * maxOps.
//       */
//      private final int matchInternal(final int maxOps) {
//          //Optimization: a crude "exclusion method".  First we check if the two
//          //strings are identical.  If not, we use the more expensive dynamic
//          //programming algorithm on the parts of the strings that don't match.
//          int i=diff(s1, s2);
//          if (i<0)
//              return 0;
//          else
//              return matchInternal(maxOps,
//                                   s1.substring(i),
//                                   s2.substring(i));
//      }


    /**
     * If the edit distance between s1 and s2 is less than or equal to maxOps,
     * returns the edit distance.  Otherwise returns some number greater than
     * maxOps.
     */
    private int matchInternal(final int maxOps) {
        //A classic implementation using dynamic programming.  d[i,j] is the
        //edit distance between s1[0..i-1] and s2[0..j-1] and is defined
        //recursively.  For all i, j, d[0][j]=j and d[i][0]=i; these "margins"
        //provide the base case.  See Chapter 11 of _Algorithms on Strings,
        //Trees, and Sequences_ by Dan Gusfield for a complete discussion.
        //
        //There are two novel twists to the usual algorithm.  First, we fill in
        //the matrix anti-diagonally instead of one row at a time.  Secondly, we
        //only fill in part of the row.  (See Chapter 12 of Gusfield for the
        //bounded k-difference algorithm.)  Finally, we stop if the minimum
        //value of the last two diagonals is greater than maxOps.
        final int s1n=s1.length();
        final int s2n=s2.length();
        Assert.that(s1n<=s2n);
        
        if (maxOps<=0)
            return (diff(s1, s2)==-1) ? 0 : 1;
        //Strings of vastly differing lengths don't match.  This is necessary to
        //prevent the last return statement below from incorrectly returning
        //zero.
        else if (Math.abs(s1n-s2n) > maxOps) {
            return maxOps+1;
        }
        //If one of the strings is empty, the distance is trivial to calculate.
        else if (s1n==0) { //s2n==0 ==> s1n==0           
            return s2n;
        }
        
        //Optimization: recycle buffer for matrix if possible. 
        int[][] d;
        if (buffer!=null
                && (bufSize >= Math.max(s1n+1, s2n+1)))
            d=buffer; 
        else
            d=new int[s1n+1][s2n+1];               //Note d[0][0]==0
        int diagonals=2*Math.min(s1n+1, s2n+1)-1
                         +Math.min(s2n-s1n, maxOps);
        int minThisDiag;              //The min value of this diagonal
        int minLastDiag=0;            //The min value of last diagonal
        
        //For each k'th anti-diagonal except first (measured from the origin)...
        for (int k=1; k<diagonals; k++) {            
            //1. Calculate indices of left corner of diagonal (i1, j1) and upper
            //right corner (i2, j2).  This is black magic.  You really need to
            //look at a diagram to see why it works.
            int i1=k/2+maxOps/2;
            int j1=k/2-maxOps/2;
            int i2=k/2-maxOps/2;
            int j2=k/2+maxOps/2;            
            if ((k%2)!=0) {              //odd k?
                if ((maxOps%2)==0) {     //even maxOps?
                    //out and away from last endpoint
                    j1++;
                    i2++;
                } else {
                    //in towards the diagonal
                    i1++;
                    j2++;
                }
            }           
            //If endpoints don't fall on board, adjust accordingly
            if (j1<0 || i1>s1n) {
                i1=Math.min(k, s1n);
                j1=k-i1;
            }
            if (i2<0 || j2>s2n) {
                j2=Math.min(k, s2n);
                i2=k-j2;
            }
            
            //2. Calculate matrix values for corners. This is just like the loop
            //below except (1) we need to be careful of array index problems 
            //and (2) we don't bother looking to the left of (i1, j1) or above 
            //(i2, j2) if it's on the outer diagonal.
            Assert.that(i1>0, "Zero i1");  //j1 may be zero
            Assert.that(j2>0, "Zero j2");  //i2 may be zero
            //   a) Look in towards diagonal
            d[i1][j1]=d[i1-1][j1]+1;
            d[i2][j2]=d[i2][j2-1]+1;                            
            //   b) Look along the diagonal, unless on edge of matrix
            if (j1>0) 
                d[i1][j1]=Math.min(d[i1][j1],
                              d[i1-1][j1-1] + diff(get(s1,i1-1), get(s2,j1-1)));
            if (i2>0)
                d[i2][j2]=Math.min(d[i2][j2],
                              d[i2-1][j2-1] + diff(get(s1,i2-1), get(s2,j2-1)));
            //   c) Look out away from the diagonal if "inner diagonal" or on
            //   bottom row, unless on edge of matrix.
            boolean innerDiag=(k%2)!=(maxOps%2);
            if ((innerDiag || i1==s1n) && j1>0)
                d[i1][j1]=Math.min(d[i1][j1],
                                   d[i1][j1-1]+1);            
            if (innerDiag && i2>0) 
                d[i2][j2]=Math.min(d[i2][j2],
                                   d[i2-1][j2]+1);
            minThisDiag=Math.min(d[i1][j1], d[i2][j2]);

            //3. Calculate matrix value for each element of the diagonal except
            //the endpoints...
            int i=i1-1;
            int j=j1+1;
            while (i>i2 && j<j2) {
                d[i][j]=1;
                //Fill in d[i][j] using previous calculated values
                int dij=min3(d[i-1][j-1] + diff(get(s1,i-1), get(s2,j-1)),
                             d[i-1][j]   + 1,
                             d[i][j-1]   + 1); 
                d[i][j]=dij;
                minThisDiag=Math.min(minThisDiag, dij);
                //Move up and to the right in the matrix.
                i--;
                j++;
            }
            
            //If min value on last two diags is too big, quit.
            if (minThisDiag>maxOps && minLastDiag>maxOps) {
                return minThisDiag;
            }
            minLastDiag=minThisDiag;
        }     

        return d[s1n][s2n];
    }

    /** Returns 0 if a==b, or 1 otherwise.  Here "==" ignores case
     *  if ignoreCase==true.  Also, ' '=='_' if specialUnderscores */
    private final int diff(char a, char b) {
        if (specialUnderscores) {
            if (a=='_')
                a=' ';
            if (b=='_')
                b=' ';
        }

        if (ignoreCase) {
            if (a==b || a==StringUtils.toOtherCase(b))
                return 0;
            else
                return 1;            
        } else {
            if (a==b) 
                return 0;
            else 
                return 1;
        }
    }

    /** Returns the first i s.t. s1[i]!=s2[i], or -1 if s1 and s2 are equal.  The
     *  returned value is not necessarily a valid index to both strings.  For
     *  example, equal("ab", "abc") or equal("abc", "ab") returns 2.  Case is
     *  ignored if ignoreCase==true. */
    private final int diff(String s1, String s2) {
        //String.compareToIgnoreCase isn't in JDK1.1.  So it's implemented here.
        //This method also has the advantage of comparing strings backwards and
        //without regard to case as necessary.
        int s1n=s1.length();
        int s2n=s2.length();
        int n=Math.min(s1n, s2n);
        for (int i=0; i<n; i++) {
            char c1=get(s1, i);
            char c2=get(s2, i);
            if (diff(c1, c2)==1)
                return i;
        }
        if (s1n==s2n)
            return -1;
        else 
            return n;
    }

    /** If !compareBackwards, return a.charAt(i).
     *  Otherwise, returns a.reverse().charAt(i). */
    private final char get(final String a, final int i) {
        final int n=a.length();
        if (compareBackwards) {
            return a.charAt(n-i-1);
        } else
            return a.charAt(i);
    }

    private static int min3(int n1, int n2, int n3) {
        return( Math.min( n1, Math.min( n2, n3 ) ) );
    }

    /** Unit test */
    /*
    public static void main(String[] args) {
        ApproximateMatcher matcher=new ApproximateMatcher("", "");
        Assert.that(2/2==1);
        Assert.that(3/2==1);
        Assert.that(matcher.diff("", "")==-1);
        Assert.that(matcher.diff("a", "")==0);
        Assert.that(matcher.diff("", "a")==0);
        Assert.that(matcher.diff("ace", "ace")==-1);
        Assert.that(matcher.diff("abc", "acd")==1);
        Assert.that(matcher.diff("abcdef", "abc")==3);
        Assert.that(matcher.diff("abc", "abcdef")==3);

        //1. Basic tests.  Try with and without compareBackwards.
        ApproximateMatcher.setBuffer(7);
        test("vintner", "writers", 5);
        test("", "", 0); 
        test("a", "", 1);
        test("", "a", 1);
        test("a", "b", 1);
        test("abc", "abbd", 2);
        test("abcd", "bd", 2);
        test("abcd", "abcd", 0);
        test("k", "abcdefghiklmnopqrst", 18);
        test("l", "abcdefghiklmnopqrst", 18);
        ApproximateMatcher.releaseBuffer();

        //2. Case insensitive tests.
        matcher=new ApproximateMatcher("AbcD", "ABcdx");
        Assert.that(matcher.match()==3);
        matcher.setIgnoreCase(true);
        Assert.that(matcher.match()==1);
        matcher=new ApproximateMatcher("AbcD", "ABcd");
        Assert.that(matcher.match()==2);
        matcher.setIgnoreCase(true);
        Assert.that(matcher.match()==0);
        
        //3. Fractional matching.
        matcher=new ApproximateMatcher("abcd", "abxy");
        Assert.that(matcher.matches(0.f));
        Assert.that(matcher.matches(0.5f));
        Assert.that(! matcher.matches(1.f));
        matcher=new ApproximateMatcher("01234567890123456789",    
                                       "01234X67890123456789");
        Assert.that(matcher.matches(0.f));
        Assert.that(matcher.matches(0.9f));
        Assert.that(! matcher.matches(1.f));     

        matcher=new ApproximateMatcher("abcdefghijklmnopqrWXYZ", 
                                       "abcdefghijklmnopqr1234");
        Assert.that(! matcher.matches(3));
        matcher.setCompareBackwards(true);
        Assert.that(! matcher.matches(3));        

        //4. Underscore test
        matcher=new ApproximateMatcher(" a_", "_a ");
        Assert.that(matcher.match()==2);
        matcher.setSpecialUnderscores(true);
        Assert.that(matcher.match()==0);
    }


    private static void test(String s1, String s2, int expected) {
        ApproximateMatcher matcher=new ApproximateMatcher(s1, s2);
        matcher.setCompareBackwards(false);
        Assert.that(matcher.match()==expected);
        matcher.setCompareBackwards(true);
        Assert.that(matcher.match()==expected);

        for (int i=-1; i<expected; i++) {
            matcher.setCompareBackwards(false);
            Assert.that(! matcher.matches(i),
                        "i="+i+
                        ", expected="+expected+
                        " match="+matcher.match());
            matcher.setCompareBackwards(true);
            Assert.that(! matcher.matches(i));
        }
        matcher.setCompareBackwards(false);
        Assert.that(matcher.matches(expected));
        matcher.setCompareBackwards(true);
        Assert.that(matcher.matches(expected));
        matcher.setCompareBackwards(false);
        Assert.that(matcher.matches(expected+1));
        matcher.setCompareBackwards(true);
        Assert.that(matcher.matches(expected+1));                    
    }
    */
}

