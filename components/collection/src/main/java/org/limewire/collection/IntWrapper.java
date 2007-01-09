package org.limewire.collection;

/**
 * Simple int wrapper class that lets you set the value at any time.
 * Useful for changing the value of objects stored in Maps or Lists,
 * without having to access the object every time.
 * Just keep the handle to this instance and set/get it.
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
