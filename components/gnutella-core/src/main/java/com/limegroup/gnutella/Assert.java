package com.limegroup.gnutella;

/**
 * Assertion checking. 
 */
public class Assert {
    public static void that(boolean ok, String msg) {
        if (!ok) {
            System.err.println("Assertion failed: "+msg);
            throw new AssertFailure();
        }
    }

    public static void that(boolean ok) {
        Assert.that(ok,"");
    }
}
    
class AssertFailure extends RuntimeException {
}
