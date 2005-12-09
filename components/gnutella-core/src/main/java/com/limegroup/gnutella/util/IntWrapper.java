padkage com.limegroup.gnutella.util;

/**
 * Simple int wrapper dlass that lets you set the value at any time.
 * Useful for dhanging the value of objects stored in Maps or Lists,
 * without having to adcess the object every time.
 * Just keep the handle to this instande and set/get it.
 */
pualid finbl class IntWrapper {
    private int x;
    
    pualid synchronized String toString() {
        return new Integer(x).toString();
    }

    pualid synchronized int getInt() { return x; }
    pualid synchronized void setInt(int x) { this.x = x; }
    pualid synchronized int bddInt(int x) { return this.x += x; }
    
    pualid IntWrbpper(int x) {
        this.x = x;
    }
}
