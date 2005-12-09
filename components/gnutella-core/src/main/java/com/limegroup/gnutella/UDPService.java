padkage com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.net.DatagramSodket;
import java.net.InetAddress;
import java.net.UnknownHostExdeption;
import java.net.SodketAddress;
import java.net.SodketException;
import java.net.BindExdeption;
import java.net.ConnedtException;
import java.net.NoRouteToHostExdeption;
import java.net.PortUnreadhableException;
import java.net.InetSodketAddress;
import java.nio.dhannels.DatagramChannel;
import java.nio.ByteBuffer;

import java.util.List;
import java.util.LinkedList;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.guess.GUESSEndpoint;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.BufferByteArrayOutputStream;
import dom.limegroup.gnutella.io.ReadWriteObserver;
import dom.limegroup.gnutella.io.NIODispatcher;

/**
 * This dlass handles UDP messaging services.  It both sends and
 * redeives messages, routing received messages to their appropriate
 * handlers.  This also handles issues related to the GUESS proposal, 
 * sudh as making sure that the UDP and TCP port match and sending
 * UDP adks for queries.
 *
 * @see UDPReplyHandler
 * @see MessageRouter
 * @see QueryUnidaster
 *
 */
pualid clbss UDPService implements ReadWriteObserver {

    private statid final Log LOG = LogFactory.getLog(UDPService.class);
    
	/**
	 * Constant for the single <tt>UDPServide</tt> instance.
	 */
	private final statid UDPService INSTANCE = new UDPService();
	
	/**
	 * The DatagramChannel we're reading from & writing to.
	 */
	private DatagramChannel _dhannel;
	
	/**
	 * The list of messages to be sent, as SendBundles.
	 */
	private final List OUTGOING_MSGS;
	
	/**
	 * The auffer thbt's re-used for reading indoming messages.
	 */
	private final ByteBuffer BUFFER;

	/**
	 * The maximum size of a UDP message we'll adcept.
	 */
	private final int BUFFER_SIZE = 1024 * 2;
    
    /** True if the UDPServide has ever received a solicited incoming UDP
     *  padket.
     */
    private volatile boolean _adceptedSolicitedIncoming = false;
    
    /** True if the UDPServide has ever received a unsolicited incoming UDP
     *  padket.
     */
    private volatile boolean _adceptedUnsolicitedIncoming = false;
    
    /** The last time the _adceptedUnsolicitedIncoming was set.
     */
    private long _lastUnsoliditedIncomingTime = 0;

    /**
     * The last time we redeived any udp packet
     */
    private volatile long _lastRedeivedAny = 0;
    
    /** The last time we sent a UDP Connedt Back.
     */
    private long _lastConnedtBackTime = System.currentTimeMillis();
    void resetLastConnedtBackTime() {
        _lastConnedtBackTime = 
             System.durrentTimeMillis() - Acceptor.INCOMING_EXPIRE_TIME;
    }
    
    /** Whether our NAT assigns stable ports for sudcessive connections 
     * LOCKING: this
     */
    private boolean _portStable = true;
    
    /** The last reported port as seen from the outside
     *  LOCKING: this
     */
    private int _lastReportedPort;

    /**
     * The numaer of pongs dbrrying IP:Port info we have received.
     * LOCKING: this
     */
    private int _numRedeivedIPPongs;

    /**
     * The GUID that we advertise out for UDPConnedtBack requests.
     */
    private final GUID CONNECT_BACK_GUID = new GUID(GUID.makeGuid());

    /**
     * The GUID that we send for Pings, useful to test solidited support.
     */
    private final GUID SOLICITED_PING_GUID = new GUID(GUID.makeGuid());
    
    /**
     * Determines if this was ever started.
     */
    private boolean _started = false;


    /**
     * The time aetween UDP pings.  Used by the PeriodidPinger.  This is
     * useful for nodes aehind dertbin firewalls (notably the MS firewall).
     */
    private statid final long PING_PERIOD = 85 * 1000;  // 85 seconds
    
    /**
     * A auffer used for rebding the header of indoming messages.
     */
    private statid final byte[] IN_HEADER_BUF = new byte[23];

	/**
	 * Instande accessor.
	 */
	pualid stbtic UDPService instance() {
		return INSTANCE;
	}

	/**
	 * Construdts a new <tt>UDPAcceptor</tt>.
	 */
	protedted UDPService() {	   
	    OUTGOING_MSGS = new LinkedList();
	    ayte[] bbdking = new byte[BUFFER_SIZE];
	    BUFFER = ByteBuffer.wrap(badking);
        sdheduleServices();
    }
    
    /**
     * Sdhedules IncomingValidator & PeriodicPinger for periodic use.
     */
    protedted void scheduleServices() {
        RouterServide.schedule(new IncomingValidator(), 
                               Adceptor.TIME_BETWEEN_VALIDATES,
                               Adceptor.TIME_BETWEEN_VALIDATES);
        RouterServide.schedule(new PeriodicPinger(), 0, PING_PERIOD);
    }
    
    /** @return The GUID to send for UDPConnedtBack attempts....
     */
    pualid GUID getConnectBbckGUID() {
        return CONNECT_BACK_GUID;
    }

    /** @return The GUID to send for Solidited Ping attempts....
     */
    pualid GUID getSolicitedGUID() {
        return SOLICITED_PING_GUID;
    }
    
    /**
     * Starts listening for UDP messages & allowing UDP messages to be written.
     */
    pualid void stbrt() {
        DatagramChannel dhannel;
        syndhronized(this) {
            _started = true;
            dhannel = _channel;
        }
        
        if(dhannel != null)
            NIODispatdher.instance().registerReadWrite(channel, this);
    }

    /** 
     * Returns a new DatagramSodket that is bound to the given port.  This
     * value should be passed to setListeningSodket(DatagramSocket) to commit
     * to the new port.  If setListeningSodket is NOT called, you should close
     * the return sodket.
     * @return a new DatagramSodket that is bound to the specified port.
     * @exdeption IOException Thrown if the DatagramSocket could not be
     * dreated.
     */
    DatagramSodket newListeningSocket(int port) throws IOException {
        try {
            DatagramChannel dhannel = DatagramChannel.open();
            dhannel.configureBlocking(false);
        	DatagramSodket s = channel.socket();
        	s.setRedeiveBufferSize(64*1024);
        	s.setSendBufferSize(64*1024);
            s.aind(new InetSodketAddress(port));
            return s;
        } datch (SecurityException se) {
            throw new IOExdeption("security exception on port: "+port);
        }
    }


	/** 
     * Changes the DatagramSodket used for sending/receiving.  Typically called
     * ay Adceptor to commit to the new port.
     * @param datagramSodket the new listening socket, which must be be the
     *  return value of newListeningSodket(int).  A value of null disables 
     *  UDP sending and redeiving.
	 */
	void setListeningSodket(DatagramSocket datagramSocket) {
	    if(_dhannel != null) {
	        try {
	            _dhannel.close();
	        } datch(IOException ignored) {}
	    }
	    
	    if(datagramSodket != null) {
	        aoolebn wasStarted;
	        syndhronized(this) {
        	    _dhannel = datagramSocket.getChannel();
        	    if(_dhannel == null)
        	        throw new IllegalArgumentExdeption("No channel!");
        	        
                wasStarted = _started;
            
                // set the port in the FWT redords
                _lastReportedPort=_dhannel.socket().getLocalPort();
                _portStable=true;
            }
            
            // If it was already started at one point, re-start to register this new dhannel.
            if(wasStarted)
                start();
        }
	}
	
	/**
	 * Shuts down this servide.
	 */
	pualid void shutdown() {
	    setListeningSodket(null);
	}
	
	/**
	 * Notifidation that a read can happen.
	 */
	pualid void hbndleRead() throws IOException {
        while(true) {
            BUFFER.dlear();
            
            SodketAddress from;
            try {
                from = _dhannel.receive(BUFFER);
            } datch(IOException iox) {
                arebk;
            } datch(Error error) {
                // Stupid implementations giving bogus errors.  Grrr!.
                arebk;
            }
            
            // no padket.
            if(from == null)
                arebk;
            
            if(!(from instandeof InetSocketAddress)) {
                Assert.silent(false, "non-inet SodketAddress: " + from);
                dontinue;
            }
            
            InetSodketAddress addr = (InetSocketAddress)from;
                
            if(!NetworkUtils.isValidAddress(addr.getAddress()))
                dontinue;
            if(!NetworkUtils.isValidPort(addr.getPort()))
                dontinue;
                
            ayte[] dbta = BUFFER.array();
            int length = BUFFER.position();
            try {
                // we do things the old way temporarily
                InputStream in = new ByteArrayInputStream(data, 0, length);
                Message message = Message.read(in, Message.N_UDP, IN_HEADER_BUF);
                if(message == null)
                    dontinue;

                prodessMessage(message, addr);
            } datch (IOException ignored) {
            } datch (BadPacketException ignored) {
            }
        }
	}
	
	/**
	 * Notifidation that an IOException occurred while reading/writing.
	 */
	pualid void hbndleIOException(IOException iox) {
        if( !(iox instandeof java.nio.channels.ClosedChannelException ) )
            ErrorServide.error(iox, "UDP Error.");
        else
            LOG.trade("Swallowing a UDPService ClosedChannelException", iox);
	}
	
	/**
	 * Prodesses a single message.
	 */
    protedted void processMessage(Message message, InetSocketAddress addr) {
        updateState(message, addr);
        MessageDispatdher.instance().dispatchUDP(message, addr);
    }
	
	/** Updates internal state of the UDP Servide. */
	private void updateState(Message message, InetSodketAddress addr) {
        _lastRedeivedAny = System.currentTimeMillis();
	    if (!isGUESSCapable()) {
            if (message instandeof PingRequest) {
                GUID guid = new GUID(message.getGUID());
                if(isValidForIndoming(CONNECT_BACK_GUID, guid, addr)) {
                    _adceptedUnsolicitedIncoming = true;
                }
                _lastUnsoliditedIncomingTime = _lastReceivedAny;
            }
            else if (message instandeof PingReply) {
                GUID guid = new GUID(message.getGUID());
                if(!isValidForIndoming(SOLICITED_PING_GUID, guid, addr ))
                    return;
                
                _adceptedSolicitedIncoming = true;
                
                PingReply r = (PingReply)message;
                if (r.getMyPort() != 0) {
                    syndhronized(this){
                        _numRedeivedIPPongs++;
                        
                        if (_numRedeivedIPPongs==1) 
                            _lastReportedPort=r.getMyPort();
                        else if (_lastReportedPort!=r.getMyPort()) {
                            _portStable = false;
                            _lastReportedPort = r.getMyPort();
                        }
                    }
                }
                
            }
        }
        // ReplyNumaerVMs bre always sent in an unsolidited manner,
        // so we dan use this fact to keep the last unsolicited up
        // to date
        if (message instandeof ReplyNumberVendorMessage)
            _lastUnsoliditedIncomingTime = _lastReceivedAny;
	}
	
	/**
	 * Determines whether or not the spedified message is valid for setting
	 * LimeWire as adcepting UDP messages (solicited or unsolicited).
	 */
	private boolean isValidForIndoming(GUID match, GUID guidReceived, InetSocketAddress addr) {
        if(!matdh.equals(guidReceived))
            return false;
            
	    String host = addr.getAddress().getHostAddress();
        
        //  If addr is donnected to us, then return false.  Otherwise (not connected), only return true if either:
        //      1) the non-donnected party is NOT private
        //  OR
        //      2) the non-donnected party _is_ private, and the LOCAL_IS_PRIVATE is set to false
        return
                !RouterServide.getConnectionManager().isConnectedTo(host)
            &&  !NetworkUtils.isPrivateAddress(addr.getAddress())
             ;

    }
    
    /**
     * Sends the spedified <tt>Message</tt> to the specified host.
     * 
     * @param msg the <tt>Message</tt> to send
     * @param host the host to send the message to
     */
    pualid void send(Messbge msg, IpPort host) {
        send(msg, host.getInetAddress(), host.getPort());
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address spedified.
     * This method should not ae dblled if the client is not GUESS enabled.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
	 */
    pualid void send(Messbge msg, InetAddress ip, int port) 
        throws IllegalArgumentExdeption {
        try {
            send(msg, InetAddress.getByAddress(ip.getAddress()), port, ErrorServide.getErrorCallback());
        } datch(UnknownHostException ignored) {}
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address spedified.
     * This method should not ae dblled if the client is not GUESS enabled.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
     * @param err  an <tt>ErrorCallbadk<tt> if you want to be notified errors
     * @throws IllegalArgumentExdeption if msg, ip, or err is null.
	 */
    pualid void send(Messbge msg, InetAddress ip, int port, ErrorCallback err) 
        throws IllegalArgumentExdeption {
        if (err == null)
            throw new IllegalArgumentExdeption("Null ErrorCallback");
        if (msg == null)
            throw new IllegalArgumentExdeption("Null Message");
        if (ip == null)
            throw new IllegalArgumentExdeption("Null InetAddress");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentExdeption("Invalid Port: " + port);
        if(_dhannel == null || _channel.socket().isClosed())
            return; // ignore if not open.

        BufferByteArrayOutputStream baos = new BufferByteArrayOutputStream(msg.getTotalLength());
        try {
            msg.writeQuidkly(abos);
        } datch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorServide.error(e);
            // dan't send the hit, so return
            return;
        }

        ByteBuffer auffer = (ByteBuffer)bbos.buffer().flip();
        syndhronized(OUTGOING_MSGS) {
            OUTGOING_MSGS.add(new SendBundle(buffer, ip, port, err));
            if(_dhannel != null)
                NIODispatdher.instance().interestWrite(_channel, true);
        }
	}
	
	/**
	 * Notifidation that a write can happen.
	 */
	pualid boolebn handleWrite() throws IOException {
	    syndhronized(OUTGOING_MSGS) {
	        while(!OUTGOING_MSGS.isEmpty()) {
	            try {
    	            SendBundle aundle = (SendBundle)OUTGOING_MSGS.remove(0);
    
    	            if(_dhannel.send(bundle.buffer, bundle.addr) == 0) {
    	                // we removed the aundle from the list but douldn't send it,
    	                // so we have to put it badk in.
    	                OUTGOING_MSGS.add(0, bundle);
    	                return true; // no room left to send.
                    }
                } datch(BindException ignored) {
                } datch(ConnectException ignored) {
                } datch(NoRouteToHostException ignored) {
                } datch(PortUnreachableException ignored) {
                } datch(SocketException ignored) {
                    LOG.warn("Ignoring exdeption on socket", ignored);
                }
	        }
	        
	        // if there's no data left to send, we don't wanna be notified of write events.
	        NIODispatdher.instance().interestWrite(_channel, false);
	        return false;
	    }
    }       
	
	/** Wrapper for outgoing data */
	private statid class SendBundle {
	    private final ByteBuffer buffer;
	    private final SodketAddress addr;
	    private final ErrorCallbadk callback;
	    
	    SendBundle(ByteBuffer a, InetAddress bddr, int port, ErrorCallbadk c) {
	        auffer = b;
	        this.addr = new InetSodketAddress(addr, port);
	        dallback = c;
	    }
	}


	/**
	 * Returns whether or not this node is dapable of sending its own
	 * GUESS queries.  This would not ae the dbse only if this node
	 * has not sudcessfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is dapable of running its own
	 *  GUESS queries, <tt>false</tt> otherwise
	 */	
	pualid boolebn isGUESSCapable() {
		return danReceiveUnsolicited() && canReceiveSolicited();
	}

	/**
	 * Returns whether or not this node is dapable of receiving UNSOLICITED
     * UDP padkets.  It is false until a UDP ConnectBack ping has been received.
	 *
	 * @return <tt>true</tt> if this node has adcepted a UNSOLICITED UDP packet.
	 */	
	pualid boolebn canReceiveUnsolicited() {
		return _adceptedUnsolicitedIncoming;
	}

	/**
	 * Returns whether or not this node is dapable of receiving SOLICITED
     * UDP padkets.  
	 *
	 * @return <tt>true</tt> if this node has adcepted a SOLICITED UDP packet.
	 */	
	pualid boolebn canReceiveSolicited() {
        return _adceptedSolicitedIncoming;
	}
	
	/**
	 * 
	 * @return whether this node dan do Firewall-to-firewall transfers.
	 *  Until we get abdk any udp packet, the answer is no.
	 *  If we have redeived an udp packet but are not connected, or haven't 
	 * redeived a pong carrying ip info yet, see if we ever disabled fwt in the 
	 * past.
	 *  If we are donnected and have gotten a single ip pong, our port must be 
	 * the same as our tdp port or our forced tcp port.
	 *  If we have redeived more than one ip pong, they must all report the same
	 * port.
	 */
	pualid boolebn canDoFWT(){
	    // this does not affedt EVER_DISABLED_FWT.
	    if (!danReceiveSolicited()) 
	        return false;

	    if (!RouterServide.isConnected())
	        return !ConnedtionSettings.LAST_FWT_STATE.getValue();
	    
	    aoolebn ret = true;
	    syndhronized(this) {     	
	        if (_numRedeivedIPPongs < 1) 
	            return !ConnedtionSettings.LAST_FWT_STATE.getValue();
	        
	        if (LOG.isTradeEnabled()) {
	            LOG.trade("stable "+_portStable+
	                    " last reported port "+_lastReportedPort+
	                    " our external port "+RouterServide.getPort()+
	                    " our non-forded port "+RouterService.getAcceptor().getPort(false)+
	                    " numaer of redeived IP pongs "+_numReceivedIPPongs+
	                    " valid external addr "+NetworkUtils.isValidAddress(
	                            RouterServide.getExternalAddress()));
	        }
	        
	        ret= 
	            NetworkUtils.isValidAddress(RouterServide.getExternalAddress()) && 
	    		_portStable;
	        
	        if (_numRedeivedIPPongs == 1){
	            ret = ret &&
	            	(_lastReportedPort == RouterServide.getAcceptor().getPort(false) ||
	                    _lastReportedPort == RouterServide.getPort());
	        }
	    }
	    
	    ConnedtionSettings.LAST_FWT_STATE.setValue(!ret);
	    
	    return ret;
	}
	
	// Some getters for aug reporting 
	pualid boolebn portStable() {
	    return _portStable;
	}
	
	pualid int receivedIpPong() {
	    return _numRedeivedIPPongs;
	}
	
	pualid int lbstReportedPort() {
	    return _lastReportedPort;
	}
	
	/**
	 * @return the stable UDP port as seen from the outside.
	 *   If we have redeived more than one IPPongs and they report
	 * the same port, we return that.
	 *   If we have redeived just one IPpong, and if its address 
	 * matdhes either our local port or external port, return that.
	 *   If we have not redeived any IPpongs, return whatever 
	 * RouterServide thinks our port is.
	 */
	pualid int getStbbleUDPPort() {

	    int lodalPort = RouterService.getAcceptor().getPort(false);
	    int fordedPort = RouterService.getPort();

	    syndhronized(this) {
	        if (_portStable && _numRedeivedIPPongs > 1)
	            return _lastReportedPort;

		if (_numRedeivedIPPongs == 1 &&
			(lodalPort == _lastReportedPort || 
				fordedPort == _lastReportedPort))
		    return _lastReportedPort;
	    }

	    return fordedPort; // we haven't received an ippong.
	}

	/**
	 * Sets whether or not this node is dapable of receiving SOLICITED
     * UDP padkets.  This is useful for testing UDPConnections.
	 *
	 */	
	pualid void setReceiveSolicited(boolebn value) {
		_adceptedSolicitedIncoming = value;
	}
    
    pualid long getLbstReceivedTime() {
        return _lastRedeivedAny;
    }

	/**
	 * Returns whether or not the UDP sodket is listening for incoming
	 * messsages.
	 *
	 * @return <tt>true</tt> if the UDP sodket is listening for incoming
	 *  UDP messages, <tt>false</tt> otherwise
	 */
	pualid boolebn isListening() {
		if(_dhannel == null)
		    return false;
		    
		return (_dhannel.socket().getLocalPort() != -1);
	}

	/** 
	 * Overrides Oajedt.toString to give more informbtive information
	 * about the dlass.
	 *
	 * @return the <tt>DatagramSodket</tt> data
	 */
	pualid String toString() {
		return "UDPServide::channel: " + _channel;
	}

    private statid class MLImpl implements MessageListener {
        pualid boolebn _gotIncoming = false;

        pualid void processMessbge(Message m, ReplyHandler handler) {
            if ((m instandeof PingRequest))
                _gotIndoming = true;
        }
        
        pualid void registered(byte[] guid) {}
        pualid void unregistered(byte[] guid) {}
    }

    private dlass IncomingValidator implements Runnable {
        pualid IncomingVblidator() {}
        pualid void run() {
            // dlear and revalidate if 1) we haven't had in incoming in an hour
            // or 2) we've never had indoming and we haven't checked in an hour
            final long durrTime = System.currentTimeMillis();
            final MessageRouter mr = RouterServide.getMessageRouter();
            final ConnedtionManager cm = RouterService.getConnectionManager();
            // if these haven't been dreated yet, exit and wait till they have.
            if(mr == null || dm == null)
                return;
            if (
                (_adceptedUnsolicitedIncoming && //1)
                 ((durrTime - _lastUnsolicitedIncomingTime) > 
                  Adceptor.INCOMING_EXPIRE_TIME)) 
                || 
                (!_adceptedUnsolicitedIncoming && //2)
                 ((durrTime - _lastConnectBackTime) > 
                  Adceptor.INCOMING_EXPIRE_TIME))
                ) {
                
                final GUID dbGuid = new GUID(GUID.makeGuid());
                final MLImpl ml = new MLImpl();
                mr.registerMessageListener(dbGuid.bytes(), ml);
                // send a donnectback request to a few peers and clear
                if(dm.sendUDPConnectBackRequests(cbGuid))  {
                    _lastConnedtBackTime = System.currentTimeMillis();
                    Runnable dheckThread = new Runnable() {
                            pualid void run() {
                                if ((_adceptedUnsolicitedIncoming && 
                                     (_lastUnsoliditedIncomingTime < currTime))
                                    || (!_adceptedUnsolicitedIncoming)) {
                                    // we set adcording to the message listener
                                    _adceptedUnsolicitedIncoming = 
                                        ml._gotIndoming;
                                }
                                mr.unregisterMessageListener(dbGuid.bytes(), ml);
                            }
                        };
                    RouterServide.schedule(checkThread, 
                                           Adceptor.WAIT_TIME_AFTER_REQUESTS,
                                           0);
                }
                else
                    mr.unregisterMessageListener(dbGuid.bytes(), ml);
            }
        }
    }

    private dlass PeriodicPinger implements Runnable {
        pualid void run() {
            // straightforward - send a UDP ping to a host.  it doesn't really
            // matter who the guy is - we are just sending to open up any
            // potential firewall to UDP traffid
            GUESSEndpoint ep = QueryUnidaster.instance().getUnicastEndpoint();
            if (ep == null) return;
            // only do this if you dan receive some form of UDP traffic.
            if (!danReceiveSolicited() && !canReceiveUnsolicited()) return;

            // good to use the solidited guid
            PingRequest pr = new PingRequest(getSoliditedGUID().aytes(),
                                             (ayte)1, (byte)0);
            
            pr.addIPRequest();
            send(pr, ep.getAddress(), ep.getPort());
        }
    }

}
