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

    /** The receivers windowSpace defining amount of data that receiver can
        accept */
    private int               _receiverWindowSpace;

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

	/** Define the startup time before starting to send data.  Note that
        on the receivers end, they may not be setup initially.  */
	private static final long WRITE_STARTUP_WAIT_TIME = 400;

    /** Define the default time to check for an ack to a data message */
    private static final long DEFAULT_RTO_WAIT_TIME   = 400;

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
	private DataWindow        _sendWindow;

    /** The Window for receiving data */
    private DataWindow        _receiveWindow;

    /** The connectionID of this end of connection.  Used for routing */
	private byte              _myConnectionID;

    /** The connectionID of the other end of connection.  Used for routing */
	private byte              _theirConnectionID;

    /** The status of the connection */
	private int               _connectionState;

    /** Scheduled event for keeping connection alive  */
    private UDPTimerEvent     _keepaliveEvent;

    /** Scheduled event for writing data appropriately over time  */
    private UDPTimerEvent     _writeDataEvent;

    /** Scheduled event for ensuring that data is acked or resent */
    private UDPTimerEvent     _ackTimeoutEvent;


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
        _theirConnectionID   = UDPMultiplexor.UNASSIGNED_SLOT; 
		_connectionState     = PRECONNECT_STATE;
		_lastSendTime        = 0l;
    	_chunkLimit          = DATA_WINDOW_SIZE; // TODO: This varies based on
											     // Window fullness
    	_receiverWindowSpace = DATA_WINDOW_SIZE; 

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
        _sendWindow      = new DataWindow(DATA_WINDOW_SIZE, 1);
		// TODO: keep up to date
        _chunkLimit      = _sendWindow.getWindowSpace();  
        _receiveWindow   = new DataWindow(DATA_WINDOW_SIZE, 1);
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
     *  Setup and schedule the callback event for ensuring data gets acked.
     */
    private synchronized void scheduleAckTimeoutEvent(long time) {
        if ( isConnected() ) {
            if ( _ackTimeoutEvent == null ) {
                _ackTimeoutEvent  = 
                    new AckTimeoutTimerEvent(time);

                // Register ackTimout event for future use
                _scheduler.register(_ackTimeoutEvent);
            } else {
                _ackTimeoutEvent.updateTime(time);
            }

            // Notify the scheduler that there is a new ack timeout event
            _scheduler.scheduleEvent(_ackTimeoutEvent);
        }
    }

    /**
     *  Suppress ack timeout events for now
     */
    private synchronized void unscheduleAckTimeoutEvent() {
        // Nothing required if not initialized
        if ( _ackTimeoutEvent == null )
            return;

        // Set an existing event to an infinite wait
        // Note: No need to explicitly inform scheduler.
        _ackTimeoutEvent.updateTime(Long.MAX_VALUE);
    }

    /**
     *  Determine if an ackTimeout should be rescheduled  
     */
    private synchronized boolean isAckTimeoutUpdateRequired() {
        // If ack timeout not yet created then yes.
        if ( _ackTimeoutEvent == null ) 
            return true;

        // If ack timeout exists but is infinite then yes an update is required.
        return (_ackTimeoutEvent.getEventTime() == Long.MAX_VALUE);
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
     *  Return the room for new local incoming data in chunks. This should 
	 *  remain equal to the space available in the sender and receiver 
	 *  data window.
     */
	public int getChunkLimit() {
		return Math.min(_chunkLimit, _receiverWindowSpace);
	}

    /**
     *  Convenience method for sending keepalive message since we might fire 
     *  these off before waiting
     */
    private void sendKeepAlive() {
        KeepAliveMessage keepalive = null;
        try {  
              keepalive = 
                new KeepAliveMessage(_theirConnectionID, 
                  _receiveWindow.getWindowStart(), 
                  _receiveWindow.getWindowSpace());
            send(keepalive);
        } catch (BadPacketException bpe) {
            // This would not be good.   TODO: ????
            ErrorService.error(bpe);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
        }
    }

    /**
     *  Convenience method for sending data.  TODO: graceful shutdown on error
	 */
    private synchronized void sendData(Chunk chunk) {
        try {  
            DataMessage dm = new DataMessage(_theirConnectionID, 
			  _sequenceNumber, chunk.data, chunk.length);
            send(dm);
			_sendWindow.addData(_sequenceNumber, dm);  

			_sequenceNumber++;

            // If Acking check needs to be woken up then do it
            if ( isAckTimeoutUpdateRequired()) 
                scheduleAckIfNeeded();

        } catch (BadPacketException bpe) {
            // This would not be good.  
            ErrorService.error(bpe);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
        }
    }

    /**
     *  Build and send an ack with default error handling with
     *  the messages sequenceNumber, receive window start and 
     *  receive window space.
     */
    private void safeSendAck(UDPConnectionMessage msg) {
        // Ack the message
        AckMessage ack = null;
        try {
          ack = new AckMessage(
           _theirConnectionID, 
           msg.getSequenceNumber(),
           _receiveWindow.getWindowStart(),   
           _receiveWindow.getWindowSpace());

            send(ack);
        } catch (BadPacketException bpe) {
            // This would not be good.   TODO: ????
            ErrorService.error(bpe);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
        }
    }


    /**
     *  Send a message on to the UDPService
     */
    private synchronized void safeSend(UDPConnectionMessage msg) {
        try {
            send(msg); 
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



    /**
     *  Schedule an ack timeout for the oldest unacked data.
     *  If no acks are pending, then do nothing.
     */
    private synchronized void scheduleAckIfNeeded() {
        DataRecord drec = _sendWindow.getOldestUnackedBlock();
        if ( drec != null ) {
            int rto         = _sendWindow.getRTO();
            long waitTime    = drec.sentTime + ((long)rto);
            scheduleAckTimeoutEvent(waitTime);
        } else {
            unscheduleAckTimeoutEvent();
        }
    }

    /**
     *  Ensure that data is getting acked.  If not within an appropriate time, 
     *  then resend.
     */
    private synchronized void validateAckedData() {
        long currTime = System.currentTimeMillis();

        if (_sendWindow.acksAppearToBeMissing(currTime, 1)) {

            // if the older blocks ack have been missing for a while
            // resend them.

            // Calculate a good time to wait
            int rto      = _sendWindow.getRTO();
            int adjRTO   = (rto * 3) / 2;
            int waitTime = rto / 2;

            int start = _sendWindow.getWindowStart();

System.out.println("Soft resend data:"+ start+ " rto:"+rto+
" uS:"+_sendWindow.getUsedSpots());

            DataRecord drec;
            int        blockNum;

            // Resend up to 2
            for (int i = 0; i < 2; i++) {

                //blockNum = (start + i) % DataWindow.MAX_SEQUENCE_NUMBER;

                // Get the oldest unacked block out of storage
                drec = _sendWindow.getOldestUnackedBlock();

                // The assumption is that this record has not been acked
                if ( drec == null ) break;
                if ( drec.acks > 0 ) continue;

                int currentWait = (int)(currTime - drec.sentTime);

                // If it looks like we waited too long then speculatively resend
                if ( currentWait > adjRTO ) {
System.out.println("Soft resending message:"+drec.msg.getSequenceNumber());
                    safeSend(drec.msg);
                    currTime      = _lastSendTime;
                    drec.sentTime = currTime;
                    drec.sends++;
                }
            }
        } 
        scheduleAckIfNeeded();
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

			// Start looking for data to write after an initial startup time
			// Note: the caller needs to open the output connection and write
			// some data before we can do anything.
	        scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);

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
            safeSendAck(msg);
        } else if (msg instanceof AckMessage) {
            AckMessage    amsg   = (AckMessage) msg;
            int           seqNo  = amsg.getSequenceNumber();
            int           wStart = amsg.getWindowStart();
    		_receiverWindowSpace = amsg.getWindowSpace();

            // If they are Acking our SYN message, advance the state
            if ( seqNo == 0 && isConnecting() ) { 
                // The connection should be successful assuming that I
                // receive their SYN so move state to CONNECT_STATE
                // and get ready for activity
                prepareOpenConnection();
            } else {
            	// Record the ack
				_sendWindow.ackBlock(seqNo);
			}
        } else if (msg instanceof DataMessage) {
            // Pass the data message to the output window
            DataMessage dmsg = (DataMessage) msg;
			_receiveWindow.addData(_sequenceNumber, dmsg);  

            // Ack the Data message
            safeSendAck(msg);
        } else if (msg instanceof KeepAliveMessage) {
            KeepAliveMessage kmsg   = (KeepAliveMessage) msg;
            int              seqNo  = kmsg.getSequenceNumber();
            int              wStart = kmsg.getWindowStart();
    		_receiverWindowSpace    = kmsg.getWindowSpace();
        } else if (msg instanceof FinMessage) {
            // Stop sending data
            _receiverWindowSpace    = 0;

            // Ack the Fin message
            safeSendAck(msg);
        }

        // TODO: fill in
    }

    /**
     *  If there is data to be written then write it 
     *  and schedule next write time.
     */
    public synchronized void writeData() {
        long time = 0l;

		// If the input has not been started then wait again
		if ( _input == null ) {
	        scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);
			return;
		}

		Chunk chunk = _input.getChunk();
		sendData(chunk);
		//time = _sendWindow.getWaitTime();

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
            
            if ( isConnected() ) {
                validateAckedData();
            }
        }
    }
    //
    // -----------------------------------------------------------------
}
