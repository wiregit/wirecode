package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.handshaking.HandshakeResponse;

import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.*;

/** A (incomplete!) stub for ConnectionManager. */
public class ConnectionManagerStub extends ConnectionManager {
	
    boolean enableRemove=false;
    
    boolean _isSupernode = true;
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
    
    public void properRemove(ManagedConnection c) {
    	super.remove(c);
    }
    
    public boolean isSupernode() {
        return _isSupernode;
    }        

    /**
     * allows to set the supernode status
     * @param yes true if we want this stub to say its a supernode.
     */
    public void setSupernode(boolean yes) {
    	_isSupernode=yes;
    }
    
    
    /**
     * setter for the list of ultrapeer connections
     * @param list of <tt> Connection </tt> objects.
     */
    public void setInitializedConnections(List list) {
    	try {
    		PrivilegedAccessor.setValue(this,"_initializedConnections",list);
    	}catch(Exception e) { //should not happen, ever.
    		throw new RuntimeException(e.getMessage());
    	}
    }
    
    /**
     * setter for the list of leaf connections
     * @param list of <tt> Connection </tt> objects.
     */
    public void setInitializedClientConnections(List list) {
    	try {
    		PrivilegedAccessor.setValue(this,"_initializedClientConnections",list);
    	}catch(Exception e) { //should not happen, ever.
    		throw new RuntimeException(e.getMessage());
    	}
    }
    
	public boolean allowConnection(HandshakeResponse hr) {
		return true;
	}
    
    public String toString() {
        return "ConnectionManagerStub";
    }
}
