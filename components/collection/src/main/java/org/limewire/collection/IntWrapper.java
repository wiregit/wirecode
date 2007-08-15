package org.limewire.collection;


/**
 * Provides a simple int wrapper class that lets you set the value at any time.
 * <code>IntWrapper</code> is useful for changing the value of objects stored 
 * in Maps or Lists, without having to access the object every time.
 * Just keep the handle to this instance and set/get the object.
 <pre>
    IntWrapper w1 = new IntWrapper(1);
    
    ArrayList&lt;IntWrapper&gt; l = new ArrayList&lt;IntWrapper&gt;();

    l.add(0, w1);
    l.add(0, new IntWrapper(2));
    l.add(0, new IntWrapper(3));
    l.add(0, new IntWrapper(4));
    System.out.println(l);
    w1.setInt(10);
    System.out.println(l);
 
    Output:
        [4, 3, 2, 1]
        [4, 3, 2, 10]

 </pre>
 * 
 */
public final class IntWrapper {
    private int x;
    
    public synchronized String toString() {
        return new Integer(x).toString();
    }

    public synchronized int getInt() { return x; }
    public synchronized void setInt(int x) { this.x = x; }
    public synchronized int addInt(int x) { return this.x += x; }
    
    public IntWrapper(int x) {
        this.x = x;
    }
}
