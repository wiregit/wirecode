package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;

/**
 * An approximate string matcher.  Two strings are considered
 * "approximately equal" if one can be transformed into the other
 * through some series of inserts, deletes, and substitutions.<p>
 *
 * The approximate matcher has options to ignore case and whitespace.
 * It also has switches to make it perform better by comparing strings
 * backwards and reusing a buffer.  The latter breaks thread safety and
 * modularity but yields significant performance gains.
 */
public class ApproximateMatcher
{
    private String s1;
    private String s2;
    
    /** For avoiding allocations.  This can only be used by one thread at a
     *  time.  INVARIANT: buffer!=null => buffer is a bufSize by bufSize array.
     */
    private static volatile int[][] buffer;
    private static volatile int bufSize=0;
    

    /** 
     * Creates a new matcher to match s1 against s2.
     *
     * @param ignoreWhitespace true iff the characters ' ' and '_'
     *  should be ignored when matching
     * @param ignoreCase true iff case should be ignored when matching
     * @param compareBackwards true iff the comparison should be done backwards.
     *  This is solely an optimization if you expect more differences at the
     *  end of the word than the beginning.
     */
    public ApproximateMatcher(String s1, String s2,
                              boolean ignoreWhitespace,
                              boolean ignoreCase,
                              boolean compareBackwards) {
        //Preprocess s1 and s2, reversing, removing whitespace, and changing
        //case as needed.  This code could be factored if desired.
        if (compareBackwards) {
            StringBuffer buf=null;
            //Process s1
            buf=new StringBuffer(s1.length());
            for (int i=0; i<s1.length(); i++) {
                char c=s1.charAt(s1.length()-i-1);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
            s1=buf.toString();
            //Process s2
            buf=new StringBuffer(s2.length());
            for (int i=0; i<s2.length(); i++) {
                char c=s2.charAt(s2.length()-i-1);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
            s2=buf.toString();
        } else {                  //Exactly like above, but forward.
            StringBuffer buf=null;
            //Process s1
            buf=new StringBuffer(s1.length());
            for (int i=0; i<s1.length(); i++) {
                char c=s1.charAt(i);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
            s1=buf.toString();
            //Process s2
            buf=new StringBuffer(s2.length());
            for (int i=0; i<s2.length(); i++) {
                char c=s2.charAt(i);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
            s2=buf.toString();            
        }

        //Store into instance variable.  Ensure s1.length<=s2.
        if (s1.length()<=s2.length()) {
            this.s1=s1;
            this.s2=s2;
        } else {
            this.s2=s1;
            this.s1=s2;
        }
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
        //recursively.  Note that there is are "margins" of 1 on the left and
        //top of this matrix.  See Chapter 11 of _Algorithms on Strings, Trees,
        //and Sequences_ by Dan Gusfield for a complete discussion.
        //
        //A key optimization is that we only fill in part of the row.  This is
        //based on the observation that any maxOps-difference global alignment
        //must not contain any cell (i, i+l) or (i,i-l), where l>maxOps.
        //
        //There are two additional twists to the usual algorithm.  First, we fill in
        //the matrix anti-diagonally instead of one row at a time.  Secondly, we
        //stop if the minimum value of the last two diagonals is greater than
        //maxOps.
        final int s1n=s1.length();
        final int s2n=s2.length();
        Assert.that(s1n<=s2n);
        
        if (maxOps<=0)
            return (s1.equals(s2)) ? 0 : 1;
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
                              d[i1-1][j1-1] + diff(s1.charAt(i1-1),
                                                   s2.charAt(j1-1)));
            if (i2>0)
                d[i2][j2]=Math.min(d[i2][j2],
                              d[i2-1][j2-1] + diff(s1.charAt(i2-1),
                                                   s2.charAt(j2-1)));
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
                int dij=min3(d[i-1][j-1] + diff(s1.charAt(i-1), s2.charAt(j-1)),
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

    /** Returns 0 if a==b, or 1 otherwise. */
    private static int diff(char a, char b) {
        if (a==b) 
            return 0;
        else 
            return 1;
    }

    private static int min3(int n1, int n2, int n3) {
        return( Math.min( n1, Math.min( n2, n3 ) ) );
    }

    /** Unit test */
    /*
    public static void main(String[] args) {
        ApproximateMatcher matcher=null;

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
        matcher=new ApproximateMatcher("AbcD", "ABcdx",
                                       false,
                                       false,
                                       false);
        Assert.that(matcher.match()==3);
        matcher=new ApproximateMatcher("AbcD", "ABcdx",
                                       false,
                                       true,
                                       false);
        Assert.that(matcher.match()==1);
        
        //3. Fractional matching.
        matcher=new ApproximateMatcher("abcd", "abxy",
                                       false, false, false);
        Assert.that(matcher.matches(0.f));
        Assert.that(matcher.matches(0.5f));
        Assert.that(! matcher.matches(1.f));
        matcher=new ApproximateMatcher("01234567890123456789",    
                                       "01234X67890123456789",
                                       false, false, false);
        Assert.that(matcher.matches(0.f));
        Assert.that(matcher.matches(0.9f));
        Assert.that(! matcher.matches(1.f));     

        //4. Whitespace
        matcher=new ApproximateMatcher(" a_", "_a ",
                                       false,
                                       false,
                                       false);
        Assert.that(matcher.match()==2);
        matcher=new ApproximateMatcher(" a_", "_a ",
                                       true,
                                       false,
                                       false);
        Assert.that(matcher.match()==0);
        matcher=new ApproximateMatcher("a b", "ab",
                                       false,
                                       false,
                                       false);
        Assert.that(matcher.match()==1);
        matcher=new ApproximateMatcher("a b", "ab",
                                       true,
                                       false,
                                       false);
        Assert.that(matcher.match()==0);
        matcher=new ApproximateMatcher("ab", "a_b",
                                       false,
                                       false,
                                       false);
        Assert.that(matcher.match()==1);
        matcher=new ApproximateMatcher("ab", "a_b",
                                       true,
                                       false,
                                       false);
        Assert.that(matcher.match()==0);
    }


    private static void test(String s1, String s2, int expected) {
        //Match forward and backwards
        ApproximateMatcher matcher=null;
        matcher=new ApproximateMatcher(s1, s2,
                                        false,
                                        false,
                                        true);
        Assert.that(matcher.match()==expected);
        matcher=new ApproximateMatcher(s1, s2,
                                        false,
                                        false,
                                        false);
        Assert.that(matcher.match()==expected);

        //Bounded match
        for (int i=-1; i<expected; i++) {
            Assert.that(! matcher.matches(i),
                        "i="+i+
                        ", expected="+expected+
                        " match="+matcher.match());
        }
        Assert.that(matcher.matches(expected));
        Assert.that(matcher.matches(expected+1));
    }
    */
}

