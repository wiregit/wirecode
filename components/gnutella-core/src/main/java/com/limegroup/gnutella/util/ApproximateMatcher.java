package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;

/**
 * An approximate string matcher.  Two strings are considered
 * "approximately equal" if one can be transformed into the other
 * through some series of inserts, deletes, and substitutions.
 */
public class ApproximateMatcher
{
    private String s1;
    private String s2;
    private boolean ignoreCase=false;
    private boolean compareBackwards=false;

    /** 
     * Creates a new matcher to match s1 against s2..  The default
     * matcher is case-sensitive and compares forwards.
     */
    public ApproximateMatcher( String s1, String s2) {
        this.s1=s1;
        this.s2=s2;
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

    /*
     * Returns the edit distance between s1 and s2.  That is, returns the number
     * of insertions, deletions, or replacements necessary to transform s1 into
     * s2.  A value of 0 means the strings match exactly.  Case is ignored if
     * ignoreCase==true.  
     */
    public final int match() {
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
    

    /**
     * If the edit distance between s1 and s2 is less than or equal to maxOps,
     * returns the edit distance.  Otherwise returns some number greater than
     * maxOps.
     */
    private final int matchInternal(final int maxOps) {
        //A classic implementation using dynamic programming.  d[i,j] is the
        //edit distance between s1[0..i-1] and s2[0..j-1] and is defined
        //recursively.  For all i, j, d[0][j]=j and d[i][0]=i; these "margins"
        //provide the base case.  See Chapter 11 of _Algorithms on Strings,
        //Trees, and Sequences_ by Dan Gusfield for a complete discussion.
        //
        //There are two novel twists to the usual algorithm.  First, we fill in
        //the matrix anti-diagonally instead of one row at a time.  Secondly, we
        //stop if the minimum value of the last two diagonals is greater than
        //maxOps.
        final String s1=this.s1;           //optimization
        final int s1n=s1.length();
        final String s2=this.s2;           //optimization
        final int s2n=s2.length();
        
        //Optimization for common case: equal strings can be matched O(s1n) time
        //with zero allocs.
        if (s1n==s2n) {
            if (equal(s1, s2))
                return 0;
        } 
        //Optimization for common case: strings of vastly differing lengths
        //don't match.
        else if (Math.abs(s1n-s2n) > maxOps) {
            return maxOps+1;
        }
        

        int[][] d=new int[s1n+1][s2n+1];   //Note d[0][0]==0
        int diagonals=s1n+s2n+1;           //The number of diagonals.
        int minThisDiag;                   //The min value of this diagonal
        int minLastDiag=0;                 //The min value of last diagonal

        //For each k'th anti-diagonal except first (measured from the origin)...
        for (int k=1; k<diagonals; k++) {
            minThisDiag=Integer.MAX_VALUE;
            //Fill in bottom left corner, if needed.
            if (k<=s1n) {
                d[k][0]=k;
                minThisDiag=Math.min(minThisDiag, k);
            }
            //Fill in upper right corner, if needed.
            if (k<=s2n) {
                d[0][k]=k;
                minThisDiag=Math.min(minThisDiag, k);
            }

            //For each element of the diagonal except the endpoints...
            int i=Math.min(k-1,s1n);
            int j=k-i;
            while (i>0 && j<=s2n) {
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
     *  if ignoreCase==true. */
    private final int diff(char a, char b) {
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

    /** True iff both strings match.  Case is ignored if ignoreCase==true.
     *    @requires |s1|==|s2| */
    private final boolean equal(String s1, String s2) {
        //String.compareToIgnoreCase isn't in JDK1.1.  So it's implemented here.
        //This method also has the advantage of comparing strings backwards and
        //without regard to case as necessary.
        final int n=s1.length();
        for (int i=0; i<n; i++) {
            char c1=get(s1, i);
            char c2=get(s2, i);
            if (diff(c1, c2)==1)
                return false;
        }
        return true;        
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
        ApproximateMatcher matcher=null;    

        //1. Basic tests.  Try with and without compareBackwards.
        test("", "", 0);
        test("a", "", 1);
        test("", "a", 1);
        test("a", "b", 1);
        test("abc", "abbd", 2);
        test("abcd", "bd", 2);
        test("abcd", "abcd", 0);
        test("vintner", "writers", 5);
        test("k", "abcdefghiklmnopqrst", 18);

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
        
    }

    private static void test(String s1, String s2, int expected) {
        ApproximateMatcher matcher=new ApproximateMatcher(s1, s2);
        matcher.setCompareBackwards(false);
        Assert.that(matcher.match()==expected);
        matcher.setCompareBackwards(true);
        Assert.that(matcher.match()==expected);

        for (int i=-1; i<expected; i++) {
            matcher.setCompareBackwards(false);
            Assert.that(! matcher.matches(i));
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

