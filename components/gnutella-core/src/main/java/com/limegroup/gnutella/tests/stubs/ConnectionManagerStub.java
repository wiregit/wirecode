package com.limegroup.gnutella.tests.stubs;

import com.limegroup.gnutella.*;

/** A (incomplete!) stub for ConnectionManager. */
public class ConnectionManagerStub extends ConnectionManager {
    boolean enableRemove=false;

    /** Same as this(false) */
    public ConnectionManagerStub() {
        this(false);
    }

    /** @param enableRemove true if remove(c) should do work
     *  @see remove */
    public ConnectionManagerStub(boolean enableRemove) {
        super(null, null);
        this.enableRemove=enableRemove;
    }

    /** Calls c.close iff enableRemove */
    public void remove(ManagedConnection c) {
        if (enableRemove) 
            c.close();
    }
    
    public boolean isSupernode() {
        return true;
    }        

    public boolean allowConnection(boolean outgoing,
                                   String ultrapeerHeader,
                                   String useragentHeader) {
        //Needed to make ConnectionManagerTest pass. 
        //See ConnectionManagerTest.setUp.
        return true;
    }
}
