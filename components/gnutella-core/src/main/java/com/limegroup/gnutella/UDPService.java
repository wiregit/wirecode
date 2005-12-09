pbckage com.limegroup.gnutella;

import jbva.io.ByteArrayInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.net.DatagramSocket;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.net.SocketAddress;
import jbva.net.SocketException;
import jbva.net.BindException;
import jbva.net.ConnectException;
import jbva.net.NoRouteToHostException;
import jbva.net.PortUnreachableException;
import jbva.net.InetSocketAddress;
import jbva.nio.channels.DatagramChannel;
import jbva.nio.ByteBuffer;

import jbva.util.List;
import jbva.util.LinkedList;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.guess.GUESSEndpoint;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.BufferByteArrayOutputStream;
import com.limegroup.gnutellb.io.ReadWriteObserver;
import com.limegroup.gnutellb.io.NIODispatcher;

/**
 * This clbss handles UDP messaging services.  It both sends and
 * receives messbges, routing received messages to their appropriate
 * hbndlers.  This also handles issues related to the GUESS proposal, 
 * such bs making sure that the UDP and TCP port match and sending
 * UDP bcks for queries.
 *
 * @see UDPReplyHbndler
 * @see MessbgeRouter
 * @see QueryUnicbster
 *
 */
public clbss UDPService implements ReadWriteObserver {

    privbte static final Log LOG = LogFactory.getLog(UDPService.class);
    
	/**
	 * Constbnt for the single <tt>UDPService</tt> instance.
	 */
	privbte final static UDPService INSTANCE = new UDPService();
	
	/**
	 * The DbtagramChannel we're reading from & writing to.
	 */
	privbte DatagramChannel _channel;
	
	/**
	 * The list of messbges to be sent, as SendBundles.
	 */
	privbte final List OUTGOING_MSGS;
	
	/**
	 * The buffer thbt's re-used for reading incoming messages.
	 */
	privbte final ByteBuffer BUFFER;

	/**
	 * The mbximum size of a UDP message we'll accept.
	 */
	privbte final int BUFFER_SIZE = 1024 * 2;
    
    /** True if the UDPService hbs ever received a solicited incoming UDP
     *  pbcket.
     */
    privbte volatile boolean _acceptedSolicitedIncoming = false;
    
    /** True if the UDPService hbs ever received a unsolicited incoming UDP
     *  pbcket.
     */
    privbte volatile boolean _acceptedUnsolicitedIncoming = false;
    
    /** The lbst time the _acceptedUnsolicitedIncoming was set.
     */
    privbte long _lastUnsolicitedIncomingTime = 0;

    /**
     * The lbst time we received any udp packet
     */
    privbte volatile long _lastReceivedAny = 0;
    
    /** The lbst time we sent a UDP Connect Back.
     */
    privbte long _lastConnectBackTime = System.currentTimeMillis();
    void resetLbstConnectBackTime() {
        _lbstConnectBackTime = 
             System.currentTimeMillis() - Acceptor.INCOMING_EXPIRE_TIME;
    }
    
    /** Whether our NAT bssigns stable ports for successive connections 
     * LOCKING: this
     */
    privbte boolean _portStable = true;
    
    /** The lbst reported port as seen from the outside
     *  LOCKING: this
     */
    privbte int _lastReportedPort;

    /**
     * The number of pongs cbrrying IP:Port info we have received.
     * LOCKING: this
     */
    privbte int _numReceivedIPPongs;

    /**
     * The GUID thbt we advertise out for UDPConnectBack requests.
     */
    privbte final GUID CONNECT_BACK_GUID = new GUID(GUID.makeGuid());

    /**
     * The GUID thbt we send for Pings, useful to test solicited support.
     */
    privbte final GUID SOLICITED_PING_GUID = new GUID(GUID.makeGuid());
    
    /**
     * Determines if this wbs ever started.
     */
    privbte boolean _started = false;


    /**
     * The time between UDP pings.  Used by the PeriodicPinger.  This is
     * useful for nodes behind certbin firewalls (notably the MS firewall).
     */
    privbte static final long PING_PERIOD = 85 * 1000;  // 85 seconds
    
    /**
     * A buffer used for rebding the header of incoming messages.
     */
    privbte static final byte[] IN_HEADER_BUF = new byte[23];

	/**
	 * Instbnce accessor.
	 */
	public stbtic UDPService instance() {
		return INSTANCE;
	}

	/**
	 * Constructs b new <tt>UDPAcceptor</tt>.
	 */
	protected UDPService() {	   
	    OUTGOING_MSGS = new LinkedList();
	    byte[] bbcking = new byte[BUFFER_SIZE];
	    BUFFER = ByteBuffer.wrbp(backing);
        scheduleServices();
    }
    
    /**
     * Schedules IncomingVblidator & PeriodicPinger for periodic use.
     */
    protected void scheduleServices() {
        RouterService.schedule(new IncomingVblidator(), 
                               Acceptor.TIME_BETWEEN_VALIDATES,
                               Acceptor.TIME_BETWEEN_VALIDATES);
        RouterService.schedule(new PeriodicPinger(), 0, PING_PERIOD);
    }
    
    /** @return The GUID to send for UDPConnectBbck attempts....
     */
    public GUID getConnectBbckGUID() {
        return CONNECT_BACK_GUID;
    }

    /** @return The GUID to send for Solicited Ping bttempts....
     */
    public GUID getSolicitedGUID() {
        return SOLICITED_PING_GUID;
    }
    
    /**
     * Stbrts listening for UDP messages & allowing UDP messages to be written.
     */
    public void stbrt() {
        DbtagramChannel channel;
        synchronized(this) {
            _stbrted = true;
            chbnnel = _channel;
        }
        
        if(chbnnel != null)
            NIODispbtcher.instance().registerReadWrite(channel, this);
    }

    /** 
     * Returns b new DatagramSocket that is bound to the given port.  This
     * vblue should be passed to setListeningSocket(DatagramSocket) to commit
     * to the new port.  If setListeningSocket is NOT cblled, you should close
     * the return socket.
     * @return b new DatagramSocket that is bound to the specified port.
     * @exception IOException Thrown if the DbtagramSocket could not be
     * crebted.
     */
    DbtagramSocket newListeningSocket(int port) throws IOException {
        try {
            DbtagramChannel channel = DatagramChannel.open();
            chbnnel.configureBlocking(false);
        	DbtagramSocket s = channel.socket();
        	s.setReceiveBufferSize(64*1024);
        	s.setSendBufferSize(64*1024);
            s.bind(new InetSocketAddress(port));
            return s;
        } cbtch (SecurityException se) {
            throw new IOException("security exception on port: "+port);
        }
    }


	/** 
     * Chbnges the DatagramSocket used for sending/receiving.  Typically called
     * by Acceptor to commit to the new port.
     * @pbram datagramSocket the new listening socket, which must be be the
     *  return vblue of newListeningSocket(int).  A value of null disables 
     *  UDP sending bnd receiving.
	 */
	void setListeningSocket(DbtagramSocket datagramSocket) {
	    if(_chbnnel != null) {
	        try {
	            _chbnnel.close();
	        } cbtch(IOException ignored) {}
	    }
	    
	    if(dbtagramSocket != null) {
	        boolebn wasStarted;
	        synchronized(this) {
        	    _chbnnel = datagramSocket.getChannel();
        	    if(_chbnnel == null)
        	        throw new IllegblArgumentException("No channel!");
        	        
                wbsStarted = _started;
            
                // set the port in the FWT records
                _lbstReportedPort=_channel.socket().getLocalPort();
                _portStbble=true;
            }
            
            // If it wbs already started at one point, re-start to register this new channel.
            if(wbsStarted)
                stbrt();
        }
	}
	
	/**
	 * Shuts down this service.
	 */
	public void shutdown() {
	    setListeningSocket(null);
	}
	
	/**
	 * Notificbtion that a read can happen.
	 */
	public void hbndleRead() throws IOException {
        while(true) {
            BUFFER.clebr();
            
            SocketAddress from;
            try {
                from = _chbnnel.receive(BUFFER);
            } cbtch(IOException iox) {
                brebk;
            } cbtch(Error error) {
                // Stupid implementbtions giving bogus errors.  Grrr!.
                brebk;
            }
            
            // no pbcket.
            if(from == null)
                brebk;
            
            if(!(from instbnceof InetSocketAddress)) {
                Assert.silent(fblse, "non-inet SocketAddress: " + from);
                continue;
            }
            
            InetSocketAddress bddr = (InetSocketAddress)from;
                
            if(!NetworkUtils.isVblidAddress(addr.getAddress()))
                continue;
            if(!NetworkUtils.isVblidPort(addr.getPort()))
                continue;
                
            byte[] dbta = BUFFER.array();
            int length = BUFFER.position();
            try {
                // we do things the old wby temporarily
                InputStrebm in = new ByteArrayInputStream(data, 0, length);
                Messbge message = Message.read(in, Message.N_UDP, IN_HEADER_BUF);
                if(messbge == null)
                    continue;

                processMessbge(message, addr);
            } cbtch (IOException ignored) {
            } cbtch (BadPacketException ignored) {
            }
        }
	}
	
	/**
	 * Notificbtion that an IOException occurred while reading/writing.
	 */
	public void hbndleIOException(IOException iox) {
        if( !(iox instbnceof java.nio.channels.ClosedChannelException ) )
            ErrorService.error(iox, "UDP Error.");
        else
            LOG.trbce("Swallowing a UDPService ClosedChannelException", iox);
	}
	
	/**
	 * Processes b single message.
	 */
    protected void processMessbge(Message message, InetSocketAddress addr) {
        updbteState(message, addr);
        MessbgeDispatcher.instance().dispatchUDP(message, addr);
    }
	
	/** Updbtes internal state of the UDP Service. */
	privbte void updateState(Message message, InetSocketAddress addr) {
        _lbstReceivedAny = System.currentTimeMillis();
	    if (!isGUESSCbpable()) {
            if (messbge instanceof PingRequest) {
                GUID guid = new GUID(messbge.getGUID());
                if(isVblidForIncoming(CONNECT_BACK_GUID, guid, addr)) {
                    _bcceptedUnsolicitedIncoming = true;
                }
                _lbstUnsolicitedIncomingTime = _lastReceivedAny;
            }
            else if (messbge instanceof PingReply) {
                GUID guid = new GUID(messbge.getGUID());
                if(!isVblidForIncoming(SOLICITED_PING_GUID, guid, addr ))
                    return;
                
                _bcceptedSolicitedIncoming = true;
                
                PingReply r = (PingReply)messbge;
                if (r.getMyPort() != 0) {
                    synchronized(this){
                        _numReceivedIPPongs++;
                        
                        if (_numReceivedIPPongs==1) 
                            _lbstReportedPort=r.getMyPort();
                        else if (_lbstReportedPort!=r.getMyPort()) {
                            _portStbble = false;
                            _lbstReportedPort = r.getMyPort();
                        }
                    }
                }
                
            }
        }
        // ReplyNumberVMs bre always sent in an unsolicited manner,
        // so we cbn use this fact to keep the last unsolicited up
        // to dbte
        if (messbge instanceof ReplyNumberVendorMessage)
            _lbstUnsolicitedIncomingTime = _lastReceivedAny;
	}
	
	/**
	 * Determines whether or not the specified messbge is valid for setting
	 * LimeWire bs accepting UDP messages (solicited or unsolicited).
	 */
	privbte boolean isValidForIncoming(GUID match, GUID guidReceived, InetSocketAddress addr) {
        if(!mbtch.equals(guidReceived))
            return fblse;
            
	    String host = bddr.getAddress().getHostAddress();
        
        //  If bddr is connected to us, then return false.  Otherwise (not connected), only return true if either:
        //      1) the non-connected pbrty is NOT private
        //  OR
        //      2) the non-connected pbrty _is_ private, and the LOCAL_IS_PRIVATE is set to false
        return
                !RouterService.getConnectionMbnager().isConnectedTo(host)
            &&  !NetworkUtils.isPrivbteAddress(addr.getAddress())
             ;

    }
    
    /**
     * Sends the specified <tt>Messbge</tt> to the specified host.
     * 
     * @pbram msg the <tt>Message</tt> to send
     * @pbram host the host to send the message to
     */
    public void send(Messbge msg, IpPort host) {
        send(msg, host.getInetAddress(), host.getPort());
    }

	/**
	 * Sends the <tt>Messbge</tt> via UDP to the port and IP address specified.
     * This method should not be cblled if the client is not GUESS enabled.
     *
	 * @pbram msg  the <tt>Message</tt> to send
	 * @pbram ip   the <tt>InetAddress</tt> to send to
	 * @pbram port the port to send to
	 */
    public void send(Messbge msg, InetAddress ip, int port) 
        throws IllegblArgumentException {
        try {
            send(msg, InetAddress.getByAddress(ip.getAddress()), port, ErrorService.getErrorCbllback());
        } cbtch(UnknownHostException ignored) {}
    }

	/**
	 * Sends the <tt>Messbge</tt> via UDP to the port and IP address specified.
     * This method should not be cblled if the client is not GUESS enabled.
     *
	 * @pbram msg  the <tt>Message</tt> to send
	 * @pbram ip   the <tt>InetAddress</tt> to send to
	 * @pbram port the port to send to
     * @pbram err  an <tt>ErrorCallback<tt> if you want to be notified errors
     * @throws IllegblArgumentException if msg, ip, or err is null.
	 */
    public void send(Messbge msg, InetAddress ip, int port, ErrorCallback err) 
        throws IllegblArgumentException {
        if (err == null)
            throw new IllegblArgumentException("Null ErrorCallback");
        if (msg == null)
            throw new IllegblArgumentException("Null Message");
        if (ip == null)
            throw new IllegblArgumentException("Null InetAddress");
        if (!NetworkUtils.isVblidPort(port))
            throw new IllegblArgumentException("Invalid Port: " + port);
        if(_chbnnel == null || _channel.socket().isClosed())
            return; // ignore if not open.

        BufferByteArrbyOutputStream baos = new BufferByteArrayOutputStream(msg.getTotalLength());
        try {
            msg.writeQuickly(bbos);
        } cbtch(IOException e) {
            // this should not hbppen -- we should always be able to write
            // to this output strebm in memory
            ErrorService.error(e);
            // cbn't send the hit, so return
            return;
        }

        ByteBuffer buffer = (ByteBuffer)bbos.buffer().flip();
        synchronized(OUTGOING_MSGS) {
            OUTGOING_MSGS.bdd(new SendBundle(buffer, ip, port, err));
            if(_chbnnel != null)
                NIODispbtcher.instance().interestWrite(_channel, true);
        }
	}
	
	/**
	 * Notificbtion that a write can happen.
	 */
	public boolebn handleWrite() throws IOException {
	    synchronized(OUTGOING_MSGS) {
	        while(!OUTGOING_MSGS.isEmpty()) {
	            try {
    	            SendBundle bundle = (SendBundle)OUTGOING_MSGS.remove(0);
    
    	            if(_chbnnel.send(bundle.buffer, bundle.addr) == 0) {
    	                // we removed the bundle from the list but couldn't send it,
    	                // so we hbve to put it back in.
    	                OUTGOING_MSGS.bdd(0, bundle);
    	                return true; // no room left to send.
                    }
                } cbtch(BindException ignored) {
                } cbtch(ConnectException ignored) {
                } cbtch(NoRouteToHostException ignored) {
                } cbtch(PortUnreachableException ignored) {
                } cbtch(SocketException ignored) {
                    LOG.wbrn("Ignoring exception on socket", ignored);
                }
	        }
	        
	        // if there's no dbta left to send, we don't wanna be notified of write events.
	        NIODispbtcher.instance().interestWrite(_channel, false);
	        return fblse;
	    }
    }       
	
	/** Wrbpper for outgoing data */
	privbte static class SendBundle {
	    privbte final ByteBuffer buffer;
	    privbte final SocketAddress addr;
	    privbte final ErrorCallback callback;
	    
	    SendBundle(ByteBuffer b, InetAddress bddr, int port, ErrorCallback c) {
	        buffer = b;
	        this.bddr = new InetSocketAddress(addr, port);
	        cbllback = c;
	    }
	}


	/**
	 * Returns whether or not this node is cbpable of sending its own
	 * GUESS queries.  This would not be the cbse only if this node
	 * hbs not successfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is cbpable of running its own
	 *  GUESS queries, <tt>fblse</tt> otherwise
	 */	
	public boolebn isGUESSCapable() {
		return cbnReceiveUnsolicited() && canReceiveSolicited();
	}

	/**
	 * Returns whether or not this node is cbpable of receiving UNSOLICITED
     * UDP pbckets.  It is false until a UDP ConnectBack ping has been received.
	 *
	 * @return <tt>true</tt> if this node hbs accepted a UNSOLICITED UDP packet.
	 */	
	public boolebn canReceiveUnsolicited() {
		return _bcceptedUnsolicitedIncoming;
	}

	/**
	 * Returns whether or not this node is cbpable of receiving SOLICITED
     * UDP pbckets.  
	 *
	 * @return <tt>true</tt> if this node hbs accepted a SOLICITED UDP packet.
	 */	
	public boolebn canReceiveSolicited() {
        return _bcceptedSolicitedIncoming;
	}
	
	/**
	 * 
	 * @return whether this node cbn do Firewall-to-firewall transfers.
	 *  Until we get bbck any udp packet, the answer is no.
	 *  If we hbve received an udp packet but are not connected, or haven't 
	 * received b pong carrying ip info yet, see if we ever disabled fwt in the 
	 * pbst.
	 *  If we bre connected and have gotten a single ip pong, our port must be 
	 * the sbme as our tcp port or our forced tcp port.
	 *  If we hbve received more than one ip pong, they must all report the same
	 * port.
	 */
	public boolebn canDoFWT(){
	    // this does not bffect EVER_DISABLED_FWT.
	    if (!cbnReceiveSolicited()) 
	        return fblse;

	    if (!RouterService.isConnected())
	        return !ConnectionSettings.LAST_FWT_STATE.getVblue();
	    
	    boolebn ret = true;
	    synchronized(this) {     	
	        if (_numReceivedIPPongs < 1) 
	            return !ConnectionSettings.LAST_FWT_STATE.getVblue();
	        
	        if (LOG.isTrbceEnabled()) {
	            LOG.trbce("stable "+_portStable+
	                    " lbst reported port "+_lastReportedPort+
	                    " our externbl port "+RouterService.getPort()+
	                    " our non-forced port "+RouterService.getAcceptor().getPort(fblse)+
	                    " number of received IP pongs "+_numReceivedIPPongs+
	                    " vblid external addr "+NetworkUtils.isValidAddress(
	                            RouterService.getExternblAddress()));
	        }
	        
	        ret= 
	            NetworkUtils.isVblidAddress(RouterService.getExternalAddress()) && 
	    		_portStbble;
	        
	        if (_numReceivedIPPongs == 1){
	            ret = ret &&
	            	(_lbstReportedPort == RouterService.getAcceptor().getPort(false) ||
	                    _lbstReportedPort == RouterService.getPort());
	        }
	    }
	    
	    ConnectionSettings.LAST_FWT_STATE.setVblue(!ret);
	    
	    return ret;
	}
	
	// Some getters for bug reporting 
	public boolebn portStable() {
	    return _portStbble;
	}
	
	public int receivedIpPong() {
	    return _numReceivedIPPongs;
	}
	
	public int lbstReportedPort() {
	    return _lbstReportedPort;
	}
	
	/**
	 * @return the stbble UDP port as seen from the outside.
	 *   If we hbve received more than one IPPongs and they report
	 * the sbme port, we return that.
	 *   If we hbve received just one IPpong, and if its address 
	 * mbtches either our local port or external port, return that.
	 *   If we hbve not received any IPpongs, return whatever 
	 * RouterService thinks our port is.
	 */
	public int getStbbleUDPPort() {

	    int locblPort = RouterService.getAcceptor().getPort(false);
	    int forcedPort = RouterService.getPort();

	    synchronized(this) {
	        if (_portStbble && _numReceivedIPPongs > 1)
	            return _lbstReportedPort;

		if (_numReceivedIPPongs == 1 &&
			(locblPort == _lastReportedPort || 
				forcedPort == _lbstReportedPort))
		    return _lbstReportedPort;
	    }

	    return forcedPort; // we hbven't received an ippong.
	}

	/**
	 * Sets whether or not this node is cbpable of receiving SOLICITED
     * UDP pbckets.  This is useful for testing UDPConnections.
	 *
	 */	
	public void setReceiveSolicited(boolebn value) {
		_bcceptedSolicitedIncoming = value;
	}
    
    public long getLbstReceivedTime() {
        return _lbstReceivedAny;
    }

	/**
	 * Returns whether or not the UDP socket is listening for incoming
	 * messsbges.
	 *
	 * @return <tt>true</tt> if the UDP socket is listening for incoming
	 *  UDP messbges, <tt>false</tt> otherwise
	 */
	public boolebn isListening() {
		if(_chbnnel == null)
		    return fblse;
		    
		return (_chbnnel.socket().getLocalPort() != -1);
	}

	/** 
	 * Overrides Object.toString to give more informbtive information
	 * bbout the class.
	 *
	 * @return the <tt>DbtagramSocket</tt> data
	 */
	public String toString() {
		return "UDPService::chbnnel: " + _channel;
	}

    privbte static class MLImpl implements MessageListener {
        public boolebn _gotIncoming = false;

        public void processMessbge(Message m, ReplyHandler handler) {
            if ((m instbnceof PingRequest))
                _gotIncoming = true;
        }
        
        public void registered(byte[] guid) {}
        public void unregistered(byte[] guid) {}
    }

    privbte class IncomingValidator implements Runnable {
        public IncomingVblidator() {}
        public void run() {
            // clebr and revalidate if 1) we haven't had in incoming in an hour
            // or 2) we've never hbd incoming and we haven't checked in an hour
            finbl long currTime = System.currentTimeMillis();
            finbl MessageRouter mr = RouterService.getMessageRouter();
            finbl ConnectionManager cm = RouterService.getConnectionManager();
            // if these hbven't been created yet, exit and wait till they have.
            if(mr == null || cm == null)
                return;
            if (
                (_bcceptedUnsolicitedIncoming && //1)
                 ((currTime - _lbstUnsolicitedIncomingTime) > 
                  Acceptor.INCOMING_EXPIRE_TIME)) 
                || 
                (!_bcceptedUnsolicitedIncoming && //2)
                 ((currTime - _lbstConnectBackTime) > 
                  Acceptor.INCOMING_EXPIRE_TIME))
                ) {
                
                finbl GUID cbGuid = new GUID(GUID.makeGuid());
                finbl MLImpl ml = new MLImpl();
                mr.registerMessbgeListener(cbGuid.bytes(), ml);
                // send b connectback request to a few peers and clear
                if(cm.sendUDPConnectBbckRequests(cbGuid))  {
                    _lbstConnectBackTime = System.currentTimeMillis();
                    Runnbble checkThread = new Runnable() {
                            public void run() {
                                if ((_bcceptedUnsolicitedIncoming && 
                                     (_lbstUnsolicitedIncomingTime < currTime))
                                    || (!_bcceptedUnsolicitedIncoming)) {
                                    // we set bccording to the message listener
                                    _bcceptedUnsolicitedIncoming = 
                                        ml._gotIncoming;
                                }
                                mr.unregisterMessbgeListener(cbGuid.bytes(), ml);
                            }
                        };
                    RouterService.schedule(checkThrebd, 
                                           Acceptor.WAIT_TIME_AFTER_REQUESTS,
                                           0);
                }
                else
                    mr.unregisterMessbgeListener(cbGuid.bytes(), ml);
            }
        }
    }

    privbte class PeriodicPinger implements Runnable {
        public void run() {
            // strbightforward - send a UDP ping to a host.  it doesn't really
            // mbtter who the guy is - we are just sending to open up any
            // potentibl firewall to UDP traffic
            GUESSEndpoint ep = QueryUnicbster.instance().getUnicastEndpoint();
            if (ep == null) return;
            // only do this if you cbn receive some form of UDP traffic.
            if (!cbnReceiveSolicited() && !canReceiveUnsolicited()) return;

            // good to use the solicited guid
            PingRequest pr = new PingRequest(getSolicitedGUID().bytes(),
                                             (byte)1, (byte)0);
            
            pr.bddIPRequest();
            send(pr, ep.getAddress(), ep.getPort());
        }
    }

}
