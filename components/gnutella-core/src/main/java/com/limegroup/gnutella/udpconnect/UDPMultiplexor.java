package com.limegroup.gnutella.udpconnect;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

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
    private static UDPMultiplexor     _instance    = new UDPMultiplexor();

	/** The 0 slot is for incoming new connections so it is not assigned */
	public static final byte          UNASSIGNED_SLOT   = 0;

	/** Keep track of the assigned connections */
	private volatile WeakReference[]  _connections;

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
                UDPConnectionProcessor con = (UDPConnectionProcessor)conRef.get();
                if(con != null && host.equals(con.getInetAddress())) {
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
	public synchronized byte register(UDPConnectionProcessor con) {
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
				copy[connID] = new WeakReference(con);
				_connections=copy;
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
		
		WeakReference[] copy = new WeakReference[_connections.length];
		for (int i= 0 ; i< _connections.length;i++) 
		    copy[i] = _connections[i];
		
		if ( copy[connID]!=null && copy[connID].get() == con ) {
		    copy[connID].clear();
		    copy[connID]=null;
		}
		_connections=copy;
	}

    /**
     *  Route a message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public void routeMessage(UDPConnectionMessage msg, 
	  InetAddress senderIP, int senderPort) {

		UDPConnectionProcessor con;
		WeakReference[] array = _connections;
		
		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instanceof SynMessage ) {
            if(LOG.isDebugEnabled())  {
                LOG.debug("Receiving SynMessage :"+msg);
            }
			for (int i = 1; i < array.length; i++) {
				if (array[i]==null)
					con=null;
				else
					con = (UDPConnectionProcessor)array[i].get();
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
			if (array[connID]==null)
				con = null;
			else
				con = (UDPConnectionProcessor)array[connID].get();

			if ( con != null && con.matchAddress(senderIP, senderPort) ) {
				con.handleMessage(msg);
			}
		}
	}
}
