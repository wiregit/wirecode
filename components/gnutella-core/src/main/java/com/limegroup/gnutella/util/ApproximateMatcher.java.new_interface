package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;

/**
 * An approximate string matcher.  Two strings are considered
 * "approximately equal" if one can be transformed into the other
 * through some series of inserts, deletes, and substitutions.<p>
 *
 * The approximate matcher has options to ignore case and whitespace.  It also
 * has switches to make it perform better by comparing strings backwards and
 * reusing a buffer.  <b>However, this class is not thread safe.</b>
 */
final public class ApproximateMatcher
{
    private boolean ignoreCase=false;
    private boolean ignoreWhitespace=false;
    private boolean compareBackwards=false;
    
    /** For avoiding allocations.  This can only be used by one thread at a
     *  time.  INVARIANT: buffer!=null => buffer is a bufSize by bufSize array.
     */
    private volatile int[][] buffer;
    private volatile int bufSize;
    
    /*
     * Creates a new approximate matcher that compares respects case and
     * whitespace, and compares forwards.  Compared to ApproximateMatcher(int),
     * This constructor is useful if the matcher is used infrequently and memory
     * is at a premium.  
     */
    public ApproximateMatcher() {
        this.buffer=null;
    }

    /**
     * Like ApproximateMatcher() except that the new matcher can compare strings
     * of the given size without any significant allocations.  This is a useful
     * optimization if you need to make many comparisons with one matcher.  The
     * matcher will still be able to compare larger strings, but it will require
     * an allocation.  The buffer is not released until this is garbage
     * collected.  
     */
    public ApproximateMatcher(int size) {
        bufSize=size+1;
        buffer=new int[bufSize][bufSize]; //need "margins" of 1 on each side
    }
    
    /*
     * @param ignoreCase true iff case should be ignored when matching.  Default
     * value is false.
     */
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase=ignoreCase;
    }

    /*
     * @param ignoreWhitespace true iff the characters ' ' and '_' should be
     * ignored when matching. Default value is false.
     */
    public void setIgnoreWhitespace(boolean ignoreWhitespace) {
        this.ignoreWhitespace=ignoreWhitespace;
    }

    /*
     * @param compareBackwards true iff the comparison should be done backwards.
     * This is solely an optimization if you expect more differences at the end
     * of the word than the beginning.  Default value is false.
     */
    public void setCompareBackwards(boolean compareBackwards) {
        this.compareBackwards=compareBackwards;
    }
    

    ///////////////////////// Public Matching Methods //////////////////////////

    /*
     * Returns the edit distance between s1 and s2.  That is, returns the number
     * of insertions, deletions, or replacements necessary to transform s1 into
     * s2.  A value of 0 means the strings match exactly.  Case is ignored if
     * ignoreCase==true.  
     */
    public final int match(String s1, String s2) {
        //Let m=s1.length(), n=s2.length(), and k be the edit difference between
        //s1 and s2.  It's possible to reduce the time from O(mn) time to O(kn)
        //time by repeated iterations of the the k-difference algorithm.  But
        //this is a bit complicated.
        return matchInternal(s1, s2, Integer.MAX_VALUE);
    }

    /**
     * Returns true if the edit distance between s1 and s2 is less than or equal
     * to maxOps.  That is, returns true if s1 can be transformed into s2
     * through no more than maxOps insertions, deletions, or replacements.  Case
     * is ignored if ignoreCase==true.  This method is generally more efficient
     * than match(..) if you only care whether two strings approximately match.
     */
    public final boolean matches(String s1, String s2, int maxOps) {
        return matchInternal(s1, s2, maxOps)<=maxOps;
    }

    /** 
     * Returns true if s1 can be transformed into s2 without changing more than
     * the given fraction of s1's letters.  For example, matches(1.) is the same
     * as an exact comparison, while matches(0.) always returns true as long as
     * |s1|>=|s2|.  matches(0.9) means "s1 and s2 match pretty darn closely".
     *
     * @requires 0.<=match<=1.  
     */
    public final boolean matches(String s1, String s2, float precision) {
        int s1n=s1.length();
        int n=(int)(precision*((float)s1n));  //number UNchanged
        int maxOps=s1n-n;                     //number changed
        return matches(s1, s2, maxOps);
    }
        

    /**
     * If the edit distance between s1 and s2 is less than or equal to maxOps,
     * returns the edit distance.  Otherwise returns some number greater than
     * maxOps.
     */    
    private int matchInternal(String s1, String s2, int maxOps) {
        //Remove whitespace, etc.
        s1=process(s1);
        s2=process(s2);

        //Call matchInternal.  Swap if necessary to ensure |s1|<=|s2|.
        if (s1.length()<=s2.length()) 
            return matchInternalProcessed(s1, s2, maxOps);
        else 
            return matchInternalProcessed(s2, s1, maxOps);
    }

    /** 
     * Returns a version of s1 suitable for passing to matchInternal.  This
     * means that s1 could be stripped of whitespace, lower-cased, or reversed.  
     */
    private String process(String s) {
        //Optimize for special case.
        if (! (ignoreCase || compareBackwards || ignoreWhitespace))
            return s;

        StringBuffer buf=new StringBuffer(s.length());
        if (compareBackwards) {
            for (int i=0; i<s.length(); i++) {
                char c=s.charAt(s.length()-i-1);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
        } else {                  //Exactly like above, but forward.
            for (int i=0; i<s.length(); i++) {
                char c=s.charAt(i);
                if (ignoreCase)
                    c=Character.toLowerCase(c);
                if (ignoreWhitespace) 
                    if (c==' ' || c=='_')
                        continue;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    ///////////////////////////// Core algorithm //////////////////////////


    /**
     * Same as matchInternal, but with weaker precondition.
     *     @requires s1 and s2 have been processed, |s1|<=|s2|
     */
    private int matchInternalProcessed(
            String s1, String s2, final int maxOps) {
        //A classic implementation using dynamic programming.  d[i,j] is the
        //edit distance between s1[0..i-1] and s2[0..j-1] and is defined
        //recursively.  Note that there are "margins" of 1 on the left and
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


    ////////////////////////////////////////////////////////////////////////


    /** Unit test */
    public static void main(String[] args) {
        ApproximateMatcher matcher=null;

        //1. Basic tests.  Try with and without compareBackwards.
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

        //2. Case insensitive tests.
        matcher=new ApproximateMatcher();
        Assert.that(matcher.match("AbcD", "ABcdx")==3);
        matcher.setIgnoreCase(true);
        Assert.that(matcher.match("AbcD", "ABcdx")==1);
        
        //3. Fractional matching.
        matcher=new ApproximateMatcher();
        Assert.that(matcher.matches("abcd", "abxy", 0.f));
        Assert.that(matcher.matches("abcd", "abxy", 0.5f));
        Assert.that(! matcher.matches("abcd", "abxy", 1.f));
        Assert.that(matcher.matches("01234567890123456789",    
                                    "01234X67890123456789",
                                    0.5f));
        Assert.that(matcher.matches("01234567890123456789",    
                                    "01234X67890123456789",
                                    0.9f));
        Assert.that(! matcher.matches("01234567890123456789",    
                                      "01234X67890123456789",
                                      1.f));     

        //4. Whitespace
        matcher=new ApproximateMatcher();
        Assert.that(matcher.match(" a_", "_a ")==2);
        matcher.setIgnoreWhitespace(true);
        Assert.that(matcher.match(" a_", "_a ")==0);

        matcher=new ApproximateMatcher();
        Assert.that(matcher.match("a b", "ab")==1);
        matcher.setIgnoreWhitespace(true);
        Assert.that(matcher.match("a b", "ab")==0);

        matcher=new ApproximateMatcher();
        Assert.that(matcher.match("ab", "a_b")==1);
        matcher.setIgnoreWhitespace(true);
        Assert.that(matcher.match("ab", "a_b")==0);

        //5. Speed test.
        matcher=new ApproximateMatcher(100);
        matcher.setCompareBackwards(true);
        matcher.setIgnoreWhitespace(true);
        matcher.setIgnoreCase(true);

        long start=System.currentTimeMillis();
        String s1="   Some long string Britney spears.mp3 extr garbage";
        String s2="Some long string Bratney spars.mp3 extra garbage_____";
        System.out.println("Starting benchmark...");
        for (int i=0; i<10000; i++) {
            Assert.that(matcher.matches(s1, s2, 3));
        }
        long elapsed=System.currentTimeMillis()-start;
        System.out.println("Done in "+elapsed+" msec");
            
    }


    private static void test(String s1, String s2, int expected) {
        //Match forward and backwards
        ApproximateMatcher matcher=null;
        matcher=new ApproximateMatcher(5);  //reuse buffer for some,not all
        matcher.setCompareBackwards(true);
        Assert.that(matcher.match(s1, s2)==expected);
        matcher.setCompareBackwards(false);
        Assert.that(matcher.match(s1, s2)==expected);

        //Bounded match
        for (int i=-1; i<expected; i++) {
            Assert.that(! matcher.matches(s1, s2, i),
                        "i="+i+
                        ", expected="+expected+
                        " match="+matcher.match(s1, s2));
        }
        Assert.that(matcher.matches(s1, s2, expected));
        Assert.that(matcher.matches(s1, s2, expected+1));
    }
}

