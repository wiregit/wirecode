package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Pollable;

/** 
 *  Manage the assignment of connectionIDs and the routing of 
 *  UDPConnectionMessages. 
 */
public class UDPMultiplexor implements Pollable {

    private static final Log LOG =
      LogFactory.getLog(UDPMultiplexor.class);

	/** Keep track of a singleton instance */
    private static UDPMultiplexor     _instance    = new UDPMultiplexor();

	/** The 0 slot is for incoming new connections so it is not assigned */
	public static final byte          UNASSIGNED_SLOT   = 0;

	/** Keep track of the assigned connections */
	private volatile UDPConnectionProcessor[] _connections;

	/** Keep track of the last assigned connection id so that we can use a 
		circular assignment algorithm.  This should cut down on message
		collisions after the connection is shut down. */
	private int                       _lastConnectionID;

    /**
     *  Return the UDPMultiplexor singleton.
     */
    public static UDPMultiplexor instance() {
		return _instance;
    }      

    /**
     *  Initialize the UDPMultiplexor.
     */
    private UDPMultiplexor() {
		_connections       = new UDPConnectionProcessor[256];
		_lastConnectionID  = 0;
        NIODispatcher.instance().addPollable(this);
    }
    
    /**
     * Determines if we're connected to the given host.
     */
    public boolean isConnectedTo(InetAddress host) {
        UDPConnectionProcessor[] array = _connections;

        if (_lastConnectionID == 0)
            return false;
        for (int i = 0; i < array.length; i++) {
            UDPConnectionProcessor con = array[i];
            if (con != null && host.equals(con.getSocketAddress().getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register a UDPConnectionProcessor for receiving incoming events and return the assigned connectionID;
     */
    public synchronized byte register(UDPConnectionProcessor con) throws IOException {
        int connID;

        UDPConnectionProcessor[] copy = new UDPConnectionProcessor[_connections.length];
        for (int i = 0; i < _connections.length; i++)
            copy[i] = _connections[i];

        for (int i = 1; i <= copy.length; i++) {
            connID = (_lastConnectionID + i) % 256;

            // We don't assign zero.
            if (connID == 0)
                continue;

            // If the slot is open, take it.
            if (copy[connID] == null) {
                _lastConnectionID = connID;
                copy[connID] = con;
                _connections = copy;
                return (byte) connID;
            }
        }

        throw new IOException("no room for connection");
    }
    
    /**
     * Returns the SelectionKey associated w/ the connection for
     * all connections that are ready for an operation.
     */
    public Set poll() {
        Set selected = null;
        
        UDPConnectionProcessor[] array = _connections;
        UDPConnectionProcessor[] removed = null;
        
        for(int i = 0; i < array.length; i++) {
            UDPConnectionProcessor con = (UDPConnectionProcessor)array[i];
            if(con == null)
                continue;
            
            SelectionKey key = con.getChannel().keyFor(null);
            if(key != null) {
                if(key.isValid()) {
                    if ((key.readyOps() & key.interestOps()) != 0) {
                        if (selected == null)
                            selected = new HashSet(5);
                        selected.add(key);
                    }
                } else {
                    if(removed == null)
                        removed = new UDPConnectionProcessor[array.length];
                    removed[i] = con;
                }
            }
        }
        
        // Go through the removed list & remove them from _connections.
        // _connections may have changed (since we didn't lock while polling),
        // so we need to check and ensure the given UDPConnectionProcessor
        // is the same.
        if (removed != null) {
            synchronized (this) {
                UDPConnectionProcessor[] copy = new UDPConnectionProcessor[_connections.length];
                for (int i = 0; i < _connections.length; i++) {
                    if(_connections[i] == removed[i])
                        copy[i] = null;
                    else
                        copy[i] = _connections[i];
                }
                _connections = copy;
            }
        }
        
        return selected == null ? Collections.EMPTY_SET : selected;
    }

    /**
     *  Route a message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public void routeMessage(UDPConnectionMessage msg, InetSocketAddress addr) {
        UDPConnectionProcessor[] array = _connections;
		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instanceof SynMessage ) {
            if(LOG.isDebugEnabled())
                LOG.debug("Receiving SynMessage :"+msg);
            
			for (int i = 1; i < array.length; i++) {
                UDPConnectionProcessor conn = (UDPConnectionProcessor)array[i];
                if(conn == null)
                    continue;
                
				if ( conn.isConnecting() && conn.matchAddress(addr) ) {
                    if(LOG.isDebugEnabled()) 
                        LOG.debug("routeMessage to conn:"+i+" Syn:"+msg);

                    conn.handleMessage(msg);
					break;
				} 
			}
			// Note: eventually these messages should find a match
			// so it is safe to throw away premature ones

		} else if(array[connID] != null) {  // If valid connID then send on to connection
            UDPConnectionProcessor conn = (UDPConnectionProcessor)array[connID];
			if (conn.matchAddress(addr) )
                conn.handleMessage(msg);
		}
	}
}
