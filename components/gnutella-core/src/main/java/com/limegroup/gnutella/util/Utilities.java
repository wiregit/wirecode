package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.Assert;

/**
 * Provides utility methods like checking set intersection etc.
 * @author Anurag Singla
 */
public class Utilities 
{
    /**
     * Determines if two sets have non-void intersection
     * @param set1 First set
     * @param set2 Second set
     * @return true, if two sets have non-void intersection, false otherwise
     */
    public static boolean hasIntersection(Set set1, Set set2)
    {
        //Iterate over the first set, and check the value in the second set
        for(Iterator iterator = set1.iterator(); iterator.hasNext();)
        {
            //if second set contains the entry, return true
            if(set2.contains(iterator.next()))
                return true;
        }
        //if no match found, return true
        return false;
    }

    /**
     * An optimized replacement for Arrays.fill that takes advantage of System.arraycopy.
     * On my Windows machine, this is over nearly twice as fast for arrays greater than
     * 1000 elements.  The savings decrease somewhat over 32000 elements.
     *
     * @param array the array to fill
     * @param start the starting offset, inclusive
     * @param stop the stop offset+1.  <b>MUST be greater than start; this differs
     *  from Arrays.fill.</b>
     * @param value the value to write into the array
     */
    public static void fill(int array[], int start, int stop, int value) {
        array[start] = value;
        int span=1;
        for (int i=start+1; i<stop; ) {
            System.arraycopy(array, start, array, i, Math.min(span, stop-i));
            i+=span;
            span=span*2;
        }
    }

    /*
    public static void main(String args[]) {
        //Test correctness
        int[] in=null;
        int[] out=null;
        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {1, 1, 1, 1, 0, 0, 0};
        fill(in, 0, 4, 1);
        Assert.that(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {1, 1, 1, 1, 1, 0, 0};
        fill(in, 0, 5, 1);
        Assert.that(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {0, 0, 1, 1, 1, 1, 0};
        fill(in, 2, 6, 1);
        Assert.that(Arrays.equals(in, out));

        in= new int[] {0, 0, 0, 0, 0, 0, 0};
        out=new int[] {0, 0, 1, 1, 1, 0, 0};
        fill(in, 2, 5, 1);
        Assert.that(Arrays.equals(in, out));

        //Performance benchmark
        int ITERATIONS=10000;
        System.out.println("n Arrays.fill fill");
        for (int n=4; n<100000; n=n*2) {
            System.out.print(n+" ");
            int[] buf=new int[n];
            long start, elapsed;
            
            start=System.currentTimeMillis();
            for (int i=0; i<ITERATIONS; i++) {
                Arrays.fill(buf, 0, buf.length, i);
            }
            elapsed=System.currentTimeMillis()-start;
            System.out.print(elapsed+" ");

            start=System.currentTimeMillis();
            for (int i=0; i<ITERATIONS; i++) {
                fill(buf, 0, buf.length, i);
            }
            elapsed=System.currentTimeMillis()-start;
            System.out.print(elapsed+"\n");
        }        
    } 
    */
}
