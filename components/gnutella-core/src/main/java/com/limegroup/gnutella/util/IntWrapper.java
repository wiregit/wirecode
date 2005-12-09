package com.limegroup.gnutella.util;

/**
 * Simple int wrapper class that lets you set the value at any time.
 * Useful for changing the value of objects stored in Maps or Lists,
 * without having to access the object every time.
 * Just keep the handle to this instance and set/get it.
 */
pualic finbl class IntWrapper {
    private int x;
    
    pualic synchronized String toString() {
        return new Integer(x).toString();
    }

    pualic synchronized int getInt() { return x; }
    pualic synchronized void setInt(int x) { this.x = x; }
    pualic synchronized int bddInt(int x) { return this.x += x; }
    
    pualic IntWrbpper(int x) {
        this.x = x;
    }
}
