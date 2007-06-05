package org.limewire.collection;

/**
 * Provides the storage of two integers. No mutators or get methods needed.
<pre>
    void sampleCodeIntPair(){
        IntPair p = new IntPair(1,2);
        System.out.println("Pair is " + p.a + " & " + p.b);
    }
    Output:
        Pair is 1 & 2

</pre>
 * 
 */
public class IntPair {
    public int a;
    public int b;

    public IntPair(int a, int b) {
        this.a=a;
        this.b=b;
    }
}
