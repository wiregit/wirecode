package com.limegroup.gnutella;

/**
 * Assertion checking. 
 */
public class Assert {

    public static void that(boolean ok, String msg) {
        if (!ok) {
            System.err.println("Assertion failed: "+msg);
			RuntimeException re = new AssertFailure(msg);
			RouterService rs = RouterService.instance();
			if(rs != null) {
				ActivityCallback callback = rs.getCallback();
				callback.error(ActivityCallback.ASSERT_ERROR, re);
			}
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
