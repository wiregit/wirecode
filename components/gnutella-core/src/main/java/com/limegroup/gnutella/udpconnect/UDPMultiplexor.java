package com.limegroup.gnutella.udpconnect;

import java.lang.ref.WeakReference;
import java.net.*;
import com.limegroup.gnutella.messages.BadPacketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 *  Manage the assignment of connectionIDs and the routing of 
 *  UDPConnectionMessages. 
 */
public class UDPMultiplexor {

    private static final Log LOG =
      LogFactory.getLog(UDPMultiplexor.class);

	/** Keep track of a singleton instance */
    private static UDPMultiplexor     _instance    = null;

	/** The 0 slot is for incoming new connections so it is not assigned */
	public static final byte          UNASSIGNED_SLOT   = 0;

	/** Keep track of the assigned connections */
	private WeakReference[]  _connections;

	/** Keep track of the last assigned connection id so that we can use a 
		circular assignment algorithm.  This should cut down on message
		collisions after the connection is shut down. */
	private int                       _lastConnectionID;

    /**
     *  Return the UDPMultiplexor singleton.
     */
    public static synchronized UDPMultiplexor instance() {
		// Create the singleton if it doesn't yet exist
		if ( _instance == null ) {
			_instance = new UDPMultiplexor();
		}
		return _instance;
    }

    /**
     *  Initialize the UDPMultiplexor.
     */
    private UDPMultiplexor() {
		_connections       = new WeakReference[256];
		_lastConnectionID  = 0;
    }

    /**
     *  Register a UDPConnectionProcessor for receiving incoming events and 
	 *  return the assigned connectionID;
     */
	public synchronized byte register(UDPConnectionProcessor con) {
		int connID;
		for (int i = 1; i <= _connections.length; i++) { 
			connID = (_lastConnectionID + i) % 256;

			// We don't assign zero.
			if ( connID == 0 )
				continue;

			// If the slot is open, take it.
			if (_connections[connID] == null ||
					_connections[connID].get()==null) {
				_lastConnectionID     = connID;
				_connections[connID] = new WeakReference(con);
				return (byte) connID;
			}
		}
		return UNASSIGNED_SLOT;
	}

    /**
     *  Unregister a UDPConnectionProcessor for receiving incoming messages.  
	 *  Free up the slot.
     */
	public synchronized void unregister(UDPConnectionProcessor con) {
		int connID = (int) con.getConnectionID() & 0xff;
		if ( _connections[connID]!=null &&
				_connections[connID].get() == con ) {
		    _connections[connID].clear();
		    _connections[connID]=null;
		}
	}

    /**
     *  Route a message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public synchronized void routeMessage(UDPConnectionMessage msg, 
	  InetAddress senderIP, int senderPort) {

		UDPConnectionProcessor con;

		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instanceof SynMessage ) {
            if(LOG.isDebugEnabled())  {
                LOG.debug("Receiving SynMessage :"+msg);
            }
			for (int i = 1; i < _connections.length; i++) {
				if (_connections[i]==null)
					con=null;
				else
					con = (UDPConnectionProcessor)_connections[i].get();
				if ( con != null && 
					 con.isConnecting() &&
					 con.matchAddress(senderIP, senderPort) ) {

                    if(LOG.isDebugEnabled())  {
                        LOG.debug("routeMessage to conn:"+i+" Syn:"+msg);
                    }

					 con.handleMessage(msg);
					 break;
				} 
			}
			// Note: eventually these messages should find a match
			// so it is safe to throw away premature ones

		} else {  // If valid connID then send on to connection
			if (_connections[connID]==null)
				con=null;
			else
				con = (UDPConnectionProcessor)_connections[connID].get();
			if ( con != null &&
                 con.matchAddress(senderIP, senderPort) ) {
				con.handleMessage(msg);
			}
		}
	}
}
