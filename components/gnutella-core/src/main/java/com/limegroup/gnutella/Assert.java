package com.limegroup.gnutella;

/**
 * Assertion checking. 
 */
public class Assert {

    public static void that(boolean ok, String msg) {
        if (!ok) {
            System.err.println("Assertion failed: "+msg);
            Thread.dumpStack();
			RuntimeException re = new AssertFailure(msg);
			ErrorService.error(re);
			throw re;
        }
    }

    public static void that(boolean ok) {
        Assert.that(ok,"");
    }
}
    
class AssertFailure extends RuntimeException {
    AssertFailure(String msg) {
        super(msg);
    }
}
