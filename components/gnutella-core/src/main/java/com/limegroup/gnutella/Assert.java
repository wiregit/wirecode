package com.limegroup.gnutella;

/**
 * Assertion checking. 
 */
public class Assert {

	private static volatile ActivityCallback _callback;

	/**
	 * Sets the reference to the ActivityCallback instance.
	 *
	 * @param callback The callback instance
	 */
	public static void setCallback(ActivityCallback callback) {
		_callback = callback;
	}

    public static void that(boolean ok, String msg) {
        if (!ok) {
            System.err.println("Assertion failed: "+msg);
			RuntimeException re = new AssertFailure(msg);
			if(_callback != null) _callback.error(re);
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
