package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;

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
        super(ProviderHacks.getNetworkManager());
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

	public HandshakeStatus allowConnection(HandshakeResponse hr) {
		return HandshakeStatus.OK;
	}
    
    public String toString() {
        return "ConnectionManagerStub";
    }
}
