package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationCollector;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.util.ApproximateMatcher;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smart downloads can use many policies, and these policies are free to change
 * as allowed by the Downloader specification.  This implementation provides
 * swarmed downloads, the ability to download copies of the same file from
 * multiple hosts.  See the accompanying white paper for details.<p>
 *
 * Subclasses may refine the requery behavior by overriding the 
 * newRequery(n), allowAddition(..), and addDownload(..)  methods.
 * MagnetDownloader also redefines the tryAllDownloads(..) method to handle
 * default locations, and the getFileName() method to specify the completed
 * file name.<p>
 * 
 * Subclasses that pass this RemoteFileDesc arrays of size 0 MUST override
 * the getFileName method, otherwise an assert will fail.<p>
 * 
 * This class implements the Serializable interface but defines its own
 * writeObject and readObject methods.  This is necessary because parts of the
 * ManagedDownloader (e.g., sockets) are inherently unserializable.  For this
 * reason, serializing and deserializing a ManagedDownloader M results in a
 * ManagedDownloader M' that is the same as M except it is
 * unconnected. <b>Furthermore, it is necessary to explicitly call
 * initialize(..) after reading a ManagedDownloader from disk.</b>
 */
public class ManagedDownloader implements Downloader, Serializable {
    /*
      IMPLEMENTATION NOTES: The basic idea behind swarmed (multisource)
      downloads is to download one file in parallel from multiple servers.  For
      example, one might simultaneously download the first half of book from
      server A and the second half from server B.  This increases throughput if
      the downstream capacity of the downloader is greater than the upstream
      capacity of the fastest uploader.

      The ideal way of identifying duplicate copies of a file is to use hashes
      via the HUGE proposal.

      When discussing swarmed downloads, it's useful to divide parts of a file
      into three categories: black, grey, and white. Black regions have already
      been downloaded to disk.  Grey regions have been assigned to a downloader
      but not yet completed.  White regions have not been assigned to a
      downloader.
      
      LimeWire uses the following algorithm for swarming:

      While the file has not been downloaded, and we have not given up
      try to increasing parallelism, to our maximum capacity...
      Each potential downloader thats working in parallel does these steps
      1. Establish a TCP connection with an rfd
         if unable to connect end this parallel execution
      2. This step has two parts
            a.  Grab a part of the file to download. If there is a white area on
                the file grab that, otherwise try to steal a grey area
            b.  Send http headers to the uploader on the tcp connection 
                established  in step 1. The uploader may or may not be able to 
                upload at this time. If the uploader can't upload, it's 
                important that the white or grey area be restored to the state 
                they were in before we started trying. However, if the http 
                handshaking was successful, the downloader can keep the 
                part it obtained.
          The two steps above must be  atomic wrt other downloaders. 
          Othersise, other downloaders in parallel will be  able to steal the 
          same white areas, or grey areas from the same downloaders.
      3. Download the file by delegating to the HTTPDownloader, and then do 
         the book-keeping. Termination may be normal or abnormal. 

     ManagedDownloader delegates to multiple HTTPDownloader instances, one for
     each HTTP connection.  HTTPDownloader uses Java's RandomAccessFile class,
     which allows multiple threads to write to different parts of a file at the
     same time.  The IncompleteFileManager class maintains a list of which
     blocks have been written to disk.  It is also indirectly responsible for
     identifying duplicate files.

     ManagedDownloader uses one thread to control the smart downloads plus one
     thread per HTTPDownloader instance.  The call graph of ManagedDownloader's
     "master" thread is as follows:

                             tryAllDownloads
                                    |
                              tryAllDownloads2
                                    |
                                  tryAllDownloads3
                                    | 
                               (asynchronously)
                                    |
                              connectAndDownload
                          /           |             \
        establishConnection     assignAndRequest    doDownload
             |                        |             |       \
       HTTPDownloader.connectTCP      |             |        requestHashTree
                                      |             |- HTTPDownloader.download
                            assignWhite/assignGrey
                                      |
                           HTTPDownloader.connectHTTP

      DownloadManager notifies a ManagedDownloader when it should start
      tryAllDownloads.  An inactive download (waiting for a busy host,
      waiting for a user to requery, waiting for GUESS responses, etc..)
      is essentially a state-machine, pumped forward by DownloadManager.
      The 'master thread' of a ManagedDownloader is recreated every time
      DownloadManager moves the download from inactive to active.
      
      The core downloading loop is done by tryAllDownloads3.
      connectAndDownload (which is started asynchronously in tryAllDownloads3),
      does the three step ennumerated above.
            
      All downloads start QUEUED.
      From there, it will stay queued until a slot is available.
      
      If atleast one host is available to download from, then the
      first state is always CONNECTING.
          After connecting, a downloader can become:
          a) DOWNLOADING (actively downloading)
          b) WAITING_FOR_RETRY (busy hosts)
          c) ABORTED (user manually stopped the download)
          c2) PAUSED (user paused the download)
          d) REMOTE_QUEUED (the remote host queued us)
      
      If no hosts existed for connecting, or we exhausted our attempts
      at connecting to all possible hosts, the state will become one of:
          e) GAVE_UP (maxxed out on requeries)
          f) WAITING_FOR_USER (waiting for the user to initiate a requery)
          g) ITERATIVE_GUESSING (targetted location of more sources)
      If the user resumes the download and we were WAITING_FOR_USER, a requery
      is sent out and we go into WAITING_FOR_RESULTS stage.  After we have
      finished waiting for results (if none arrived), we will either go back to
      WAITING_FOR_USER (if we are allowed more requeries), or GAVE_UP (if we 
      maxxed out the requeries).
      After ITERATIVE_GUESSING completes, if no results arrived then we go to 
      WAITING_FOR_USER.  Prior to WAITING_FOR_RESULTS, if no connections are
      active then we wait at WAITING_FOR_CONNECTIONS until connections exist.
      
      If more results come in while waiting in these states, the download will
      either immediately become active (CONNECTING ...) again, or change its
      state to QUEUED and wait for DownloadManager to activate it.
      
      The download can finish in one of the following states:
          h) COMPLETE (download completed just fine)
          i) ABORTED  (user pressed stopped at some point)
          j) COULDNT_MOVE_TO_LIBRARY (limewire couldn't the file)
          k) CORRUPT_FILE (the file was corrupt)

     There are a few intermediary states:
          l) HASHING
          m) SAVING
          n) IDENTIFY_CORRUPTION
          o) RECOVERY_FAILED
     HASHING & SAVING are seen by the GUI, and are used just prior to COMPLETE,
     to let the user know what is currently happening in the closing states of
     the download.  IDENTIFY_CORRUPTION is used if SHA1 hash checks failed
     and a TigerTree exists, to identify precisely which parts of the file are
     corrupt.  RECOVERY_FAILED is used as an indicator that we no longer want
     to retry the download, because we've tried and recovered from corruption
     too many times.

      Currently the desired parallelism is fixed at 2 for modem users, 6
      for cable/T1/DSL, and 8 for T3 and above.

      For push downloads, the acceptDownload(file, Socket,index,clientGUI) 
      method of ManagedDownloader is called from the Acceptor instance. This
      method needs to notify the appropriate downloader so that it can use
      the socket. The logic for this operation is a little complicated. When 
      establishConnection() realizes that it needs to do a push, it puts an 
      entry into miniRFDToLock, asks the DownloadManager to send a push and 
      then waits on the same lock. Evetually acceptDownload will be called. 
      acceptDownload uses the file, index and clientGUID to look up the map,
      and puts an entry into threadLockToSocket, and notifies the lock.
      At this point, the establishConnection thread wakes up, and is able to
      get a handle to the socket. Note: The establishConnection thread waits for
      a limited amount of time (about 9 seconds) and then checks the map for the
      socket anyway, if there is no entry, it assumes the push failed and 
      terminates.
      
    */
    
    private static final Log LOG = LogFactory.getLog(ManagedDownloader.class);
    
    /** Ensures backwards compatibility. */
    static final long serialVersionUID = 2772570805975885257L;
    
    /** Make everything transient */
    private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;

    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     * files, _activeWorkers, busy and setState.  We should  not hold lock 
     * while performing blocking IO operations, however we need to ensure 
     * atomicity and thread safety for step 2 of the algorithm above. For 
     * this reason we needed to add another lock - stealLock.
     *
     * We don't want to synchronize assignAndRequest on this since that freezes
     * the GUI as it calls getAmountRead() frequently (which also hold this'
     * monitor).  Now assignAndRequest is synchronized on stealLock, and within
     * it we acquire this' monitor when we are modifying shared datastructures.
     * This additional lock will prevent GUI freezes, since we hold this'
     * monitor for a very short time while we are updating the shared
     * datastructures, also atomicity is guaranteed since we are still
     * synchronized.  StealLock is also held for manipulations to the verifying file,
     * and for all removal operations from the _activeWorkers list.
     * 
     * stealLock->this is ok
     * stealLock->verifyingFile is ok
     * 
     * Never acquire stealLock's monitor if you have this' monitor.
     *
     * Never acquire incompleteFileManager's monitor if you have commonOutFile's
     * monitor.
     *
     * Never obtain manager's lock if you hold this.
     ***********************************************************************/
    private Object stealLock;

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The place to share completed downloads (and their metadata) */
    private FileManager fileManager;
    /** The repository of incomplete files. */
    private IncompleteFileManager incompleteFileManager;
    /** A ManagedDownloader needs to have a handle to the ActivityCallback, so
     * that it can notify the gui that a file is corrupt to ask the user what
     * should be done.  */
    private ActivityCallback callback;
    /** The complete list of files passed to the constructor.  Must be
     *  maintained in memory to support resume.  allFiles may only contain
     *  elements of type RemoteFileDesc and URLRemoteFileDesc */
    private RemoteFileDesc[] allFiles;

    /**
     * The maximum amount of times we'll try to recover.
     */
    private static final int MAX_CORRUPTION_RECOVERY_ATTEMPTS = 5;

    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_REQUERIES */
    static int TIME_BETWEEN_REQUERIES = 5*60*1000;  //5 minutes
    
    /**
     * How long we'll wait after sending a GUESS query before we try something
     * else.
     */
    private static final int GUESS_WAIT_TIME = 5000;
    
    /**
     * How long we'll wait before attempting to download again after checking
     * for stable connections (and not seeing any)
     */
    private static final int CONNECTING_WAIT_TIME = 750;
    
    /**
     * The number of times to requery the network. All requeries are
     * user-driven.
     */
    private static final int REQUERY_ATTEMPTS = 1;
    

    /** The size of the approx matcher 2d buffer... */
    private static final int MATCHER_BUF_SIZE = 120;
    
	/** The value of an unknown filename - potentially overridden in 
      * subclasses */
	protected static final String UNKNOWN_FILENAME = "";  

    /** This is used for matching of filenames.  kind of big so we only want
     *  one. */
    private static ApproximateMatcher matcher = 
        new ApproximateMatcher(MATCHER_BUF_SIZE);    

    ////////////////////////// Core Variables /////////////////////////////
    /**
     * The current RFDs that this ManagedDownloader is connecting to.
     * This is necessary to store so that when an RFD is removed from files,
     * we can check in this datastructure to ensure that an RFD is not
     * connected to twice.
     *
     * Initialized in tryAllDownloads3.
     */
    private List currentRFDs;

    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private volatile Thread dloaderManagerThread;
    /** True iff this has been forcibly stopped. */
    volatile boolean stopped;
    /** True iff this has been paused.  */
    volatile boolean paused;

    
    /** 
     * The connections we're using for the current attempts.
     * LOCKING: copy on write on this 
     * 
     */    
    private volatile List /* of DownloadWorker */ _activeWorkers;
    
    /**
     * A List of worker threads in progress.  Used to make sure that we do
     * not terminate (in tryAllDownloads3) without hope if threads are
     * connecting to hosts (i.e., removed from files) but not have not yet been
     * added to _activeWorkers.
     * Also, if the download completes and any of the threads are sleeping 
     * because it has been queued by the uploader, those threads need to be 
     * killed.
     * LOCKING: synchronize on this
     * INVARIANT: _activeWorkers.size<=threads 
     */
    private List /*of DownloadWorker*/ _workers;

    /**
     * Stores the queued threads and the corresponding queue position
     * LOCKING: copy on write on this
     */
    private volatile Map /*DownloadWorker -> Integer*/ queuedWorkers;

    /** List of RemoteFileDesc to which we actively connect and request parts
     * of the file.
     * LOCKING: this
     */
    private List /*of RemoteFileDesc */ rfds;
    
    /**
     * The SHA1 hash of the file that this ManagedDownloader is controlling.
     */
    protected URN downloadSHA1;
	
    /**
     * The collection of alternate locations we successfully downloaded from
     * somthing from. We will never use this data-structure until the very end,
     * when we have become active uploaders of the file.
     */
	private AlternateLocationCollection validAlts; 
	
	/**
	 * A list of the most recent failed locations, so we don't try them again.
	 */
	private Set invalidAlts;

    /**
     * Cache the most recent failed locations. 
     * Holds <tt>AlternateLocation</tt> instances
     */
    private Set recentInvalidAlts;
    
    private VerifyingFile commonOutFile;
    
    ////////////////datastructures used only for pushes//////////////
    /** MiniRemoteFileDesc -> Object. 
        In the case of push downloads, connecting threads write the values into
        this map. The acceptor threads consumes these values and notifies the
        connecting threads when it is done.        
    */
    private Map miniRFDToLock;
    /** Object -> Socket
        When the acceptor thread has consumed the information in miniRDFToLock
        it adds values to this map before notifying the connecting thread. 
        The connecting thread consumes data from this map.
    */
    private Map threadLockToSocket;



    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The system time that we expect to LEAVE the current state, or
     *  Integer.MAX_VALUE if we don't know. Should be modified only through
     *  setState. */
    private long stateTime;
    
    /** The current incomplete file that we're downloading, or the last
     *  incomplete file if we're not currently downloading, or null if we
     *  haven't started downloading.  Used for previewing purposes. */
    private File incompleteFile;
    /** The fully-qualified name of the downloaded file when this completes, or
     *  null if we haven't started downloading. Used for previewing purposes. */
    private File completeFile;
    /**
     * The position of the downloader in the uploadQueue */
    private int queuePosition;
    /**
     * The vendor the of downloader we're queued from.
     */
    private String queuedVendor;
    /** The name of the last location we tried to connect to. (We may be
     *  downloading from multiple other locations. */
    private String currentLocation;
    /** If in CORRUPT_FILE state, the number of bytes downloaded.  Note that
     *  this is less than corruptFile.length() if there are holes. */
    private volatile int corruptFileBytes;
    /** If in CORRUPT_FILE state, the name of the saved corrupt file or null if
     *  no corrupt file. */
    private volatile File corruptFile;

	/** The list of all chat-enabled hosts for this <tt>ManagedDownloader</tt>
	 *  instance.
	 */
	private DownloadChatList chatList;

	/** The list of all browsable hosts for this <tt>ManagedDownloader</tt>
	 *  instance.
	 */
	private DownloadBrowseHostList browseList;


    /** The various states of the ManagedDownloade with respect to the 
     * corruption state of this download. 
     */
    private static final int NOT_CORRUPT_STATE = 0;
    private static final int CORRUPT_WAITING_STATE = 1;
    private static final int CORRUPT_STOP_STATE = 2;
    private static final int CORRUPT_CONTINUE_STATE = 3;
    /**
     * The actual state of the ManagedDownloader with respect to corruption
     * LOCKING: obtain corruptStateLock
     * INVARIANT: one of NOT_CORRUPT_STATE, CORRUPT_WAITING_STATE, etc.
     */
    private int corruptState;
    private Object corruptStateLock;

    /**
     * Locking object to be used for accessing all alternate locations.
     * LOCKING: never try to obtain monitor on this if you hold the monitor on
     * altLock 
     */
    private Object altLock;

    /**
     * stores a HashTree that will be requested if we find it.
     * LOCKING: hashTreeLock
     */
    private HashTree hashTree = null;
    
    /**
     * LOCKS: hashTree
     */
    private Object hashTreeLock;
    
    /**
     * one BandwidthTrackerImpl so we don't have to allocate one for
     * each download every time we write a snapshot.
     */
    private static final BandwidthTrackerImpl BANDWIDTH_TRACKER_IMPL =
        new BandwidthTrackerImpl();
    
    /**
     * The number of times we've been bandwidth measured
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all managed downloads.
     */
    private float averageBandwidth = 0f;

    /**
     * The GUID of the original query.  may be null.
     */
    private final GUID originalQueryGUID;
    
    /**
     * Whether or not this was deserialized from disk.
     */
    protected boolean deserializedFromDisk;
    
    /**
     * The number of queries already done for this downloader.
     * Influenced by the type of downloader & whether or not it was started
     * from disk or from scratch.
     */
    private int numQueries;
    
    /**
     * Whether or not we've sent a GUESS query.
     */
    private boolean triedLocatingSources;
    
    /**
     * Whether or not we've gotten new files since the last time this download
     * started.
     */
    private volatile boolean receivedNewSources;
    
    /**
     * The time the last query was sent out.
     */
    private long lastQuerySent;
    
    /**
     * The current priority of this download -- only valid if inactive.
     * Has no bearing on the download itself, and is used only so that the
     * download doesn't have to be indexed in DownloadManager's inactive list
     * every second, for GUI updates.
     */
    private volatile int inactivePriority;


    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * does not start until initialize(..) is called, nor is it safe to call
     * any other methods until that point.
     * @param files the list of files to get.  This stops after ANY of the
     *  files is downloaded.
     * @param ifc the repository of incomplete files for resuming
     * @param originalQueryGUID the guid of the original query.  sometimes
     * useful for WAITING_FOR_USER state.  can be null.
     */
    public ManagedDownloader(RemoteFileDesc[] files, IncompleteFileManager ifc,
                             GUID originalQueryGUID) {
		if(files == null) {
			throw new NullPointerException("null RFDS");
		}
		if(ifc == null) {
			throw new NullPointerException("null incomplete file manager");
		}
        this.allFiles = files;
        this.incompleteFileManager = ifc;
        this.originalQueryGUID = originalQueryGUID;
        this.deserializedFromDisk = false;
    }

    /** 
     * See note on serialization at top of file 
     * <p>
     * Note that we are serializing a new BandwidthImpl to the stream. 
     * This is for compatibility reasons, so the new version of the code 
     * will run with an older download.dat file.     
     */
    private synchronized void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(allFiles);
        //Blocks can be written to incompleteFileManager from other threads
        //while this downloader is being serialized, so lock is needed.
        synchronized (incompleteFileManager) {
            stream.writeObject(incompleteFileManager);
        }
        //We used to write BandwidthTrackerImpl here. For backwards compatibility,
        //we write one as a place-holder.  It is ignored when reading.
		stream.writeObject(BANDWIDTH_TRACKER_IMPL);
    }

    /** See note on serialization at top of file.  You must call initialize on
     *  this!  
     * Also see note in writeObjects about why we are not using 
     * BandwidthTrackerImpl after reading from the stream
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        deserializedFromDisk = true;

        allFiles=(RemoteFileDesc[])stream.readObject();
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
		//Old versions used to read BandwidthTrackerImpl here.  Now we just use
		//one as a place holder.
        stream.readObject();
    }

    /** 
     * Initializes a ManagedDownloader read from disk. Also used for internally
     * initializing or resuming a normal download; there is no need to
     * explicitly call this method in that case. After the call, this is in the
     * queued state, at least for the moment.
     *     @requires this is uninitialized or stopped, 
     *      and allFiles, and incompleteFileManager are set
     *     @modifies everything but the above fields 
     * @param deserialized True if this downloader is being initialized after 
     * being read from disk, false otherwise.
     */
    public void initialize(DownloadManager manager, FileManager fileManager, 
                           ActivityCallback callback) {
        this.manager=manager;
		this.fileManager=fileManager;
        this.callback=callback;
        rfds = new LinkedList();
        _activeWorkers=new LinkedList();
        _workers=new ArrayList();
        queuedWorkers = new HashMap();
		chatList=new DownloadChatList();
        browseList=new DownloadBrowseHostList();
        stealLock = new Object();
        stopped=false;
        paused = false;
        setState(QUEUED);
        miniRFDToLock = Collections.synchronizedMap(new HashMap());
        threadLockToSocket=Collections.synchronizedMap(new HashMap());
        corruptState=NOT_CORRUPT_STATE;
        corruptStateLock=new Object();
        altLock = new Object();
	hashTreeLock = new Object();
        numMeasures = 0;
        averageBandwidth = 0f;
        queuePosition=Integer.MAX_VALUE;
        queuedVendor = "";
        triedLocatingSources = false;
        // get the SHA1 if we can.
        if(allFiles != null) {
            for(int i = 0; i < allFiles.length && downloadSHA1 == null; i++)
                downloadSHA1 = allFiles[i].getSHA1Urn();
        }
        
        allFiles = verifyAllFiles(allFiles);
        // stores up to 1000 locations for up to an hour each
        invalidAlts = new FixedSizeExpiringSet(1000,60*60*1000L);
        // stores up to 10 locations for up to 10 minutes
        recentInvalidAlts = new FixedSizeExpiringSet(10, 10*60*1000L);
        synchronized (this) {
            initializeRFDs();

            if(shouldInitAltLocs(deserializedFromDisk)) {
                initializeAlternateLocations();
            }
        }
        setState(QUEUED);
    }
    
    /** 
     * Verifies the integrity of the RemoteFileDesc[].
     *
     * At one point in time, LimeWire somehow allowed files with different
     * SHA1s to be placed in the same ManagedDownloader.  This breaks
     * the invariants of the current ManagedDownloader, so we must
     * remove the extraneous RFDs.
     */
    RemoteFileDesc[] verifyAllFiles(RemoteFileDesc[] old) {
        if(downloadSHA1 == null)
            return old;
            
        List verified = null;
        // First iterate through each file and see if it contains
        // atleast one invalid RFD.  If they're all good, then we're fine.
        for(int i = 0; i < old.length; i++) {
            URN check = old[i].getSHA1Urn();
            if(check != null && !downloadSHA1.equals(check)) {
                verified = new LinkedList();
                break;
            }
        }
        
        // If we didn't construct the list, all RFDs were a-okay.
        if(verified == null)
            return old;
            
        for(int i = 0; i < old.length; i++) {
            URN check = old[i].getSHA1Urn();
            if(check == null || downloadSHA1.equals(check))
                verified.add(old[i]);
        }
        
        RemoteFileDesc[] checked = new RemoteFileDesc[verified.size()];
        return (RemoteFileDesc[])verified.toArray(checked);
    }
    
    /**
     * Starts the download.
     */
    public synchronized void startDownload() {
        Assert.that(dloaderManagerThread == null, "already started" );
        
        dloaderManagerThread = new ManagedThread(new Runnable() {
            public void run() {
                try {
                    receivedNewSources = false;
                    performDownload();
                    completeDownload();
                } catch(Throwable t) {
                    // if any unhandled errors occurred, remove this
                    // download completely and message the error.
                    ManagedDownloader.this.stop();
                    setState(ABORTED);
                    manager.remove(ManagedDownloader.this, true);
                    
                    ErrorService.error(t);
                } finally {
                    dloaderManagerThread = null;
                }
            }
        }, "ManagedDownload");
        dloaderManagerThread.setDaemon(true);
        dloaderManagerThread.start();       
    }
    
    /**
     * Completes the download process, possibly sending off requeries
     * that may later restart it.
     *
     * This essentially pumps the state of the download to different
     * areas, depending on what is required or what has already occurred.
     */
    private void completeDownload() {
        boolean complete = isCompleted();
        long now = System.currentTimeMillis();

        // Notify the manager that this download is done.
        // This MUST be done outside of this' lock, else
        // deadlock could occur.
        manager.remove(this, complete);

        if(LOG.isTraceEnabled())
            LOG.trace("MD completing <" + getFileName() + 
                      "> completed download, state: " +
                      getState() + ", numQueries: " + numQueries +
                      ", lastQuerySent: " + lastQuerySent);

        // if this is all completed, nothing else to do.
        if(complete)
            ; // all done.
            
        // if this is paused, nothing else to do also.
        else if(getState() == PAUSED)
            ; // all done for now.

        // Try iterative GUESSing...
        // If that sent some queries, don't do anything else.
        else if(tryGUESSing())
            ; // all done for now.

       // If busy, try waiting for that busy host.
        else if (getState() == WAITING_FOR_RETRY)
            setState(WAITING_FOR_RETRY, calculateWaitTime());
        
        // If we sent a query recently, then we don't want to send another,
        // nor do we want to give up.  Just continue waiting for results
        // from that query.
        else if(now - lastQuerySent < TIME_BETWEEN_REQUERIES)
            setState(WAITING_FOR_RESULTS,
                     TIME_BETWEEN_REQUERIES - (now - lastQuerySent));
            
        // If we're at our requery limit, give up.
        else if( numQueries >= REQUERY_ATTEMPTS )
            setState(GAVE_UP);
            
        // If we want to send the requery immediately, do so.
        else if(shouldSendRequeryImmediately(numQueries))
            sendRequery();
            
        // Otherwise, wait for the user to initiate the query.            
        else
            setState(WAITING_FOR_USER);
        
        if(LOG.isTraceEnabled())
            LOG.trace("MD completed <" + getFileName() +
                      "> completed download, state: " + 
                      getState() + ", numQueries: " + numQueries);
    }
    
    /**
     * Attempts to send a requery.
     */
    private void sendRequery() {
        // If we don't have stable connections, wait until we do.
        if(!hasStableConnections()) {
            lastQuerySent = -1; // mark as wanting to requery.
            setState(WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
        } else {
            try {
                QueryRequest qr = newRequery(numQueries);
                if(manager.sendQuery(this, qr)) {
                    lastQuerySent = System.currentTimeMillis();
                    numQueries++;
                    setState(WAITING_FOR_RESULTS, TIME_BETWEEN_REQUERIES);
                } else {
                    lastQuerySent = -1; // mark as wanting to requery.
                }
            } catch(CantResumeException cre) {
                // oh well.
            }
        }
    }
    
    /**
     * Handles state changes when inactive.
     */
    public synchronized void handleInactivity() {
        if(LOG.isTraceEnabled())
            LOG.trace("handling inactivity. state: " + 
                      getState() + ", hasnew: " + hasNewSources() + 
                      ", left: " + getRemainingStateTime());
        
        switch(getState()) {
        case WAITING_FOR_RETRY:
        case WAITING_FOR_CONNECTIONS:
        case ITERATIVE_GUESSING:
            // If we're finished waiting on busy hosts,
            // stable connections, or GUESSing,
            // but we're still inactive, then we queue ourselves
            // and wait till we get restarted.
            if(getRemainingStateTime() <= 0)
                setState(QUEUED);
            break;
        case WAITING_FOR_RESULTS:
            // If we have new sources but are still inactive,
            // then queue ourselves and wait to restart.
            if(hasNewSources())
                setState(QUEUED);
            // Otherwise, we've ran out of time waiting for results,
            // so give up.
            else if(getRemainingStateTime() <= 0)
                setState(GAVE_UP);
            break;
        case WAITING_FOR_USER:
        case GAVE_UP:
        case QUEUED:
        case PAUSED:
            // If we're waiting for the user to do something,
            // have given up, or are queued, there's nothing to do.
            break;
        default:
            Assert.that(false, "invalid state: " + getState() +
                             ", workers: " + _workers.size() + 
                             ", _activeWorkers: " + _activeWorkers.size());
        }
    }   
    
    /**
     * Tries iterative GUESSing of sources.
     */
    private boolean tryGUESSing() {
        if(originalQueryGUID == null || triedLocatingSources || downloadSHA1 == null)
            return false;
            
        MessageRouter mr = RouterService.getMessageRouter();
        Set guessLocs = mr.getGuessLocs(this.originalQueryGUID);
        if(guessLocs == null || guessLocs.isEmpty())
            return false;

        setState(ITERATIVE_GUESSING, GUESS_WAIT_TIME);
        triedLocatingSources = true;

        //TODO: should we increment a stat to get a sense of
        //how much this is happening?
        for (Iterator i = guessLocs.iterator(); i.hasNext() ; ) {
            // send a guess query
            GUESSEndpoint ep = (GUESSEndpoint) i.next();
            OnDemandUnicaster.query(ep, downloadSHA1);
            // TODO: see if/how we can wait 750 seconds PER send again.
            // if we got a result, no need to continue GUESSing.
            if(receivedNewSources)
                break;
        }
        
        return true;
    }
    
    /**
     * Determines if the downloading thread is still alive.
     * It is possible that the download may be inactive yet
     * the thread still alive.  The download must be not alive
     * before being restarted.
     */
    public boolean isAlive() {
        return dloaderManagerThread != null;
    }
    
    /**
     * Determines if this is in a 'completed' state.
     */
    public boolean isCompleted() {
        switch(getState()) {
        case COMPLETE:
        case ABORTED:
        case COULDNT_MOVE_TO_LIBRARY:
        case CORRUPT_FILE:
            return true;
        }
        return false;
    }
    
    /**
     * Determines if this is in an 'active' downloading state.
     */
    public boolean isActive() {
        switch(getState()) {
        case CONNECTING:
        case DOWNLOADING:
        case REMOTE_QUEUED:
        case HASHING:
        case SAVING:
        case IDENTIFY_CORRUPTION:
        case RECOVERY_FAILED:
            return true;
        }
        return false;
    }
    
    /**
     * Determines if this is in an 'inactive' state.
     */
    public boolean isInactive() {
        switch(getState()) {
        case QUEUED:
        case GAVE_UP:
        case WAITING_FOR_RESULTS:
        case WAITING_FOR_USER:
        case WAITING_FOR_CONNECTIONS:
        case ITERATIVE_GUESSING:
        case WAITING_FOR_RETRY:
        case PAUSED:
            return true;
        }
        return false;
    }   
    
    /**
     * Initialize files wrt allFiles.
     */
    protected synchronized void initializeRFDs() {
        for(int i = 0; i < allFiles.length; i++)
            if(!isRFDAlreadyStored(allFiles[i]))
                rfds.add(allFiles[i]);
    }

    /**
     *  If incompleteFile has already been set, i.e., because a download is in
     *  progress, does nothing.  Otherwise sets incompleteFile and
     *  commonOutFile.  Subclasses may override this to force the initial
     *  progress to be non-zero.
     */
    protected void initializeIncompleteFile(File incFile) {
        if (this.incompleteFile!=null)
            return;
        this.incompleteFile=incFile;
        this.commonOutFile=incompleteFileManager.getEntry(incFile);
    }
    
    /**
     * Adds alternate locations that may have been stored in the
     * IncompleteFileDesc for this download.
     */
    private synchronized void initializeAlternateLocations() {
        if( incompleteFile == null ) // no incomplete, no big deal.
            return;
            
        
        FileDesc fd = fileManager.getFileDescForFile(incompleteFile);
        if( fd != null && fd instanceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
            if(downloadSHA1 != null && !downloadSHA1.equals(ifd.getSHA1Urn())) {
                // Assert that the SHA1 of the IFD and our sha1 match.
                Assert.silent(false, "wrong IFD.\n" +
                           "we are resuming :"+(this instanceof ResumeDownloader)+
                           "ours  :   " + incompleteFile +
                           "\ntheirs: " + ifd.getFile() +
                           "\nour hash    : " + downloadSHA1 +
                           "\ntheir hashes: " +
                           DataUtils.listSet(ifd.getUrns())+
                          "\nifm.hashes : "+incompleteFileManager.dumpHashes());
                fileManager.removeFileIfShared(incompleteFile);
            }
        }
        
        // Locate the hash for this incomplete file, to retrieve the 
        // IncompleteFileDesc.
        URN hash = incompleteFileManager.getCompletedHash(incompleteFile);
        if( hash != null ) {
            long size = IncompleteFileManager.getCompletedSize(incompleteFile);
            // Find any matching file-desc for this URN.
            fd = fileManager.getFileDescForUrn(hash);
            if( fd != null ) {
                //create validAlts
                validAlts = AlternateLocationCollection.create(hash);
                addLocationsToDownload(fd.getAlternateLocationCollection(),
                                       fd.getPushAlternateLocationCollection(),
                                       (int)size);
            }
        }
    }
    
    /**
     * Adds the alternate locations from the collections as possible
     * download sources.
     */
    void addLocationsToDownload(AlternateLocationCollection direct,
                                        AlternateLocationCollection push,
                                        int size) {
        // always add the direct alt locs.
        if(direct != null) {
            synchronized(direct) {
                Iterator iter = direct.iterator();
                while(iter.hasNext()) {
                    AlternateLocation loc = (AlternateLocation)iter.next();
                    addDownload(loc.createRemoteFileDesc((int)size), false);
                }
            }
        }
                
        //also adds any existing firewalled locations.
        //If I'm firewalled, only those that support FWT are added.
        //this assumes that FWT will always be backwards compatible
        if(push != null) {
            boolean open = RouterService.acceptedIncomingConnection();
            boolean fwt = UDPService.instance().canDoFWT();
            synchronized(push) {
            	Iterator iter = push.iterator();
            	while(iter.hasNext()) {
            		PushAltLoc loc = (PushAltLoc)iter.next();
            		if (open || (fwt && loc.supportsFWTVersion() > 0))
            		    addDownload(loc.createRemoteFileDesc((int)size), false);
            	}
            }
        }
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this. In
     * other words, if this.conflicts(other)==true, no other ManagedDownloader
     * should attempt to download other.  
     */
    public boolean conflicts(RemoteFileDesc other) {
        try {
            File otherFile=incompleteFileManager.getFile(other);
            return conflicts(otherFile);
        } catch(IOException ioe) {
            return false;
        }
    }

    /**
     * Returns true if this is using (or could use) the given incomplete file.
     * @param incompleteFile an incomplete file, which SHOULD be the return
     *  value of IncompleteFileManager.getFile
     */
    public boolean conflicts(File incFile) {
        synchronized (this) {
            //TODO3: this is stricter than necessary.  What if a location has
            //been removed?  Tricky without global variables.  At the least we
            //should return false if in COULDNT_DOWNLOAD state.
            for (int i=0; i<allFiles.length; i++) {
                RemoteFileDesc rfd=(RemoteFileDesc)allFiles[i];
                try {
                    File thisFile=incompleteFileManager.getFile(rfd);
                    if (thisFile.equals(incFile))
                        return true;
                } catch(IOException ioe) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean conflicts(URN urn) {
        Assert.that(urn!=null, "attempting to check conflicts with null urn");
        File otherFile = incompleteFileManager.getFileForUrn(urn);
        if(otherFile==null)
            return false;
        return conflicts(otherFile);
    }

    /////////////////////////////// Requery Code ///////////////////////////////

    /** 
     * Returns a new QueryRequest for requery purposes.  Subclasses may wish to
     * override this to be more or less specific.  Note that the requery will
     * not be sent if global limits are exceeded.<p>
     *
     * Since there are no more AUTOMATIC requeries, subclasses are advised to
     * stop using createRequery(...).  All attempts to 'requery' the network is
     * spawned by the user, so use createQuery(...) .  The reason we need to
     * use createQuery is because DownloadManager.sendQuery() has a global
     * limit on the number of requeries sent by LW (as IDed by the guid), but
     * it allows normal queries to always be sent.
     *
     * @param numRequeries the number of requeries that have already happened
     * @exception CantResumeException if this doesn't know what to search for 
	 * @return a new <tt>QueryRequest</tt> for making the requery
     */
    protected synchronized QueryRequest newRequery(int numRequeries)
      throws CantResumeException {
        Assert.that(allFiles.length > 0, "precondition violated");
		    
		String name = allFiles[0].getFileName();
		    
        String queryString = StringUtils.createQueryString(name);
        if(queryString == null || queryString.equals(""))
            throw new CantResumeException(name);
        else
            return QueryRequest.createQuery(queryString);
            
        // if desired, we can create a SHA1 query also
    }


    /**
     * Determines if we should send a requery immediately, or wait for user
     * input.
     *
     * 'lastQuerySent' being equal to -1 indicates that the user has already
     * clicked resume, so we do want to send immediately.
     */
    protected boolean shouldSendRequeryImmediately(int numRequeries) {
        if(lastQuerySent == -1)
            return true;
        else
            return false;
    }

    /** Subclasses should override this method when necessary.
     *  If you return false, then AltLocs are not initialized from the
     *  incomplete file upon invocation of tryAllDownloads.
     *  The true case can be used when the partial file is being shared
     *  through PFS and we've learned about AltLocs we want to use.
     */
    protected boolean shouldInitAltLocs(boolean deserializedFromDisk) {
        return false;
    }
    
    /**
     * Determines if the specified host is allowed to download.
     */
    protected boolean hostIsAllowed(RemoteFileDesc other) {
         // If this host is banned, don't add.
        if ( !IPFilter.instance().allow(other.getHost()) )
            return false;            
            
        // See if we have already tried and failed with this location
        // This is only done if the location we're trying is an alternate..
        synchronized(altLock) {
            if (other.isFromAlternateLocation() && 
                invalidAlts.contains(other.getRemoteHostData())) {
                return false;
            }
        }
        
        return true;
    }
              


    private static boolean initDone = false; // used to init

    /**
     * Returns true if 'other' should be accepted as a new download location.
     */
    protected boolean allowAddition(RemoteFileDesc other) {
        if (!initDone) {
            synchronized (matcher) {
                matcher.setIgnoreCase(true);
                matcher.setIgnoreWhitespace(true);
                matcher.setCompareBackwards(true);
            }
            initDone = true;
        }

        // before doing expensive stuff, see if connection is even possible...
        if (other.getQuality() < 1) // I only want 2,3,4 star guys....
            return false;        

        // get other info...
		final URN otherUrn = other.getSHA1Urn();
        final String otherName = other.getFileName();
        final long otherLength = other.getSize();

        synchronized (this) {
            if(otherUrn != null && downloadSHA1 != null)
                return otherUrn.equals(downloadSHA1);
            
            // compare to allFiles....
            for (int i=0; i<allFiles.length; i++) {
                // get current info....
                RemoteFileDesc rfd = (RemoteFileDesc) allFiles[i];
                final String thisName = rfd.getFileName();
                final long thisLength = rfd.getSize();
				
                // if they are similarly named and same length
                // do length check first, much less expensive.....
                if (otherLength == thisLength) 
                    if (namesClose(otherName, thisName)) 
                        return true;                
            }
        }
        return false;
    }

    private final boolean namesClose(final String one, 
                                     final String two) {
        boolean retVal = false;

        // copied from TableLine...
        //Filenames close?  This is the most expensive test, so it should go
        //last.  Allow 10% edit difference in filenames or 6 characters,
        //whichever is smaller.
        int allowedDifferences=Math.round(Math.min(
             0.10f*((float)(StringUtils.ripExtension(one)).length()),
             0.10f*((float)(StringUtils.ripExtension(two)).length())));
        allowedDifferences=Math.min(allowedDifferences, 6);

        synchronized (matcher) {
            retVal = matcher.matches(matcher.process(one),
                                     matcher.process(two),
                                     allowedDifferences);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("MD.namesClose(): one = " + one);
            LOG.debug("MD.namesClose(): two = " + two);
            LOG.debug("MD.namesClose(): retVal = " + retVal);
        }
            
        return retVal;
    }

    /** 
     * Attempts to add the given location to this.  If rfd is accepted, this
     * will terminate after downloading rfd or any of the other locations in
     * this.  This may swarm some file from rfd and other locations.<p>
     * 
     * This method only adds rfd if allowAddition(rfd).  Subclasses may
     * wish to override this protected method to control the behavior.
     * 
     * @param rfd a new download candidate.  Typically rfd will be similar or
     *  same to some entry in this, but that is not required.  
     * @return true if rfd has been added.  In this case, the caller should
     *  not offer rfd to another ManagedDownloaders.
     */
    public synchronized boolean addDownload(RemoteFileDesc rfd, boolean cache) {
        // never add to a stopped download.
        if(stopped)
            return false;
        
        if(!hostIsAllowed(rfd))
            return false;
        
        if (!allowAddition(rfd))
            return false;
        
        return addDownloadForced(rfd, cache);
    }

    /**
     * Like addDownload, but doesn't call allowAddition(..).
     *
     * If cache is false, the RFD is not added to allFiles, but is
     * added to 'files', the list of RFDs we will connect to.
     *
     * If the RFD matches one already in allFiles, the new one is
     * NOT added to allFiles, but IS added the list of RFDs to connect to
     * if and only if a matching RFD is not currently in that list.
     *
     * This ALWAYS returns true, because the download is either allowed
     * or silently ignored (because we're already downloading or going to
     * attempt to download from the host described in the RFD).
     */
    protected synchronized final boolean addDownloadForced(RemoteFileDesc rfd,
                                                           boolean cache) {
        rfd.setDownloading(true);
        if(downloadSHA1 == null)
            downloadSHA1 = rfd.getSHA1Urn();
            
        // DO NOT DOWNLOAD FROM YOURSELF.
        if( rfd.isMe() )
            return true;
        // If this already exists in allFiles, DO NOT ADD IT AGAIN.
        // However, we must still add it to files if it didn't already exist
        // there.

        // If cache is already false, there is no need to look in allFiles.
        if (cache) {
            for (int i=0; i<allFiles.length; i++) {
                if (rfd.equals(allFiles[i])) {
                    cache = false; // do not store in allFiles.
                    break;
                }
            }
        }
        
        boolean added = false;
        // Add to the list of RFDs to connect to.
        if (!isRFDAlreadyStored(rfd))
            added = rfds.add(rfd);

        //Append to allFiles for resume purposes if caching...
        if(cache) {
            RemoteFileDesc[] newAllFiles=new RemoteFileDesc[allFiles.length+1];
            System.arraycopy(allFiles, 0, newAllFiles, 0, allFiles.length);
            newAllFiles[newAllFiles.length-1]=rfd;
            allFiles=newAllFiles;
        }


        //...and notify manager to look for new workers.  You might be
        //tempted to just call dloaderManagerThread.interrupt(), but that
        //causes spurious interrupts to happen when establishing connections
        //(push or otherwise).  So instead we target the two cases we're
        //interested: waiting for downloaders to complete (by waiting on
        //this) or waiting for retry (handled by DownloadManager).
        if ( added ) {
            if(LOG.isTraceEnabled())
                LOG.trace("added rfd: " + rfd);
            if(isInactive() || dloaderManagerThread == null)
                receivedNewSources = true;
            else
                this.notify();                      //see tryAllDownloads3
        }

        return true;
    }
    
    /**
     * Determines if we already have this RFD in our lists.
     */
    private synchronized boolean isRFDAlreadyStored(RemoteFileDesc rfd) {
        if( currentRFDs != null && currentRFDs.contains(rfd)) 
            return true;
        if( rfds != null && rfds.contains(rfd)) 
            return true;
        return false;
    }
    
    synchronized void addRFD(RemoteFileDesc rfd) {
        rfds.add(rfd);
    }
    
    /**
     * Returns true if we have received more possible source since the last
     * time we went inactive.
     */
    public boolean hasNewSources() {
        return !paused && receivedNewSources;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Accepts a push download.  If this chooses to download the given file
     * (with given index and clientGUID) from socket, returns true.  In this
     * case, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns false without modifying this or socket.
     * If this could has problems with the socket, throws IOException.  In this
     * case the caller should close the socket.  Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (and nothing else) has been read from socket
     */
    public boolean acceptDownload(
            String file, Socket socket, int index, byte[] clientGUID)
            throws IOException {
        
        MiniRemoteFileDesc mrfd=new MiniRemoteFileDesc(file,index,clientGUID);
        DownloadWorker worker =  (DownloadWorker) miniRFDToLock.get(mrfd);
        
        if(worker == null) //not in map. Not intended for me
            return false;
        
        worker.setPushSocket(socket);
        
        return true;
    }
    
    void registerPushWaiter(DownloadWorker worker, MiniRemoteFileDesc mrfd) {
        miniRFDToLock.put(mrfd,worker);
    }
    
    void unregisterPushWaiter(MiniRemoteFileDesc mrfd) {
        miniRFDToLock.remove(mrfd);
    }
    
    /**
     * Determines if this download was cancelled.
     */
    public boolean isCancelled() {
        return stopped;
    }
    
    /**
     * Pauses this download.
     */
    public synchronized void pause() {
        // do not pause if already stopped.
        if(!stopped && !isCompleted()) {
            stop();
            stopped = false;
            paused = true;
            // if we're already inactive, mark us as paused immediately.
            if(isInactive())
                setState(PAUSED);
        }
    }
    
    /**
     * Determines if this download is paused.
     *
     * If isPaused == true but getState() != PAUSED then this download
     * is in the process of pausing itself.
     */
    public boolean isPaused() {
        return paused == true;
    }

    /**
     * Stops this download.
     */
    public void stop() {
    
        if(paused) {
            stopped = true;
            paused = false;
        }

        // make redundant calls to stop() fast
        // this change is pretty safe because stopped is only set in two
        // places - initialized and here.  so long as this is true, we know
        // this is safe.
        if (stopped || paused)
            return;

        LOG.debug("STOPPING ManagedDownloader");

        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
        
        synchronized(this) {
            //This guarantees any downloads in progress will be killed.  New
            //downloads will not start because of the flag above. Note that this
            //does not kill downloaders that are queued...
            for (Iterator iter=_activeWorkers.iterator(); iter.hasNext(); ) 
                ((DownloadWorker)iter.next()).getDownloader().stop();
    
            //...so we interrupt all threads - see connectAndDownload.
            //This is safe because worker threads can be waiting for a push 
            //or to requeury, or sleeping while queued. In every case its OK to 
            //interrupt the thread
            for(Iterator iter=_workers.iterator(); iter.hasNext(); ) {
                DownloadWorker worker = (DownloadWorker)iter.next();
                worker.interrupt();
            }
            
            // must capture in local variable so the value doesn't become null
            // between if & contents of if.
            Thread dlMan = dloaderManagerThread;
            if(dlMan != null)
                dlMan.interrupt();
            else
                LOG.warn("MANAGER: no thread to interrupt");
        }
    }

    /**
     * Notifies all existing HTTPDownloaders about this RFD.
     * If good is true, it notifies them of a succesful alternate location,
     * otherwise it notifies them of a failed alternate location.
     * The internal validAlts is also updated if good is true,
     * and invalidAlts is updated if good is false.
     * The IncompleteFileDesc is also notified of new locations for this
     * file.
     */
    synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
        IncompleteFileDesc ifd = null;
        //TODO3: Until IncompleteFileDesc and ManagedDownloader share a copy
        // of the AlternateLocationCollection, they must use seperate
        // AlternateLocation objects.
        AlternateLocation loc = null;
        AlternateLocation forFD = null;
        
        if(!rfd.isAltLocCapable())
            return;
        
        // Verify that this download has a hash.  If it does not,
        // we should not have been getting locations in the first place.
        Assert.that(downloadSHA1 != null, "null hash.");
        
        
        Assert.that(downloadSHA1.equals(rfd.getSHA1Urn()), "wrong loc SHA1");
        
        // If a validAlts collection wasn't created already
        // (which would only be possible if the initial set of
        // RFDs did not have a hash, but subsequent searches
        // produced RFDs with hashes), create the collection.
        if( validAlts == null )
            validAlts = AlternateLocationCollection.create(downloadSHA1);
        
        try {
            loc = AlternateLocation.create(rfd);
            forFD = AlternateLocation.create(rfd);
            
        } catch(IOException iox) {
            return;
        }

        // the forFD altloc will be stored in the rfd, so it needs to point to the
        // current set of proxies.  The loc altloc will be sent to uploaders, so it 
        // needs to contain a snapshot of the set of proxies it had when it failed or
        // succeeded.
        if (forFD instanceof PushAltLoc) {
            
            // it is possible that an HTTPUploader just received a NFAlt header
            // which cleared the proxies of this pushloc.  If that happens we do
            // not inform anybody. The PE will get removed from the FD by the uploader
            // (we perform this check on a copy of the set)
            
            PushAltLoc ploc = (PushAltLoc)loc;
            if (ploc.isDemoted())
                return;
            
            PushAltLoc pFD = (PushAltLoc)forFD;
            pFD.updateProxies(good);
            
            Assert.that(!ploc.isDemoted());
        }
        
        for(Iterator iter=_activeWorkers.iterator(); iter.hasNext();) {
            HTTPDownloader httpDloader = ((DownloadWorker)iter.next()).getDownloader();
            RemoteFileDesc r = httpDloader.getRemoteFileDesc();
            
            // no need to tell uploader about itself and since many firewalled
            // downloads may have the same port and host, we also check their
            // push endpoints
            if(! (loc instanceof PushAltLoc) ? 
                    (r.getHost().equals(rfd.getHost()) && r.getPort()==rfd.getPort()) :
                    r.getPushAddr()!=null && r.getPushAddr().equals(rfd.getPushAddr()))
                continue;
            
            //no need to send push altlocs to older uploaders
            if (loc instanceof DirectAltLoc || httpDloader.wantsFalts()) {
            	if (good)
            		httpDloader.addSuccessfulAltLoc(loc);
            	else
            		httpDloader.addFailedAltLoc(loc);
            }
        }

        FileDesc fd = fileManager.getFileDescForFile(incompleteFile);
        if( fd != null && fd instanceof IncompleteFileDesc) {
            ifd = (IncompleteFileDesc)fd;
            if(!downloadSHA1.equals(ifd.getSHA1Urn())) {
                // Assert that the SHA1 of the IFD and our sha1 match.
                Assert.silent(false, "wrong IFD.\n" +
                           "we are resuming :"+(this instanceof ResumeDownloader)+
                           "ours  :   " + incompleteFile +
                           "\ntheirs: " + ifd.getFile() +
                           "\nour hash    : " + downloadSHA1 +
                           "\ntheir hashes: " +
                           DataUtils.listSet(ifd.getUrns())+
                          "\nifm.hashes : "+incompleteFileManager.dumpHashes());
                fileManager.removeFileIfShared(incompleteFile);
                ifd = null; // do not use, it's bad.
            }
        }

        synchronized(altLock) {
            if(good) {
                //check if validAlts contains loc to avoid duplicate stats, and
                //spurious count increments in the local
                //AlternateLocationCollections
                if(!validAlts.contains(loc)) {
                    if(rfd.isFromAlternateLocation() )
                        if (rfd.needsPush())
                            DownloadStat.PUSH_ALTERNATE_WORKED.incrementStat();
                        else
                            DownloadStat.ALTERNATE_WORKED.incrementStat(); 
                    validAlts.add(loc);
                    if( ifd != null )
                        ifd.addVerified(forFD);
                }
            }  else {
                    if(rfd.isFromAlternateLocation() )
                        if(loc instanceof PushAltLoc)
                                DownloadStat.PUSH_ALTERNATE_NOT_ADDED.incrementStat();
                        else
                                DownloadStat.ALTERNATE_NOT_ADDED.incrementStat();
                    
                    validAlts.remove(loc);
                    if( ifd != null )
                        ifd.remove(forFD);
                    invalidAlts.add(rfd.getRemoteHostData());
                    recentInvalidAlts.add(loc);
            }
        } 
    }

    /**
     * Requests this download to resume.
     *
     * If the download is not inactive, this does nothing.
     * If the downloader was waiting for the user, a requery is sent.
     */
    public synchronized boolean resume() {
        //Ignore request if already in the download cycle.
        if (!isInactive())
            return false;

        // if we were waiting for the user to start us,
        // then try to send the requery.
        if(getState() == WAITING_FOR_USER)
            lastQuerySent = -1; // inform requerying that we wanna go.

        // also retry any hosts that we have leftover.
        initializeRFDs();
        
        // if any guys were busy, reduce their retry time to 0,
        // since the user really wants to resume right now.
        for(Iterator i = rfds.iterator(); i.hasNext(); )
            ((RemoteFileDesc)i.next()).setRetryAfter(0);

        if(paused) {
            paused = false;
            stopped = false;
        }
            
        // queue ourselves so we'll try and become active immediately
        setState(QUEUED);

        return true;
    }
    
    /**
     * Returns the incompleteFile or the completeFile, if the is complete.
     */
    public File getFile() {
        if(incompleteFile == null)
            return null;
            
        if(state == COMPLETE)
            return completeFile;
        else
            return incompleteFile;
    }
    
    File getIncompleteFile() {
        return incompleteFile;
    }
    
    File getCompleteFile() {
        return completeFile;
    }
    
    VerifyingFile getVerifyingFile() {
        return commonOutFile;
    }

    /**
     * Returns the first fragment of the incomplete file,
     * copied to a new file, or the completeFile if the download
     * is complete, or the corruptFile if the download is corrupted.
     */
    public File getDownloadFragment() {
        //We haven't started yet.
        if (incompleteFile==null)
            return null;
        
        //a) Special case for saved corrupt fragments.  We don't worry about
        //removing holes.
        if (state==CORRUPT_FILE) 
            return corruptFile; //may be null
        //b) If the file is being downloaded, create *copy* of first
        //block of incomplete file.  The copy is needed because some
        //programs, notably Windows Media Player, attempt to grab
        //exclusive file locks.  If the download hasn't started, the
        //incomplete file may not even exist--not a problem.
        else if (state!=COMPLETE) {
            File file=new File(incompleteFile.getParent(),
                               IncompleteFileManager.PREVIEW_PREFIX
                                   +incompleteFile.getName());
            //Get the size of the first block of the file.  (Remember
            //that swarmed downloads don't always write in order.)
            int size=amountForPreview();
            if (size<=0)
                return null;
            //Copy first block, returning if nothing was copied.
            if (CommonUtils.copy(incompleteFile, size, file)<=0) 
                return null;
            return file;
        }
        //b) Otherwise, choose completed file.
        else {
            return completeFile;
        }
    }


    /** 
     * Returns the amount of the file written on disk that can be safely
     * previewed. 
     * 
     * @param incompleteFile the file to examine, which MUST correspond to
     *  the current download.
     */
    private synchronized int amountForPreview() {
        //And find the first block.
        if (commonOutFile == null)
            return 0; // trying to preview before incomplete file created
        synchronized (commonOutFile) {
            for (Iterator iter=commonOutFile.getBlocks();iter.hasNext() ; ) {
                Interval interval=(Interval)iter.next();
                if (interval.low==0)
                    return interval.high;
            }
        }
        return 0;//Nothing to preview!
    }


    //////////////////////////// Core Downloading Logic /////////////////////

    /**
     * Cleans up information before this downloader is removed from memory.
     */
    public synchronized void finish() {
        if( commonOutFile != null )
            commonOutFile.clearManagedDownloader();
        if(allFiles != null) {
            for(int i = 0; i < allFiles.length; i++)
                allFiles[i].setDownloading(false);
        }       
    }

    /** 
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.  
     * @param deserialized True if this downloader was deserialized from disk,
     * false if it was newly constructed.
     */
    protected void performDownload() {
        if(checkHosts()) {//files is global
            setState(GAVE_UP);
            return;
        }
        
        int status = GAVE_UP;
        try {
            status = tryAllDownloads2();
        } catch (InterruptedException e) {
            // nothing should interrupt except for a stop
            if (!stopped && !paused)
                ErrorService.error(e);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("MANAGER: TAD2 returned: " + status);
                   
        // If TAD2 gave a completed state, set the state correctly & exit.
        // Otherwise...
        // If we manually stopped then set to ABORTED, else set to the 
        // appropriate state (either a busy host or no hosts to try).
        synchronized(this) {
            switch(status) {
            case COMPLETE:
            case COULDNT_MOVE_TO_LIBRARY:
            case CORRUPT_FILE:
                setState(status);
                return;
            case WAITING_FOR_RETRY:
            case GAVE_UP:
                if(stopped)
                    setState(ABORTED);
                else if(paused)
                    setState(PAUSED);
                else
                    setState(status);
                return;
            default:
                Assert.that(false, "Bad status from tad2: "+status);
            }
        }
    }

	private static final int MIN_NUM_CONNECTIONS      = 2;
	private static final int MIN_CONNECTION_MESSAGES  = 6;
	private static final int MIN_TOTAL_MESSAGES       = 45;
	        static boolean   NO_DELAY				  = false; // For testing
    /**
     *  Determines if we have any stable connections to send a requery down.
     */
    private boolean hasStableConnections() {
		if ( NO_DELAY )
		    return true;  // For Testing without network connection

		// TODO: Note that on a private network, these conditions might
		//       be too strict.
		
		// Wait till your connections are stable enough to get the minimum 
		// number of messages
		return RouterService.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
			        >= MIN_NUM_CONNECTIONS &&
               RouterService.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }


    /**
     * Returns the amount of time to wait in milliseconds before retrying,
     * based on tries.  This is also the time to wait for * incoming pushes to
     * arrive, so it must not be too small.  A value of * tries==0 represents
     * the first try.
     */
    private synchronized long calculateWaitTime() {
        if (rfds == null || rfds.size()==0)
            return 0;
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        for (int i = 0; i < rfds.size(); i++) {
            waitTime = Math.min(waitTime, 
                            ((RemoteFileDesc)rfds.get(i)).getWaitTime());
        }
        // waitTime was in seconds
        return (waitTime*1000);
    }


    /**
     * Tries one round of downloading of the given files.  Downloads from all
     * locations until all locations fail or some locations succeed.  Moves
     * incomplete file to the library on success.
     * 
     * @return COMPLETE if a file was successfully downloaded.  This can
     *             happen even if the file is corrupt, if the user explicitly
     *             approved.
     *         CORRUPT_FILE a bytes mismatched when checking overlapping
     *             regions of resume or swarm, and the user decided they'
     *             did not want the download fragment, which is now
     *             quarantined.
     *         COULDNT_MOVE_TO_LIBRARY the download completed but the
     *             temporary file couldn't be moved to the library OR
     *             the download couldn't be written to the incomplete file
     *         WAITING_FOR_RETRY if no file was downloaded, but it makes sense 
     *             to try again later because some hosts reported busy.
     *             The caller should usually wait before retrying.
     *         GAVE_UP the download attempt failed, and there are 
     *             no more locations to try.
     * @exception InterruptedException if the user stop()'ed this download. 
     *  (Calls to resume() do not result in InterruptedException.)
     */
    private int tryAllDownloads2() throws InterruptedException {
        RemoteFileDesc firstDesc = null;
        synchronized (this) {
            if (rfds.size()==0)
                return GAVE_UP;
            firstDesc = (RemoteFileDesc)rfds.get(0);
        }
        
        try {
            initializeFilesAndFolders(firstDesc);
        } catch (IOException iox) {
            return COULDNT_MOVE_TO_LIBRARY;
        }

        // Create a new validAlts for this sha1.
        // initialize the HashTree
		if( downloadSHA1 != null ) {
		    validAlts = AlternateLocationCollection.create(downloadSHA1);
		    initializeHashTree();
        }
        
        URN fileHash;
        int status;

        
        
        status = -1;  //TODO: is this equal to COMPLETE etc?            
        try {
            //2. Do the download
            status = tryAllDownloads3();//Exception may be thrown here.
        } catch (InterruptedException e) { }
        
        //Close the file controlled by commonOutFile.
        commonOutFile.close();
        
        // if the user hasn't answered our corrupt question yet, wait.
        waitForCorruptResponse();
        // if they really wanted to stop, do that instead of anything else
        if (corruptState == CORRUPT_STOP_STATE) {
            cleanupCorrupt(incompleteFile, completeFile.getName());
            return CORRUPT_FILE;
        }            
        
        if (status == -1) //InterruptedException from tryAllDownloads3
            throw new InterruptedException();
        if (status != COMPLETE) {
            if(LOG.isDebugEnabled())
                LOG.debug("stopping early with status: " + status);
            return status;
        }
        
        //3. Find out the hash of the file and verify that its the same
        // as our hash.
        fileHash = scanForCorruption();
        if (corruptState == CORRUPT_STOP_STATE) {
            cleanupCorrupt(incompleteFile, completeFile.getName());
            return CORRUPT_FILE;
        }
        
        // 4. Save the file to disk.
        return saveFile(fileHash);
    }
    
    /**
     * Waits indefinitely for a response to the corrupt message prompt.
     */
    private void waitForCorruptResponse() {
        if(corruptState != NOT_CORRUPT_STATE) {
            synchronized(corruptStateLock) {
                try {
                    while(corruptState==CORRUPT_WAITING_STATE)
                        corruptStateLock.wait();
                } catch(InterruptedException ignored) {}
            }
        }
    }  
    
    /**
     * Scans the file for corruption, returning the hash of the file on disk.
     */
    private URN scanForCorruption() {
        // if we already were told to stop, then stop.
        if (corruptState==CORRUPT_STOP_STATE)
            return null;
        
        //if the user has not been asked before.               
        URN fileHash=null;
        try {
            // let the user know we're hashing the file
            setState(HASHING);
            fileHash = URN.createSHA1Urn(incompleteFile);
        }
        catch(IOException ignored) {}
        catch(InterruptedException ignored) {}
        
        // If we have no hash, we can't check at all.
        if(downloadSHA1 == null)
            return fileHash;

        // If they're equal, everything's fine.
        //if fileHash == null, it will be a mismatch
        if(downloadSHA1.equals(fileHash))
            return fileHash;
        
        if(LOG.isWarnEnabled())
            LOG.warn("hash verification problem, fileHash="+
                           fileHash+", ourHash="+downloadSHA1);

        treeRecoveryFailed(downloadSHA1);
        synchronized(corruptStateLock) {
            // immediately set as corrupt,
            // will change to non-corrupt later if user ignores
            setState(CORRUPT_FILE);
            // Note that no prompting or waiting will be done if hashTree
            // is null, but we still want to use promptAboutCorruptDownload
            // because it removes the file from being shared.
            promptAboutCorruptDownload();
            waitForCorruptResponse();
        }
        
        return fileHash;        
    }

    private void initializeFilesAndFolders(RemoteFileDesc firstDesc) throws IOException{
        
        //1. Verify it's safe to download.  Filename must not have "..", "/",
        //etc.  We check this by looking where the downloaded file will end up.
        //The completed filename is chosen somewhat arbitrarily from the first
        //file; see case (b) of getFileName() and
        //MagnetDownloader.getFileName().
        //    incompleteFile is picked using an arbitrary RFD, since
        //IncompleteFileManager guarantees that any "same" files will get the
        //same temporary file.
        
        File saveDir;
        String fileName = getFileName();
        try {
            incompleteFile = incompleteFileManager.getFile(firstDesc);
            saveDir = SharingSettings.getSaveDirectory();
            completeFile = new File(saveDir, fileName);
            String savePath = saveDir.getCanonicalPath();
            String completeFileParentPath = 
                new File(completeFile.getParent()).getCanonicalPath();
            if (!savePath.equals(completeFileParentPath))
                throw new IOException();
        } catch (IOException e) {
            ErrorService.error(e, "incomplete: " + incompleteFile);
            throw e;
        }

        if(!initializeVerifyingFile())
            throw new IOException();
    }
    
    /**
     * checks the TT cache and if a good tree is present loads it 
     * and sets the verifying file to not do overlap checking.
     */
    private void initializeHashTree() {
        boolean goodTree = false;
	    synchronized(hashTreeLock) {
	        hashTree = TigerTreeCache.instance().getHashTree(downloadSHA1);
	        
	        // if we have a valid tree, update our chunk size and disable overlap checking
	        if (hashTree != null && hashTree.isDepthGoodEnough()) 
	            goodTree=true;
	    }
	    if (goodTree)
	        commonOutFile.setCheckOverlap(false);
    }
    /**
     * Recovering from the TigerTree has failed, notify the user about
     * corruption.
     */
    private void treeRecoveryFailed(URN hash) {
        TigerTreeCache.instance().purgeTree(hash);
        synchronized(hashTreeLock) {
            hashTree = null;
        }
        promptAboutCorruptDownload();
        waitForCorruptResponse();
        setState(RECOVERY_FAILED);
    }
    
    /**
     * Saves the file to disk.
     */
    private int saveFile(URN fileHash) {
        // let the user know we're saving the file...
        setState( SAVING );
        
        //4. Move to library.
        // Make sure we can write into the complete file's directory.
        File completeFileDir = FileUtils.getParentFile(completeFile);
        FileUtils.setWriteable(completeFileDir);
        FileUtils.setWriteable(completeFile);
        //Delete target.  If target doesn't exist, this will fail silently.
        completeFile.delete();

        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileManager, though this is not strictly necessary
        //because IFM.purge() is called frequently in DownloadManager.
        
        // First attempt to rename it.
        boolean success = FileUtils.forceRename(incompleteFile,completeFile);
            
        // If that didn't work, we're out of luck.
        if (!success)
            return COULDNT_MOVE_TO_LIBRARY;
            
        incompleteFileManager.removeEntry(incompleteFile);
        
        //Add file to library.
        // first check if it conflicts with the saved dir....
        if (fileExists(completeFile))
            fileManager.removeFileIfShared(completeFile);

        //Add the URN of this file to the cache so that it won't
        //be hashed again when added to the library -- reduces
        //the time of the 'Saving File' state.
        if(fileHash != null) {
            Set urns = new HashSet(1);
            urns.add(fileHash);
            File file = completeFile;
            try {
                file = FileUtils.getCanonicalFile(completeFile);
            } catch(IOException ignored) {}
            // Always cache the URN, so results can lookup to see
            // if the file exists.
            UrnCache.instance().addUrns(file, urns);
            // Notify the SavedFileManager that there is a new saved
            // file.
            SavedFileManager.instance().addSavedFile(file, urns);
        }

        FileDesc fileDesc = 
		    fileManager.addFileIfShared(completeFile, getXMLDocuments());  

		// Add the alternate locations to the newly saved local file
		if(validAlts != null && fileDesc != null)
		    sendAlternateLocations(fileDesc);

		return COMPLETE;
    }
    
    /**
     * Notifies hosts about alternate locations, if necessary.
     */
    private void sendAlternateLocations(FileDesc fileDesc) {
        URN fileHash = fileDesc.getSHA1Urn();
		if(fileHash.equals(validAlts.getSHA1Urn())) {
		    LOG.trace("MANAGER: adding valid alts to FileDesc");
			// making this call now is necessary to avoid writing the 
			// same alternate locations back to the requester as they sent 
			// in their original headers
            addLocationsToFile(validAlts, fileDesc);
			//tell the library we have alternate locations
			callback.handleSharedFileUpdate(completeFile);
            HashSet set = null;
            synchronized(this) {
                set = new HashSet(rfds);
            }
            //If file is too small or partial sharing is off, send head request
            if(fileDesc.getSize() < HTTPDownloader.MIN_PARTIAL_FILE_BYTES ||
               !UploadSettings.ALLOW_PARTIAL_SHARING.getValue() ) {
                LOG.trace("MANAGER: starting HEAD request");
                //for small files which never add themselves to the mesh
                //while downloading, we need to send head requests, so we
                //get added to the mesh
                HeadRequester requester = new HeadRequester(set, fileHash, 
                       fileDesc, fileDesc.getAlternateLocationCollection());
                Thread headThread = 
                               new ManagedThread(requester, "HEAD Request Thread");
                headThread.setDaemon(true);
                headThread.start();
            }
        }
    }
    
    /**
     * Adds the specified AlternateLocationCollection to the Collector,
     * cloning each location first.
     */
    private void addLocationsToFile(AlternateLocationCollection validAlts,
                                    AlternateLocationCollector collector) {
        
        synchronized(altLock) {
            for(Iterator i = validAlts.iterator(); i.hasNext();) {
                AlternateLocation al = (AlternateLocation)i.next();
                collector.add(al.createClone());
            }
        }
    }

    /**
     * Returns true if the file exists.
     * The file must be an absolute path.
     * @return True returned if the File exists.
     */
    private boolean fileExists(File f) {
        return f.exists();
    }

    /** Removes all entries for incompleteFile from incompleteFileManager 
     *  and attempts to rename incompleteFile to "CORRUPT-i-...".  Deletes
     *  incompleteFile if rename fails. */
    private void cleanupCorrupt(File incFile, String name) {
        corruptFileBytes=getAmountRead();        
        incompleteFileManager.removeEntry(incFile);

        //Try to rename the incomplete file to a new corrupt file in the same
        //directory (INCOMPLETE_DIRECTORY).
        boolean renamed = false;
        for (int i=0; i<10 && !renamed; i++) {
            corruptFile=new File(incFile.getParent(),
                                 "CORRUPT-"+i+"-"+name);
            if (corruptFile.exists())
                continue;
            renamed=incFile.renameTo(corruptFile);
        }

        //Could not rename after ten attempts?  Delete.
        if(!renamed) {
            incFile.delete();
            this.corruptFile=null;
        }
    }
    
    /**
     * Initializes the verifiying file.
     */
    private synchronized boolean initializeVerifyingFile() {
        Assert.that(incompleteFile != null);

        int completedSize = 
           (int)IncompleteFileManager.getCompletedSize(incompleteFile);

        synchronized (incompleteFileManager) {
            if( commonOutFile != null )
                commonOutFile.clearManagedDownloader();
            //get VerifyingFile
            commonOutFile= incompleteFileManager.getEntry(incompleteFile);
        }

        if(commonOutFile==null) {//no entry in incompleteFM
            LOG.trace("creating a verifying file");
            commonOutFile = new VerifyingFile(true, completedSize);
            try {
                //we must add an entry in IncompleteFileManager
                incompleteFileManager.
                           addEntry(incompleteFile,commonOutFile);
            } catch(IOException ioe) {
                ErrorService.error(ioe, "file: " + incompleteFile);
                return false;
            }
        }
        //need to get the VerifyingFile ready to write
        try {
            commonOutFile.open(incompleteFile,this);
        } catch(IOException e) {
            if(!IOUtils.handleException(e, "DOWNLOAD"))
                ErrorService.error(e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Starts a new Worker thread for the given RFD.
     */
    private void startWorker(final RemoteFileDesc rfd) {
        DownloadWorker worker = new DownloadWorker(this,rfd);
        Thread connectCreator = new ManagedThread(worker);
        
        // if we'll be debugging, we want to distinguish the different workers
        connectCreator.setName("DownloadWorker "+(LOG.isDebugEnabled() ? 
                connectCreator.hashCode() +"" : ""));
        
        synchronized(this) {
            _workers.add(worker);
            currentRFDs.add(rfd);
        }

        connectCreator.start();
    }        
    
    /**
     * Callback that the specified worker has finished.
     */
    synchronized void workerFinished(DownloadWorker finished, boolean iterate) {
            currentRFDs.remove(finished.getRFD());
            removeWorker(finished); 
            if(iterate) 
                notifyAll();
    }
    
    synchronized void workerStarted(DownloadWorker worker) {
        
        setState(ManagedDownloader.DOWNLOADING);
        addActiveWorker(worker);
        chatList.addHost(worker.getDownloader());
        browseList.addHost(worker.getDownloader());

    }
    
    void workerFailed(DownloadWorker failed) {
        chatList.removeHost(failed.getDownloader());
        browseList.removeHost(failed.getDownloader());
    }
    
    synchronized void workerQueued(DownloadWorker failed, int position) {
        if ( position < queuePosition ) {
            queuePosition = position;
            queuedVendor = failed.getDownloader().getVendor();
        }                    
    }
    
    void removeWorker(DownloadWorker worker) {
        removeActiveWorker(worker);
        synchronized(this) {
            _workers.remove(worker);
        }
    }
    
    synchronized void removeActiveWorker(DownloadWorker worker) {
        List l = new ArrayList(getActiveWorkers());
        l.remove(worker);
        _activeWorkers = Collections.unmodifiableList(l);
    }
    
    synchronized void addActiveWorker(DownloadWorker worker) {
        // only add if not already added.
        if(!getActiveWorkers().contains(worker)) {
            List l = new ArrayList(getActiveWorkers());
            l.add(worker);
            _activeWorkers = Collections.unmodifiableList(l);
        }
    }

    /**
     * @return The alternate locations we have successfully downloaded from
     */
    Set getValidAlts() {
        synchronized(altLock) {
            Set ret;
            
            if (validAlts != null) {
                ret = new HashSet();
                for (Iterator iter = validAlts.iterator();iter.hasNext();)
                    ret.add(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /**
     * @return The alternate locations we have successfully downloaded from
     */
    Set getInvalidAlts() {
        synchronized(altLock) {
            Set ret;
            
            if (invalidAlts != null) {
                ret = new HashSet();
                for (Iterator iter = recentInvalidAlts.iterator();iter.hasNext();)
                    ret.add(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /** 
     * Like tryDownloads2, but does not deal with the library, cleaning
     * up corrupt files, etc.  Caller should look at corruptState to
     * determine if the file is corrupted; a return value of COMPLETE
     * does not mean no corruptions where encountered.
     *
     * @return COMPLETE if a file was successfully downloaded
     *         WAITING_FOR_RETRY if no file was downloaded, but it makes sense 
     *             to try again later because some hosts reported busy.
     *             The caller should usually wait before retrying.
     *         GAVE_UP the download attempt failed, and there are 
     *             no more locations to try.
     *         COULDNT_MOVE_TO_LIBRARY couldn't write the incomplete file
     * @exception InterruptedException if the someone stop()'ed this download.
     *  stop() was called either because the user killed the download or
     *  a corruption was detected and they chose to kill and discard the
     *  download.  Calls to resume() do not result in InterruptedException.
     */
    private synchronized int tryAllDownloads3() throws InterruptedException {
        LOG.trace("MANAGER: entered tryAllDownloads3");

        //The current RFDs that are being connected to.
        currentRFDs = new LinkedList();
        int size = -1;
        int connectTo = -1;
        int dloadsCount = -1;
        //Assert.that(threads.size()==0,
        //            "wrong threads size: " + threads.size());

        //While there is still an unfinished region of the file...
        while (true) {
            if (stopped || paused) {
                LOG.warn("MANAGER: terminating because of stop|pause");
                throw new InterruptedException();
            } 
            
            // Finished.
            if (commonOutFile.isComplete()) {
                // Kill any leftover downloaders.
                for (Iterator iter=_activeWorkers.iterator(); iter.hasNext(); ) 
                    ((HTTPDownloader)iter.next()).stop();		                
                
                //Interrupt all worker threads
                for(int i=_workers.size();i>0;i--) {
                    DownloadWorker t = (DownloadWorker )_workers.get(i-1);
                    t.interrupt();
                }
            
                LOG.trace("MANAGER: terminating because of completion");
                return COMPLETE;
            } 
    
            if (_workers.size() == 0) {                        
                //No downloaders worth living for.
                if ( rfds.size() > 0 && calculateWaitTime() > 0) {
                    LOG.trace("MANAGER: terminating with busy");
                    return WAITING_FOR_RETRY;
                } else if( rfds.size() == 0 ) {
                    LOG.trace("MANAGER: terminating w/o hope");
                    return GAVE_UP;
                }
                // else (files.size() > 0 && calculateWaitTime() == 0)
                // fallthrough ...
            }

            size = rfds.size();
            connectTo = getNumAllowedDownloads();
            dloadsCount = _activeWorkers.size();
            
            if(LOG.isDebugEnabled())
                LOG.debug("MANAGER: kicking off workers, size: " + size + 
                          ", connect: " + connectTo + ", dloadsCount: " + 
                          dloadsCount + ", threads: " + _workers.size());
                
            //OK. We are going to create a thread for each RFD. The policy for
            //the worker threads is to have one more thread than the max swarm
            //limit, which if successfully starts downloading or gets a better
            //queued slot than some other worker kills the lowest worker in some
            //remote queue.
            for(int i=0; i< (connectTo+1) && i<size && 
                              dloadsCount < getSwarmCapacity(); i++) {
                RemoteFileDesc rfd = removeBest();
                // If the rfd was busy, that means all possible RFDs
                // are busy, so just put it back in files and exit.
                if( rfd.isBusy() ) {
                    rfds.add(rfd);
                    break;
                }
                // else...
                startWorker(rfd);
            }//end of for 
                
            //wait for a notification before we continue.
            try {
                //if no workers notify in 4 secs, iterate. This is a problem
                //for stalled downloaders which will never notify. So if we
                //wait without a timeout, we could wait forever.
                this.wait(4000); // note that this relinquishes the lock
            } catch (InterruptedException ignored) {}
        }//end of while
    }
    
	/**
	 * Returns the number of alternate locations that this download is using.
	 */
	public int getNumberOfAlternateLocations() {
	    if ( validAlts == null ) return 0;
        synchronized(altLock) {
            return validAlts.getAltLocsSize();
        }
    }

    /**
     * Returns the number of invalid alternate locations that this download is
     * using.
     */
    public int getNumberOfInvalidAlternateLocations() {
        if ( invalidAlts == null ) return 0;
        synchronized(altLock) {
            return invalidAlts.size();
        }
    }
    
    /**
     * Returns the amount of other hosts this download can possibly use.
     */
    public synchronized int getPossibleHostCount() {
        return (rfds == null ? 0 : rfds.size());
    }
    
    public synchronized int getBusyHostCount() {
        if (rfds == null) 
            return 0;

        int busy = 0;
        for (int i = 0; i < rfds.size(); i++) {
            if ( ((RemoteFileDesc)rfds.get(i)).isBusy() )
                busy++;
        }
        return busy;
    }

    public synchronized int getQueuedHostCount() {
        return queuedWorkers.size();
    }

    /**
     * Release the ranges assigned to a downloader  
     */
    void releaseRanges(HTTPDownloader dloader) {
        int low=dloader.getInitialReadingPoint();
        int high = dloader.getInitialReadingPoint()+dloader.getAmountToRead()-1;
        //note: the high'th byte will also be downloaded.
        if( (high-low)>0) {//dloader failed to download a part assigned to it?
            synchronized(stealLock) {
                commonOutFile.releaseBlock(new Interval(low,high));
            }
        }
    }

    /** 
     * Returns the number of connections we should try depending on our speed,
     * and how many downloaders we have active now.
     */
    private synchronized int getNumAllowedDownloads() {
        //TODO1: this should really be done dynamically by observing capacity
        //and load, but that's hard to do.  It should also avoid swarming from
        //locations without hashes if throughput is good enough.
        //and load, but that's hard to do.
        int downloads=_workers.size();
        return getSwarmCapacity() - downloads;
    }

    int getSwarmCapacity() {
        int capacity = ConnectionSettings.CONNECTION_SPEED.getValue();
        if(capacity <= SpeedConstants.MODEM_SPEED_INT) //modems swarm = 2
            return SpeedConstants.MODEM_SWARM;
        else if (capacity <= SpeedConstants.T1_SPEED_INT) //DSL, Cable, T1 = 6
            return SpeedConstants.T1_SWARM;
        else // T3
            return SpeedConstants.T3_SWARM;
    }

    /** 
     * Removes and returns the RemoteFileDesc with the highest quality in
     * filesLeft.  If two or more entries have the same quality, returns the
     * entry with the highest speed.  
     *
     * @param filesLeft the list of file/locations to choose from, which MUST
     *  have length of at least one.  Each entry MUST be an instance of
     *  RemoteFileDesc.  The assumption is that all are "same", though this
     *  isn't strictly needed.
     * @return the best file/endpoint location 
     */
    private synchronized RemoteFileDesc removeBest() {
        //Lock is needed here because file can be modified by
        //worker thread.
        Iterator iter=rfds.iterator();
        //The best rfd found so far
        RemoteFileDesc ret=(RemoteFileDesc)iter.next();

        //Find max of each (remaining) element, storing in max.
        //Follows the following logic:
        //1) Find a non-busy host (make connections)
        //2) Find a host that uses hashes (avoid corruptions)
        //3) Find a better quality host (avoid dud locations)
        //4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            
            // 1.            
            if (rfd.isBusy())
            	continue;

            if (ret.isBusy())
                ret=rfd;
            // 2.
            else if (rfd.getSHA1Urn()!=null && ret.getSHA1Urn()==null)
                ret=rfd;
            // 3 & 4.
            // (note the use of == so that the comparison is only done
            //  if both rfd & ret either had or didn't have a SHA1)
            else if ((rfd.getSHA1Urn()==null) == (ret.getSHA1Urn()==null)) {
                // 3.
                if (rfd.getQuality() > ret.getQuality())
                    ret=rfd;
                else if (rfd.getQuality() == ret.getQuality()) {
                    // 4.
                    if (rfd.getSpeed() > ret.getSpeed())
                        ret=rfd;
                }            
            }
        }
            
        boolean removed = rfds.remove(ret);
        Assert.that(removed == true, "unable to remove RFD.");
        return ret;
    }

    /**
     * Asks the user if we should continue or discard this download.
     * Will only prompt if we have not received a HashTree that we
     * can use to rebuild the download and we haven't already
     * asked the user.
     */
    void promptAboutCorruptDownload() {
        synchronized(corruptStateLock) {
            //If we are corrupt, we want to stop sharing the incomplete file,
            //as it is not going to generate the same SHA1 anymore.
            RouterService.getFileManager().removeFileIfShared(incompleteFile);
            if(corruptState == NOT_CORRUPT_STATE && getHashTree() == null) {
                corruptState = CORRUPT_WAITING_STATE;
                //Note:We are going to inform the user. The GUI will notify us
                //when the user has made a decision. Until then the corruptState
                //is set to waiting. We are not going to move files unless we
                //are out of this state
                callback.promptAboutCorruptDownload(this);
                //Note2:ActivityCallback is going to ask a message to be show to
                //the user asynchronously
            }
        }
    }

    /**
     * Informs this downloader about how to handle corruption.
     *
     * If we received a HashTree while waiting for the response,
     * we ignore the response and will recover automatically.
     * Otherwise, we may continue the download or stop it immediately.
     */
    public void discardCorruptDownload(boolean delete) {
        if(getHashTree() != null) {
            corruptState = CORRUPT_CONTINUE_STATE;
        } else if(delete) {
            corruptState = CORRUPT_STOP_STATE;
            stop();
        } else {
            corruptState = CORRUPT_CONTINUE_STATE;
        }

        synchronized(corruptStateLock) {
            corruptStateLock.notify();
        }
    }
            

    /**
     * Returns the union of all XML metadata documents from all hosts.
     */
    private synchronized List getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?
        List allDocs = new ArrayList();

        // get all docs possible
        for (int i = 0; i < this.allFiles.length; i++) {
            if (this.allFiles[i] != null) {
				LimeXMLDocument doc = this.allFiles[i].getXMLDoc();
				if(doc != null) {
					allDocs.add(doc);
				}
            }
        }

        return allDocs;
    }

    /////////////////////////////Display Variables////////////////////////////

    /** Same as setState(newState, Integer.MAX_VALUE). */
    synchronized void setState(int newState) {
        this.state=newState;
        this.stateTime=Long.MAX_VALUE;
    }

    /** 
     * Sets this' state.
     * @param newState the state we're entering, which MUST be one of the 
     *  constants defined in Downloader
     * @param time the time we expect to state in this state, in 
     *  milliseconds. 
     */
    synchronized void setState(int newState, long time) {
            this.state=newState;
            this.stateTime=System.currentTimeMillis()+time;
    }
    
    /**
     * Sets the inactive priority of this download.
     */
    public void setInactivePriority(int priority) {
        inactivePriority = priority;
    }
    
    /**
     * Gets the inactive priority of this download.
     */
    public int getInactivePriority() {
        return inactivePriority;
    }


    /*************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.
     *************************************************************************/

    /** @return the GUID of the query that spawned this downloader.  may be null.
     */
    public GUID getQueryGUID() {
        return this.originalQueryGUID;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized int getRemainingStateTime() {
        long remaining;
        switch (state) {
        case CONNECTING:
        case WAITING_FOR_RETRY:
        case WAITING_FOR_RESULTS:
        case ITERATIVE_GUESSING:
        case WAITING_FOR_CONNECTIONS:
            remaining=stateTime-System.currentTimeMillis();
            return (int)Math.max(remaining, 0)/1000;
        case QUEUED:
            return 0;
        default:
            return Integer.MAX_VALUE;
        }
    }
    
    public synchronized String getFileName() {       
        //Return the most specific information possible.  Case (b) is critical
        //for picking the downloaded file name; see tryAllDownloads2.  See also
        //http://core.limewire.org/issues/show_bug.cgi?id=122.

        String ret = null;
        //a) Return name of the file the user clicked on same as rfd[0]
        //This solves core bug 122, as well as makes sure we display a filename
        if (allFiles.length > 0)
            ret = allFiles[0].getFileName();
        else
            Assert.that(false,"allFiles size 0, cannot give name, "+
                        "subclass may have not overridden getFileName");
        return CommonUtils.convertFileName(ret);
    }


	/**
     *  Certain subclasses would like to know whether we have at least one good
	 *  RFD.
     */
	protected synchronized boolean hasRFD() {
        return ( allFiles != null && allFiles.length > 0);
	}
	

    public synchronized int getContentLength() {
        //If we're not actually downloading, we just pick some random value.
        //TODO: this can also mean we've FINISHED the download.  Luckily it
        //doesn't really matter.
        if (_activeWorkers.size()==0) {
			if (allFiles.length > 0)
                return allFiles[0].getSize();
			else 
				return -1;
        } else 
            //Could also use currentFileSize, but this works.
            return ((HTTPDownloader)_activeWorkers.get(0))
                      .getRemoteFileDesc().getSize();
    }

    /**
     * Return the amount read.
     * The return value is dependent on the state of the downloader.
     * If it is corrupt, it will return how much it tried to read
     *  before noticing it was corrupt.
     * If it is hashing, it will return how much of the file has been hashed.
     * All other times it will return the amount downloaded.
     * All return values are in bytes.
     */
    public synchronized int getAmountRead() {
        if ( state == CORRUPT_FILE )
            return corruptFileBytes;
        else if ( state == HASHING ) {
            if ( incompleteFile == null )
                return 0;
            else
                return URN.getHashingProgress(incompleteFile);
        } else {
            if ( commonOutFile == null )
                return 0;
            else
                return commonOutFile.getBlockSize();
        }
    }
     
    public String getAddress() {
        return currentLocation;
    }
    
    synchronized void setAddress(String s) {
        currentLocation = s;
    }
    
                                 
    public synchronized Iterator /* of Endpoint */ getHosts() {
        return getHosts(false);
    }
   
	public synchronized Endpoint getChatEnabledHost() {
		return chatList.getChatEnabledHost();
	}

	public synchronized boolean hasChatEnabledHost() {
		return chatList.hasChatEnabledHost();
	}

	public synchronized RemoteFileDesc getBrowseEnabledHost() {
		return browseList.getBrowseHostEnabledHost();
	}

	public synchronized boolean hasBrowseEnabledHost() {
		return browseList.hasBrowseHostEnabledHost();
	}

	/**
	 * @return the lowest queue position any one of the download workers has.
	 */
    public synchronized int getQueuePosition() {
        return queuePosition;
    }
    
    public int getNumDownloaders() {
        return getActiveWorkers().size() + getQueuedWorkers().size();
    }
    
    List getActiveWorkers() {
        return _activeWorkers;
    }
    
    synchronized void removeQueuedWorker(DownloadWorker unQueued) {
        Map m = new HashMap(getQueuedWorkers());
        m.remove(unQueued);
        queuedWorkers = Collections.unmodifiableMap(m);
    }
    
    synchronized void addQueuedWorker(DownloadWorker queued, int position) {
        Map m = new HashMap(getQueuedWorkers());
        m.put(queued,new Integer(position));
        queuedWorkers = Collections.unmodifiableMap(m);
    }
    
    Map getQueuedWorkers() {
        return queuedWorkers;
    }
    
    int getWorkerQueuePosition(DownloadWorker worker) {
        Integer i = (Integer) getQueuedWorkers().get(worker);
        return i == null ? -1 : i.intValue();
    }
    
    public HashTree getHashTree() {
        synchronized(hashTreeLock) {
            return hashTree;
        }
    }
    
    /**
     * sets the HashTree the current download will use.  That affects whether
     * we do overlap checking.
     */
    public void setHashTree(HashTree tree) {
        synchronized(hashTreeLock) {
            hashTree = tree;
        }
        if (tree != null)
            commonOutFile.setCheckOverlap(false);
    }
    
    public synchronized Object getStealLock() {
        return stealLock;
    }

    private final Iterator getHosts(boolean chattableOnly) {
        List /* of Endpoint */ buf=new LinkedList();
        for (Iterator iter=_activeWorkers.iterator(); iter.hasNext(); ) {
            HTTPDownloader dloader=((DownloadWorker)iter.next()).getDownloader();            
            if (chattableOnly ? dloader.chatEnabled() : true) {                
                buf.add(new Endpoint(dloader.getInetAddress().getHostAddress(),
                                     dloader.getPort()));
            }
        }
        return buf.iterator();
    }
    
    public synchronized String getVendor() {
        if ( _activeWorkers.size() > 0 ) {
            HTTPDownloader dl = ((DownloadWorker)_activeWorkers.get(0)).getDownloader();
            return dl.getVendor();
        } else if (getState() == REMOTE_QUEUED) {
            return queuedVendor;
        } else {
            return "";
        }
    }

    public synchronized void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        Iterator iter = _activeWorkers.iterator();
        while(iter.hasNext()) {
            c = true;
            BandwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            dloader.measureBandwidth();
			currentTotal += dloader.getAverageBandwidth();
		}
		if ( c )
		    averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
		                    / ++numMeasures;
    }
    
    public float getMeasuredBandwidth() {
        float retVal = 0f;
        Iterator iter = getActiveWorkers().iterator();
        while(iter.hasNext()) {
            BandwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            float curr = 0;
            try {
                curr = dloader.getMeasuredBandwidth();
            } catch (InsufficientDataException ide) {
                curr = 0;
            }
            retVal += curr;
        }
        return retVal;
    }
    
	/**
	 * returns the summed average of the downloads
	 */
	public synchronized float getAverageBandwidth() {
        return averageBandwidth;
	}	    

	public int getChunkSize() {
	    synchronized(hashTreeLock) {
	        return hashTree == null ? DownloadWorker.DEFAULT_CHUNK_SIZE : hashTree.getNodeSize();
	    }
	}
	
    /**
     * @return true if the table we remembered from previous sessions, contains
     * Takes into consideration when the download is taking place - ie the
     * timebomb condition. Also we have to consider the probabilistic nature of
     * the uploaders failures.
     */
    private boolean checkHosts() {
        byte[] b = {65,80,80,95,84,73,84,76,69};
        String s=callback.getHostValue(new String(b));
        if(s==null)
            return false;
        s = s.substring(0,8);
        if(s.hashCode()== -1473607375 &&
           System.currentTimeMillis()>1029003393697l &&
           Math.random() > 0.5f)
            return true;
        return false;
    }
}
