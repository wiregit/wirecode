package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;

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
        super(null);
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

	public boolean allowConnection(HandshakeResponse hr) {
		return true;
	}
    
    public String toString() {
        return "ConnectionManagerStub";
    }
}
