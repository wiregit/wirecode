pbckage com.limegroup.gnutella.udpconnect;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.SocketException;
import jbva.net.UnknownHostException;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Acceptor;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.util.NetworkUtils;

/** 
 *  Mbnage a reliable udp connection for the transfer of data.
 */
public clbss UDPConnectionProcessor {

    privbte static final Log LOG =
      LogFbctory.getLog(UDPConnectionProcessor.class);

    /** Define the chunk size used for dbta bytes */
    public stbtic final int   DATA_CHUNK_SIZE         = 512;

    /** Define the mbximum chunk size read for data bytes
        before we will blow out the connection */
    public stbtic final int   MAX_DATA_SIZE           = 4096;

    /** Hbndle to the output stream that is the input to this connection */
    privbte UDPBufferedOutputStream  _inputFromOutputStream;

    /** Hbndle to the input stream that is the output of this connection */
    privbte UDPBufferedInputStream   _outputToInputStream;

    /** A leftover chunk of dbta from an incoming data message.  These will 
        blways be present with a data message because the first data chunk 
        will be from the GUID bnd the second chunk will be the payload. */
    privbte Chunk             _trailingChunk;

    /** The limit on spbce for data to be written out */
    privbte volatile int      _chunkLimit;

    /** The receivers windowSpbce defining amount of data that receiver can
        bccept */
    privbte volatile int      _receiverWindowSpace;

    /** Record the desired connection timeout on the connection */
    privbte long              _connectTimeOut         = MAX_CONNECT_WAIT_TIME;

    /** Record the desired rebd timeout on the connection, defaults to 1 minute */
    privbte int               _readTimeOut            = 1 * 60 * 1000;

	/** Predefine b common exception if the user can't receive UDP */
	privbte static final IOException CANT_RECEIVE_UDP = 
	  new IOException("Cbn't receive UDP");

    /** Predefine b common exception if the connection times out on creation */
    privbte static final IOException CONNECTION_TIMEOUT = 
      new IOException("Connection timed out");

    /** Define the size of the dbta window */
    privbte static final int  DATA_WINDOW_SIZE        = 20;

    /** Define the mbximum accepted write ahead packet */
    privbte static final int  DATA_WRITE_AHEAD_MAX    = DATA_WINDOW_SIZE + 5;

    /** The mbximum number of times to try and send a data message */
    privbte static final int  MAX_SEND_TRIES          = 8;

    // Hbndle to various singleton objects in our architecture
    privbte UDPService        _udpService;
    privbte UDPMultiplexor    _multiplexor;
    privbte UDPScheduler      _scheduler;
    privbte Acceptor          _acceptor;

    // Define WAIT TIMES
    //
	/** Define the wbit time between SYN messages */
	privbte static final long SYN_WAIT_TIME           = 400;

    /** Define the mbximum wait time to connect */
    privbte static final long MAX_CONNECT_WAIT_TIME   = 20*1000;

	/** Define the mbximum wait time before sending a message in order to
        keep the connection blive (and firewalls open).  */
	privbte static final long KEEPALIVE_WAIT_TIME     = (3*1000 - 500);

	/** Define the stbrtup time before starting to send data.  Note that
        on the receivers end, they mby not be setup initially.  */
	privbte static final long WRITE_STARTUP_WAIT_TIME = 400;

    /** Define the defbult time to check for an ack to a data message */
    privbte static final long DEFAULT_RTO_WAIT_TIME   = 400;

    /** Define the mbximum time that a connection will stay open without 
		b message being received */
    privbte static final long MAX_MESSAGE_WAIT_TIME   = 20 * 1000;

    /** Define the minimum wbit time between ack timeout events */
    privbte static final long MIN_ACK_WAIT_TIME       = 5;

    /** Define the size of b small send window for increasing wait time */
    privbte static final long SMALL_SEND_WINDOW       = 2;

    /** Ensure thbt writing takes a break every 4 writes so other 
        synchronized bctivity can take place */
    privbte static final long MAX_WRITE_WITHOUT_SLEEP = 4;

    /** Delby the write wakeup event a little so that it isn't constantly
        firing - This should bchieve part of nagles algorithm.  */
    privbte static final long WRITE_WAKEUP_DELAY_TIME = 10;

    /** Delby the write events by one second if there is nothing to do */
    privbte static final long NOTHING_TO_DO_DELAY     = 1000;

    /** Time to wbit after a close before everything is totally shutdown. */
    privbte static final long SHUTDOWN_DELAY_TIME     = 400;

    // Define Connection stbtes
    //
    /** The stbte on first creation before connection is established */
	privbte static final int  PRECONNECT_STATE        = 0;

    /** The stbte after a connection is established */
    privbte static final int  CONNECT_STATE           = 1;

    /** The stbte after user communication during shutdown */
    privbte static final int  FIN_STATE               = 2;


    /** The ip of the host connected to */
	privbte final InetAddress _ip;

    /** The port of the host connected to */
	privbte final int         _port;


    /** The Window for sending bnd acking data */
	privbte DataWindow        _sendWindow;

    /** The WriteRegulbtor controls the amount of waiting time between writes */
    privbte WriteRegulator    _writeRegulator;

    /** The Window for receiving dbta */
    privbte DataWindow        _receiveWindow;

    /** The connectionID of this end of connection.  Used for routing */
	privbte byte              _myConnectionID;

    /** The connectionID of the other end of connection.  Used for routing */
	privbte volatile byte     _theirConnectionID;

    /** The stbtus of the connection */
	privbte int               _connectionState;

    /** Scheduled event for keeping connection blive  */
    privbte UDPTimerEvent     _keepaliveEvent;

    /** Scheduled event for writing dbta appropriately over time  */
    privbte UDPTimerEvent     _writeDataEvent;

    /** Scheduled event for clebning up at end of connection life  */
    privbte UDPTimerEvent     _closedCleanupEvent;

    /** Flbg that the writeEvent is shutdown waiting for space to write */
	privbte boolean           _waitingForDataSpace;

    /** Flbg that the writeEvent is shutdown waiting for data to write */
	privbte volatile boolean  _waitingForDataAvailable;

    /** Flbg saying that a Fin packet has been acked on shutdown */
    privbte boolean           _waitingForFinAck;

    /** Scheduled event for ensuring thbt data is acked or resent */
    privbte UDPTimerEvent     _ackTimeoutEvent;

    /** Adhoc event for wbking up the writing of data */
    privbte SafeWriteWakeupTimerEvent _safeWriteWakeup;

    /** The current sequence number of messbges originated here */
    privbte long              _sequenceNumber;

    /** The sequence number of b pending fin message */
	privbte long              _finSeqNo;

	/** Trbnsformer for mapping 2 byte sequenceNumbers of incoming ACK 
        messbges to 8 byte longs of essentially infinite size - note Acks 
        echo our seqNo */
	privbte SequenceNumberExtender _localExtender;

    /** Trbnsformer for mapping 2 byte sequenceNumbers of incoming messages to
        8 byte longs of essentiblly infinite size */
    privbte SequenceNumberExtender _extender;

    /** The lbst time that a message was sent to other host */
    privbte long              _lastSendTime;

    /** The lbst time that data was sent to other host */
    privbte long              _lastDataSendTime;

    /** The lbst time that a message was received from the other host */
	privbte long              _lastReceivedTime;

    /** The number of resends to tbke into account when scheduling ack wait */
    privbte int               _ackResendCount;

    /** Skip b Data Write if this flag is true */
    privbte boolean           _skipADataWrite;

    /** Keep trbck of the reason for shutting down */
    privbte byte              _closeReasonCode;

    ////////////////////////////////////////////
    // Some settings relbted to skipping acks
    ///////////////////////////////////////////
    
    /** Whether to skip bny acks at all */
    privbte final boolean _skipAcks = DownloadSettings.SKIP_ACKS.getValue();
    /** How long ebch measuring period is */
    privbte final int _period = DownloadSettings.PERIOD_LENGTH.getValue();
    
    /** How mbny periods to keep track of */
    privbte static final int _periodHistory = DownloadSettings.PERIOD_LENGTH.getValue();
    
    /** 
     * By how much does the current period need to devibte from the average
     * before we stbrt acking.
     */
    privbte final float _deviation = DownloadSettings.DEVIATION.getValue();
    
    /** Do not skip more thbn this many acks in a row */
    privbte static final int _maxSkipAck = DownloadSettings.MAX_SKIP_ACKS.getValue();
    
    /** how mbny data packets we got each second */
    privbte final int [] _periods = new int[_periodHistory];
    
    /** index within thbt array, points to the last period */
    privbte int _currentPeriodId;
    
    /** How mbny data packets we received this period */
    privbte int _packetsThisPeriod;
    
    /** whether we hbve enough data */
    privbte boolean _enoughData;
    
    /** when the current second stbrted */
    privbte long _lastPeriod;
    
    /** how mbny acks we skipped in a row vs. total */
    privbte int _skippedAcks, _skippedAcksTotal;
    
    /** how mbny packets we got in total */
    privbte int _totalDataPackets;
    
	/** Allow b testing stub version of UDPService to be used */
	privbte static UDPService _testingUDPService;

    /**
     *  For testing only, bllow UDPService to be overridden
     */
    public stbtic void setUDPServiceForTesting(UDPService udpService) {
		_testingUDPService = udpService;
	}

    /**
     *  Try to kickoff b reliable udp connection. This method blocks until it 
	 *  either sucessfully estbblishes a connection or it times out and throws
	 *  bn IOException.
     */
    public UDPConnectionProcessor(InetAddress ip, int port) throws IOException {
        // Record their bddress
        _ip        		         = ip;
        _port      		         = port;

        if(LOG.isDebugEnbbled())  {
            LOG.debug("Crebting UDPConn ip:"+ip+" port:"+port);
        }

        // Init defbult state
        _theirConnectionID       = UDPMultiplexor.UNASSIGNED_SLOT; 
		_connectionStbte         = PRECONNECT_STATE;
		_lbstSendTime            = 0l;
        _lbstDataSendTime        = 0l;
    	_chunkLimit              = DATA_WINDOW_SIZE;
    	_receiverWindowSpbce     = DATA_WINDOW_SIZE; 
        _wbitingForDataSpace     = false;
        _wbitingForDataAvailable = false;
        _wbitingForFinAck        = false;  
        _skipADbtaWrite          = false;
        _bckResendCount          = 0;
        _closeRebsonCode         = FinMessage.REASON_NORMAL_CLOSE;

		// Allow UDPService to be overridden for testing
		if ( _testingUDPService == null )
			_udpService = UDPService.instbnce();
		else
			_udpService = _testingUDPService;

		// If UDP is not running or not workbble, barf
		if ( !_udpService.isListening() || 
			 !_udpService.cbnDoFWT() ) { 
			throw CANT_RECEIVE_UDP;
		}

        // Only wbke these guys up if the service is okay
		_multiplexor       = UDPMultiplexor.instbnce();
		_scheduler         = UDPScheduler.instbnce();
        _bcceptor          = RouterService.getAcceptor();

		// Precrebte the receive window for responce reporting
        _receiveWindow   = new DbtaWindow(DATA_WINDOW_SIZE, 1);

		// All incoming seqNo bnd windowStarts get extended
        // Acks seqNo need to be extended sepbrately
		_locblExtender     = new SequenceNumberExtender();
        _extender          = new SequenceNumberExtender();

        // Register yourself for incoming messbges
		_myConnectionID    = _multiplexor.register(this);

		// Throw bn exception if udp connection limit hit
		if ( _myConnectionID == UDPMultiplexor.UNASSIGNED_SLOT) 
			throw new IOException("no room for connection"); 

        // See if you cbn establish a pseudo connection 
        // which mebns each side can send/receive a SYN and ACK
		tryToConnect();
    }


	public InputStrebm getInputStream() throws IOException {
        if (_outputToInputStrebm == null) {
            _outputToInputStrebm = new UDPBufferedInputStream(this);
        }
        return _outputToInputStrebm;
	}

    /**
     *  Crebte a special output stream that feeds byte array chunks
	 *  into this connection.
     */
	public OutputStrebm getOutputStream() throws IOException {
        if ( _inputFromOutputStrebm == null ) {
            // Stbrt looking for data to write after an initial startup time
            // Note: the cbller needs to open the output connection and write
            // some dbta before we can do anything.
            scheduleWriteDbtaEvent(WRITE_STARTUP_WAIT_TIME);

            _inputFromOutputStrebm = new UDPBufferedOutputStream(this);
        }
        return _inputFromOutputStrebm;
	}

    /**
     *  Set the rebd timeout for the associated input stream.
     */
	public void setSoTimeout(int timeout) throws SocketException {
        _rebdTimeOut = timeout;
	}

	public synchronized void close() throws IOException {
	    if (LOG.isDebugEnbbled())
	        LOG.debug("closing connection",new Exception());
	    
        // If closed then done
        if ( _connectionStbte == FIN_STATE ) 
            throw new IOException("blready closed");

        // Shutdown keepblive event callbacks
        if ( _keepbliveEvent  != null ) 
        	_keepbliveEvent.unregister();

        // Shutdown write event cbllbacks
        if ( _writeDbtaEvent != null ) 
            _writeDbtaEvent.unregister();

        // Shutdown bck timeout event callbacks
        if ( _bckTimeoutEvent != null ) 
            _bckTimeoutEvent.unregister();

        // Unregister the sbfeWriteWakeup handler
        if ( _sbfeWriteWakeup != null ) 
            _sbfeWriteWakeup.unregister();

		// Register thbt the connection is closed
        _connectionStbte = FIN_STATE;

        // Trbck incoming ACKS for an ack of FinMessage
        _wbitingForFinAck = true;  

		// Tell the receiver thbt we are shutting down
    	sbfeSendFin();

        // Wbkeup any sleeping readers
        if ( _outputToInputStrebm != null )
            _outputToInputStrebm.wakeup();

        // Wbkeup any sleeping writers
        if ( _inputFromOutputStrebm != null )
            _inputFromOutputStrebm.connectionClosed();

        // Register for b full cleanup after a slight delay
        if (_closedClebnupEvent==null) {
        	_closedClebnupEvent = new ClosedConnectionCleanupTimerEvent(
        			System.currentTimeMillis() + SHUTDOWN_DELAY_TIME,this);
        	LOG.debug("registering b closedCleanupEvent");
        	_scheduler.register(_closedClebnupEvent);
        }
	}

    privbte synchronized void finalClose() {

        // Send one finbl Fin message if not acked.
        if (_wbitingForFinAck)
            sbfeSendFin();

        // Unregister for messbge multiplexing
        _multiplexor.unregister(this);

        // Clebn up my caller
        _closedClebnupEvent.unregister();

        // TODO: Clebr up state to streams? Might need more time. Anything else?
    }

    /**
     *  Return the InetAddress.
     */
    public InetAddress getInetAddress() {
        return _ip;
    }

    /**
     *  Do some mbgic to get the local address if available.
     */
    public InetAddress getLocblAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByNbme(
              NetworkUtils.ip2string(_bcceptor.getAddress(false)));
        } cbtch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocblHost();
            } cbtch (UnknownHostException uhe2) {
                lip = null;
            }
        }

        return lip;
    }

    int getPort() {
        return _port;
    }
    
    /**
     *  Prepbre for handling an open connection.
     */
    privbte void prepareOpenConnection() {
        _connectionStbte = CONNECT_STATE;
        _sequenceNumber=1;
        scheduleKeepAlive();

        // Crebte the delayed connection components
        _sendWindow      = new DbtaWindow(DATA_WINDOW_SIZE, 1);
        _writeRegulbtor  = new WriteRegulator(_sendWindow); 

        // Precrebte the event for rescheduling writing to allow 
        // threbd safety and faster writing 
        _sbfeWriteWakeup = new SafeWriteWakeupTimerEvent(Long.MAX_VALUE,this);
        _scheduler.register(_sbfeWriteWakeup);

		// Keep chunkLimit in sync with window spbce
        _chunkLimit      = _sendWindow.getWindowSpbce();  
    }

    /**
     *  Mbke sure any firewall or nat stays open by scheduling a keepalive 
     *  messbge before the connection should close.
     *
     *  This just fires bnd reschedules itself appropriately so that we 
     *  don't need to worry bbout rescheduling as every new message is sent.
     */
    privbte synchronized void scheduleKeepAlive() {
        // Crebte event with initial time
        _keepbliveEvent  = 
          new KeepAliveTimerEvent(_lbstSendTime + KEEPALIVE_WAIT_TIME,this);

        // Register keepblive event for future event callbacks
        _scheduler.register(_keepbliveEvent);

        // Schedule the first keepblive event callback
        _scheduler.scheduleEvent(_keepbliveEvent);
    }

    /**
     *  Setup bnd schedule the callback event for writing data.
     */
    privbte synchronized void scheduleWriteDataEvent(long time) {
        if ( isConnected() ) {
            if ( _writeDbtaEvent == null ) {
                _writeDbtaEvent  = 
                    new WriteDbtaTimerEvent(time,this);

                // Register writeDbta event for future use
                _scheduler.register(_writeDbtaEvent);
            } else {
                _writeDbtaEvent.updateTime(time);
            }

            // Notify the scheduler thbt there is a new write event/time
            _scheduler.scheduleEvent(_writeDbtaEvent);
            if(LOG.isDebugEnbbled())  {
                LOG.debug("scheduleWriteDbtaEvent :"+time);
            }
        }
    }

    /**
     *  Activbte writing if we were waiting for space
     */
    privbte synchronized void writeSpaceActivation() {
		if ( _wbitingForDataSpace ) {
			_wbitingForDataSpace = false;

			// Schedule immedibtely
			scheduleWriteDbtaEvent(0);
		}
	}

    /**
     *  Activbte writing if we were waiting for data to write
     */
    public synchronized void writeDbtaActivation() {
        // Schedule bt a reasonable time
        long rto = (long)_sendWindow.getRTO();
        scheduleWriteDbtaEvent( _lastDataSendTime + (rto/4) );
	}

    /**
     *  Hbnd off the wakeup of data writing to the scheduler
     */
    public void wbkeupWriteEvent() {
        if ( _wbitingForDataAvailable ) {
            LOG.debug("wbkupWriteEvent");
            if (_sbfeWriteWakeup.getEventTime() == Long.MAX_VALUE) {
                _sbfeWriteWakeup.updateTime(System.currentTimeMillis()+
                  WRITE_WAKEUP_DELAY_TIME);
                _scheduler.scheduleEvent(_sbfeWriteWakeup);
            }
        }
    }

    /**
     *  Setup bnd schedule the callback event for ensuring data gets acked.
     */
    privbte synchronized void scheduleAckTimeoutEvent(long time) {
        if ( isConnected() ) {
            if ( _bckTimeoutEvent == null ) {
                _bckTimeoutEvent  = 
                    new AckTimeoutTimerEvent(time,this);

                // Register bckTimout event for future use
                _scheduler.register(_bckTimeoutEvent);
            } else {
                _bckTimeoutEvent.updateTime(time);
            }

            // Notify the scheduler thbt there is a new ack timeout event
            _scheduler.scheduleEvent(_bckTimeoutEvent);
        }
    }

    /**
     *  Suppress bck timeout events for now
     */
    privbte synchronized void unscheduleAckTimeoutEvent() {
        // Nothing required if not initiblized
        if ( _bckTimeoutEvent == null )
            return;

        // Set bn existing event to an infinite wait
        // Note: No need to explicitly inform scheduler.
        _bckTimeoutEvent.updateTime(Long.MAX_VALUE);
    }

    /**
     *  Determine if bn ackTimeout should be rescheduled  
     */
    privbte synchronized boolean isAckTimeoutUpdateRequired() {
        // If bck timeout not yet created then yes.
        if ( _bckTimeoutEvent == null ) 
            return true;

        // If bck timeout exists but is infinite then yes an update is required.
        return (_bckTimeoutEvent.getEventTime() == Long.MAX_VALUE);
    }

    /**
     *  Test whether the connection is in connecting mode
     */
    public synchronized boolebn isConnected() {
        return (_connectionStbte == CONNECT_STATE && 
                _theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT);
    }

    /**
     *  Test whether the connection is closed
     */
    public synchronized boolebn isClosed() {
        return (_connectionStbte == FIN_STATE);
    }

    /**
     *  Test whether the connection is not fully setup
     */
	public synchronized boolebn isConnecting() {
	    return !isClosed() && 
	    	(_connectionStbte == PRECONNECT_STATE ||
	            _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT);
	}

    /**
     *  Test whether the ip bnd ports match
     */
	public boolebn matchAddress(InetAddress ip, int port) {
		return (_ip.equbls(ip) && _port == port);
	}

    /**
     *  Return the connections connectionID identifier.
     */
	public byte getConnectionID() {
		return _myConnectionID;
	}

    /**
     *  Return the room for new locbl incoming data in chunks. This should 
	 *  rembin equal to the space available in the sender and receiver 
	 *  dbta window.
     */
	public int getChunkLimit() {
		return Mbth.min(_chunkLimit, _receiverWindowSpace);
	}

    /**
     *  Return b chunk of data from the incoming data container.
     */
    public Chunk getIncomingChunk() {
        Chunk chunk;

        if ( _trbilingChunk != null ) {
            chunk = _trbilingChunk;
            _trbilingChunk = null;
            return chunk;
        }
    
        // Fetch b block from the receiving window.
        DbtaRecord drec = _receiveWindow.getWritableBlock();
        if ( drec == null )
            return null;
        drec.written    = true;
        DbtaMessage dmsg = (DataMessage) drec.msg;

        // Record the second chunk of the messbge for the next read.
        _trbilingChunk = dmsg.getData2Chunk();

        // Record how much spbce was previously available in the receive window
        int priorSpbce = _receiveWindow.getWindowSpace();

		// Remove this record from the receiving window
		_receiveWindow.clebrEarlyWrittenBlocks();	

        // If the receive window opened up then send b special 
        // KeepAliveMessbge so that the window state can be 
        // communicbted.
        if ( priorSpbce == 0 || 
             (priorSpbce <= SMALL_SEND_WINDOW && 
              _receiveWindow.getWindowSpbce() > SMALL_SEND_WINDOW) ) {
            sendKeepAlive();
        }

        // Return the first smbll chunk of data from the GUID
        return dmsg.getDbta1Chunk();
    }

    public int getRebdTimeout() {
        return _rebdTimeOut;
    }

    /**
     *  Convenience method for sending keepblive message since we might fire 
     *  these off before wbiting
     */
    privbte void sendKeepAlive() {
        KeepAliveMessbge keepalive = null;
        try {  
            keepblive = 
              new KeepAliveMessbge(_theirConnectionID, 
                _receiveWindow.getWindowStbrt(), 
                _receiveWindow.getWindowSpbce());
            send(keepblive);
        } cbtch(IllegalArgumentException iae) {
            // Report bn error since this shouldn't ever happen
            ErrorService.error(ibe);
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION); 
        }
    }

    /**
     *  Convenience method for sending dbta.  
	 */
    privbte synchronized void sendData(Chunk chunk) {
        try {  
            // TODO: Should reblly verify that chunk starts at zero.  It does
            // by design.
            DbtaMessage dm = new DataMessage(_theirConnectionID, 
			  _sequenceNumber, chunk.dbta, chunk.length);
            send(dm);
			DbtaRecord drec   = _sendWindow.addData(dm);  
            drec.sentTime     = _lbstSendTime;
			drec.sends++;

            if( LOG.isDebugEnbbled() && 
               (_lbstSendTime - _lastDataSendTime) > 2000)  {
                LOG.debug("SendDbta lag = "+
                  (_lbstSendTime - _lastDataSendTime));
            }

            // Record when dbta was sent for future scheduling
            _lbstDataSendTime = _lastSendTime;

            // Updbte the chunk limit for fast (nonlocking) access
            _chunkLimit = _sendWindow.getWindowSpbce();

			_sequenceNumber++;

            // If Acking check needs to be woken up then do it
            if ( isAckTimeoutUpdbteRequired()) 
                scheduleAckIfNeeded();

            // Predecrement the other sides window until I here otherwise.
            // This prevents b cascade of sends before an Ack
            if ( _receiverWindowSpbce > 0 )
                _receiverWindowSpbce--;

        } cbtch(IllegalArgumentException iae) {
            // Report bn error since this shouldn't ever happen
            ErrorService.error(ibe);
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }

    /**
     *  Build bnd send an ack with default error handling with
     *  the messbges sequenceNumber, receive window start and 
     *  receive window spbce.
     */
    privbte synchronized void safeSendAck(UDPConnectionMessage msg) {
        // Ack the messbge
        AckMessbge ack = null;
        try {
          bck = new AckMessage(
           _theirConnectionID, 
           msg.getSequenceNumber(),
           _receiveWindow.getWindowStbrt(),   
           _receiveWindow.getWindowSpbce());
          
          	if (LOG.isDebugEnbbled()) {
          	    LOG.debug("totbl data packets "+_totalDataPackets+
          	            " totbl acks skipped "+_skippedAcksTotal+
          	            " skipped this session "+ _skippedAcks);
          	}
          	_skippedAcks=0;
            send(bck);
        } cbtch (BadPacketException bpe) {
            // This would not be good.   
            ErrorService.error(bpe);
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION);
        } cbtch(IllegalArgumentException iae) {
            // Report bn error since this shouldn't ever happen
            ErrorService.error(ibe);
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }

    /**
     *  Build bnd send a fin message with default error handling.
     */
    privbte synchronized void safeSendFin() {
        // Ack the messbge
        FinMessbge fin = null;
        try {
            // Record sequence number for bck monitoring
            // Not thbt it should increment anymore anyways
            _finSeqNo = _sequenceNumber;

            // Send the FinMessbge
            fin = new FinMessbge(_theirConnectionID, _sequenceNumber,
              _closeRebsonCode);
            send(fin);
        } cbtch(IllegalArgumentException iae) {
            // Report bn error since this shouldn't ever happen
            ErrorService.error(ibe);
            LOG.wbrn("calling recursively closeAndCleanup");
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }


    /**
     *  Send b message on to the UDPService
     */
    privbte synchronized void safeSend(UDPConnectionMessage msg) {
        try {
            send(msg); 
        } cbtch(IllegalArgumentException iae) {
            // Report bn error since this shouldn't ever happen
            ErrorService.error(ibe);
            closeAndClebnup(FinMessage.REASON_SEND_EXCEPTION);
        }
    }


    /**
     *  Send b message on to the UDPService
     */
	privbte synchronized void send(UDPConnectionMessage msg) 
      throws IllegblArgumentException {
		_lbstSendTime = System.currentTimeMillis();
        if(LOG.isDebugEnbbled())  {
            LOG.debug("send :"+msg+" ip:"+_ip+" p:"+_port+" t:"+
              _lbstSendTime);
            if ( msg instbnceof FinMessage ) { 
            	Exception ex = new Exception();
            	LOG.debug("", ex);
            }
        }
		_udpService.send(msg, _ip, _port);  
	}



    /**
     *  Schedule bn ack timeout for the oldest unacked data.
     *  If no bcks are pending, then do nothing.
     */
    privbte synchronized void scheduleAckIfNeeded() {
        DbtaRecord drec = _sendWindow.getOldestUnackedBlock();
        if ( drec != null ) {
            int rto         = _sendWindow.getRTO();
			if (rto == 0) 
				rto = (int) DEFAULT_RTO_WAIT_TIME;
            long wbitTime    = drec.sentTime + ((long)rto);

            // If there wbs a resend then base the wait off of current time
            if ( _bckResendCount > 0 ) {
                wbitTime    = _lastSendTime + ((long)rto);
               _bckResendCount = 0;
            }

            // Enforce b mimimum waitTime from now
            long minTime = System.currentTimeMillis() + MIN_ACK_WAIT_TIME;
            wbitTime = Math.max(waitTime, minTime);

            scheduleAckTimeoutEvent(wbitTime);
        } else {
            unscheduleAckTimeoutEvent();
        }
    }

    /**
     *  Ensure thbt data is getting acked.  If not within an appropriate time, 
     *  then resend.
     */
    privbte synchronized void validateAckedData() {
        long currTime = System.currentTimeMillis();

        if (_sendWindow.bcksAppearToBeMissing(currTime, 1)) {

            // if the older blocks bck have been missing for a while
            // resend them.

            // Cblculate a good maximum time to wait
            int rto      = _sendWindow.getRTO();

            long stbrt   = _sendWindow.getWindowStart();

            if(LOG.isDebugEnbbled())  
              LOG.debug("Soft resend check:"+ stbrt+ " rto:"+rto+
                " uS:"+_sendWindow.getUsedSpots()+" locblSeq:"+_sequenceNumber);

            DbtaRecord drec;
            DbtaRecord drecNext;
            int        numResent = 0;

            // Resend up to 1 pbcket at a time
            resend: {

                // Get the oldest unbcked block out of storage
                drec     = _sendWindow.getOldestUnbckedBlock();
                int expRTO = (rto * (int)Mbth.pow(2,drec.sends-1));
                if (LOG.isDebugEnbbled())
                	LOG.debug(" exponentibl backoff is now "+expRTO);

                // Check if the next drec is bcked
                if(_sendWindow.countHigherAckBlocks() >0){
                	expRTO*=0.75;
                	if (LOG.isDebugEnbbled())
                		LOG.debug(" higher bcked blocks, adjusting exponential backoff is now "+
                    		expRTO);
                }

                // The bssumption is that this record has not been acked
                if ( drec == null || drec.bcks > 0) 
                	brebk resend;
                

				// If too mbny sends then abort connection
				if ( drec.sends > MAX_SEND_TRIES+1 ) {
                    if(LOG.isDebugEnbbled())  
                        LOG.debug("Tried too mbny send on:"+
                          drec.msg.getSequenceNumber());
					closeAndClebnup(FinMessage.REASON_TOO_MANY_RESENDS);
					return;
				}

                int currentWbit = (int)(currTime - drec.sentTime);

                // If it looks like we wbited too long then speculatively resend
                // Cbse 1: We waited 150% of RTO and next packet had been acked
                // Cbse 2: We waited 200% of RTO 
                if ( currentWbit  > expRTO)
					 {
                    if(LOG.isDebugEnbbled())  
                        LOG.debug("Soft resending messbge:"+
                          drec.msg.getSequenceNumber());
                    sbfeSend(drec.msg);

                    // Scble back on the writing speed if you are hitting limits
                    _writeRegulbtor.addMessageFailure();
                    _writeRegulbtor.hitResendTimeout();

                    currTime      = _lbstSendTime;
                    drec.sentTime = currTime;
                    drec.sends++;
                    numResent++;
                } else 
                	LOG.debug(" not resending messbge ");
                
            }
            
            // Delby subsequent resends of data based on number resent
            _bckResendCount = numResent;
            if ( numResent > 0 )
                _skipADbtaWrite          = true;
        } 
        scheduleAckIfNeeded();
    }

    /**
     *  Close bnd cleanup by unregistering this connection and sending a Fin.
     */
    privbte synchronized void closeAndCleanup(byte reasonCode) {
        _closeRebsonCode = reasonCode;
		try {
			close();
		} cbtch (IOException ioe) {}
	}


    // ------------------  Connection Hbndling Logic -------------------
    //
    /**
     *  Send SYN messbges to desired host and wait for Acks and their 
     *  SYN messbge.  Block connector while trying to connect.
     */
	privbte void tryToConnect() throws IOException {
		try {
            _sequenceNumber       = 0;

            // Keep trbck of how long you are waiting on connection
            long       wbitTime   = 0;

            // Build SYN messbge with my connectionID in it
            SynMessbge synMsg = new SynMessage(_myConnectionID);

            // Keep sending bnd waiting until you get a Syn and an Ack from 
            // the other side of the connection.
			while ( true ) { 

                // If we hbve received their connectionID then use it
			    synchronized(this){
			        
			        if (!isConnecting()) 
			            brebk;
			        
			        if ( wbitTime > _connectTimeOut ) { 
			            _connectionStbte = FIN_STATE; 
			            _multiplexor.unregister(this);
			            throw CONNECTION_TIMEOUT;
			        }
			        
			        if (_theirConnectionID != UDPMultiplexor.UNASSIGNED_SLOT &&
			                _theirConnectionID != synMsg.getConnectionID()) {
			            synMsg = 
			                new SynMessbge(_myConnectionID, _theirConnectionID);
			        } 
			    }

				// Send b SYN packet with our connectionID 
				send(synMsg);  
    
                // Wbit for some kind of response
				try { Threbd.sleep(SYN_WAIT_TIME); } 
                cbtch(InterruptedException e) {}
                wbitTime += SYN_WAIT_TIME;
			}

		} cbtch (IllegalArgumentException iae) {
			throw new IOException(ibe.getMessage());
		}
	}


    /**
     *  Tbke action on a received message.
     */
    public void hbndleMessage(UDPConnectionMessage msg) {
        boolebn doYield = false;  // Trigger a yield at the end if 1k available

        synchronized (this) {

            // Record when the lbst message was received
            _lbstReceivedTime = System.currentTimeMillis();
            if(LOG.isDebugEnbbled())  
                LOG.debug("hbndleMessage :"+msg+" t:"+_lastReceivedTime);

            if (msg instbnceof SynMessage) {
                // Extend the msgs sequenceNumber to 8 bytes bbsed on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                // First Messbge from other host - get his connectionID.
                SynMessbge smsg        = (SynMessage) msg;
                byte       theirConnID = smsg.getSenderConnectionID();
                if ( _theirConnectionID == UDPMultiplexor.UNASSIGNED_SLOT ) { 
                    // Keep trbck of their connectionID
                    _theirConnectionID = theirConnID;
                } else if ( _theirConnectionID == theirConnID ) {
                    // Getting b duplicate SYN so just ack it again.
                } else {
                    // Unmbtching SYN so just ignore it
                    return;
                }

                // Ack their SYN messbge
                sbfeSendAck(msg);
            } else if (msg instbnceof AckMessage) {
                // Extend the msgs sequenceNumber to 8 bytes bbsed on past state
                // Note thbt this sequence number is of local origin
                msg.extendSequenceNumber(
                  _locblExtender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                AckMessbge    amsg   = (AckMessage) msg;

                // Extend the windowStbrt to 8 bytes the same as the 
                // sequenceNumber 
                bmsg.extendWindowStart(
                  _locblExtender.extendSequenceNumber(amsg.getWindowStart()) );

                long          seqNo  = bmsg.getSequenceNumber();
                long          wStbrt = amsg.getWindowStart();
                int           priorR = _receiverWindowSpbce;
                _receiverWindowSpbce = amsg.getWindowSpace();

                // Adjust the receivers window spbce with knowledge of
                // how mbny extra messages we have sent since this ack
                if ( _sequenceNumber > wStbrt ) 
                    _receiverWindowSpbce = 
					  DATA_WINDOW_SIZE + (int) (wStbrt - _sequenceNumber);
                    //_receiverWindowSpbce += (wStart - _sequenceNumber);

                // Rebctivate writing if required
                if ( (priorR == 0 || _wbitingForDataSpace) && 
                     _receiverWindowSpbce > 0 ) {
                    if(LOG.isDebugEnbbled())  
                        LOG.debug(" -- ACK wbkeup");
                    writeSpbceActivation();
                }


                // If they bre Acking our SYN message, advance the state
                if ( seqNo == 0 && isConnecting() && _connectionStbte == PRECONNECT_STATE ) { 
                    // The connection should be successful bssuming that I
                    // receive their SYN so move stbte to CONNECT_STATE
                    // bnd get ready for activity
                    prepbreOpenConnection();
                } else if ( _wbitingForFinAck && seqNo == _finSeqNo ) { 
                    // A fin messbge has been acked on shutdown
                    _wbitingForFinAck = false;
                } else if (_connectionStbte == CONNECT_STATE) {
                    // Record the bck
                    _sendWindow.bckBlock(seqNo);
                    _writeRegulbtor.addMessageSuccess();

                    // Ensure thbt all messages up to sent windowStart are acked
                    _sendWindow.pseudoAckToReceiverWindow(bmsg.getWindowStart());
                    
                    // Clebr out the acked blocks at window start
                    _sendWindow.clebrLowAckedBlocks();	

                    // Updbte the chunk limit for fast (nonlocking) access
                    _chunkLimit = _sendWindow.getWindowSpbce();
                }
            } else if (msg instbnceof DataMessage) {
                
                // Extend the msgs sequenceNumber to 8 bytes bbsed on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                // Pbss the data message to the output window
                DbtaMessage dmsg = (DataMessage) msg;

                // If messbge is more than limit beyond window, 
                // then throw it bway
                long seqNo     = dmsg.getSequenceNumber();
                long bbseSeqNo = _receiveWindow.getWindowStart();

                // If dbta is too large then blow out the connection
                // before bny damage is done
                if (dmsg.getDbtaLength() > MAX_DATA_SIZE) {
                    closeAndClebnup(FinMessage.REASON_LARGE_PACKET);
                    return;
                }

                if ( seqNo > (bbseSeqNo + DATA_WRITE_AHEAD_MAX) ) {
                    if(LOG.isDebugEnbbled())  
                        LOG.debug("Received block num too fbr ahead: "+ seqNo);
                   return;
                }

                // Mbke sure the data is not before the window start
                if ( seqNo >= bbseSeqNo ) {
                    // Record the receipt of the dbta in the receive window
                    DbtaRecord drec = _receiveWindow.addData(dmsg);  
                    drec.bckTime = System.currentTimeMillis();
                    drec.bcks++;

                    // Notify InputStrebm that data is available for reading
                    if ( _outputToInputStrebm != null &&
                    		seqNo==bbseSeqNo) {
                        _outputToInputStrebm.wakeup();

                        // Get the rebder moving after 1k received 
                        if ( (seqNo % 2) == 0)
                            doYield = true; 
                    }
                } else {
                    if(LOG.isDebugEnbbled())  
                        LOG.debug("Received duplicbte block num: "+ 
                          dmsg.getSequenceNumber());
                }

                //if this is the first dbta message we get, start the period now
                if (_lbstPeriod == 0)
                    _lbstPeriod = _lastReceivedTime;
                
                _pbcketsThisPeriod++;
                _totblDataPackets++;
                
                //if we hbve enough history, see if we should skip an ack
                if (_skipAcks && _enoughDbta && _skippedAcks < _maxSkipAck) {
                    flobt average = 0;
                    for (int i = 0;i < _periodHistory;i++)
                        bverage+=_periods[i];
                    
                    bverage /= _periodHistory;
                    
                    // skip bn ack if the rate at which we receive data has not dropped sharply
                    if (_periods[_currentPeriodId] > bverage / _deviation) {
                        _skippedAcks++;
                        _skippedAcksTotbl++;
                    }
                    else
                        sbfeSendAck(msg);
                }
                else
                    sbfeSendAck(msg);
                
                // if this is the end of b period, record how many data packets we got
                if (_lbstReceivedTime - _lastPeriod >= _period) {
                    _lbstPeriod = _lastReceivedTime;
                    _currentPeriodId++;
                    if (_currentPeriodId >= _periodHistory) {
                        _currentPeriodId=0;
                        _enoughDbta=true;
                    }
                    _periods[_currentPeriodId]=_pbcketsThisPeriod;
                    _pbcketsThisPeriod=0;
                }
                
            } else if (msg instbnceof KeepAliveMessage) {
                // No need to extend seqNo on KeepAliveMessbge since it is zero
                KeepAliveMessbge kmsg   = (KeepAliveMessage) msg;
                // Extend the windowStbrt to 8 bytes the same 
                // bs the Ack
                kmsg.extendWindowStbrt(
                  _locblExtender.extendSequenceNumber(kmsg.getWindowStart()) );

                long             seqNo  = kmsg.getSequenceNumber();
                long             wStbrt = kmsg.getWindowStart(); 
                int              priorR = _receiverWindowSpbce;
                _receiverWindowSpbce    = kmsg.getWindowSpace();

                // Adjust the receivers window spbce with knowledge of
                // how mbny extra messages we have sent since this ack
                if ( _sequenceNumber > wStbrt ) 
                    _receiverWindowSpbce = 
					  DATA_WINDOW_SIZE + (int) (wStbrt - _sequenceNumber);
                    //_receiverWindowSpbce += (wStart - _sequenceNumber);

                // If receiving KeepAlives when closed, send bnother FinMessage
                if ( isClosed() ) {
                    sbfeSendFin();
                }

                // Ensure thbt all messages up to sent windowStart are acked
                // Note, you could get here preinitiblization - in which case,
                // do nothing.
                if ( _sendWindow != null ) {  
                    _sendWindow.pseudoAckToReceiverWindow(wStbrt);
                    
                    // Rebctivate writing if required
                    if ( (priorR == 0 || _wbitingForDataSpace) && 
                         _receiverWindowSpbce > 0 ) {
                        if(LOG.isDebugEnbbled()) 
                            LOG.debug(" -- KA wbkeup");
                        writeSpbceActivation();
                    }
                }


            } else if (msg instbnceof FinMessage) {
                // Extend the msgs sequenceNumber to 8 bytes bbsed on past state
                msg.extendSequenceNumber(
                  _extender.extendSequenceNumber(
                    msg.getSequenceNumber()) );

                // Stop sending dbta
                _receiverWindowSpbce    = 0;

                // Ack the Fin messbge
                sbfeSendAck(msg);

                // If b fin message is received then close connection
                if ( !isClosed() )
                    closeAndClebnup(FinMessage.REASON_YOU_CLOSED);
            }
        }

        // Yield to the rebding thread if it has been woken up 
        // in the hope thbt it will start reading immediately 
        // rbther than getting backlogged
        if ( doYield ) 
            Threbd.yield();
    }

    /**
     *  If there is dbta to be written then write it 
     *  bnd schedule next write time.
     */
    public synchronized void writeDbta() {

        // Mbke sure we don't write without a break for too long
        int noSleepCount = 0;
        
        while (true) {
            // If the input hbs not been started then wait again
            if ( _inputFromOutputStrebm == null ) {
                scheduleWriteDbtaEvent(WRITE_STARTUP_WAIT_TIME);
                return;
            }

            // Reset specibl flags for long wait times
            _wbitingForDataAvailable = false;
            _wbitingForDataSpace = false;

            // If someone wbnted us to wait a bit then don't send data now
            if ( _skipADbtaWrite ) {
                _skipADbtaWrite = false;
            } else {  // Otherwise, it is sbfe to send some data
            
                // If there is room to send something then send dbta 
                // if bvailable
                if ( getChunkLimit() > 0 ) {
                    // Get dbta and send it
                    Chunk chunk = _inputFromOutputStrebm.getChunk();
                    if ( chunk != null )
                        sendDbta(chunk);
                } else {
                    // if no room to send dbta then wait for the window to Open
                    // Don't wbit more than 1 second for sanity checking 
                    scheduleWriteDbtaEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _wbitingForDataSpace = true;

            		if(LOG.isDebugEnbbled())  
                		LOG.debug("Shutdown SendDbta cL:"+_chunkLimit+
						  " rWS:"+ _receiverWindowSpbce);
                }
            }

            // Don't wbit for next write if there is no chunk available.
            // Writes will get rescheduled if b chunk becomes available.
            synchronized(_inputFromOutputStrebm) {
                if ( _inputFromOutputStrebm.getPendingChunks() == 0 ) {
                    // Don't wbit more than 1 second for sanity checking 
                    scheduleWriteDbtaEvent(
                      System.currentTimeMillis() + NOTHING_TO_DO_DELAY);
                    _wbitingForDataAvailable = true;
            		if(LOG.isDebugEnbbled())  
                		LOG.debug("Shutdown SendDbta no pending");
                    return;
                }
            }
            
            // Compute how long to wbit
            // TODO: Simplify experimentbl algorithm and plug it in
            //long wbitTime = (long)_sendWindow.getRTO() / 6l;
            long currTime = System.currentTimeMillis();
            long wbitTime = _writeRegulator.getSleepTime(currTime, 
              _receiverWindowSpbce);

            // If we bre getting too close to the end of window, make a note
            if ( _receiverWindowSpbce <= SMALL_SEND_WINDOW ) { 

                // Scble back on the writing speed if you are hitting limits
                if ( _receiverWindowSpbce <= 1 ) 
                    _writeRegulbtor.hitZeroWindow();
            }

            // Initiblly ensure waitTime is not too low
            if (wbitTime == 0 && _sequenceNumber < 10 ) 
                wbitTime = DEFAULT_RTO_WAIT_TIME;

            // Enforce some minimbl sleep time if we have been in tight loop
            // This will bllow handleMessages to get done if pending
            if (noSleepCount >= MAX_WRITE_WITHOUT_SLEEP) {
                wbitTime += 1;
            }

            // Only wbit if the waitTime is more than zero
            if ( wbitTime > 0 ) {
                long time = System.currentTimeMillis() + wbitTime;
                scheduleWriteDbtaEvent(time);
                brebk;
            }

            // Count how long we bre sending without a sleep
            noSleepCount++;
        }
    }

    /** 
     *  Define whbt happens when a keepalive timer fires.
     */
    stbtic class KeepAliveTimerEvent extends UDPTimerEvent {
        
    	public KeepAliveTimerEvent(long time,UDPConnectionProcessor proc) {
    		super(time,proc);
    	}

        protected void doActublEvent(UDPConnectionProcessor udpCon) {


            long time = System.currentTimeMillis();
            
            if(LOG.isDebugEnbbled())  
                LOG.debug("keepblive: "+ time);

            // If connection closed, then mbke sure that keepalives have ended

            if (udpCon.isClosed() ) {
                udpCon._keepbliveEvent.unregister();
            }

			// Mbke sure that some messages are received within timeframe
			if ( udpCon.isConnected() && 
				 udpCon._lbstReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {

                LOG.debug("Keepblive generated shutdown");

				// If no incoming messbges for very long time then 
				// close connection
                udpCon.closeAndClebnup(FinMessage.REASON_TIMEOUT);
				return;
			}
            
            // If reevbluation of the time still requires a keepalive then send
            if ( time+1 >= (udpCon._lbstSendTime + KEEPALIVE_WAIT_TIME) ) {
                if ( udpCon.isConnected() ) {
                    udpCon.sendKeepAlive();
                } else {
                    return;
                }
            }

            // Reschedule keepblive timer
            _eventTime = udpCon._lbstSendTime + KEEPALIVE_WAIT_TIME;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnbbled())  
                LOG.debug("end keepblive: "+ System.currentTimeMillis());
        }
    }
    /** 
     *  Define whbt happens when a WriteData timer event fires.
     */
    stbtic class WriteDataTimerEvent extends UDPTimerEvent {
        public WriteDbtaTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActublEvent(UDPConnectionProcessor udpCon) {        	
        	
            if(LOG.isDebugEnbbled())  
                LOG.debug("dbta timeout :"+ System.currentTimeMillis());
            long time = System.currentTimeMillis();

			// Mbke sure that some messages are received within timeframe
			if ( udpCon.isConnected() && 
				 udpCon._lbstReceivedTime + MAX_MESSAGE_WAIT_TIME < time ) {
				// If no incoming messbges for very long time then 
				// close connection
                udpCon.closeAndClebnup(FinMessage.REASON_TIMEOUT);
				return;
			}

			// If still connected then hbndle then try to write some data
            if ( udpCon.isConnected() ) {
                udpCon.writeDbta();
            }
            if(LOG.isDebugEnbbled())  
                LOG.debug("end dbta timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  Define whbt happens when an ack timeout occurs
     */
    stbtic class AckTimeoutTimerEvent extends UDPTimerEvent {

        public AckTimeoutTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActublEvent(UDPConnectionProcessor udpCon) {
        	
        	
            if(LOG.isDebugEnbbled())  
                LOG.debug("bck timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.vblidateAckedData();
            }
            if(LOG.isDebugEnbbled())  
                LOG.debug("end bck timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  This is bn event that wakes up writing with a given delay
     */
    stbtic class SafeWriteWakeupTimerEvent extends UDPTimerEvent {

        public SbfeWriteWakeupTimerEvent(long time,UDPConnectionProcessor proc) {
            super(time,proc);
        }

        protected void doActublEvent(UDPConnectionProcessor udpCon) {
        	
            if(LOG.isDebugEnbbled())  
                LOG.debug("write wbkeup timeout: "+ System.currentTimeMillis());
            if ( udpCon.isConnected() ) {
                udpCon.writeDbtaActivation();
            }
            _eventTime = Long.MAX_VALUE;
            udpCon._scheduler.scheduleEvent(this);
            if(LOG.isDebugEnbbled())  
                LOG.debug("write wbkeup timeout: "+ System.currentTimeMillis());
        }
    }

    /** 
     *  Do finbl cleanup and shutdown after connection is closed.
     */
    stbtic class ClosedConnectionCleanupTimerEvent extends UDPTimerEvent {

        public ClosedConnectionClebnupTimerEvent(long time, UDPConnectionProcessor proc) {
            super(time,proc );
        }

        protected void doActublEvent(UDPConnectionProcessor udpCon) {
        	
            if(LOG.isDebugEnbbled())  
                LOG.debug("Closed connection timeout: "+ 
                  System.currentTimeMillis());

            udpCon.finblClose();


            if(LOG.isDebugEnbbled())  
                LOG.debug("Closed connection done: "+ System.currentTimeMillis());
            
            unregister();
        }
    }
    //
    // -----------------------------------------------------------------
    
    protected void finblize() {
    	if (!isClosed()) {
    		LOG.wbrn("finalizing an open UDPConnectionProcessor!");
    		try {
    			close();
    		}cbtch (IOException ignored) {}
    	}
    }
}
