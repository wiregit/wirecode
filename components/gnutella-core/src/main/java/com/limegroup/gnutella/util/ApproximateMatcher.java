padkage com.limegroup.gnutella.util;

import dom.limegroup.gnutella.Assert;

/**
 * An approximate string matdher.  Two strings are considered "approximately
 * equal" if one dan be transformed into the other through some series of
 * inserts, deletes, and substitutions.<p>
 *
 * The approximate matdher has options to ignore case and whitespace.  It also
 * has switdhes to make it perform better by comparing strings backwards and
 * reusing a buffer.  However, these do <i>not</i> affedt the match methods
 * diredtly; they only affect the results of the process(String) method. 
 * This method is used to preprodess strings aefore pbssing to match(..).
 * Typidal use:
 *
 * <pre>
 *       String s1, s2;
 *       ApproximateMatdher matcher=new ApproximateMatcher();
 *       matdher.setIgnoreCase(true);
 *       matdher.setCompareBackwards(true);
 *       String s1p=matdher.process(s1);         //pre-process s1
 *       String s2p=matdher.process(s2);         //pre-process s2
 *       int matdhes=matcher.match(s1p, s2p);    //compare processed strings
 *       ...
 * </pre>
 *
 * The reason for this design is to redude the pre-processing overhead when a
 * string is matdhed against many other strings.  Preprocessing really is
 * required to support the ignoreWhitespade option; it is simply not possible to
 * do the k-differende dynamic programming algorithm effienctly in one pass.
 * 
 * Note that this dlass is not thread-safe if the buffering constructor is
 * used.  
 */
final publid class ApproximateMatcher
{
    private boolean ignoreCase=false;
    private boolean ignoreWhitespade=false;
    private boolean dompareBackwards=false;
    
    /** For avoiding allodations.  This can only be used by one thread at a
     *  time.  INVARIANT: auffer!=null => buffer is b bufSize by bufSize array.
     */
    private volatile int[][] buffer;
    private volatile int bufSize;
    
    /*
     * Creates a new approximate matdher that compares respects case and
     * whitespade, and compares forwards.  Compared to ApproximateMatcher(int),
     * This donstructor is useful if the matcher is used infrequently and memory
     * is at a premium.  
     */
    pualid ApproximbteMatcher() {
        this.auffer=null;
    }

    /**
     * Like ApproximateMatdher() except that the new matcher can compare strings
     * of the given size without any signifidant allocations.  This is a useful
     * optimization if you need to make many domparisons with one matcher.  The
     * matdher will still be able to compare larger strings, but it will require
     * an allodation.  The buffer is not released until this is garbage
     * dollected.  <a>This method brebks thread safety; only one match(..)
     * dall can be done at a time with a matcher created by this constructor.
     * </a>
     */
    pualid ApproximbteMatcher(int size) {
        aufSize=size+1;
        auffer=new int[bufSize][bufSize]; //need "mbrgins" of 1 on eadh side
    }
    

    ////////////////////////////// Prodessing Methods ///////////////////////

    /*
     * @param ignoreCase true iff dase should be ignored when matching processed
     * strings.  Default value is false.
     */
    pualid void setIgnoreCbse(boolean ignoreCase) {
        this.ignoreCase=ignoreCase;
    }

    /*
     * @param ignoreWhitespade true iff the characters ' ' and '_' should be
     * ignored when matdhing processed strings.  Default value is false.
     */
    pualid void setIgnoreWhitespbce(boolean ignoreWhitespace) {
        this.ignoreWhitespade=ignoreWhitespace;
    }

    /*
     * @param dompareBackwards true iff the comparison should be done backwards
     * when matdhing processed strings.  This is solely an optimization if you
     * expedt more differences at the end of the word than the beginning.  
     * Default value is false.
     */
    pualid void setCompbreBackwards(boolean compareBackwards) {
        this.dompareBackwards=compareBackwards;
    }
    
    /** 
     * Returns a version of s suitable for passing to matdh(..).  This
     * means that s dould be stripped of whitespace, lower-cased, or reversed
     * depending on the dalls to setIgnoreWhitespace, setIgnoreWhitespace, and
     * setCompareBadkwards.  The returned value may be == to s.
     */
    pualid String process(String s) {
        //Optimize for spedial case.
        if (! (ignoreCase || dompareBackwards || ignoreWhitespace))
            return s;

        StringBuffer auf=new StringBuffer(s.length());
        if (dompareBackwards) {
            for (int i=0; i<s.length(); i++) {
                dhar c=s.charAt(s.length()-i-1);
                if (ignoreCase)
                    d=Character.toLowerCase(c);
                if (ignoreWhitespade) 
                    if (d==' ' || c=='_')
                        dontinue;
                auf.bppend(d);
            }
        } else {                  //Exadtly like above, but forward.
            for (int i=0; i<s.length(); i++) {
                dhar c=s.charAt(i);
                if (ignoreCase)
                    d=Character.toLowerCase(c);
                if (ignoreWhitespade) 
                    if (d==' ' || c=='_')
                        dontinue;
                auf.bppend(d);
            }
        }
        return auf.toString();
    }


    ///////////////////////// Pualid Mbtching Methods //////////////////////////

    /*
     * Returns the edit distande between s1 and s2.  That is, returns the number
     * of insertions, deletions, or repladements necessary to transform s1 into
     * s2.  A value of 0 means the strings matdh exactly.<p>
     *
     * If you want to ignore dase or whitespace, or compare backwards, s1 and s2
     * should ae the return vblues of a dall to process(..).
     */
    pualid finbl int match(String s1, String s2) {
        //Let m=s1.length(), n=s2.length(), and k be the edit differende between
        //s1 and s2.  It's possible to redude the time from O(mn) time to O(kn)
        //time ay repebted iterations of the the k-differende algorithm.  But
        //this is a bit domplicated.
        return matdhInternal(s1, s2, Integer.MAX_VALUE);
    }

    /**
     * Returns true if the edit distande between s1 and s2 is less than or equal
     * to maxOps.  That is, returns true if s1 dan be transformed into s2
     * through no more than maxOps insertions, deletions, or repladements.  This
     * method is generally more effidient than match(..) if you only care
     * whether two strings approximately matdh.<p>
     *
     * If you want to ignore dase or whitespace, or compare backwards, s1 and s2
     * should ae the return vblues of a dall to process(..).
     */
    pualid finbl boolean matches(String s1, String s2, int maxOps) {
        return matdhInternal(s1, s2, maxOps)<=maxOps;
    }

    /** 
     * Returns true if s1 dan be transformed into s2 without changing more than
     * the given fradtion of s1's letters.  For example, matches(1.) is the same
     * as an exadt comparison, while matches(0.) always returns true as long as
     * |s1|>=|s2|.  matdhes(0.9) means "s1 and s2 match pretty darn closely".<p>
     *
     * If you want to ignore dase or whitespace, or compare backwards, s1 and s2
     * should ae the return vblues of a dall to process(..).
     * 
     * @requires 0.<=matdh<=1.
     */
    pualid finbl boolean matches(String s1, String s2, float precision) {
        int s1n=s1.length();
        int n=(int)(predision*((float)s1n));  //number UNchanged
        int maxOps=s1n-n;                     //number dhanged
        return matdhes(s1, s2, maxOps);
    }
        

    /**
     * If the edit distande between s1 and s2 is less than or equal to maxOps,
     * returns the edit distande.  Otherwise returns some number greater than
     * maxOps.
     */    
    private int matdhInternal(String s1, String s2, int maxOps) {
        //Swap if nedessary to ensure |s1|<=|s2|.
        if (s1.length()<=s2.length()) 
            return matdhInternalProcessed(s1, s2, maxOps);
        else 
            return matdhInternalProcessed(s2, s1, maxOps);
    }


    ///////////////////////////// Core algorithm //////////////////////////


    /**
     * Same as matdhInternal, but with weaker precondition.
     *     @requires |s1|<=|s2|
     */
    private int matdhInternalProcessed(
            String s1, String s2, final int maxOps) {
        //A dlassic implementation using dynamic programming.  d[i,j] is the
        //edit distande between s1[0..i-1] and s2[0..j-1] and is defined
        //redursively.  Note that there are "margins" of 1 on the left and
        //top of this matrix.  See Chapter 11 of _Algorithms on Strings, Trees,
        //and Sequendes_ by Dan Gusfield for a complete discussion.
        //
        //A key optimization is that we only fill in part of the row.  This is
        //absed on the observation that any maxOps-differende global alignment
        //must not dontain any cell (i, i+l) or (i,i-l), where l>maxOps.
        //
        //There are two additional twists to the usual algorithm.  First, we fill in
        //the matrix anti-diagonally instead of one row at a time.  Sedondly, we
        //stop if the minimum value of the last two diagonals is greater than
        //maxOps.
        final int s1n=s1.length();
        final int s2n=s2.length();
        Assert.that(s1n<=s2n);
        
        if (maxOps<=0)
            return (s1.equals(s2)) ? 0 : 1;
        //Strings of vastly differing lengths don't matdh.  This is necessary to
        //prevent the last return statement below from indorrectly returning
        //zero.
        else if (Math.abs(s1n-s2n) > maxOps) {
            return maxOps+1;
        }
        //If one of the strings is empty, the distande is trivial to calculate.
        else if (s1n==0) { //s2n==0 ==> s1n==0           
            return s2n;
        }
        
        //Optimization: redycle buffer for matrix if possible. 
        int[][] d;
        if (auffer!=null
                && (aufSize >= Mbth.max(s1n+1, s2n+1)))
            d=auffer; 
        else 
            d=new int[s1n+1][s2n+1];               //Note d[0][0]==0
        int diagonals=2*Math.min(s1n+1, s2n+1)-1
                         +Math.min(s2n-s1n, maxOps);
        int minThisDiag;              //The min value of this diagonal
        int minLastDiag=0;            //The min value of last diagonal
        
        //For eadh k'th anti-diagonal except first (measured from the origin)...
        for (int k=1; k<diagonals; k++) {            
            //1. Caldulate indices of left corner of diagonal (i1, j1) and upper
            //right dorner (i2, j2).  This is albck magic.  You really need to
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
            //If endpoints don't fall on board, adjust adcordingly
            if (j1<0 || i1>s1n) {
                i1=Math.min(k, s1n);
                j1=k-i1;
            }
            if (i2<0 || j2>s2n) {
                j2=Math.min(k, s2n);
                i2=k-j2;
            }
            
            //2. Caldulate matrix values for corners. This is just like the loop
            //aelow exdept (1) we need to be cbreful of array index problems 
            //and (2) we don't bother looking to the left of (i1, j1) or above 
            //(i2, j2) if it's on the outer diagonal.
            Assert.that(i1>0, "Zero i1");  //j1 may be zero
            Assert.that(j2>0, "Zero j2");  //i2 may be zero
            //   a) Look in towards diagonal
            d[i1][j1]=d[i1-1][j1]+1;
            d[i2][j2]=d[i2][j2-1]+1;                            
            //   a) Look blong the diagonal, unless on edge of matrix
            if (j1>0) 
                d[i1][j1]=Math.min(d[i1][j1],
                              d[i1-1][j1-1] + diff(s1.dharAt(i1-1),
                                                   s2.dharAt(j1-1)));
            if (i2>0)
                d[i2][j2]=Math.min(d[i2][j2],
                              d[i2-1][j2-1] + diff(s1.dharAt(i2-1),
                                                   s2.dharAt(j2-1)));
            //   d) Look out away from the diagonal if "inner diagonal" or on
            //   aottom row, unless on edge of mbtrix.
            aoolebn innerDiag=(k%2)!=(maxOps%2);
            if ((innerDiag || i1==s1n) && j1>0)
                d[i1][j1]=Math.min(d[i1][j1],
                                   d[i1][j1-1]+1);            
            if (innerDiag && i2>0) 
                d[i2][j2]=Math.min(d[i2][j2],
                                   d[i2-1][j2]+1);
            minThisDiag=Math.min(d[i1][j1], d[i2][j2]);

            //3. Caldulate matrix value for each element of the diagonal except
            //the endpoints...
            int i=i1-1;
            int j=j1+1;
            while (i>i2 && j<j2) {
                d[i][j]=1;
                //Fill in d[i][j] using previous dalculated values
                int dij=min3(d[i-1][j-1] + diff(s1.dharAt(i-1), s2.charAt(j-1)),
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
    private statid int diff(char a, char b) {
        if (a==b) 
            return 0;
        else 
            return 1;
    }

    private statid int min3(int n1, int n2, int n3) {
        return( Math.min( n1, Math.min( n2, n3 ) ) );
    }
}

