package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.ErrorService;

/** 
 *  Manage a reliable udp connection for the transfer of data.
 */
public class UDPConnectionProcessor {

    /** Define the chunk size used for data bytes */
    public static final int   DATA_CHUNK_SIZE         = 512;

    /** Handle to the output stream that is the input to this connection */
    private UDPBufferedOutputStream  _input;

    /** The limit on space for data to be written out */
    private int               _chunkLimit;

    /** Record the desired connection timeout on the connection */
    private long              connectTimeOut          = MAX_CONNECT_WAIT_TIME;

    /** Record the desired read timeout on the connection */
    private int               readTimeOut             = 0;

	/** Predefine a common exception if the user can't receive UDP */
	private static final IOException CANT_RECEIVE_UDP = 
	  new IOException("Can't receive UDP");

    /** Predefine a common exception if the connection times out on creation */
    private static final IOException CONNECTION_TIMEOUT = 
      new IOException("Connection timed out");

    /** Define the max sequence number before rollover */
    private static final int  MAX_SEQUENCE_NUMBER     = 0xffff;

    /** Define the size of the data window */
    private static final int  DATA_WINDOW_SIZE        = 20;

    // Handle to various singleton objects in our architecture
    private UDPService        _udpService;
    private UDPMultiplexor    _multiplexor;
    private UDPScheduler      _scheduler;

    // Define WAIT TIMES
    //
	/** Define the wait time between SYN messages */
	private static final long SYN_WAIT_TIME           = 400;

    /** Define the maximum wait time to connect */
    private static final long MAX_CONNECT_WAIT_TIME   = 20*1000;

	/** Define the maximum wait time before sending a message in order to
        keep the connection alive (and firewalls open) */
	private static final long KEEPALIVE_WAIT_TIME     = (3*1000 - 500);


    /** Keep one KeepaliveMessage around for handy use - lazy instanciation */
    private KeepAliveMessage  KEEPALIVE_MSG;

    // Define Connection states
    //
    /** The state on first creation before connection is established */
	private static final int  PRECONNECT_STATE        = 0;

    /** The state after a connection is established */
    private static final int  CONNECT_STATE           = 1;

    /** The state after user communication during shutdown */
    private static final int  FIN_STATE               = 2;


    /** The ip of the host connected to */
	private final InetAddress _ip;

    /** The port of the host connected to */
	private final int         _port;


    /** The Window for sending and acking data */
	private DataWindow        _senderWindow;

    /** The Window for receiving data */
    private DataWindow        _receiverWindow;

    /** The connectionID of this end of connection.  Used for routing */
	private byte              _myConnectionID;

    /** The connectionID of the other end of connection.  Used for routing */
	private byte              _theirConnectionID;

    /** The status of the connection */
	private int               _connectionState;

    /** Scheduled event for keeping connection alive  */
    private UDPTimerEvent     _keepaliveEvent;

    /** Scheduled event for keeping connection alive  */
    private UDPTimerEvent     _writeDataEvent;


    /** The current sequence number of messages originated here */
	private int               _sequenceNumber;

    /** The last time that a message was sent to other host */
    private long              _lastSendTime;

    /** The last time that a message was received from the other host */
	private long              _lastReceivedTime;

    /**
     *  Try to kickoff a reliable udp connection. This method blocks until it 
	 *  either sucessfully establishes a connection or it times out and throws
	 *  an IOException.
     */
    public UDPConnectionProcessor(InetAddress ip, int port) throws IOException {
        // Record their address
        _ip        		   = ip;
        _port      		   = port;

        // Init default state
        _theirConnectionID = UDPMultiplexor.UNASSIGNED_SLOT; 
		_connectionState   = PRECONNECT_STATE;
		_lastSendTime      = 0l;
    	_chunkLimit        = DATA_WINDOW_SIZE; // TODO: This varies based on
											   // Window fullness

		_udpService        = UDPService.instance();

		// If UDP is not running or not workable, barf
		if ( !_udpService.isListening() || 
			 !_udpService.canReceiveSolicited() ) { //TODO: Check this
			throw CANT_RECEIVE_UDP;
		}

        // Only wake these guys up if the service is okay
		_multiplexor       = UDPMultiplexor.instance();
		_scheduler         = UDPScheduler.instance();

        // Register yourself for incoming messages
		_myConnectionID    = _multiplexor.register(this);

        // See if you can establish a pseudo connection 
        // which means each side can send/receive a SYN and ACK
		tryToConnect();
    }

	public InputStream getInputStream() throws IOException {
        return null;
	}

    /**
     *  Create a special output stream that feeds byte array chunks
	 *  into this connection.
     */
	public OutputStream getOutputStream() throws IOException {
        _input = new UDPBufferedOutputStream(this);
        return _input;
	}

	public void setSoTimeout(int timeout) throws SocketException {
	}

	public void close() throws IOException {
	}

    /**
     *  Prepare for handling an open connection.
     */
    private synchronized void prepareOpenConnection() {
        _connectionState = CONNECT_STATE;
        incrementSequenceNumber();
        scheduleKeepAlive();

        // Create the delayed connection components
        _senderWindow    = new DataWindow(DATA_WINDOW_SIZE, 1);
        _receiverWindow  = new DataWindow(DATA_WINDOW_SIZE, 1);
        try {
            KEEPALIVE_MSG    = new KeepAliveMessage(_theirConnectionID);
        } catch (BadPacketException bpe) {
            // This would not be good.   TODO: ????
            ErrorService.error(bpe);
        }
    }

    /**
     *  Make sure any firewall or nat stays open by scheduling a keepalive 
     *  message before the connection should close.
     *
     *  This just fires and reschedules itself appropriately so that we 
     *  don't need to worry about rescheduling as every new message is sent.
     */
    private synchronized void scheduleKeepAlive() {
        // Create event with initial time
        _keepaliveEvent  = 
          new KeepAliveTimerEvent(_lastSendTime + KEEPALIVE_WAIT_TIME);

        // Register keepalive event for future event callbacks
        _scheduler.register(_keepaliveEvent);

        // Schedule the first keepalive event callback
        _scheduler.scheduleEvent(_keepaliveEvent);
    }

    /**
     *  Setup and schedule the callback event for writing data.
     */
    private synchronized void scheduleWriteDataEvent(long time) {
        if ( isConnected() ) {
            if ( _writeDataEvent == null ) {
                _writeDataEvent  = 
                    new WriteDataTimerEvent(time);

                // Register writeData event for future use
                _scheduler.register(_writeDataEvent);
            } else {
                _writeDataEvent.updateTime(time);
            }

            // Notify the scheduler that there is a new write event/time
            _scheduler.scheduleEvent(_writeDataEvent);
        }
    }

    /**
     *  Move the outgoing message sequence number forward with possible 
     *  rollover.
     */
    private synchronized void incrementSequenceNumber() {
        _sequenceNumber = (_sequenceNumber + 1) % MAX_SEQUENCE_NUMBER;
    }

    /**
     *  Test whether the connection is in connecting mode
     */
    public synchronized boolean isConnected() {
        return (_connectionState == CONNECT_STATE);
    }

    /**
     *  Test whether the connection is not fully setup
     */
	public synchronized boolean isConnecting() {
		return (_connectionState   == PRECONNECT_STATE ||
                _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT);
	}

    /**
     *  Test whether the ip and ports match
     */
	public boolean matchAddress(InetAddress ip, int port) {
		return (_ip.equals(ip) && _port == port);
	}

    /**
     *  Return the connections connectionID identifier.
     */
	public byte getConnectionID() {
		return _myConnectionID;
	}

    /**
     *  Return the room for new local incoming data in chunks
     */
	public int getChunkLimit() {
		return _chunkLimit;
	}

    /**
     *  Convenience method for sending keepalive message since we might fire 
     *  these off before waiting
     */
    private void sendKeepAlive() throws IllegalArgumentException {
        try {  
            send(KEEPALIVE_MSG);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
        }
    }

    /**
     *  Send a message on to the UDPService
     */
	private synchronized void send(UDPConnectionMessage msg) 
      throws IllegalArgumentException {
		_lastSendTime = System.currentTimeMillis();
		_udpService.send(msg, _ip, _port);  // TODO: performance
	}

    // ------------------  Connection Handling Logic -------------------
    //
    /**
     *  Send SYN messages to desired host and wait for Acks and their 
     *  SYN message.  Block connector while trying to connect.
     */
	private void tryToConnect() throws IOException {
		try {
            _sequenceNumber       = 0;

            // Keep track of how long you are waiting on connection
            long       waitTime   = 0;

            // Build SYN message with my connectionID in it
            SynMessage synMsg     = null;
            try {
                synMsg     = new SynMessage(_myConnectionID);
            } catch (BadPacketException bpe) {
                throw new IOException(bpe.getMessage());
            }

            // Keep sending and waiting until you get a Syn and an Ack from 
            // the other side of the connection.
			while ( isConnecting() ) { 

                if ( waitTime > connectTimeOut )
                    throw CONNECTION_TIMEOUT;

				// Send a SYN packet with our connectionID 
				// TODO: performance of send?
				send(synMsg);  
    
                // Wait for some kind of response
				try { Thread.sleep(SYN_WAIT_TIME); } 
                catch(InterruptedException e) {}
                waitTime += SYN_WAIT_TIME;
			}
		} catch (IllegalArgumentException iae) {
			throw new IOException(iae.getMessage());
		}
	}


    /**
     *  Take action on a received message.
     */
    public synchronized void handleMessage(UDPConnectionMessage msg) {

        if (msg instanceof SynMessage) {
            // First Message from other host - get his connectionID.
            SynMessage smsg        = (SynMessage) msg;
            byte       theirConnID = smsg.getSenderConnectionID();
            if ( _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT ) { 
                // Keep track of their connectionID
                _theirConnectionID = theirConnID;
            } else if ( _theirConnectionID == theirConnID ) {
                // Getting a duplicate SYN so just ack it again.
            } else {
                // Unmatching SYN so just ignore it
                return;
            }

            // Ack their SYN message
            AckMessage ack = null;
            try {
              ack = new AckMessage(theirConnID, smsg.getSequenceNumber());
            } catch (BadPacketException bpe) {
                // This would not be good.   TODO: ????
                ErrorService.error(bpe);
            }
            try {  
                send(ack);
            } catch(IllegalArgumentException iae) {
                // Report an error since this shouldn't ever happen
                ErrorService.error(iae);
            }
        } else if (msg instanceof AckMessage) {
            AckMessage amsg  = (AckMessage) msg;
            int        seqNo = amsg.getSequenceNumber();

            // If they are Acking our SYN message, advance the state
            if ( seqNo == 0 && isConnecting() ) { 
                // The connection should be successful assuming that I
                // receive their SYN so move state to CONNECT_STATE
                // and get ready for activity
                prepareOpenConnection();
            }
        }

        // TODO: fill in
    }

    /**
     *  If there is data to be written then write it 
     *  and schedule next write time.
     */
    public synchronized void writeData() {
        long time = 0l;

        // TODO: fill in

        scheduleWriteDataEvent(time);
    }

    /** 
     *  Define what happens when a keepalive timer fires.
     */
    class  KeepAliveTimerEvent extends UDPTimerEvent {
        public KeepAliveTimerEvent(long time) {
            super(time);
        }

        public void handleEvent() {
            long time = System.currentTimeMillis();
            
            // If reevaluation of the time still requires a keepalive then send
            if ( time+1 >= (_lastSendTime + KEEPALIVE_WAIT_TIME) ) {
                if ( isConnected() ) {
                    sendKeepAlive();
                } else {
                    return;
                }
            }

            // Reschedule keepalive timer
            _eventTime = _lastSendTime + KEEPALIVE_WAIT_TIME;
            _scheduler.scheduleEvent(this);
        }
    }
    /** 
     *  Define what happens when a WriteData timer event fires.
     */
    class  WriteDataTimerEvent extends UDPTimerEvent {
        public WriteDataTimerEvent(long time) {
            super(time);
        }

        public void handleEvent() {
            if ( isConnected() ) {
                writeData();
            }
        }
    }

    /** 
     *  Define what happens when an ack timeout occurs
     */
    class  AckTimeoutTimerEvent extends UDPTimerEvent {
        public AckTimeoutTimerEvent(long time) {
            super(time);
        }

        public void handleEvent() {
        }
    }
    //
    // -----------------------------------------------------------------
}
