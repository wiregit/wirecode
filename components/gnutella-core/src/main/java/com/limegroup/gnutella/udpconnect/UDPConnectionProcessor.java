package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.util.NetworkUtils;

/** 
 *  Manage a reliable udp connection for the transfer of data.
 */
public class UDPConnectionProcessor {

    private static final Log LOG =
      LogFactory.getLog(UDPConnectionProcessor.class);

    /** Define the chunk size used for data bytes */
    public static final int   DATA_CHUNK_SIZE         = 512;

    /** Define the maximum chunk size read for data bytes
        before we will blow out the connection */
    public static final int   MAX_DATA_SIZE           = 4096;

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

    /** Record the desired read timeout on the connection, defaults to 1 minute */
    private int               _readTimeOut            = 1 * 60 * 1000;

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
    private Acceptor          _acceptor;

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
    private static final long SMALL_SEND_WINDOW       = 2;

    /** Ensure that writing takes a break every 4 writes so other 
        synchronized activity can take place */
    private static final long MAX_WRITE_WITHOUT_SLEEP = 4;

    /** Delay the write wakeup event a little so that it isn't constantly
        firing - This should achieve part of nagles algorithm.  */
    private static final long WRITE_WAKEUP_DELAY_TIME = 10;

    /** Delay the write events by one second if there is nothing to do */
    private static final long NOTHING_TO_DO_DELAY     = 1000;

    /** Time to wait after a close before everything is totally shutdown. */
    private static final long SHUTDOWN_DELAY_TIME     = 400;

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
	private volatile byte     _theirConnectionID;

    /** The status of the connection */
	private int               _connectionState;

    /** Scheduled event for keeping connection alive  */
    private UDPTimerEvent     _keepaliveEvent;

    /** Scheduled event for writing data appropriately over time  */
    private UDPTimerEvent     _writeDataEvent;

    /** Scheduled event for cleaning up at end of connection life  */
    private UDPTimerEvent     _closedCleanupEvent;

    /** Flag that the writeEvent is shutdown waiting for space to write */
	private boolean           _waitingForDataSpace;

    /** Flag that the writeEvent is shutdown waiting for data to write */
	private volatile boolean  _waitingForDataAvailable;

    /** Flag saying that a Fin packet has been acked on shutdown */
    private boolean           _waitingForFinAck;

    /** Scheduled event for ensuring that data is acked or resent */
    private UDPTimerEvent     _ackTimeoutEvent;

    /** Adhoc event for waking up the writing of data */
    private SafeWriteWakeupTimerEvent _safeWriteWakeup;

    /** The current sequence number of messages originated here */
    private long              _sequenceNumber;

    /** The sequence number of a pending fin message */
	private long              _finSeqNo;

	/** Transformer for mapping 2 byte sequenceNumbers of incoming ACK 
        messages to 8 byte longs of essentially infinite size - note Acks 
        echo our seqNo */
	private SequenceNumberExtender _localExtender;

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

    /** Keep track of the reason for shutting down */
    private byte              _closeReasonCode;

    ////////////////////////////////////////////
    // Some settings related to skipping acks
    ///////////////////////////////////////////
    
    /** Whether to skip any acks at all */
    private final boolean _skipAcks = DownloadSettings.SKIP_ACKS.getValue();
    /** How long each measuring period is */
    private final int _period = DownloadSettings.PERIOD_LENGTH.getValue();
    
    /** How many periods to keep track of */
    private static final int _periodHistory = DownloadSettings.PERIOD_LENGTH.getValue();
    
    /** 
     * By how much does the current period need to deviate from the average
     * before we start acking.
     */
    private final float _deviation = DownloadSettings.DEVIATION.getValue();
    
    /** Do not skip more than this many acks in a row */
    private static final int _maxSkipAck = DownloadSettings.MAX_SKIP_ACKS.getValue();
    
    /** how many data packets we got each second */
    private final int [] _periods = new int[_periodHistory];
    
    /** index within that array, points to the last period */
    private int _currentPeriodId;
    
    /** How many data packets we received this period */
    private int _packetsThisPeriod;
    
    /** whether we have enough data */
    private boolean _enoughData;
    
    /** when the current second started */
    private long _lastPeriod;
    
    /** how many acks we skipped in a row vs. total */
    private int _skippedAcks, _skippedAcksTotal;
    
    /** how many packets we got in total */
    private int _totalDataPackets;
    
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

        if(LOG.isDebugEnabled())  {
            LOG.debug("Creating UDPConn ip:"+ip+" port:"+port);
        }

        // Init default state
        _theirConnectionID       = UDPMultiplexor.UNASSIGNED_SLOT; 
		_connectionState         = PRECONNECT_STATE;
		_lastSendTime            = 0l;
        _lastDataSendTime        = 0l;
    	_chunkLimit              = DATA_WINDOW_SIZE;
    	_receiverWindowSpace     = DATA_WINDOW_SIZE; 
        _waitingForDataSpace     = false;
        _waitingForDataAvailable = false;
        _waitingForFinAck        = false;  
        _skipADataWrite          = false;
        _ackResendCount          = 0;
        _closeReasonCode         = FinMessage.REASON_NORMAL_CLOSE;

		// Allow UDPService to be overridden for testing
		if ( _testingUDPService == null )
			_udpService = UDPService.instance();
		else
			_udpService = _testingUDPService;

		// If UDP is not running or not workable, barf
		if ( !_udpService.isListening() || 
			 !_udpService.canDoFWT() ) { 
			throw CANT_RECEIVE_UDP;
		}

        // Only wake these guys up if the service is okay
		_multiplexor       = UDPMultiplexor.instance();
		_scheduler         = UDPScheduler.instance();
        _acceptor          = RouterService.getAcceptor();

		// Precreate the receive window for responce reporting
        _receiveWindow   = new DataWindow(DATA_WINDOW_SIZE, 1);

		// All incoming seqNo and windowStarts get extended
        // Acks seqNo need to be extended separately
		_localExtender     = new SequenceNumberExtender();
        _extender          = new SequenceNumberExtender();

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
        if (_outputToInputStream == null) {
            _outputToInputStream = new UDPBufferedInputStream(this);
        }
        return _outputToInputStream;
	}

    /**
     *  Create a special output stream that feeds byte array chunks
	 *  into this connection.
     */
	public OutputStream getOutputStream() throws IOException {
        if ( _inputFromOutputStream == null ) {
            // Start looking for data to write after an initial startup time
            // Note: the caller needs to open the output connection and write
            // some data before we can do anything.
            scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);

            _inputFromOutputStream = new UDPBufferedOutputStream(this);
        }
        return _inputFromOutputStream;
	}

    /**
     *  Set the read timeout for the associated input stream.
     */
	public void setSoTimeout(int timeout) throws SocketException {
        _readTimeOut = timeout;
	}

	public synchronized void close() throws IOException {
	    if (LOG.isDebugEnabled())
	        LOG.debug("closing connection",new Exception());
	    
        // If closed then done
        if ( _connectionState == FIN_STATE ) 
            throw new IOException("already closed");

        // Shutdown keepalive event callbacks
        if ( _keepaliveEvent  != null ) 
        	_keepaliveEvent.unregister();

        // Shutdown write event callbacks
        if ( _writeDataEvent != null ) 
            _writeDataEvent.unregister();

        // Shutdown ack timeout event callbacks
        if ( _ackTimeoutEvent != null ) 
            _ackTimeoutEvent.unregister();

        // Unregister the safeWriteWakeup handler
        if ( _safeWriteWakeup != null ) 
            _safeWriteWakeup.unregister();

		// Register that the connection is closed
        _connectionState = FIN_STATE;

        // Track incoming ACKS for an ack of FinMessage
        _waitingForFinAck = true;  

		// Tell the receiver that we are shutting down
    	safeSendFin();

        // Wakeup any sleeping readers
        if ( _outputToInputStream != null )
            _outputToInputStream.wakeup();

        // Wakeup any sleeping writers
        if ( _inputFromOutputStream != null )
            _inputFromOutputStream.connectionClosed();

        // Register for a full cleanup after a slight delay
        if (_closedCleanupEvent==null) {
        	_closedCleanupEvent = new ClosedConnectionCleanupTimerEvent(
        			System.currentTimeMillis() + SHUTDOWN_DELAY_TIME,this);
        	LOG.debug("registering a closedCleanupEvent");
        	_scheduler.register(_closedCleanupEvent);
        }
	}

    private synchronized void finalClose() {

        // Send one final Fin message if not acked.
        if (_waitingForFinAck)
            safeSendFin();

        // Unregister for message multiplexing
        _multiplexor.unregister(this);

        // Clean up my caller
        _closedCleanupEvent.unregister();

        // TODO: Clear up state to streams? Might need more time. Anything else?
    }

    /**
     *  Return the InetAddress.
     */
    public InetAddress getInetAddress() {
        return _ip;
    }

    /**
     *  Do some magic to get the local address if available.
     */
    public InetAddress getLocalAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByName(
              NetworkUtils.ip2string(_acceptor.getAddress(false)));
        } catch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocalHost();
            } catch (UnknownHostException uhe2) {
                lip = null;
            }
        }

        return lip;
    }

    /**
     *  Prepare for handling an open connection.
     */
    private void prepareOpenConnection() {
        _connectionState = CONNECT_STATE;
        _sequenceNumber=1;
        scheduleKeepAlive();

        // Create the delayed connection components
        _sendWindow      = new DataWindow(DATA_WINDOW_SIZE, 1);
        _writeRegulator  = new WriteRegulator(_sendWindow); 

        // Precreate the event for rescheduling writing to allow 
        // thread safety and faster writing 
        _safeWriteWakeup = new SafeWriteWakeupTimerEvent(Long.MAX_VALUE,this);
        _scheduler.register(_safeWriteWakeup);

		// Keep chunkLimit in sync with window space
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
          new KeepAliveTimerEvent(_lastSendTime + KEEPALIVE_WAIT_TIME,this);

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
                    new WriteDataTimerEvent(time,this);

                // Register writeData event for future use
                _scheduler.register(_writeDataEvent);
            } else {
                _writeDataEvent.updateTime(time);
            }

            // Notify the scheduler that there is a new write event/time
            _scheduler.scheduleEvent(_writeDataEvent);
            if(LOG.isDebugEnabled())  {
                LOG.debug("scheduleWriteDataEvent :"+time);
            }
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
                _safeWriteWakeup.updateTime(System.currentTimeMillis()+
                  WRITE_WAKEUP_DELAY_TIME);
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
                    new AckTimeoutTimerEvent(time,this);

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
        return (_connectionState == CONNECT_STATE && 
                _theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT);
    }

    /**
     *  Test whether the connection is closed
     */
    public synchronized boolean isClosed() {
        return (_connectionState == FIN_STATE);
    }

    /**
     *  Test whether the connection is not fully setup
     */
	public synchronized boolean isConnecting() {
	    return !isClosed() && 
	    	(_connectionState == PRECONNECT_STATE ||
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
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION); 
        }
    }

    /**
     *  Convenience method for sending data.  
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

            if( LOG.isDebugEnabled() && 
               (_lastSendTime - _lastDataSendTime) > 2000)  {
                LOG.debug("SendData lag = "+
                  (_lastSendTime - _lastDataSendTime));
            }

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

        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
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
          
          	if (LOG.isDebugEnabled()) {
          	    LOG.debug("total data packets "+_totalDataPackets+
          	            " total acks skipped "+_skippedAcksTotal+
          	            " skipped this session "+ _skippedAcks);
          	}
          	_skippedAcks=0;
            send(ack);
        } catch (BadPacketException bpe) {
            // This would not be good.   
            ErrorService.error(bpe);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }

    /**
     *  Build and send a fin message with default error handling.
     */
    private synchronized void safeSendFin() {
        // Ack the message
        FinMessage fin = null;
        try {
            // Record sequence number for ack monitoring
            // Not that it should increment anymore anyways
            _finSeqNo = _sequenceNumber;

            // Send the FinMessage
            fin = new FinMessage(_theirConnectionID, _sequenceNumber,
              _closeReasonCode);
            send(fin);
        } catch(IllegalArgumentException iae) {
            // Report an error since this shouldn't ever happen
            ErrorService.error(iae);
            LOG.warn("calling recursively closeAndCleanup");
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
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
            closeAndCleanup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }


    /**
     *  Send a message on to the UDPService
     */
	private synchronized void send(UDPConnectionMessage msg) 
      throws IllegalArgumentException {
		_lastSendTime = System.currentTimeMillis();
        if(LOG.isDebugEnabled())  {
            LOG.debug("send :"+msg+" ip:"+_ip+" p:"+_port+" t:"+
              _lastSendTime);
            if ( msg instanceof FinMessage ) { 
            	Exception ex = new Exception();
            	LOG.debug("", ex);
            }
        }
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

        if (_sendWindow.acksAppearToBeMissing(currTime, 1)) {

            // if the older blocks ack have been missing for a while
            // resend them.

            // Calculate a good maximum time to wait
            int rto      = _sendWindow.getRTO();

            long start   = _sendWindow.getWindowStart();

            if(LOG.isDebugEnabled())  
              LOG.debug("Soft resend check:"+ start+ " rto:"+rto+
                " uS:"+_sendWindow.getUsedSpots()+" localSeq:"+_sequenceNumber);

            DataRecord drec;
            DataRecord drecNext;
            int        numResent = 0;

            // Resend up to 1 packet at a time
            resend: {

                // Get the oldest unacked block out of storage
                drec     = _sendWindow.getOldestUnackedBlock();
                int expRTO = (rto * (int)Math.pow(2,drec.sends-1));
                if (LOG.isDebugEnabled())
                	LOG.debug(" exponential backoff is now "+expRTO);

                // Check if the next drec is acked
                if(_sendWindow.countHigherAckBlocks() >0){
                	expRTO*=0.75;
                	if (LOG.isDebugEnabled())
                		LOG.debug(" higher acked blocks, adjusting exponential backoff is now "+
                    		expRTO);
                }

                // The assumption is that this record has not been acked
                if ( drec == null || drec.acks > 0) 
                	break resend;
                

				// If too many sends then abort connection
				if ( drec.sends > MAX_SEND_TRIES+1 ) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Tried too many send on:"+
                          drec.msg.getSequenceNumber());
					closeAndCleanup(FinMessage.REASON_TOO_MANY_RESENDS);
					return;
				}

                int currentWait = (int)(currTime - drec.sentTime);

                // If it looks like we waited too long then speculatively resend
                // Case 1: We waited 150% of RTO and next packet had been acked
                // Case 2: We waited 200% of RTO 
                if ( currentWait  > expRTO)
					 {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Soft resending message:"+
                          drec.msg.getSequenceNumber());
                    safeSend(drec.msg);

                    // Scale back on the writing speed if you are hitting limits
                    _writeRegulator.addMessageFailure();
                    _writeRegulator.hitResendTimeout();

                    currTime      = _lastSendTime;
                    drec.sentTime = currTime;
                    drec.sends++;
                    numResent++;
                } else 
                	LOG.debug(" not resending message ");
                
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
    private synchronized void closeAndCleanup(byte reasonCode) {
        _closeReasonCode = reasonCode;
		try {
			close();
		} catch (IOException ioe) {}
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
            SynMessage synMsg = new SynMessage(_myConnectionID);

            // Keep sending and waiting until you get a Syn and an Ack from 
            // the other side of the connection.
			while ( true ) { 

                // If we have received their connectionID then use it
			    synchronized(this){
			        
			        if (!isConnecting()) 
			            break;
			        
			        if ( waitTime > _connectTimeOut ) { 
			            _connectionState = FIN_STATE; 
			            _multiplexor.unregister(this);
			            throw CONNECTION_TIMEOUT;
			        }
			        
			        if (_theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT &&
			                _theirConnectionID != synMsg.getConnectionID()) {
			            synMsg = 
			                new SynMessage(_myConnectionID, _theirConnectionID);
			        } 
			    }

				// Send a SYN packet with our connectionID 
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
    public void handleMessage(UDPConnectionMessage msg) {
        boolean doYield = false;  // Trigger a yield at the end if 1k available

        synchronized (this) {

            // Record when the last message was received
            _lastReceivedTime = System.currentTimeMillis();
            if(LOG.isDebugEnabled())  
                LOG.debug("handleMessage :"+msg+" t:"+_lastReceivedTime);

            if (msg instanceof SynMessage) {
                // Extend the msgs sequenceNumber to 8 bytes based on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

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
                // Extend the msgs sequenceNumber to 8 bytes based on past state
                // Note that this sequence number is of local origin
                msg.extendSequenceNumber(
                  _localExtender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                AckMessage    amsg   = (AckMessage) msg;

                // Extend the windowStart to 8 bytes the same as the 
                // sequenceNumber 
                amsg.extendWindowStart(
                  _localExtender.extendSequenceNumber(amsg.getWindowStart()) );

                long          seqNo  = amsg.getSequenceNumber();
                long          wStart = amsg.getWindowStart();
                int           priorR = _receiverWindowSpace;
                _receiverWindowSpace = amsg.getWindowSpace();

                // Adjust the receivers window space with knowledge of
                // how many extra messages we have sent since this ack
                if ( _sequenceNumber > wStart ) 
                    _receiverWindowSpace = 
					  DATA_WINDOW_SIZE + (int) (wStart - _sequenceNumber);
                    //_receiverWindowSpace += (wStart - _sequenceNumber);

                // Reactivate writing if required
                if ( (priorR == 0 || _waitingForDataSpace) && 
                     _receiverWindowSpace > 0 ) {
                    if(LOG.isDebugEnabled())  
                        LOG.debug(" -- ACK wakeup");
                    writeSpaceActivation();
                }


                // If they are Acking our SYN message, advance the state
                if ( seqNo == 0 && isConnecting() && _connectionState == PRECONNECT_STATE ) { 
                    // The connection should be successful assuming that I
                    // receive their SYN so move state to CONNECT_STATE
                    // and get ready for activity
                    prepareOpenConnection();
                } else if ( _waitingForFinAck && seqNo == _finSeqNo ) { 
                    // A fin message has been acked on shutdown
                    _waitingForFinAck = false;
                } else if (_connectionState == CONNECT_STATE) {
                    // Record the ack
                    _sendWindow.ackBlock(seqNo);
                    _writeRegulator.addMessageSuccess();

                    // Ensure that all messages up to sent windowStart are acked
                    _sendWindow.pseudoAckToReceiverWindow(amsg.getWindowStart());
                    
                    // Clear out the acked blocks at window start
                    _sendWindow.clearLowAckedBlocks();	

                    // Update the chunk limit for fast (nonlocking) access
                    _chunkLimit = _sendWindow.getWindowSpace();
                }
            } else if (msg instanceof DataMessage) {
                
                // Extend the msgs sequenceNumber to 8 bytes based on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                // Pass the data message to the output window
                DataMessage dmsg = (DataMessage) msg;

                // If message is more than limit beyond window, 
                // then throw it away
                long seqNo     = dmsg.getSequenceNumber();
                long baseSeqNo = _receiveWindow.getWindowStart();

                // If data is too large then blow out the connection
                // before any damage is done
                if (dmsg.getDataLength() > MAX_DATA_SIZE) {
                    closeAndCleanup(FinMessage.REASON_LARGE_PACKET);
                    return;
                }

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
                    if ( _outputToInputStream != null &&
                    		seqNo==baseSeqNo) {
                        _outputToInputStream.wakeup();

                        // Get the reader moving after 1k received 
                        if ( (seqNo % 2) == 0)
                            doYield = true; 
                    }
                } else {
                    if(LOG.isDebugEnabled())  
                        LOG.debug("Received duplicate block num: "+ 
                          dmsg.getSequenceNumber());
                }

                //if this is the first data message we get, start the period now
                if (_lastPeriod == 0)
                    _lastPeriod = _lastReceivedTime;
                
                _packetsThisPeriod++;
                _totalDataPackets++;
                
                //if we have enough history, see if we should skip an ack
                if (_skipAcks && _enoughData && _skippedAcks < _maxSkipAck) {
                    float average = 0;
                    for (int i = 0;i < _periodHistory;i++)
                        average+=_periods[i];
                    
                    average /= _periodHistory;
                    
                    // skip an ack if the rate at which we receive data has not dropped sharply
                    if (_periods[_currentPeriodId] > average / _deviation) {
                        _skippedAcks++;
                        _skippedAcksTotal++;
                    }
                    else
                        safeSendAck(msg);
                }
                else
                    safeSendAck(msg);
                
                // if this is the end of a period, record how many data packets we got
                if (_lastReceivedTime - _lastPeriod >= _period) {
                    _lastPeriod = _lastReceivedTime;
                    _currentPeriodId++;
                    if (_currentPeriodId >= _periodHistory) {
                        _currentPeriodId=0;
                        _enoughData=true;
                    }
                    _periods[_currentPeriodId]=_packetsThisPeriod;
                    _packetsThisPeriod=0;
                }
                
            } else if (msg instanceof KeepAliveMessage) {
                // No need to extend seqNo on KeepAliveMessage since it is zero
                KeepAliveMessage kmsg   = (KeepAliveMessage) msg;
                // Extend the windowStart to 8 bytes the same 
                // as the Ack
                kmsg.extendWindowStart(
                  _localExtender.extendSequenceNumber(kmsg.getWindowStart()) );

                long             seqNo  = kmsg.getSequenceNumber();
                long             wStart = kmsg.getWindowStart(); 
                int              priorR = _receiverWindowSpace;
                _receiverWindowSpace    = kmsg.getWindowSpace();

                // Adjust the receivers window space with knowledge of
                // how many extra messages we have sent since this ack
                if ( _sequenceNumber > wStart ) 
                    _receiverWindowSpace = 
					  DATA_WINDOW_SIZE + (int) (wStart - _sequenceNumber);
                    //_receiverWindowSpace += (wStart - _sequenceNumber);

                // If receiving KeepAlives when closed, send another FinMessage
                if ( isClosed() ) {
                    safeSendFin();
                }

                // Ensure that all messages up to sent windowStart are acked
                // Note, you could get here preinitialization - in which case,
                // do nothing.
                if ( _sendWindow != null ) {  
                    _sendWindow.pseudoAckToReceiverWindow(wStart);
                    
                    // Reactivate writing if required
                    if ( (priorR == 0 || _waitingForDataSpace) && 
                         _receiverWindowSpace > 0 ) {
                        if(LOG.isDebugEnabled()) 
                            LOG.debug(" -- KA wakeup");
                        writeSpaceActivation();
                    }
                }


            } else if (msg instanceof FinMessage) {
                // Extend the msgs sequenceNumber to 8 bytes based on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                // Stop sending data
                _receiverWindowSpace    = 0;

                // Ack the Fin message
                safeSendAck(msg);

                // If a fin message is received then close connection
                if ( !isClosed() )
                    closeAndCleanup(FinMessage.REASON_YOU_CLOSED);
            }
        }

        // Yield to the reading thread if it has been woken up 
        // in the hope that it will start reading immediately 
        // rather than getting backlogged
        if ( doYield ) 
            Thread.yield();
    }

    /**
     *  If there is data to be written then write it 
     *  and schedule next write time.
     */
    public synchronized void writeData() {

        // Make sure we don't write without a break for too long
        int noSleepCount = 0;
        
        while (true) {
            // If the input has not been started then wait again
            if ( _inputFromOutputStream == null ) {
                scheduleWriteDataEvent(WRITE_STARTUP_WAIT_TIME);
                return;
            }

            // Reset special flags for long wait times
            _waitingForDataAvailable = false;
            _waitingForDataSpace = false;

            // If someone wanted us to wait a bit then don't send data now
            if ( _skipADataWrite ) {
                _skipADataWrite = false;
            } else {  // Otherwise, it is safe to send some data
            
                // If there is room to send something then send data 
                // if available
                if ( getChunkLimit() > 0 ) {
                    // Get data and send it
                    Chunk chunk = _inputFromOutputStream.getChunk();
                    if ( chunk != null )
                        sendData(chunk);
                } else {
                    // if no room to send data then wait for the window to Open
                    // Don't wait more than 1 second for sanity checking 
                    scheduleWriteDataEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _waitingForDataSpace = true;

            		if(LOG.isDebugEnabled())  
                		LOG.debug("Shutdown SendData cL:"+_chunkLimit+
						  " rWS:"+ _receiverWindowSpace);
                }
            }

            // Don't wait for next write if there is no chunk available.
            // Writes will get rescheduled if a chunk becomes available.
            synchronized(_inputFromOutputStream) {
                if ( _inputFromOutputStream.getPendingChunks() == 0 ) {
                    // Don't wait more than 1 second for sanity checking 
                    scheduleWriteDataEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _waitingForDataAvailable = true;
            		if(LOG.isDebugEnabled())  
                		LOG.debug("Shutdown SendData no pending");
                    return;
                }
            }
            
            // Compute how long to wait
            // TODO: Simplify experimental algorithm and plug it in
            //long waitTime = (long)_sendWindow.getRTO() / 6l;
            long currTime = System.currentTimeMillis();
            long waitTime = _writeRegulator.getSleepTime(currTime, 
              _receiverWindowSpace);

            // If we are getting too close to the end of window, make a note
            if ( _receiverWindowSpace <= SMALL_SEND_WINDOW ) { 

                // Scale back on the writing speed if you are hitting limits
                if ( _receiverWindowSpace <= 1 ) 
                    _writeRegulator.hitZeroWindow();
            }

            // Initially ensure waitTime is not too low
            if (waitTime == 0 && _sequenceNumber < 10 ) 
                waitTime = DEFAULT_RTO_WAIT_TIME;

            // Enforce some minimal sleep time if we have been in tight loop
            // This will allow handleMessages to get done if pending
            if (noSleepCount >= MAX_WRITE_WITHOUT_SLEEP) {
                waitTime += 1;
            }

            // Only wait if the waitTime is more than zero
            if ( waitTime > 0 ) {
                long time = System.currentTimeMillis() + waitTime;
                scheduleWriteDataEvent(time);
                break;
            }

            // Count how long we are sending without a sleep
            noSleepCount++;
        }
    }

    /** 
     *  Define what happens when a keepalive timer fires.
     */
    static class KeepAliveTimerEvent extends UDPTimerEvent {
        
    	public KeepAliveTimerEvent(long time,UDPConnectionProcessor proc) {
    		super(time,proc);
    	}

        protected void doActualEvent(UDPConnectionProcessor udpCon) {


            long time = System.currentTimeMillis();
            
            if(LOG.isDebugEnabled())  
                LOG.debug("keepalive: "+ time);

            // If connection closed, then make sure that keepalives have ended

            if (udpCon.isClosed() ) {
                udpCon._keepaliveEvent.unregister();
            }

			// Make sure that some messages are received within timeframe
			if ( udpCon.isConnected() && 
				 udpCon._lastReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {

                LOG.debug("Keepalive generated shutdown");

				// If no incoming messages for very long time then 
				// close connection
                udpCon.closeAndCleanup(FinMessage.REASON_TIMEOUT);
				return;
			}
            
            // If reevaluation of the time still requires a keepalive then send
            if ( time+1 >= (udpCon._lastSendTime + KEEPALIVE_WAIT_TIME) ) {
                if ( udpCon.isConnected() ) {
                    udpCon.sendKeepAlive();
                } else {
                    return;
                }
            }

            // Reschedule keepalive timer
            _eventTime = udpCon._lastSendTime + KEEPALIVE_WAIT_TIME;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnabled())  
                LOG.debug("end keepalive: "+ System.currentTimeMillis());
        }
    }
    /** 
     *  Define what happens when a WriteData timer event fires.
     */
    static class WriteDataTimerEvent extends UDPTimerEvent {
        public WriteDataTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActualEvent(UDPConnectionProcessor udpCon) {        	
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("data timeout :"+ System.currentTimeMillis());
            long time = System.currentTimeMillis();

			// Make sure that some messages are received within timeframe
			if ( udpCon.isConnected() && 
				 udpCon._lastReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {
				// If no incoming messages for very long time then 
				// close connection
                udpCon.closeAndCleanup(FinMessage.REASON_TIMEOUT);
				return;
			}

			// If still connected then handle then try to write some data
            if ( udpCon.isConnected() ) {
                udpCon.writeData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end data timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  Define what happens when an ack timeout occurs
     */
    static class AckTimeoutTimerEvent extends UDPTimerEvent {

        public AckTimeoutTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActualEvent(UDPConnectionProcessor udpCon) {
        	
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("ack timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.validateAckedData();
            }
            if(LOG.isDebugEnabled())  
                LOG.debug("end ack timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  This is an event that wakes up writing with a given delay
     */
    static class SafeWriteWakeupTimerEvent extends UDPTimerEvent {

        public SafeWriteWakeupTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActualEvent(UDPConnectionProcessor udpCon) {
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.writeDataActivation();
            }
            _eventTime = Long.MAX_VALUE;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnabled())  
                LOG.debug("write wakeup timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  Do final cleanup and shutdown after connection is closed.
     */
    static class ClosedConnectionCleanupTimerEvent extends UDPTimerEvent {

        public ClosedConnectionCleanupTimerEvent(long time, UDPConnectionProcessor proc) {
            super(time,proc );
        }

        protected void doActualEvent(UDPConnectionProcessor udpCon) {
        	
            if(LOG.isDebugEnabled())  
                LOG.debug("Closed connection timeout: "+ 
                  System.currentTimeMillis());

            udpCon.finalClose();


            if(LOG.isDebugEnabled())  
                LOG.debug("Closed connection done: "+ System.currentTimeMillis());
            
            unregister();
        }
    }
    //
    // -----------------------------------------------------------------
    
    protected void finalize() {
    	if (!isClosed()) {
    		LOG.warn("finalizing an open UDPConnectionProcessor!");
    		try {
    			close();
    		}catch (IOException ignored) {}
    	}
    }
}
