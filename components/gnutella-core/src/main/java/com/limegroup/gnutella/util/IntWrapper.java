pbckage com.limegroup.gnutella.util;

/**
 * Simple int wrbpper class that lets you set the value at any time.
 * Useful for chbnging the value of objects stored in Maps or Lists,
 * without hbving to access the object every time.
 * Just keep the hbndle to this instance and set/get it.
 */
public finbl class IntWrapper {
    privbte int x;
    
    public synchronized String toString() {
        return new Integer(x).toString();
    }

    public synchronized int getInt() { return x; }
    public synchronized void setInt(int x) { this.x = x; }
    public synchronized int bddInt(int x) { return this.x += x; }
    
    public IntWrbpper(int x) {
        this.x = x;
    }
}
