package com.limegroup.gnutella.udpconnect;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
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
	private volatile WeakReference[] /* ConnectionPair */  _connections;

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
		_connections       = new WeakReference[256];
		_lastConnectionID  = 0;
        NIODispatcher.instance().addPollable(this);
    }
    
    /**
     * Determines if we're connected to the given host.
     */
    public boolean isConnectedTo(InetAddress host) {
        WeakReference[] array = _connections;
        
        if(_lastConnectionID == 0)
            return false;
        for(int i = 0; i < array.length; i++) {
            WeakReference conRef = array[i];
            if(conRef != null) {
                ConnectionPair pair = (ConnectionPair)conRef.get();
                if(pair != null && host.equals(pair.connection.getInetAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *  Register a UDPConnectionProcessor for receiving incoming events and 
	 *  return the assigned connectionID;
     */
	public synchronized byte register(UDPConnectionProcessor con, UDPSelectionKey key) {
		int connID;
		
		WeakReference[] copy = new WeakReference[_connections.length];
		for (int i= 0 ; i< _connections.length;i++) 
		    copy[i] = _connections[i];
		
		for (int i = 1; i <= copy.length; i++) { 
			connID = (_lastConnectionID + i) % 256;

			// We don't assign zero.
			if ( connID == 0 )
				continue;

			// If the slot is open, take it.
			if (copy[connID] == null || copy[connID].get()==null) {
				_lastConnectionID = connID;
                ConnectionPair pair = new ConnectionPair(key, con);
				copy[connID] = new WeakReference(pair);
				_connections=copy;
				return (byte) connID;
			}
		}
		return UNASSIGNED_SLOT;
	}
    
    /**
     * Returns the SelectionKey associated w/ the connection for
     * all connections that are ready for an operation.
     */
    public Set poll() {
        Set selected = null;
        
        // TODO: remove closed connections.
        
        WeakReference[] array = _connections;
        for(int i = 0; i < array.length; i++) {
            if(array[i] == null)
                continue;
            ConnectionPair pair = (ConnectionPair)array[i].get();
            if(pair != null && pair.key != null && (pair.key.readyOps() & pair.key.interestOps()) != 0) {
                if(selected == null)
                    selected = new HashSet(5);
                selected.add(pair.key);
            }
        }
        
        return selected == null ? Collections.EMPTY_SET : selected;
    }

    /**
     *  Route a message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public void routeMessage(UDPConnectionMessage msg, InetAddress senderIP, int senderPort) {
		WeakReference[] array = _connections;
		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instanceof SynMessage ) {
            if(LOG.isDebugEnabled())
                LOG.debug("Receiving SynMessage :"+msg);
            
			for (int i = 1; i < array.length; i++) {
				if (array[i]==null)
					continue;
                
				ConnectionPair pair = (ConnectionPair)array[i].get();
				if ( pair != null && 
					 pair.connection.isConnecting() &&
                     pair.connection.matchAddress(senderIP, senderPort) ) {

                    if(LOG.isDebugEnabled()) 
                        LOG.debug("routeMessage to conn:"+i+" Syn:"+msg);

                    pair.connection.handleMessage(msg);
					break;
				} 
			}
			// Note: eventually these messages should find a match
			// so it is safe to throw away premature ones

		} else if(array[connID] != null) {  // If valid connID then send on to connection
            ConnectionPair pair = (ConnectionPair)array[connID].get();
			if (pair != null && pair.connection.matchAddress(senderIP, senderPort) )
                pair.connection.handleMessage(msg);
		}
	}
    
    private static class ConnectionPair {
        private UDPSelectionKey key;
        private UDPConnectionProcessor connection;
        
        public ConnectionPair(UDPSelectionKey key, UDPConnectionProcessor connection) {
            this.connection = connection;
            this.key = key;
        }
    }
}
