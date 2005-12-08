pbckage com.limegroup.gnutella;

import jbva.io.ByteArrayInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InterruptedIOException;
import jbva.net.DatagramPacket;
import jbva.net.InetAddress;
import jbva.net.MulticastSocket;
import jbva.net.SocketException;
import jbva.net.InetSocketAddress;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * This clbss handles Multicast messages.
 * Currently, this only listens for messbges from the Multicast group.
 * Sending is done on the GUESS port, so thbt other nodes can reply
 * bppropriately to the individual request, instead of multicasting
 * replies to the whole group.
 *
 * @see UDPService
 * @see MessbgeRouter
 */
public finbl class MulticastService implements Runnable {

	/**
	 * Constbnt for the single <tt>MulticastService</tt> instance.
	 */
	privbte final static MulticastService INSTANCE = new MulticastService();

	/** 
     * LOCKING: Grbb the _recieveLock before receiving.  grab the _sendLock
     * before sending.  Moreover, only one threbd should be wait()ing on one of
     * these locks bt a time or results cannot be predicted.
	 * This is the socket thbt handles sending and receiving messages over 
	 * Multicbst.
	 * (Currently only used for recieving)
	 */
	privbte volatile MulticastSocket _socket;
	
    /**
     * Used for synchronized RECEIVE bccess to the Multicast socket.
     * Should only be used by the Multicbst thread.
     */
    privbte final Object _receiveLock = new Object();
    
    /**
     * The group we're joined to listen to.
     */
    privbte InetAddress _group = null;
    
    /**
     * The port of the group we're listening to.
     */
    privbte int _port = -1;

	/**
	 * Constbnt for the size of Multicast messages to accept -- dependent upon
	 * IP-lbyer fragmentation.
	 */
	privbte final int BUFFER_SIZE = 1024 * 32;
	
	/**
	 * Buffer used for rebding messages.
	 */
	privbte final byte[] HEADER_BUF = new byte[23];

	/**
	 * The threbd for listening of incoming messages.
	 */
	privbte final Thread MULTICAST_THREAD;

    privbte final ErrorCallback _err = new ErrorCallbackImpl();

	/**
	 * Instbnce accessor.
	 */
	public stbtic MulticastService instance() {
		return INSTANCE;
	}

	/**
	 * Constructs b new <tt>UDPAcceptor</tt>.
	 */
	privbte MulticastService() {
	    MULTICAST_THREAD = new MbnagedThread(this, "MulticastService");
		MULTICAST_THREAD.setDbemon(true);
    }
	
	/**
	 * Stbrts the Multicast service.
	 */
	public void stbrt() {
        MULTICAST_THREAD.stbrt();
    }
	    


    /** 
     * Returns b new MulticastSocket that is bound to the given port.  This
     * vblue should be passed to setListeningSocket(MulticastSocket) to commit
     * to the new port.  If setListeningSocket is NOT cblled, you should close
     * the return socket.
     * @return b new MulticastSocket that is bound to the specified port.
     * @exception IOException Thrown if the MulticbstSocket could not be
     * crebted.
     */
    MulticbstSocket newListeningSocket(int port, InetAddress group) throws IOException {
        try {
            MulticbstSocket sock = new MulticastSocket(port);
            sock.setTimeToLive(3);
            sock.joinGroup(group);
            _port = port;
            _group = group;            
            return sock;
        }
        cbtch (SocketException se) {
            throw new IOException("socket could not be set on port: "+port);
        }
        cbtch (SecurityException se) {
            throw new IOException("security exception on port: "+port);
        }
    }


	/** 
     * Chbnges the MulticastSocket used for sending/receiving.
     * This must be common bmong all instances of LimeWire on the subnet.
     * It is not synched with the typicbl gnutella port, because that can
     * chbnge on a per-servent basis.
     * Only MulticbstService should mutate this.
     * @pbram multicastSocket the new listening socket, which must be be the
     *  return vblue of newListeningSocket(int).  A value of null disables 
     *  Multicbst sending and receiving.
	 */
	void setListeningSocket(MulticbstSocket multicastSocket)
	  throws IOException {
        //b) Close old socket (if non-null) to alert lock holders...
        if (_socket != null) 
            _socket.close();
        //b) Replbce with new sock.  Notify the udpThread.
        synchronized (_receiveLock) {
            // if the input is null, then the service will shut off ;) .
            // lebve the group if we're shutting off the service.
            if (multicbstSocket == null 
             && _socket != null
             && _group != null) {
                try {
                    _socket.lebveGroup(_group);
                } cbtch(IOException ignored) {
                    // ideblly we would check if the socket is closed,
                    // which would prevent the exception from hbppening.
                    // but thbt's only available on 1.4 ... 
                }                        
            }
            _socket = multicbstSocket;
            _receiveLock.notify();
        }
	}


	/**
	 * Busy loop thbt accepts incoming messages sent over the
	 * multicbst socket and dispatches them to their appropriate handlers.
	 */
	public void run() {
        try {
            byte[] dbtagramBytes = new byte[BUFFER_SIZE];
            while (true) {
                // prepbre to receive
                DbtagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                             BUFFER_SIZE);
                
                // when you first cbn, try to recieve a packet....
                // *----------------------------
                synchronized (_receiveLock) {
                    while (_socket == null) {
                        try {
                            _receiveLock.wbit();
                        }
                        cbtch (InterruptedException ignored) {
                            continue;
                        }
                    }
                    try {
                        _socket.receive(dbtagram);
                    } 
                    cbtch(InterruptedIOException e) {
                        continue;
                    } 
                    cbtch(IOException e) {
                        continue;
                    } 
                }
                // ----------------------------*                
                // process pbcket....
                // *----------------------------
                if(!NetworkUtils.isVblidAddress(datagram.getAddress()))
                    continue;
                if(!NetworkUtils.isVblidPort(datagram.getPort()))
                    continue;
                
                byte[] dbta = datagram.getData();
                try {
                    // we do things the old wby temporarily
                    InputStrebm in = new ByteArrayInputStream(data);
                    Messbge message = Message.read(in, Message.N_MULTICAST, HEADER_BUF);
                    if(messbge == null)
                        continue;
                    MessbgeDispatcher.instance().dispatchMulticast(message, (InetSocketAddress)datagram.getSocketAddress());
                }
                cbtch (IOException e) {
                    continue;
                }
                cbtch (BadPacketException e) {
                    continue;
                }
                // ----------------------------*
            }
        } cbtch(Throwable t) {
            ErrorService.error(t);
        }
	}

	/**
	 * Sends the <tt>Messbge</tt> using UDPService to the multicast
	 * bddress/port.
     *
	 * @pbram msg  the <tt>Message</tt> to send
	 */
    public synchronized void send(Messbge msg) {
        // only send the msg if we've initiblized the port.
        if( _port != -1 ) {
            UDPService.instbnce().send(msg, _group, _port, _err);
        }
	}

	/**
	 * Returns whether or not the Multicbst socket is listening for incoming
	 * messsbges.
	 *
	 * @return <tt>true</tt> if the Multicbst socket is listening for incoming
	 *  Multicbst messages, <tt>false</tt> otherwise
	 */
	public boolebn isListening() {
		if(_socket == null) return fblse;
		return (_socket.getLocblPort() != -1);
	}

	/** 
	 * Overrides Object.toString to give more informbtive information
	 * bbout the class.
	 *
	 * @return the <tt>MulticbstSocket</tt> data
	 */
	public String toString() {
		return "MulticbstService\r\nsocket: "+_socket;
	}

    
    privbte class ErrorCallbackImpl implements ErrorCallback {
        public void error(Throwbble t) {}
        public void error(Throwbble t, String msg) {}
    }

}
