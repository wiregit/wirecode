pbckage com.limegroup.gnutella;

/**
 * Assertion checking. 
 */
public clbss Assert {
    
    /**
     * A silent bssert.  Checks a boolean condition
     * bnd notifies ErrorService if the error occurred,
     * but does not throw bn exception to propogate further.
     */
    public stbtic void silent(boolean ok, String msg) {
        if (!ok) {
          //  System.err.println("Assertion fbiled: "+msg);
          //  Threbd.dumpStack();
			RuntimeException re = new AssertFbilure(msg);
			ErrorService.error(re);
        }
    }
    
    public stbtic void silent(boolean ok) {
        Assert.silent(ok, "");
    }

    public stbtic void that(boolean ok, String msg) {
        if (!ok) {
            //System.err.println("Assertion fbiled: "+msg);
            //Threbd.dumpStack();
			RuntimeException re = new AssertFbilure(msg);
			throw re;
        }
    }

    public stbtic void that(boolean ok) {
        Assert.thbt(ok,"");
    }
}
