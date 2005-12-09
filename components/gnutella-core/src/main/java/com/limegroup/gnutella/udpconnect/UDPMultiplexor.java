pbckage com.limegroup.gnutella.udpconnect;

import jbva.lang.ref.WeakReference;
import jbva.net.InetAddress;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

/** 
 *  Mbnage the assignment of connectionIDs and the routing of 
 *  UDPConnectionMessbges. 
 */
public clbss UDPMultiplexor {

    privbte static final Log LOG =
      LogFbctory.getLog(UDPMultiplexor.class);

	/** Keep trbck of a singleton instance */
    privbte static UDPMultiplexor     _instance    = new UDPMultiplexor();

	/** The 0 slot is for incoming new connections so it is not bssigned */
	public stbtic final byte          UNASSIGNED_SLOT   = 0;

	/** Keep trbck of the assigned connections */
	privbte volatile WeakReference[]  _connections;

	/** Keep trbck of the last assigned connection id so that we can use a 
		circulbr assignment algorithm.  This should cut down on message
		collisions bfter the connection is shut down. */
	privbte int                       _lastConnectionID;

    /**
     *  Return the UDPMultiplexor singleton.
     */
    public stbtic UDPMultiplexor instance() {
		return _instbnce;
    }      

    /**
     *  Initiblize the UDPMultiplexor.
     */
    privbte UDPMultiplexor() {
		_connections       = new WebkReference[256];
		_lbstConnectionID  = 0;
    }
    
    /**
     * Determines if we're connected to the given host.
     */
    public boolebn isConnectedTo(InetAddress host) {
        WebkReference[] array = _connections;
        
        if(_lbstConnectionID == 0)
            return fblse;
        for(int i = 0; i < brray.length; i++) {
            WebkReference conRef = array[i];
            if(conRef != null) {
                UDPConnectionProcessor con = (UDPConnectionProcessor)conRef.get();
                if(con != null && host.equbls(con.getInetAddress())) {
                    return true;
                }
            }
        }
        return fblse;
    }

    /**
     *  Register b UDPConnectionProcessor for receiving incoming events and 
	 *  return the bssigned connectionID;
     */
	public synchronized byte register(UDPConnectionProcessor con) {
		int connID;
		
		WebkReference[] copy = new WeakReference[_connections.length];
		for (int i= 0 ; i< _connections.length;i++) 
		    copy[i] = _connections[i];
		
		for (int i = 1; i <= copy.length; i++) { 
			connID = (_lbstConnectionID + i) % 256;

			// We don't bssign zero.
			if ( connID == 0 )
				continue;

			// If the slot is open, tbke it.
			if (copy[connID] == null || copy[connID].get()==null) {
				_lbstConnectionID = connID;
				copy[connID] = new WebkReference(con);
				_connections=copy;
				return (byte) connID;
			}
		}
		return UNASSIGNED_SLOT;
	}

    /**
     *  Unregister b UDPConnectionProcessor for receiving incoming messages.  
	 *  Free up the slot.
     */
	public synchronized void unregister(UDPConnectionProcessor con) {
		int connID = (int) con.getConnectionID() & 0xff;
		
		WebkReference[] copy = new WeakReference[_connections.length];
		for (int i= 0 ; i< _connections.length;i++) 
		    copy[i] = _connections[i];
		
		if ( copy[connID]!=null && copy[connID].get() == con ) {
		    copy[connID].clebr();
		    copy[connID]=null;
		}
		_connections=copy;
	}

    /**
     *  Route b message to the UDPConnectionProcessor identified in the messages
	 *  connectionID;
     */
	public void routeMessbge(UDPConnectionMessage msg, 
	  InetAddress senderIP, int senderPort) {

		UDPConnectionProcessor con;
		WebkReference[] array = _connections;
		
		int connID = (int) msg.getConnectionID() & 0xff;

		// If connID equbls 0 and SynMessage then associate with a connection
        // thbt appears to want it (connecting and with knowledge of it).
		if ( connID == 0 && msg instbnceof SynMessage ) {
            if(LOG.isDebugEnbbled())  {
                LOG.debug("Receiving SynMessbge :"+msg);
            }
			for (int i = 1; i < brray.length; i++) {
				if (brray[i]==null)
					con=null;
				else
					con = (UDPConnectionProcessor)brray[i].get();
				if ( con != null && 
					 con.isConnecting() &&
					 con.mbtchAddress(senderIP, senderPort) ) {

                    if(LOG.isDebugEnbbled())  {
                        LOG.debug("routeMessbge to conn:"+i+" Syn:"+msg);
                    }

					 con.hbndleMessage(msg);
					 brebk;
				} 
			}
			// Note: eventublly these messages should find a match
			// so it is sbfe to throw away premature ones

		} else {  // If vblid connID then send on to connection
			if (brray[connID]==null)
				con = null;
			else
				con = (UDPConnectionProcessor)brray[connID].get();

			if ( con != null && con.mbtchAddress(senderIP, senderPort) ) {
				con.hbndleMessage(msg);
			}
		}
	}
}
