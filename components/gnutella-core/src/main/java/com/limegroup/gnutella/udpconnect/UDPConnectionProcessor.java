package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.ErrorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 *  Manage a reliable udp connection for the transfer of data.
 */
public class UDPConnectionProcessor {

    private static final Log LOG =
      LogFactory.getLog(UDPConnectionProcessor.class);

    /** Define the chunk size used for data bytes */
    public static final int   DATA_CHUNK_SIZE         = 512;

    /** Handle to the output stream that is the input to this connection */
    private UDPBufferedOutputStream  _inputFromOutputStream;

    /** Handle to the input stream that is the output of this connection */
    private UDPBufferedInputStream   _outputToInputStream;

    /** A leftover chunk of data from an incoming data message.  These will 
        always be present with a data message because the first data chunk 
        will be from the GUID and the second chunk will be the payload. */
    private Chunk             _trailingChunk;

    /** The limit on space for data to be written out */
    private volatile int      _chunkLimit;

    /** The receivers windowSpace defining amount of data that receiver can
        accept */
    private volatile int      _receiverWindowSpace;

    /** Record the desired connection timeout on the connection */
    private long              _connectTimeOut         = MAX_CONNECT_WAIT_TIME;

    /** Record the desired read timeout on the connection */
    private int               _readTimeOut            = 0;

	/** Predefine a common exception if the user can't receive UDP */
	private static final IOException CANT_RECEIVE_UDP = 
	  new IOException("Can't receive UDP");

    /** Predefine a common exception if the connection times out on creation */
    private static final IOException CONNECTION_TIMEOUT = 
      new IOException("Connection timed out");

    /** Define the size of the data window */
    private static final int  DATA_WINDOW_SIZE        = 20;

    /** Define the maximum accepted write ahead packet */
    private static final int  DATA_WRITE_AHEAD_MAX    = DATA_WINDOW_SIZE + 5;

    /** The maximum number of times to try and send a data message */
    private static final int  MAX_SEND_TRIES          = 8;

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
        keep the connection alive (and firewalls open).  */
	private static final long KEEPALIVE_WAIT_TIME     = (3*1000 - 500);

	/** Define the startup time before starting to send data.  Note that
        on the receivers end, they may not be setup initially.  */
	private static final long WRITE_STARTUP_WAIT_TIME = 400;

    /** Define the default time to check for an ack to a data message */
    private static final long DEFAULT_RTO_WAIT_TIME   = 400;

    /** Define the maximum time that a connection will stay open without 
		a message being received */
    private static final long MAX_MESSAGE_WAIT_TIME   = 20 * 1000;

    /** Define the minimum wait time between ack timeout events */
    private static final long MIN_ACK_WAIT_TIME       = 5;

    /** Define the size of a small send window for increasing wait time */
    private static final long SMALL_SEND_WINDOW       = 4;

    /** Define the size of a small send window for increasing wait time */
    private static final long SMALL_WINDOW_MULTIPLE   = 3;

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

    /** The WriteRegulator controls the amount of waiting time between writes */
    private WriteRegulator    _writeRegulator;

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

    /** Flag that the writeEvent is shutdown waiting for space to write */
	private boolean           _waitingForDataSpace;

    /** Flag that the writeEvent is shutdown waiting for data to write */
	private volatile boolean  _waitingForDataAvailable;

    /** Scheduled event for ensuring that data is acked or resent */
    private UDPTimerEvent     _ackTimeoutEvent;

    /** Adhoc event for waking up the writing of data */
    private SafeWriteWakeupTimerEvent _safeWriteWakeup;

    /** The current sequence number of messages originated here */
	private long              _sequenceNumber;

	/** Transformer for mapping 2 byte sequenceNumbers of incoming messages to
		8 byte longs of essentially infinite size */
	private SequenceNumberExtender _extender;

    /** The last time that a message was sent to other host */
    private long              _lastSendTime;

    /** The last time that data was sent to other host */
    private long              _lastDataSendTime;

    /** The last time that a message was received from the other host */
	private long              _lastReceivedTime;

    /** The number of resends to take into account when scheduling ack wait */
    private int               _ackResendCount;

    /** Skip a Data Write if this flag is true */
    private boolean           _skipADataWrite;


	/** Allow a testing stub version of UDPService to be used */
	private static UDPService _testingUDPService;

    /**
     *  For testing only, allow UDPService to be overridden
     */
    public static void setUDPServiceForTesting(UDPService udpService) {
		_testingUDPService = udpService;
	}

    /**
     *  Try to kickoff a reliable udp connection. This method blocks until it 
	 *  either sucessfully establishes a connection or it times out and throws
	 *  an IOException.
     */
    public UDPConnectionProcessor(InetAddress ip, int port) throws IOException {
        // Record their address
        _ip        		         = ip;
        _port      		         = port;

        // Init default state
        _theirConnectionID       = UDPMultiplexor.UNASSIGNED_SLOT; 
		_connectionState         = PRECONNECT_STATE;
		_lastSendTime            = 0l;
        _lastDataSendTime        = 0l;
    	_chunkLimit              = DATA_WINDOW_SIZE;// TODO:This varies based 
											        // on Window fullness
    	_receiverWindowSpace     = DATA_WINDOW_SIZE; 
        _waitingForDataSpace     = false;
        _waitingForDataAvailable = false;
        _skipADataWrite          = false;
        _ackResendCount          = 0;

		// Allow UDPService to be overridden for testing
		if ( _testingUDPService == null )
			_udpService = UDPService.instance();
		else
			_udpService = _testingUDPService;

		// If UDP is not running or not workable, barf
		if ( !_udpService.isListening() || 
			 !_udpService.canReceiveSolicited() ) { //TODO: Check this
			throw CANT_RECEIVE_UDP;
		}

        // Only wake these guys up if the service is okay
		_multiplexor       = UDPMultiplexor.instance();
		_scheduler         = UDPScheduler.instance();

		// Precreate the receive window for responce reporting
        _receiveWindow   = new DataWindow(DATA_WINDOW_SIZE, 1);

		// All incoming messages get incremented
		_extender        = new SequenceNumberExtender();

        // Register yourself for incoming messages
		_myConnectionID    = _multiplexor.register(this);

		// Throw an exception if udp connection limit hit
		if ( _myConnectionID == UDPMultiplexor.UNASSIGNED_SLOT) 
			throw new IOException("no room for connection"); 

        // See if you can establish a pseudo connection 
        // which means each side can send/receive a SYN and ACK
		tryToConnect();
    }


	public InputStream getInputStream() throws IOException {
        _outputToInputStream = new UDPBufferedInputStream(this);
        return _outputToInputStream;
	}

    /**
     *  Create a special output stream that feeds byte array chunks
	 *  into this connection.
     */
	public OutputStream getOutputStream() throws IOException {
		// Start looking for data to write after an initial startup time
		// Note: the caller needs to open the output connection and write
		// some data before we can do anything.
		scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);

        _inputFromOutputStream = new UDPBufferedOutputStream(this);

        return _inputFromOutputStream;
	}

    /**
     *  Set the read timeout for the associated input stream.
     */
	public void setSoTimeout(int timeout) throws SocketException {
        _readTimeOut = timeout;
	}

	public void close() throws IOException {

        // If closed then done
        if ( _connectionState == FIN_STATE ) 
            return;

        // Shutdown keepalive event callbacks
        if ( _keepaliveEvent  != null ) 
        	_scheduler.unregister(_keepaliveEvent);

        // Shutdown write event callbacks
        if ( _writeDataEvent != null ) 
            _scheduler.unregister(_writeDataEvent);

        // Shutdown ack timeout event callbacks
        if ( _ackTimeoutEvent != null ) 
            _scheduler.unregister(_ackTimeoutEvent);

        // Unregister the safeWriteWakeup handler
        if ( _safeWriteWakeup != null ) 
            _scheduler.unregister(_safeWriteWakeup);

		// Register that the connection is closed
        _connectionState = FIN_STATE;

		// Tell the receiver that we are shutting down
    	safeSendFin();

		// TODO: should I wait for ack. Communicate state to streams.
        // There should likely be a shutdown event that resends the FIN
        // a few times if the ack doesn't come back.  As it is, something
        // is occasionally generating multiple FINs - probably from closing
        // repeatedly.
	}

    /**
     *  Prepare for handling an open connection.
     */
    private synchronized void prepareOpenConnection() {
        _connectionState = CONNECT_STATE;
        _sequenceNumber++;
        scheduleKeepAlive();

        // Create the delayed connection components
        _sendWindow      = new DataWindow(DATA_WINDOW_SIZE, 1);
        _writeRegulator  = new WriteRegulator(_sendWindow); 

        // Precreate the event for rescheduling writing to allow 
        // thread safety and faster writing 
        _safeWriteWakeup = new SafeWriteWakeupTimerEvent(Long.MAX_VALUE);
        _scheduler.register(_safeWriteWakeup);

		// TODO: keep up to date
        _chunkLimit      = _sendWindow.getWindowSpace();  
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
            LOG.debug("scheduleWriteDataEvent");
        }
    }

    /**
     *  Activate writing if we were waiting for space
     */
    private synchronized void writeSpaceActivation() {
		if ( _waitingForDataSpace ) {
			_waitingForDataSpace = false;

			// Schedule immediately
			scheduleWriteDataEvent(0);
		}
	}

    /**
     *  Activate writing if we were waiting for data to write
     */
    public synchronized void writeDataActivation() {
        // Schedule at a reasonable time
        long rto = (long)_sendWindow.getRTO();
        scheduleWriteDataEvent( _lastDataSendTime + (rto/4) );
	}

    /**
     *  Hand off the wakeup of data writing to the scheduler
     */
    public void wakeupWriteEvent() {
        if ( _waitingForDataAvailable ) {
            LOG.debug("wakupWriteEvent");
            if (_safeWriteWakeup.getEventTime() == Long.MAX_VALUE) {
                _safeWriteWakeup.updateTime(System.currentTimeMillis()+2);
                _scheduler.scheduleEvent(_safeWriteWakeup);
            }
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
     *  Return a chunk of data from the incoming data container.
     */
    public Chunk getIncomingChunk() {
        Chunk chunk;

        if ( _trailingChunk != null ) {
            chunk = _trailingChunk;
            _trailingChunk = null;
            return chunk;
        }
    
        // Fetch a block from the receiving window.
        DataRecord drec = _receiveWindow.getWritableBlock();
        if ( drec == null )
            return null;
        drec.written    = true;
        DataMessage dmsg = (DataMessage) drec.msg;

        // Record the second chunk of the message for the next read.
        _trailingChunk = dmsg.getData2Chunk();

        // Record how much space was previously available in the receive window
        int priorSpace = _receiveWindow.getWindowSpace();

		// Remove this record from the receiving window
		_receiveWindow.clearEarlyWrittenBlocks();	

        // If the receive window opened up then send a special 
        // KeepAliveMessage so that the window state can be 
        // communicated.
        if ( priorSpace == 0 || 
             (priorSpace <= SMALL_SEND_WINDOW && 
              _receiveWindow.getWindowSpace() > SMALL_SEND_WINDOW) ) {
            sendKeepAlive();
        }

        // Return the first small chunk of data from the GUID
        return dmsg.getData1Chunk();
    }

    public int getReadTimeout() {
        return _readTimeOut;
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
            // TODO: Should really verify that chunk starts at zero.  It does
            // by design.
            DataMessage dm = new DataMessage(_theirConnectionID, 
			  _sequenceNumber, chunk.data, chunk.length);
            send(dm);
			DataRecord drec   = _sendWindow.addData(dm);  
            drec.sentTime     = _lastSendTime;
			drec.sends++;

            // Record when data was sent for future scheduling
            _lastDataSendTime = _lastSendTime;

            // Update the chunk limit for fast (nonlocking) access
            _chunkLimit = _sendWindow.getWindowSpace();

			_sequenceNumber++;

            // If Acking check needs to be woken up then do it
            if ( isAckTimeoutUpdateRequired()) 
                scheduleAckIfNeeded();

            // Predecrement the other sides window until I here otherwise.
            // This prevents a cascade of sends before an Ack
            if ( _receiverWindowSpace > 0 )
                _receiverWindowSpace--;

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
    private synchronized void safeSendAck(UDPConnectionMessage msg) {
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
     *  Build and send a fin message with default error handling.
     */
    private synchronized void safeSendFin() {
        // Ack the message
        FinMessage fin = null;
        try {
            fin = new FinMessage(_theirConnectionID, _sequenceNumber);
            send(fin);
        } catch (BadPacketException bpe) {
            // This would not be good.   
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
        if(LOG.isDebugEnabled())  
            LOG.debug("send :"+msg+" ip:"+_ip+" p:"+_port+" t:"+
              _lastSendTime);
		_udpService.send(msg, _ip, _port);  
	}



    /**
     *  Schedule an ack timeout for the oldest unacked data.
     *  If no acks are pending, then do nothing.
     */
    private synchronized void scheduleAckIfNeeded() {
        DataRecord drec = _sendWindow.getOldestUnackedBlock();
        if ( drec != null ) {
            int rto         = _sendWindow.getRTO();
			if (rto == 0) 
				rto = (int) DEFAULT_RTO_WAIT_TIME;
            long waitTime    = drec.sentTime + ((long)rto);

            // If there was a resend then base the wait off of current time
            if ( _ackResendCount > 0 ) {
                waitTime    = _lastSendTime + ((long)rto);
               _ackResendCount = 0;
            }

            // Enforce a mimimum waitTime from now
            long minTime = System.currentTimeMillis() + MIN_ACK_WAIT_TIME;
            waitTime = Math.max(waitTime, minTime);

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

        if (_sendWindow.acksAppearToBeMissing(currTime, 3)) {

            // if the older blocks ack have been missing for a while
            // resend them.

            // Calculate a good time to wait
            int rto      = _sendWindow.getRTO();
            int adjRTO   = (rto * 5) / 2;
            int waitTime = rto / 2;

            long start   = _sendWindow.getWindowStart();

            if(LOG.isDebugEnabled())  
              LOG.debug("Soft resend data:"+ start+ " rto:"+rto+
                " uS:"+_sendWindow.getUsedSpots());

            // Scale back on the writing speed if you are hitting limits
            _writeRegulator.hitResendTimeout();

            DataRecord drec;
            int        numResent = 0;

            // Resend up to 1
            for (int i = 0; i < 1; i++) {

                // Get the oldest unacked block out of storage
                drec = _sendWindow.getOldestUnackedBlock();

                // The assumption is that this record has not been acked
                if ( drec == null ) break;
                if ( drec.acks > 0 ) continue;

				// If too many sends then abort connection
				if ( drec.sends > MAX_SEND_TRIES+1 ) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Tried too many send on:"+
                          drec.msg.getSequenceNumber());
					closeAndCleanup();
					return;
				}

                int currentWait = (int)(currTime - drec.sentTime);

                // If it looks like we waited too long then speculatively resend
                if ( currentWait > adjRTO ) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Soft resending message:"+
                          drec.msg.getSequenceNumber());
                    safeSend(drec.msg);
                    currTime      = _lastSendTime;
                    drec.sentTime = currTime;
                    drec.sends++;
                    numResent++;
                }
            }
            
            // Delay subsequent resends of data based on number resent
            _ackResendCount = numResent;
            if ( numResent > 0 )
                _skipADataWrite          = true;
        } 
        scheduleAckIfNeeded();
    }

    /**
     *  Close and cleanup by unregistering this connection and sending a Fin.
     */
    private synchronized void closeAndCleanup() {
		try {
			close();
		} catch (IOException ioe) {
			ErrorService.error(ioe);
		}
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

                if ( waitTime > _connectTimeOut )
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

		// Extend the msgs sequenceNumber to 8 bytes based on past state
		msg.extendSequenceNumber(
		  _extender.extendSequenceNumber(msg.getSequenceNumber()) );

		// Record when the last message was received
		_lastReceivedTime = System.currentTimeMillis();
        if(LOG.isDebugEnabled())  
            LOG.debug("handleMessage :"+msg+" t:"+_lastReceivedTime);

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

			// Extend the windowStart to 8 bytes the same as the sequenceNumber
			amsg.extendWindowStart(
		  	  _extender.extendSequenceNumber(amsg.getWindowStart()) );

            long          seqNo  = amsg.getSequenceNumber();
            long          wStart = amsg.getWindowStart();
    		int           priorR = _receiverWindowSpace;
    		_receiverWindowSpace = amsg.getWindowSpace();

			// Reactivate writing if required
			if ( priorR == 0 && _receiverWindowSpace > 0 )
    			writeSpaceActivation();

            // If they are Acking our SYN message, advance the state
            if ( seqNo == 0 && isConnecting() ) { 
                // The connection should be successful assuming that I
                // receive their SYN so move state to CONNECT_STATE
                // and get ready for activity
                prepareOpenConnection();
            } else {
            	// Record the ack
				_sendWindow.ackBlock(seqNo);

				// Ensure that all messages up to sent windowStart are acked
				_sendWindow.pseudoAckToReceiverWindow(amsg.getWindowStart());
				
				// Clear out the acked blocks at window start
				_sendWindow.clearLowAckedBlocks();	

                // Update the chunk limit for fast (nonlocking) access
                _chunkLimit = _sendWindow.getWindowSpace();
			}
        } else if (msg instanceof DataMessage) {
            // Pass the data message to the output window
            DataMessage dmsg = (DataMessage) msg;

            // If message is more than limit beyond window, then throw it away
            long seqNo     = dmsg.getSequenceNumber();
            long baseSeqNo = _receiveWindow.getWindowStart();
            if ( seqNo > (baseSeqNo + DATA_WRITE_AHEAD_MAX) ) {
                if(LOG.isDebugEnabled())  
                    LOG.debug("Received block num too far ahead: "+ seqNo);
               return;
            }

            // Make sure the data is not before the window start
            if ( seqNo >= baseSeqNo ) {
				// Record the receipt of the data in the receive window
                DataRecord drec = _receiveWindow.addData(dmsg);  
            	drec.ackTime = System.currentTimeMillis();
				drec.acks++;

				// Notify InputStream that data is available for reading
				if ( _outputToInputStream != null )
					_outputToInputStream.wakeup();
            } else {
                if(LOG.isDebugEnabled())  
                    LOG.debug("Received duplicate block num: "+ 
                      dmsg.getSequenceNumber());
            }

            // Ack the Data message
            safeSendAck(msg);
        } else if (msg instanceof KeepAliveMessage) {
            KeepAliveMessage kmsg   = (KeepAliveMessage) msg;

			// Extend the windowStart to 8 bytes the same as the sequenceNumber
			kmsg.extendWindowStart(
		  	  _extender.extendSequenceNumber(kmsg.getWindowStart()) );

            long             seqNo  = kmsg.getSequenceNumber();
			// TODO: make use of this as a pseudo ack
            long             wStart = kmsg.getWindowStart(); 
    		int              priorR = _receiverWindowSpace;
    		_receiverWindowSpace    = kmsg.getWindowSpace();

			// Ensure that all messages up to sent windowStart are acked
			_sendWindow.pseudoAckToReceiverWindow(wStart);
				

			// Reactivate writing if required
			if ( priorR == 0 && _receiverWindowSpace > 0 )
    			writeSpaceActivation();

        } else if (msg instanceof FinMessage) {
            // Stop sending data
            _receiverWindowSpace    = 0;

            // Ack the Fin message
            safeSendAck(msg);

			// If a fin message is received then close connection
			closeAndCleanup();
        }

        // TODO: fill in
    }

    /**
     *  If there is data to be written then write it 
     *  and schedule next write time.
     */
    public synchronized void writeData() {

        while (true) {
            // If the input has not been started then wait again
            if ( _inputFromOutputStream == null ) {
                scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);
                return;
            }

            // If someone wanted us to wait a bit then don't send data now
            if ( _skipADataWrite ) {
                _skipADataWrite = false;
                return;
            }

            // Reset special flags for long wait times
            _waitingForDataAvailable = false;
            _waitingForDataSpace = false;

            // If there is room to send something then send data if available
            if ( getChunkLimit() > 0 ) {
                // Get data and send it
                Chunk chunk = _inputFromOutputStream.getChunk();
                if ( chunk != null )
                    sendData(chunk);
            } else {
                // if no room to send data then wait for the window to Open
                scheduleWriteDataEvent(Long.MAX_VALUE);
                _waitingForDataSpace = true;
            }

            // Don't wait for next write if there is no chunk available.
            // Writes will get rescheduled if a chunk becomes available.
            synchronized(_inputFromOutputStream) {
                if ( _inputFromOutputStream.getPendingChunks() == 0 ) {
                    scheduleWriteDataEvent(Long.MAX_VALUE);
                    _waitingForDataAvailable = true;
                    return;
                }
            }
            
            // Compute how long to wait
            // TODO: Simplify experimental algorithm and plug it in
            //long waitTime = (long)_sendWindow.getRTO() / 6l;
            long currTime = System.currentTimeMillis();
            long waitTime = _writeRegulator.getSleepTime(currTime);

            if ( _receiverWindowSpace <= SMALL_SEND_WINDOW ) { 
                // If send window getting small
                // then wait longer
                waitTime *= SMALL_WINDOW_MULTIPLE;

                // Scale back on the writing speed if you are hitting limits
                _writeRegulator.hitZeroWindow();
            }

            // Initially ensure waitTime is not too low
            if (waitTime == 0 && _sequenceNumber < 10 ) 
                waitTime = DEFAULT_RTO_WAIT_TIME;

            // Only wait if the waitTime is more than zero
            if ( waitTime > 0 ) {
                long time = System.currentTimeMillis() + waitTime;
                scheduleWriteDataEvent(time);
                break;
            }
        }
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
            if(LOG.isDebugEnabled())  
                LOG.debug("keepalive: "+ time);

			// Make sure that some messages are received within timeframe
			if ( isConnected() && 
				 _lastReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {
//          LOG.debug("shutdown");
				// If no incoming messages for very long time then 
				// close connection
				closeAndCleanup();
				return;
			}
            
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
            if(LOG.isDebugEnabled())  
                LOG.debug("end keepalive: "+ System.currentTimeMillis());
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
            if(LOG.isDebugEnabled())  
                LOG.debug("data timeout :"+ System.currentTimeMillis());
            long time = System.currentTimeMillis();

			// Make sure that some messages are received within timeframe
			if ( isConnected() && 
				 _lastReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {
				// If no incoming messages for very long time then 
				// close connection
				closeAndCleanup();
				return;
			}

			// If still connected then handle then try to write some data
            if ( isConnected() ) {
                writeData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end data timeout: "+ System.currentTimeMillis());
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
            if(LOG.isDebugEnabled())  
                LOG.debug("ack timeout: "+ System.currentTimeMillis());
            if ( isConnected() ) {
                validateAckedData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end ack timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  This is an event that wakes up writing with a given delay
     */
    class  SafeWriteWakeupTimerEvent extends UDPTimerEvent {

        public SafeWriteWakeupTimerEvent(long time) {
            super(time);
        }

        public void handleEvent() {
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
            if ( isConnected() ) {
                writeDataActivation();
            }
            _eventTime = Long.MAX_VALUE;
            _scheduler.scheduleEvent(this);
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
        }
    }
    //
    // -----------------------------------------------------------------
}
