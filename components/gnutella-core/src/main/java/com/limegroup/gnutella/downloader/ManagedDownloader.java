package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;

import java.io.*;
import java.net.*;

import java.util.StringTokenizer;

import java.util.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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

    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     * files, dloaders, busy and setState.  We should  not hold lock 
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
     * synchronized.
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

    ///////////////////////// Policy Controls ///////////////////////////
    /** The time to wait trying to establish each normal connection, in
     *  milliseconds.*/
    private static final int NORMAL_CONNECT_TIME=10000; //10 seconds
    /** The time to wait trying to establish each push connection, in
     *  milliseconds.  This needs to be larger than the normal time. */
    private static final int PUSH_CONNECT_TIME=20000;  //20 seconds
    /** The time to wait trying to establish a push connection
     * if only a UDP push has been sent (as is in the case of altlocs) */
    private static final int UDP_PUSH_CONNECT_TIME=6000; //6 seconds
    /** The smallest interval that can be split for parallel download */
    private static final int MIN_SPLIT_SIZE=100000;      //100 KB        
    /** The interval size for downloaders with persistenace support  */
    private static final int CHUNK_SIZE=100000;//100 KB-chosen after studying
    /** The lowest (cumulative) bandwith we will accept without stealing the
     * entire grey area from a downloader for a new one */
    private static final float MIN_ACCEPTABLE_SPEED = 
		DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue() < 8 ? 
		0.1f:
		0.5f;
    /** The number of bytes to overlap when swarming and resuming, used to help
     *  verify that different sources are serving the same content. */
    static final int OVERLAP_BYTES=10;
    
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
    
    /**
     * The number of seconds to wait for hosts that don't have any ranges we
     *  would be interested in.
     */
    private static final int NO_RANGES_RETRY_AFTER = 60 * 5; // 5 minutes
    
    /**
     * The number of seconds to wait for hosts that failed once.
     */
    private static final int FAILED_RETRY_AFTER = 60 * 1; // 1 minute
    
    /**
     * The number of seconds to wait for a busy host (if it didn't give us a
     * retry after header) if we don't have any active downloaders.
     *
     * Note that there are some acceptable problems with the way this
     * values are used.  Namely, if we have sources X & Y and source
     * X is tried first, but is busy, its busy-time will be set to
     * 1 minute.  Then source Y is tried and is accepted, source X
     * will still retry after 1 minute.  This 'problem' is considered
     * an acceptable issue, given the complexity of implementing
     * a method that will work under the circumstances.
     */
    private static final int RETRY_AFTER_NONE_ACTIVE = 60 * 1; // 1 minute
    
    /**
     * The minimum number of seconds to wait for a busy host if we do
     * have some active downloaders.
     *
     * Note that there are some acceptable problems with the way this
     * values are used.  Namely, if we have sources X & Y and source
     * X is tried first and is accepted.  Then source Y is tried and
     * is busy, so its busy-time is set to 10 minutes.  Then X disconnects,
     * leaving Y with 9 or so minutes left before being retried, despite
     * no other sources available.  This 'problem' is considered
     * an acceptable issue, given the complexity of implementing
     * a method that will work under the circumstances.
     */
    private static final int RETRY_AFTER_SOME_ACTIVE = 60 * 10; // 10 minutes
    
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
    private Thread dloaderManagerThread;
    /** True iff this has been forcibly stopped. */
    private volatile boolean stopped;

    
    /** The connections we're using for the current attempts. */    
    private List /* of HTTPDownloader */ dloaders;
    /**
     * A List of worker threads in progress.  Used to make sure that we do
     * not terminate (in tryAllDownloads3) without hope if threads are
     * connecting to hosts (i.e., removed from files) but not have not yet been
     * added to dloaders.
     * Also, if the download completes and any of the threads are sleeping 
     * because it has been queued by the uploader, those threads need to be 
     * killed.
     * LOCKING: synchronize on this 
     * INVARIANT: dloaders.size<=threads 
     */
    private List /*of Threads*/ threads;

    /**
     * Stores the queued threads and the corresponding queue position
     */
    private Map /*Thread -> Integer*/ queuedThreads;

    /** List of RemoteFileDesc to which we actively connect and request parts
     * of the file.*/
    private List /*of RemoteFileDesc */ files;
    
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
    private String queuePosition;
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
     */
    private HashTree hashTree = null;
    
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
        files = new LinkedList();
        dloaders=new LinkedList();
        threads=new ArrayList();
        queuedThreads = new HashMap();
		chatList=new DownloadChatList();
        browseList=new DownloadBrowseHostList();
        stealLock = new Object();
        stopped=false;
        setState(QUEUED);
        miniRFDToLock = Collections.synchronizedMap(new HashMap());
        threadLockToSocket=Collections.synchronizedMap(new HashMap());
        corruptState=NOT_CORRUPT_STATE;
        corruptStateLock=new Object();
        altLock = new Object();
        numMeasures = 0;
        averageBandwidth = 0f;
        queuePosition = "";
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
            initializeFiles();

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
        dloaderManagerThread = new ManagedThread(new Runnable() {
            public void run() {
                try {
                    receivedNewSources = false;
                    tryAllDownloads();
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
        // dead could occur.
        manager.remove(this, complete);

        if(LOG.isTraceEnabled())
            LOG.trace("MD completing <" + getFileName() + 
                      "> completed download, state: " +
                      getState() + ", numQueries: " + numQueries +
                      ", lastQuerySent: " + lastQuerySent);

        // if this is all completed, nothing else to do.
        if(complete)
            ; // all done.

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
                if(manager.sendQuery(this, newRequery(numQueries))) {
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
            // If we're waiting for the user to do something,
            // have given up, or are queued, there's nothing to do.
            break;
        default:
            Assert.that(false, "invalid state: " + getState());
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
            return true;
        }
        return false;
    }   
    
    /**
     * Initialize files wrt allFiles.
     */
    protected synchronized void initializeFiles() {
        for(int i = 0; i < allFiles.length; i++)
            if(!isRFDAlreadyStored(allFiles[i]))
                files.add(allFiles[i]);
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
            
        // Locate the hash for this incomplete file, to retrieve the 
        // IncompleteFileDesc.
        URN hash = incompleteFileManager.getCompletedHash(incompleteFile);
        if( hash != null ) {
            long size = IncompleteFileManager.getCompletedSize(incompleteFile);
            // Find any matching file-desc for this URN.
            FileDesc fd = fileManager.getFileDescForUrn(hash);
            if( fd != null ) {
                //create validAlts
                validAlts = AlternateLocationCollection.create(hash);
                // Retrieve the alternate locations (without adding ourself)
                AlternateLocationCollection coll = 
                                            fd.getAlternateLocationCollection();
                synchronized(coll) {
                    Iterator iter = coll.iterator();
                    while(iter.hasNext()) {
                        AlternateLocation loc = (AlternateLocation)iter.next();
                        addDownload(loc.createRemoteFileDesc((int)size),false);
                    }
                }
                
                //also adds any existing firewalled locations.
                //If I'm firewalled, only those that support FWT are added.
                //this assumes that FWT will always be backwards compatible
                boolean open = RouterService.acceptedIncomingConnection();
                coll = fd.getPushAlternateLocationCollection();
                synchronized(coll) {
                	Iterator iter = coll.iterator();
                	while(iter.hasNext()) {
                		PushAltLoc loc = (PushAltLoc)iter.next();
                		if (open || loc.supportsFWTVersion()>0)
                		    addDownload(loc.createRemoteFileDesc((int)size),false);
                	}
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
     * The default implementation uses extractQueryString, which
     * includes all non-trivial keywords found in the first RFD in allFiles.
     * A keyword is "non-trivial" if it is not a number of a common English
     * article (e.g., "the"), a number (e.g., "2"), or the file extension.
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
		    
		if(allFiles[0].getSHA1Urn() == null)
			return QueryRequest.createQuery(extractQueryString(name));
        else // this is where a SHA1 query would be sent, if desired
            return QueryRequest.createQuery(extractQueryString(name));
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
     * Returns a string to be used for querying from the given name.
     */
    protected final String extractQueryString(String name) {
        String retString = null;

        final int MAX_LEN = 30;

        // Put the keywords into a string, up to MAX_LEN
        Set intersection=keywords(name);
        if (intersection.size() < 1) // nothing to extract!
            retString = StringUtils.truncate(name, 30);
        else {
            StringBuffer sb = new StringBuffer();
            int numWritten = 0;
            for (Iterator keys=intersection.iterator(); 
                 keys.hasNext() && (numWritten < MAX_LEN); 
                 ) {
                String currKey = (String) keys.next();
                
                // if we have space to add the keyword
                if ((numWritten + currKey.length()) < MAX_LEN) {
                    if (numWritten > 0) // add a space if we've written before
                        sb.append(" ");
                    sb.append(currKey); // add the new keyword
                    numWritten += currKey.length() + (numWritten == 0 ? 0 : 1);
                }
            }

            retString = sb.toString();

            //one small problem - if every keyword in the filename is
            //greater than MAX_LEN, then the string returned will be empty.
            //if this happens just truncate the first word....
            if (retString.equals(""))
                retString = StringUtils.truncate(name, 30);
        }

        // Added a bunch of asserts to catch bugs.  There is some form of
        // input we are not considering in our algorithms....
        Assert.that(retString.length() <= MAX_LEN, 
                    "Original filename: " + name +
                    ", converted: " + retString);
        Assert.that(!retString.equals(""), 
                    "Original filename: " + name);
        Assert.that(retString != null, 
                    "Original filename: " + name);
        retString = I18NConvert.instance().getNorm(retString);
        Assert.that(!retString.equals(""), 
                    "I18N: Original filename: " + name);
        Assert.that(retString != null, 
                    "I18N: Original filename: " + name);
        return retString;
    }

    /** Returns the canonicalized non-trivial search keywords in fileName. */
    private static final Set keywords(String fileName) {
        //Remove extension
        fileName = ripExtension(fileName);
        
        //Separate by whitespace and _, etc.
        Set ret=new HashSet();
        String delim = FileManager.DELIMETERS;
        char[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuffer sb = new StringBuffer(delim.length() + illegal.length);
        sb.append(FileManager.DELIMETERS);
        sb.append(illegal);
        StringTokenizer st = new StringTokenizer(fileName, sb.toString());
        while (st.hasMoreTokens()) {
            final String currToken = st.nextToken().toLowerCase();
            try {                
                //Ignore if a number
                //(will trigger NumberFormatException if not)
                new Double(currToken);
                continue;
            } catch (NumberFormatException normalWord) {
                //Add non-numeric words that are not an (in)definite article.
                if (! TRIVIAL_WORDS.contains(currToken))
                    ret.add(currToken);
            }
        }
        return ret;
    }

    /** Returns fileName without any file extension. */
    private static String ripExtension(String fileName) {
        String retString = null;
        int extStart = fileName.lastIndexOf('.');
        if (extStart == -1)
            retString = fileName;
        else
            retString = fileName.substring(0, extStart);
        return retString;
    }

    private static final List TRIVIAL_WORDS=new ArrayList(3); {
        TRIVIAL_WORDS.add("the");  //must be lower-case
        TRIVIAL_WORDS.add("an");
        TRIVIAL_WORDS.add("a");
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
             0.10f*((float)(ripExtension(one)).length()),
             0.10f*((float)(ripExtension(two)).length())));
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
        if( NetworkUtils.isMe(rfd.getHost(), rfd.getPort()) )
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
            added = files.add(rfd);

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
        //this) or waiting for retry (by sleeping).
        if ( added ) {
            if(LOG.isTraceEnabled())
                LOG.trace("added rfd: " + rfd);
            if(isInactive() || dloaderManagerThread == null) {
                LOG.trace("inactive, marking as new source");
                receivedNewSources = true;
            } else {
                this.notify();                      //see tryAllDownloads3
            }
            // we could manager.requestStart, but that is synchronized on
            // manager and we can't grab that lock if we're synchronized on
            // this.  since we can't guarantee we won't be holding this' lock,
            // we leave it up to manager to start us off if we have new sources
        }

        return true;
    }
    
    /**
     * Determines if we already have this RFD in our lists.
     */
    private synchronized boolean isRFDAlreadyStored(RemoteFileDesc rfd) {
        if( currentRFDs != null && currentRFDs.contains(rfd)) 
            return true;
        if( files != null && files.contains(rfd)) 
            return true;
        return false;
    }
    
    /**
     * Returns true if we have received more possible source since the last
     * time we went inactive.
     */
    public boolean hasNewSources() {
        return receivedNewSources;
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
        Object lock =  miniRFDToLock.get(mrfd);
        if(lock == null) //not in map. Not intended for me
            return false;
        threadLockToSocket.put(lock,socket);
        synchronized(lock) {
            lock.notify();
        }
        return true;
    }

    public void stop() {
        // make redundant calls to stop() fast
        // this change is pretty safe because stopped is only set in two
        // places - initialized and here.  so long as this is true, we know
        // this is safe.
        if (stopped)
            return;

        LOG.debug("STOPPING ManagedDownloader");

        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
        
        // Tell the manager to remove us if we're inactive.
        // The "if waiting" part may be an optimization, since I don't think
        // anything bad will occur if a download is removed multiple times,
        // but it is safest to only remove if we know it's not going to
        // remove shortly.
        // This call MUST be outside this' lock, else deadlock could occur.
        manager.removeIfWaiting(this);
        
        synchronized(this) {
            //This guarantees any downloads in progress will be killed.  New
            //downloads will not start because of the flag above. Note that this
            //does not kill downloaders that are queued...
            for (Iterator iter=dloaders.iterator(); iter.hasNext(); ) 
                ((HTTPDownloader)iter.next()).stop();
    
            //...so we interrupt all threads - see connectAndDownload.
            //This is safe because worker threads can be waiting for a push 
            //or to requeury, or sleeping while queued. In every case its OK to 
            //interrupt the thread
            for(Iterator iter=threads.iterator(); iter.hasNext(); )
                ((Thread)iter.next()).interrupt();

            if(dloaderManagerThread != null)
                dloaderManagerThread.interrupt();
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
    private synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
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
        for(Iterator iter=dloaders.iterator(); iter.hasNext();) {
            HTTPDownloader httpDloader = (HTTPDownloader)iter.next();
            RemoteFileDesc r = httpDloader.getRemoteFileDesc();
            
            // no need to tell uploader about itself and since many firewalled
            // downloads may have the same port and host, we also check their
            // push endpoints
            if(!r.needsPush() ? 
                    (r.getHost().equals(rfd.getHost()) && r.getPort()==rfd.getPort()) :
                    r.getPushAddr()!=null && r.getPushAddr().equals(rfd.getPushAddr()))
                continue;
            
            
            
            //no need to send push altlocs to older uploaders
            if (loc instanceof DirectAltLoc || 
            		httpDloader.wantsFalts())
            	if (good)
            		httpDloader.addSuccessfulAltLoc(loc);
            	else
            		httpDloader.addFailedAltLoc(loc);
            
           	
        }

        FileDesc fd = fileManager.getFileDescForFile(incompleteFile);
        if( fd != null && fd instanceof IncompleteFileDesc) {
            ifd = (IncompleteFileDesc)fd;
            if(!downloadSHA1.equals(ifd.getSHA1Urn())) {
                // Assert that the SHA1 of the IFD and our sha1 match.
                Assert.silent(false, "wrong IFD.\n" +
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
                        if(rfd.needsPush())
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
    public boolean resume() {
        synchronized(this) {
            //Ignore request if already in the download cycle.
            if (!isInactive())
                return false;
    
            // if we were waiting for the user to start us, then try to send the
            // requery.
            if(getState() == WAITING_FOR_USER)
                sendRequery();
            // also retry any hosts that we have leftover.
            initializeFiles();
        }
        
        // Notify the manager that we want to resume.
        // It will start us if we're allowed to start.
        manager.requestStart(this);
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
    protected void tryAllDownloads() {
        if(checkHosts()) {//files is global
            setState(GAVE_UP);
            return;
        }
        
        int status = GAVE_UP;
        try {
            status = tryAllDownloads2();
        } catch (InterruptedException e) {
            // nothing should interrupt except for a stop
            if (!stopped)
                ErrorService.error(e);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("MANAGER: TAD2 returned: " + status);
                   
        // If TAD2 gave a completed state, set the state correctly & exit.
        // Otherwise...
        // If we manually stopped then set to ABORTED, else set to the 
        // appropriate state (either a busy host or no hosts to try).
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
            else
                setState(status);
            return;
        default:
            Assert.that(false, "Bad status from tad2: "+status);
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
        if (files == null || files.size()==0)
            return 0;
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        for (int i = 0; i < files.size(); i++) {
            waitTime = Math.min(waitTime, 
                            ((RemoteFileDesc)files.get(i)).getWaitTime());
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
        synchronized (this) {
            if (files.size()==0)
                return GAVE_UP;
        }

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
            incompleteFile = incompleteFileManager.getFile(
                                         (RemoteFileDesc)files.get(0));
            saveDir = SharingSettings.getSaveDirectory();
            completeFile = new File(saveDir, fileName);
            String savePath = saveDir.getCanonicalPath();
            String completeFileParentPath = 
                new File(completeFile.getParent()).getCanonicalPath();
            if (!savePath.equals(completeFileParentPath))
                return COULDNT_MOVE_TO_LIBRARY;
        } catch (IOException e) {
            ErrorService.error(e, "incomplete: " + incompleteFile);
            return COULDNT_MOVE_TO_LIBRARY;
        }

        // Create a new validAlts for this sha1.
        // initialize the HashTree
		if( downloadSHA1 != null ) {
		    validAlts = AlternateLocationCollection.create(downloadSHA1);
            hashTree = TigerTreeCache.instance().getHashTree(downloadSHA1);  
        }
        
        URN fileHash;
        int status;

        // Continue doing the download until we've recovered all
        // corrupt bytes.
        for(int i = 0; ; i++) {
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
            //If the hash is different, we try to automatically recover if
            //we have a HashTree.
            fileHash = scanForCorruption(i);
            if (corruptState == CORRUPT_STOP_STATE) {
                cleanupCorrupt(incompleteFile, completeFile.getName());
                return CORRUPT_FILE;
            }

            //if we didn't identify any corrupted ranges, break out of the loop
            if(state != IDENTIFY_CORRUPTION)
                break;                
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
    private URN scanForCorruption(int iteration) {
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
        
        // If we wanted to stop, stop.
        if (corruptState==CORRUPT_STOP_STATE)
            return fileHash;
            
        // only try recovering MAX_CURROPTION_RECOVERY_ATTEMPTS times.
        if(iteration == MAX_CORRUPTION_RECOVERY_ATTEMPTS) {
            treeRecoveryFailed(downloadSHA1);
        } else if (hashTree != null) {
            // we can try to use the hashtree to identify corrupt ranges!
            try {
                setState(IDENTIFY_CORRUPTION);
                LOG.debug("identifying corruption...");
                int deleted = commonOutFile.deleteCorruptedBlocks(hashTree,
                                                             incompleteFile);
                if(LOG.isDebugEnabled())
                    LOG.debug("deleted " + deleted + " blocks");
                
                corruptState = NOT_CORRUPT_STATE;
                if(deleted == 0)
                    treeRecoveryFailed(downloadSHA1);
            } catch (IOException ioe) {
                LOG.debug(ioe);
                treeRecoveryFailed(downloadSHA1);
            }
        }
        
        return fileHash;        
    }
    
    /**
     * Recovering from the TigerTree has failed, notify the user about
     * corruption.
     */
    private void treeRecoveryFailed(URN hash) {
        TigerTreeCache.instance().purgeTree(hash);
        hashTree = null;
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
                set = new HashSet(files);
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
    private synchronized void startWorker(final RemoteFileDesc rfd) {
        Thread connectCreator = new ManagedThread("DownloadWorker") {
            public void managedRun() {
                boolean iterate = false;
                try {
                    iterate = connectAndDownload(rfd);
                } catch (Throwable e) {
                    iterate = true;
                     // Ignore InterruptedException -- the JVM throws
                     // them for some reason at odd times, even though
                     // we've caught and handled all of them
                     // appropriately.
                    if(!(e instanceof InterruptedException)) {
                        //This is a "firewall" for reporting unhandled
                        //errors.  We don't really try to recover at
                        //this point, but we do attempt to display the
                        //error in the GUI for debugging purposes.
                        ErrorService.error(e);
                    }
                } finally {
                    synchronized (ManagedDownloader.this) {
                        currentRFDs.remove(rfd);
                        threads.remove(this); 
                        if(iterate) 
                            ManagedDownloader.this.notifyAll();
                    }
                }
            }//end of run
        };

        threads.add(connectCreator);
        currentRFDs.add(rfd);

        connectCreator.start();
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

        if(!initializeVerifyingFile())
            return COULDNT_MOVE_TO_LIBRARY;

        //The current RFDs that are being connected to.
        currentRFDs = new LinkedList();
        int size = -1;
        int connectTo = -1;
        int dloadsCount = -1;
        //Assert.that(threads.size()==0,
        //            "wrong threads size: " + threads.size());

        //While there is still an unfinished region of the file...
        while (true) {
            if (stopped) {
                LOG.warn("MANAGER: terminating because of stop");
                throw new InterruptedException();
            } 
            
            // Finished.
            if (commonOutFile.isComplete()) {
                // Kill any leftover downloaders.
                for (Iterator iter=dloaders.iterator(); iter.hasNext(); ) 
                    ((HTTPDownloader)iter.next()).stop();		                
                
                //Interrupt all worker threads
                for(int i=threads.size();i>0;i--) {
                    Thread t = (Thread)threads.get(i-1);
                    t.interrupt();
                }
            
                LOG.trace("MANAGER: terminating because of completion");
                return COMPLETE;
            } 
    
            if (threads.size() == 0) {                        
                //No downloaders worth living for.
                if ( files.size() > 0 && calculateWaitTime() > 0) {
                    LOG.trace("MANAGER: terminating with busy");
                    return WAITING_FOR_RETRY;
                } else if( files.size() == 0 ) {
                    LOG.trace("MANAGER: terminating w/o hope");
                    return GAVE_UP;
                }
                // else (files.size() > 0 && calculateWaitTime() == 0)
                // fallthrough ...
            }

            size = files.size();
            connectTo = getNumAllowedDownloads();
            dloadsCount = dloaders.size();
            
            if(LOG.isDebugEnabled())
                LOG.debug("MANAGER: kicking off workers, size: " + size + 
                          ", connect: " + connectTo + ", dloadsCount: " + 
                          dloadsCount + ", threads: " + threads.size());
                
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
                    files.add(rfd);
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
     * Top level method of the thread. Calls three methods 
     * a. Establish a TCP Connection.
     * b. Assign this thread a part of the file, and do HTTP handshaking
     * c. get the file.
     * Each of these steps can run into errors, which have to be dealt with
     * differently.
     * @return true if this worker thread should notify, false otherwise.
     * currently this method returns false iff NSEEx is  thrown. 
     */
    private boolean connectAndDownload(RemoteFileDesc rfd) {
        if(LOG.isTraceEnabled())
            LOG.trace("connectAndDownload for: " + rfd);
        
        //this make throw an exception if we were not able to establish a 
        //direct connection and push was unsuccessful too
        HTTPDownloader dloader = null;
        
        //Step 1. establish a TCP Connection, either by opening a socket,
        //OR by sending a push request.
        dloader = establishConnection(rfd);
        
        if(dloader == null)//any exceptions in the method internally?
            return true;//no work was done, try to get another thread

        //initilaize the newly created HTTPDownloader with whatever AltLocs we
        //have discovered so far. These will be cleared out after the first
        //write, from them on, only newly successful rfds will be sent as alts
        if(validAlts != null) {
            synchronized(altLock) {
                Iterator iter = validAlts.iterator();
                int count = 0;
                while(iter.hasNext() && count < 10) {
                	AlternateLocation current = (AlternateLocation)iter.next();
                	dloader.addSuccessfulAltLoc(current);
                	count++;
                }
                iter = recentInvalidAlts.iterator();
                while(iter.hasNext()) {
                	AlternateLocation current = (AlternateLocation)iter.next();
                	dloader.addFailedAltLoc(current);
                	count++;
                }
            }
        }
        
        //Note: http11 is true or false depending on what we think thevalue
        //should be for rfd is at the start, before connecting. We may later
        //find that the we are wrong, in which case we update the rfd's http11
        //value. But while we are in connectAndDownload we continue to use this
        //local variable because the code is incapable of handling a change in
        //http11 status while inside connectAndDownload.
        boolean http11 = true;//must enter the loop
        
        try {

        while(http11) {
            //Step 2. OK. We have established TCP Connection. This 
            //downloader should choose a part of the file to download
            //and send the appropriate HTTP hearders
            //Note: 0=disconnected,1=tcp-connected,2=http-connected            
            ConnectionStatus status;
            http11 = rfd.isHTTP11();
            while(true) { //while queued, connect and sleep if we queued
                status = null;
                
                // request THEX from te dloader if the tree we have
                // isn't good enough (or we don't have a tree)
                if (dloader.hasHashTree() &&
                  (hashTree == null || !hashTree.isDepthGoodEnough())) {
                    status = dloader.requestHashTree();
                    if(status.isThexResponse()) {
                        HashTree temp = status.getHashTree();
                        if (temp.isBetterTree(hashTree)) {
                            hashTree = temp;
                            // persist the hashTree in the TigerTreeCache
                            // because we won't save it in ManagedDownloader
                            TigerTreeCache.instance().addHashTree(
                                    rfd.getSHA1Urn(),
                                    hashTree);
                        }
                    }
                }
                
                // before requesting the next range,
                // consume the prior request's body
                // if there was any.
                dloader.consumeBodyIfNecessary();
                synchronized(stealLock) {
                    // if we didn't get queued doing the tree request,
                    // request another file.
                    try {
                        if(status == null || !status.isQueued()) {
                            status = assignAndRequest(dloader, http11);
                        }
                    } finally {
                        if( status == null || !status.isConnected() )
                            releaseRanges(dloader);
                    }
                }
                
                if(status.isPartialData()) {
                    // loop again if they had partial ranges.
                    continue;
                } else if(status.isNoFile() || status.isNoData()) {
                    //if they didn't have the file or we didn't need data,
                    //break out of the loop.
                    break;
                }
                
                // must be queued or connected.
                Assert.that(status.isQueued() || status.isConnected());
                boolean addQueued = killQueuedIfNecessary(status);
                
                // we should have been told to stay alive if we're connected
                // but it's possible that we are above our swarm capacity
                // and nothing else was queued, in which case we really should
                // kill ourselves, but there's no reason to not accept the
                // extra host.
                if(status.isConnected())
                    break;
                
                Assert.that(status.isQueued());
                // if we didn't want to stay queued
                // or we got interrupted while sleeping,
                // then try other sources
                if(!addQueued || handleQueued(status, dloader))
                    return true;
            }
            //we have been given a slot remove this thread from queuedThreads
            synchronized(this) {
                //no problem even if this thread is  not in there
                queuedThreads.remove(Thread.currentThread()); 
            }

            switch(status.getType()) {
            case ConnectionStatus.TYPE_NO_FILE:
                // close the connection for now.            
                dloader.stop();
                return true;            
            case ConnectionStatus.TYPE_NO_DATA:
                // close the connection since we're finished.
                dloader.stop();
                return false;
            case ConnectionStatus.TYPE_CONNECTED:
                break;
            default:
                throw new IllegalStateException("illegal status: " + 
                                                status.getType());
            }

            Assert.that(status.isConnected());
            //Step 3. OK, we have successfully connected, start saving the
            // file to disk
            boolean downloadOK = false;
            
            try {
                downloadOK = doDownload(dloader, http11);
            } finally {
                // Always release the ranges that were leftover from this download
                releaseRanges(dloader);
            }
            
            // If the download failed, don't keep trying to download.
            if(!downloadOK)
                break;
        } // end of while(http11)
        
        } finally {
            // we must ensure that all dloaders are removed from the data
            // structure before returning from this method.
            synchronized(this) {
                dloaders.remove(dloader);
            }
        }
        
        //tell manager to iterate for more sources
        return true;
    }
    
    /**
     * Handles a queued downloader with the given ConnectionStatus.
     * BLOCKING (while sleeping).
     *
     * @return true if we need to tell the manager to churn another
     *         connection and let this one die, false if we are
     *         going to try this connection again.
     */
    private boolean handleQueued(ConnectionStatus status,
                                 HTTPDownloader dloader) {
        try {
            // make sure that we're not in dloaders if we're
            // sleeping/queued.  this would ONLY be possible
            // if some uploader was misbehaved and queued
            // us after we succesfully managed to download some
            // information.  despite the rarity of the situation,
            // we should be prepared.
            synchronized(this) {
                dloaders.remove(dloader);
            }
            Thread.sleep(status.getQueuePollTime());//value from QueuedException
            return false;
        } catch (InterruptedException ix) {
            if(LOG.isWarnEnabled())
                LOG.warn("worker: interrupted while asleep in "+
                  "queue" + dloader);
            queuedThreads.remove(Thread.currentThread());
            dloader.stop();//close connection
                    // notifying will make no diff, coz the next 
                    //iteration will throw interrupted exception.
            return true;
        }
    }
    
    /**
     * Interrupts a remotely queued thread if we this status is connected,
     * or if the status is queued and our queue position is better than
     * an existing queued status.
     *
     * @param status The ConnectionStatus of this downloader.
     *
     * @return true if this thread should be kept around, false otherwise --
     * explicitly, there is no need to kill any threads, or if the currentThread
     * is already in the queuedThreads, or if we did kill a thread worse than
     * this thread.  
     */
    private boolean killQueuedIfNecessary(ConnectionStatus status) {
        //Either I am queued or downloading, find the highest queued thread
        Thread killThread = null;
        Thread currentThread = Thread.currentThread();
        int queuePos = status.isQueued() ? status.getQueuePosition() : -1;

        synchronized(this) {
            // No replacement required?...
            if(getNumDownloaders() < getSwarmCapacity()) {
                if(status.isQueued())
                    queuedThreads.put(currentThread, new Integer(queuePos));
                return true;
            }
            
            // Already Queued?...
            if(queuedThreads.containsKey(currentThread)) {
                // update position
                if(status.isQueued())
                    queuedThreads.put(currentThread, new Integer(queuePos));
                return true;
            }
            
            // Search for the queued thread with a slot worse than ours.
            Iterator iter = queuedThreads.keySet().iterator();
            int highest = queuePos; // -1 if we aren't queued.
            while(iter.hasNext()) {
                Object o = iter.next();
                int currQueue = ((Integer)queuedThreads.get(o)).intValue();
                if(currQueue > highest) {
                    killThread=(Thread)o;
                    highest = currQueue;
                }
            }

            // No one worse than us?... kill us.
            if(killThread == null)
                return false;

            //OK. let's kill this guy 
            killThread.interrupt();

            //OK. I should add myself to queuedThreads if I am queued
            if(status.isQueued())
                    queuedThreads.put(currentThread, new Integer(queuePos));
            
            return true;
        }        
    }
    

    /** 
     * Returns an un-initialized (only established a TCP Connection, 
     * no HTTP headers have been exchanged yet) connectable downloader 
     * from the given list of locations.
     * <p> 
     * method tries to establish connection either by push or by normal
     * ways.
     * <p>
     * If the connection fails for some reason, or needs a push the mesh needs 
     * to be informed that this location failed.
     * @param rfd the RemoteFileDesc to connect to
     * <p> 
     * The following exceptions may be thrown within this method, but they are
     * all dealt with internally. So this method does not throw any exception
     * <p>
     * NoSuchElementException thrown when (both normal and push) connections 
     * to the given rfd fail. We discard the rfd by doing nothing and return 
     * null.
     * @exception InterruptedException this thread was interrupted while waiting
     * to connect. Remember this rfd by putting it back into files and return
     * null 
     */
    private HTTPDownloader establishConnection(RemoteFileDesc rfd) {
        if(LOG.isTraceEnabled())
            LOG.trace("establishConnection(" + rfd + ")");
        
        if (rfd == null) //bad rfd, discard it and return null
            return null; // throw new NoSuchElementException();
        
        if (stopped) {//this rfd may still be useful remember it
            synchronized(this){
                files.add(rfd);
            }
            return null;
        }

        File incFile = incompleteFile;
        HTTPDownloader ret;
        boolean needsPush = rfd.needsPush();
        
        
        synchronized (this) {
            currentLocation=rfd.getHost();
            //If we're just increasing parallelism, stay in DOWNLOADING
            //state.  Otherwise the following call is needed to restart
            //the timer.
            if (dloaders.size()==0 && getState()!=COMPLETE && 
                getState()!=ABORTED && getState()!=GAVE_UP && 
                getState()!=COULDNT_MOVE_TO_LIBRARY && getState()!=CORRUPT_FILE 
                && getState()!=HASHING && getState()!=SAVING && 
                queuedThreads.size()==0) {
                    if(Thread.currentThread().isInterrupted())
                        return null; // we were signalled to stop.
                    setState(CONNECTING, 
                            needsPush ? PUSH_CONNECT_TIME : NORMAL_CONNECT_TIME);
                }
        }

        if(LOG.isDebugEnabled())
            LOG.debug("WORKER: attempting connect to "
              + rfd.getHost() + ":" + rfd.getPort());        
        
        DownloadStat.CONNECTION_ATTEMPTS.incrementStat();

        // for multicast replies, try pushes first
        // and then try direct connects.
        // this is because newer clients work better with pushes,
        // but older ones didn't understand them
        if( rfd.isReplyToMulticast() ) {
            try {
                ret = connectWithPush(rfd, incFile);
            } catch(IOException e) {
                try {
                    ret = connectDirectly(rfd, incFile);
                } catch(IOException e2) {
                    return null; // impossible to connect.
                }
            }
            return ret;
        }        
        
        // otherwise, we're not multicast.
        // if we need a push, go directly to a push.
        // if we don't, try direct and if that fails try a push.        
        if( !needsPush ) {
            try {
                ret = connectDirectly(rfd, incFile);
                return ret;
            } catch(IOException e) {
                // fall through to the push ...
            }
        }
        try {
                 ret = connectWithPush(rfd, incFile);
                 return ret;
        } catch(IOException e) {
                // even the push failed :(
        }
        
        
        // if we're here, everything failed.
        
        informMesh(rfd, false);
        
        return null;
    }
        

    /**
     * Attempts to directly connect through TCP to the remote end.
     */
    private HTTPDownloader connectDirectly(RemoteFileDesc rfd, 
      File incFile) throws IOException {
        LOG.trace("WORKER: attempt direct connection");
        HTTPDownloader ret;
        //Establish normal downloader.              
        ret = new HTTPDownloader(rfd, incFile);
        // Note that connectTCP can throw IOException
        // (and the subclassed CantConnectException)
        try {
            ret.connectTCP(NORMAL_CONNECT_TIME);
            DownloadStat.CONNECT_DIRECT_SUCCESS.incrementStat();
        } catch(IOException iox) {
            DownloadStat.CONNECT_DIRECT_FAILURES.incrementStat();
            throw iox;
        }
        return ret;
    }
    
    /**
     * Attempts to connect by using a push to the remote end.
     * BLOCKING.
     */
    private HTTPDownloader connectWithPush(RemoteFileDesc rfd,
      File incFile) throws IOException {
        LOG.trace("WORKER: attempt push connection");
        HTTPDownloader ret;
        
        //When the push is complete and we have a socket ready to use
        //the acceptor thread is going to notify us using this object
        Object threadLock = new Object();
        MiniRemoteFileDesc mrfd = new MiniRemoteFileDesc(
                     rfd.getFileName(),rfd.getIndex(),rfd.getClientGUID());
       
        miniRFDToLock.put(mrfd,threadLock);

        boolean pushSent;
        synchronized(threadLock) {
            // only wait if we actually were able to send the push
            manager.sendPush(rfd, threadLock);
            
            //No loop is actually needed here, assuming spurious
            //notify()'s don't occur.  (They are not allowed by the Java
            //Language Specifications.)  Look at acceptDownload for
            //details.
            try {
                threadLock.wait(rfd.isFromAlternateLocation()? 
                        UDP_PUSH_CONNECT_TIME: 
                            PUSH_CONNECT_TIME);  
            } catch(InterruptedException e) {
                DownloadStat.PUSH_FAILURE_INTERRUPTED.incrementStat();
                throw new IOException("push interupted.");
            }
            
        }
        
        //Done waiting or were notified.
        Socket pushSocket = (Socket)threadLockToSocket.remove(threadLock);
        if (pushSocket==null) {
            DownloadStat.PUSH_FAILURE_NO_RESPONSE.incrementStat();
            
            throw new IOException("push socket is null");
        }
        
        miniRFDToLock.remove(mrfd);//we are not going to use it after this
        ret = new HTTPDownloader(pushSocket, rfd, incFile);
        
        //Socket.getInputStream() throws IOX if the connection is closed.
        //So this connectTCP *CAN* throw IOX.
        try {
            ret.connectTCP(0);//just initializes the byteReader in this case
            DownloadStat.CONNECT_PUSH_SUCCESS.incrementStat();
        } catch(IOException iox) {
            DownloadStat.PUSH_FAILURE_LOST.incrementStat();
            throw iox;
        }
        return ret;
    }
    
    /** 
     * Assigns a white area or a grey area to a downloader. Sets the state,
     * and checks if this downloader has been interrupted.
     * @param dloader The downloader to which this method assigns either
     * a grey area or white area.
     * @return the ConnectionStatus.
     */
    private ConnectionStatus assignAndRequest(HTTPDownloader dloader,
                                              boolean http11) {
        RemoteFileDesc rfd = dloader.getRemoteFileDesc();
        if(LOG.isTraceEnabled())
            LOG.trace(Thread.currentThread().hashCode()+" assignAndRequest for: " + rfd);
        
        try {
            if (commonOutFile.hasFreeBlocksToAssign()) {
                assignWhite(dloader,http11);
            } else {
                assignGrey(dloader,http11); 
            }
        } catch(NoSuchElementException nsex) {
            DownloadStat.NSE_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())            
                LOG.debug(Thread.currentThread().hashCode()+" nsex thrown in assingAndRequest "+dloader,nsex);
            synchronized(this) {
                // Add to files, keep checking for stalled uploader with
                // this rfd
                files.add(rfd);
            }
            return ConnectionStatus.getNoData();
        } catch (NoSuchRangeException nsrx) { 
            DownloadStat.NSR_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("nsrx thrown in assignAndRequest "+dloader,nsrx);
            synchronized(this) {
                //forget the ranges we are preteding uploader is busy.
                rfd.setAvailableRanges(null);
                //if this RFD did not already give us a retry-after header
                //then set one for it.
                if(!rfd.isBusy())
                    rfd.setRetryAfter(NO_RANGES_RETRY_AFTER);
                files.add(rfd);
            }
            rfd.resetFailedCount();                
            return ConnectionStatus.getNoFile();
        } catch(TryAgainLaterException talx) {
            DownloadStat.TAL_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("talx thrown in assignAndRequest "+dloader,talx);
                
            //if this RFD did not already give us a retry-after header
            //then set one for it.
            if ( !rfd.isBusy() ) {
                rfd.setRetryAfter(RETRY_AFTER_NONE_ACTIVE);
            }
            
            synchronized(this) {
                 //if we already have downloads going, then raise the
                 //retry-after if it was less than the appropriate amount
                if(dloaders.size() > 0 &&
                   rfd.getWaitTime() < RETRY_AFTER_SOME_ACTIVE)
                   rfd.setRetryAfter(RETRY_AFTER_SOME_ACTIVE);

                files.add(rfd);//try this rfd later
            }
            rfd.resetFailedCount();                
            return ConnectionStatus.getNoFile();
        } catch(RangeNotAvailableException rnae) {
            DownloadStat.RNA_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("rnae thrown in assignAndRequest "+dloader,rnae);
            rfd.resetFailedCount();                
            informMesh(rfd, true);
            //no need to add to files or busy we keep iterating
            return ConnectionStatus.getPartialData();
        } catch (FileNotFoundException fnfx) {
            DownloadStat.FNF_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("fnfx thrown in assignAndRequest"+dloader, fnfx);
            informMesh(rfd, false);
            return ConnectionStatus.getNoFile();
        } catch (NotSharingException nsx) {
            DownloadStat.NS_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("nsx thrown in assignAndRequest "+dloader, nsx);
            informMesh(rfd, false);
            return ConnectionStatus.getNoFile();
        } catch (QueuedException qx) { 
            DownloadStat.Q_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("qx thrown in assignAndRequest "+dloader, qx);
            synchronized(this) {
                if(dloaders.size()==0) {
                    if(stopped || Thread.currentThread().isInterrupted())
                        return ConnectionStatus.getNoData(); // we were signalled to stop.
                    setState(REMOTE_QUEUED);
                }
                int oldPos = queuePosition.equals("")?
                Integer.MAX_VALUE:Integer.parseInt(queuePosition);
                int newPos = qx.getQueuePosition();
                if ( newPos < oldPos ) {
                    queuePosition = "" + newPos;
                    queuedVendor = dloader.getVendor();
                }                    
            }
            rfd.resetFailedCount();                
            return ConnectionStatus.getQueued(qx.getQueuePosition(),
                                              qx.getMinPollTime());
        } catch(ProblemReadingHeaderException prhe) {
            DownloadStat.PRH_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("prhe thrown in assignAndRequest "+dloader,prhe);
            informMesh(rfd, false);
            return ConnectionStatus.getNoFile();
        } catch(UnknownCodeException uce) {
            DownloadStat.UNKNOWN_CODE_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("uce (" + uce.getCode() +
                          ") thrown in assignAndRequest " +
                          dloader, uce);
            informMesh(rfd, false);
            return ConnectionStatus.getNoFile();
        } catch (ContentUrnMismatchException cume) {
        	DownloadStat.CONTENT_URN_MISMATCH_EXCEPTION.incrementStat();
        	if(LOG.isDebugEnabled())
        		LOG.debug("cume thrown in assignAndRequest " + dloader, cume);
			return ConnectionStatus.getNoFile();
        } catch (IOException iox) {
            DownloadStat.IO_EXCEPTION.incrementStat();
            if(LOG.isDebugEnabled())
                LOG.debug("iox thrown in assignAndRequest "+dloader, iox);
            
            rfd.incrementFailedCount();
            
            // if this RFD had an IOX while reading headers/downloading
            // less than twice in succession, try it again.
            if( rfd.getFailedCount() < 2 ) {
                //set retry after, wait a little before retrying this RFD
                rfd.setRetryAfter(FAILED_RETRY_AFTER);
                synchronized(this) {
                    files.add(rfd); 
                }
            } else //tried the location twice -- it really is bad
                informMesh(rfd, false);         
            return ConnectionStatus.getNoFile();
        } finally {
            //add alternate locations, which we could have gotten from 
            //the downloader
            AlternateLocationCollection c = dloader.getAltLocsReceived();
            if(c!=null) {
                synchronized(c) { 
                    Iterator iter = c.iterator();
                    while(iter.hasNext()) {
                        AlternateLocation al=(AlternateLocation)iter.next();
                        RemoteFileDesc rfd1 =
                            al.createRemoteFileDesc(rfd.getSize());
                        addDownload(rfd1, false);//don't cache
                    }
                }
            }
            
            //we also want to try the firewalled push locs
            //if we are firewalled, try only those that support FWT 
            c = dloader.getPushLocsReceived();
            boolean open = RouterService.acceptedIncomingConnection();
            if(c!=null ) {
                synchronized(c) { 
                    Iterator iter = c.iterator();
                    while(iter.hasNext()) {
                        PushAltLoc al=(PushAltLoc)iter.next();
                        if (open || al.supportsFWTVersion() > 0) {
                            RemoteFileDesc rfd1 =
                                al.createRemoteFileDesc(rfd.getSize());
                            addDownload(rfd1, false);//don't cache
                        }
                    }
                }
            }
        }
        
        //did not throw exception? OK. we are downloading
        if(rfd.getFailedCount() > 0)
            DownloadStat.RETRIED_SUCCESS.incrementStat();    
        
        rfd.resetFailedCount();

        synchronized(this) {
            if (stopped || Thread.currentThread().isInterrupted()) {
                LOG.trace("Stopped in assignAndRequest");
                files.add(rfd);
                return ConnectionStatus.getNoData();
            }
            
            setState(DOWNLOADING);

            // only add if not already added.
            if(!dloaders.contains(dloader))
                dloaders.add(dloader);
            chatList.addHost(dloader);
            browseList.addHost(dloader);
        }

        DownloadStat.RESPONSE_OK.incrementStat();            
        return ConnectionStatus.getConnected();
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
        return (files == null ? 0 : files.size());
    }
    
    public synchronized int getBusyHostCount() {
        if (files == null) 
            return 0;

        int busy = 0;
        for (int i = 0; i < files.size(); i++) {
            if ( ((RemoteFileDesc)files.get(i)).isBusy() )
                busy++;
        }
        return busy;
    }

    public synchronized int getQueuedHostCount() {
        return queuedThreads.size();
    }

    /**
     * Assigns a white part of the file to a HTTPDownloader and returns it.
     * This method has side effects.
     */
    private void assignWhite(HTTPDownloader dloader, boolean http11) throws 
    IOException, TryAgainLaterException, FileNotFoundException, 
    NotSharingException , QueuedException, NoSuchRangeException,
    NoSuchElementException {
        //Assign "white" (unclaimed) interval to new downloader.
        //TODO2: assign to existing downloader if possible, without
        //      increasing parallelis
        Interval interval = null;
        
        // If it's not a partial source, take the first chunk.
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        if( !dloader.getRemoteFileDesc().isPartialSource() ) {
            if(http11)
                interval = commonOutFile.leaseWhite(CHUNK_SIZE);
            else
                interval = commonOutFile.leaseWhite();
        }
        // If it is a partial source, extract the first needed/available range
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        else {
            try {
                IntervalSet availableRanges =
                    dloader.getRemoteFileDesc().getAvailableRanges();
                if(http11)
                    interval =
                        commonOutFile.leaseWhite(availableRanges, CHUNK_SIZE);
                else
                    interval = commonOutFile.leaseWhite(availableRanges);
            } catch(NoSuchElementException nsee) {
                // if nothing satisfied this partial source, don't throw NSEE
                // because that means there's nothing left to download.
                // throw NSRE, which means that this particular source is done.
                throw new NoSuchRangeException();
            }
        }
        
        //Intervals from the IntervalSet set are INCLUSIVE on the high end, but
        //intervals passed to HTTPDownloader are EXCLUSIVE.  Hence the +1 in the
        //code below.  Note connectHTTP can throw several exceptions.
        int low = interval.low;
        int high = interval.high; // INCLUSIVE
        dloader.connectHTTP(getOverlapOffset(low), high + 1, true);
        //The dloader may have told us that we're going to read less data than
        //we expect to read.  We must release the not downloading leased intervals
        //note the confusion caused by downloading overlap
        //regions.  We only want to release a range if the reported subrange
        //was different, and was HIGHER than the low point.        
        int newLow = dloader.getInitialReadingPoint();
        int newHigh = (dloader.getAmountToRead() - 1) + newLow; // INCLUSIVE
        if(newLow > low) {
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER:"+Thread.currentThread().hashCode()+
                        " Host gave subrange, different low.  Was: " +
                          low + ", is now: " + newLow);
            commonOutFile.releaseBlock(new Interval(low, newLow-1));
        }
        if(newHigh < high) {
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER:"+Thread.currentThread().hashCode()+
                        " Host gave subrange, different high.  Was: " +
                          high + ", is now: " + newHigh);
            commonOutFile.releaseBlock(new Interval(newHigh+1, high));
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("WORKER:"+Thread.currentThread().hashCode()+
                    " assigning white " + newLow + "-" + newHigh +
                      " to " + dloader);
    }


    /**
     * Steals a grey area from the biggesr HTTPDownloader and gives it to
     * the HTTPDownloader this method will return. 
     * <p> 
     * If there is less than MIN_SPLIT_SIZE left, we will assign the entire
     * area to a new HTTPDownloader, if the current downloader is going too
     * slow.
     */
    private void assignGrey(HTTPDownloader dloader, boolean http11) throws
    NoSuchElementException,  IOException, TryAgainLaterException, 
    QueuedException, FileNotFoundException, NotSharingException,  
    NoSuchRangeException  {
        //If this dloader is a partial source, don't attempt to steal...
        //too confusing, too many problems, etc...
        if( dloader.getRemoteFileDesc().isPartialSource() )
            throw new NoSuchRangeException();

        //Split largest "gray" interval, i.e., steal part of another
        //downloader's region for a new downloader.  
        //TODO3: split interval into P-|dloaders|, etc., not just half
        //TODO3: there is a minor race condition where biggest and 
        //      dloader could write to the same region of the file
        //      I think it's ok, though it could result in >100% in the GUI
        HTTPDownloader biggest = null;
        synchronized (this) {
            if (dloaders.isEmpty()) {
                Assert.silent(commonOutFile.hasFreeBlocksToAssign());
                assignWhite(dloader,http11);
                return;
            }
            for (Iterator iter=dloaders.iterator(); iter.hasNext();) {
                HTTPDownloader h = (HTTPDownloader)iter.next();
                // If this guy isn't downloading, don't steal from him.
                if(!h.isActive())
                    continue;
                // If we have no one to steal from, use 
                if(biggest == null)
                    biggest = h;
                // Otherwise, steal only if what's left is
                // larger and there's stuff left.
                else {
                    int hLeft = h.getAmountToRead() - h.getAmountRead();
                    int bLeft = biggest.getAmountToRead() - 
                                biggest.getAmountRead();
                    if( hLeft > 0 && hLeft > bLeft )
                        biggest = h;
                }
            }                
        }
        if (biggest==null) {//Not using downloader...but RFD maybe useful
            throw new NoSuchElementException();
        }
        //Note that getAmountToRead() and getInitialReadingPoint() are
        //constant.  getAmountRead() is not, so we "capture" it into a
        //variable.
        int amountRead = biggest.getAmountRead();
        int left = biggest.getAmountToRead()-amountRead;
        //check if we need to steal the last chunk from a slow downloader.
        //TODO4: Should we check left < CHUNK_SIZE+OVERLAP_BYTES
        if ((http11 && left<CHUNK_SIZE) || (!http11 && left < MIN_SPLIT_SIZE)){ 
            float bandwidthVictim = -1;
            float bandwidthStealer = -1;
            
            try {
                bandwidthVictim = biggest.getAverageBandwidth();
                biggest.getMeasuredBandwidth(); // trigger IDE.
            } catch (InsufficientDataException ide) {
                LOG.debug(Thread.currentThread().hashCode()+" victim does not have datapoints", ide);
                bandwidthVictim = -1;
            }
            try {
                bandwidthStealer = dloader.getAverageBandwidth();
                dloader.getMeasuredBandwidth(); // trigger IDE.
            } catch(InsufficientDataException ide) {
                LOG.debug(Thread.currentThread().hashCode()+" stealer does not have datapoints", ide);
                bandwidthStealer = -1;
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER: "+Thread.currentThread().hashCode()+" " 
                        	+ dloader + " attempting to steal from " + 
                          biggest + ", stealer speed [" + bandwidthStealer +
                          "], victim speed [ " + bandwidthVictim + "]");
            
            // If we do have a measured bandwidth for the existing download,
            // and it is slower than what is acceptable, let the new guy steal.
            // OR
            // If the new guy is of an acceptable speed and his average
            // bandwidth is faster than the existing one, let him steal.
            if((bandwidthVictim != -1 &&
                bandwidthVictim < MIN_ACCEPTABLE_SPEED) ||
               (bandwidthStealer > MIN_ACCEPTABLE_SPEED &&
                bandwidthStealer > bandwidthVictim)) {
                //replace (bad boy) biggest if possible
                int start = biggest.getInitialReadingPoint() + amountRead;
                int stop = biggest.getInitialReadingPoint() + 
                           biggest.getAmountToRead();
                // If we happened to finish off the download, throw NSEX
                // and so we don't download any more.
                if(stop <= start)
                    throw new NoSuchElementException();
                //Note: we are not interested in being queued at this point this
                //line could throw a bunch of exceptions (not queuedException)
                dloader.connectHTTP(getOverlapOffset(start), stop, false);
                int newLow = dloader.getInitialReadingPoint();
                int newHigh = dloader.getAmountToRead() + newLow; // EXCLUSIVE
                // If the stealer isn't going to give us everything we need,
                // there's no point in stealing, so throw an exception and
                // don't steal.
                if( newLow > start || newHigh < stop ) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("WORKER: not stealing because stealer " +
                                  "gave a subrange.  Expected low: " + start +
                                  ", high: " + stop + ".  Was low: " + newLow +
                                  ", high: " + newHigh);
                    throw new IOException("bad stealer.");
                }
                if(LOG.isDebugEnabled())
                    LOG.debug("WORKER:"+Thread.currentThread().hashCode()+
                            " picking stolen grey "
                      +start+"-"+stop+" from "+biggest+" to "+dloader);
                biggest.stopAt(start);
                biggest.stop();
            }
            else { //less than MIN_SPLIT_SIZE...but we are doing fine...
                throw new NoSuchElementException();
            }
        }
        else { //There is a big enough chunk to split...split it
            int start;
            if(http11) { //steal CHUNK_SIZE bytes from the end
                start = biggest.getInitialReadingPoint() +
                        biggest.getAmountToRead() - CHUNK_SIZE + 1;
            } else {
                start= biggest.getInitialReadingPoint() + amountRead + left/2;
            }
            int stop = biggest.getInitialReadingPoint() + 
                       biggest.getAmountToRead();
            // If we happened to finish off the dl, don't download any more
            if(stop <= start)
                throw new NoSuchElementException();
            //this line could throw a bunch of exceptions
            dloader.connectHTTP(getOverlapOffset(start), stop, true);
            int newLow = dloader.getInitialReadingPoint();
            int newHigh = dloader.getAmountToRead() + newLow; // EXCLUSIVE
            if(newHigh < stop) {
                // We have a stealer, but he isn't going to give us all the
                // data we want, so discard him.
                if(LOG.isDebugEnabled())
                    LOG.debug("WORKER: not stealing because stealer " +
                              "gave a lower high.  Expected high: " + 
                              stop + ".  Was high: " + newHigh);
                throw new IOException("bad stealer");
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER: assigning split grey "
                  +newLow+"-"+newHigh+" from "+biggest+" to "+dloader);
            // Refocus the start with this new data, if the downloader
            // actually gave us a higher low value.  This is so we
            // don't tell the currently downloading guy to stop downloading
            // data we aren't going to get.
            if(newLow > start)
                start = newLow;
            biggest.stopAt(start);//we know that biggest must be http1.0
        }
    }


    /**
     * Offsets i by OVERLAP_BYTES.  Used when requesting the start-range
     * for downloading.
     */
    private int getOverlapOffset(int i) {
        return Math.max(0, i-OVERLAP_BYTES);
    }
    
    /**
     * Ensures that the given internal is still valid after overlap bytes
     * are subtract from it.  This is necessary because of the structure
     * of ManagedDownloader, as it always subtracts OVERLAP_BYTES from the
     * 'low' range of the interval when it requests data.
     * To ensure that we can correctly use partial file sources, we must
     * create a new interval that is offset by the bytes that will be
     * removed later.
     *
     * @return a new Interval whose low value is incremented by OVERLAP_BYTES
     *         if the increment does not exceed the high value.
     *         Otherwise( in.low + OVERLAP_BYTES >= in.high ), null.
     */
    private Interval addOverlap (Interval in) {
    	if ( in.low + OVERLAP_BYTES < in.high )
	        return new Interval (in.low + OVERLAP_BYTES, in.high);
    	else return null;
    }    

    /**
     * Attempts to run downloader.doDownload, notifying manager of termination
     * via downloaders.notify(). 
     * To determine when this downloader should be removed
     * from the dloaders list: never remove the downloader
     * from dloaders if the uploader supports persistence, unless we get an
     * exception - in which case we do not add it back to files.  If !http11,
     * then we remove from the dloaders in the finally block and add to files as
     * before if no problem was encountered.   
     * 
     * @param downloader the normal or push downloader to use for the transfer,
     * which MUST be initialized (i.e., downloader.connectTCP() and
     * connectHTTP() have been called)
     *
     * @return true if there was no IOException while downloading, false
     * otherwise.  
     */
    private boolean doDownload(HTTPDownloader downloader, boolean http11) {
        if(LOG.isTraceEnabled())
            LOG.trace("WORKER: about to start downloading "+downloader);
        boolean problem = false;
        RemoteFileDesc rfd = downloader.getRemoteFileDesc();            
        try {
            downloader.doDownload(commonOutFile);
            rfd.resetFailedCount();
            if(http11)
                DownloadStat.SUCCESFULL_HTTP11.incrementStat();
            else
                DownloadStat.SUCCESFULL_HTTP10.incrementStat();
        } catch (IOException e) {
            if(http11)
                DownloadStat.FAILED_HTTP11.incrementStat();
            else
                DownloadStat.FAILED_HTTP10.incrementStat();
            problem = true;
			chatList.removeHost(downloader);
            browseList.removeHost(downloader);
        } finally {
            int stop=downloader.getInitialReadingPoint()
                        +downloader.getAmountRead();
            if(LOG.isDebugEnabled())
                LOG.debug("    WORKER:+"+Thread.currentThread().hashCode()+
                        " terminating from "+downloader+" at "+stop+ 
                  " error? "+problem);
            synchronized (this) {
                if (problem) {
                    downloader.stop();
                    rfd.incrementFailedCount();
                    // if we failed less than twice in succession,
                    // try to use the file again much later.
                    if( rfd.getFailedCount() < 2 ) {
                        rfd.setRetryAfter(FAILED_RETRY_AFTER);
                        files.add(rfd);
                    } else
                        informMesh(rfd, false);
                } else {
                    informMesh(rfd, true);
                    if( !http11 ) // no need to add http11 dloaders to files
                        files.add(rfd);
                }
            }
        }
        
        return !problem;
    }

    /**
     * Release the ranges assigned to a downloader  
     */
    private synchronized void releaseRanges(HTTPDownloader dloader) {
        int low=dloader.getInitialReadingPoint();
        int high = dloader.getInitialReadingPoint()+dloader.getAmountToRead()-1;
        //note: the high'th byte will also be downloaded.
        if( (high-low)>0) {//dloader failed to download a part assigned to it?
            commonOutFile.releaseBlock(new Interval(low,high));
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
        int downloads=threads.size();
        return getSwarmCapacity() - downloads;
    }

    private int getSwarmCapacity() {
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
        Iterator iter=files.iterator();
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
            
        boolean removed = files.remove(ret);
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
            if(corruptState == NOT_CORRUPT_STATE && hashTree == null) {
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
        if(hashTree != null) {
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
    private void setState(int newState) {
        synchronized (this) {
            this.state=newState;
            this.stateTime=Long.MAX_VALUE;
        }
    }

    /** 
     * Sets this' state.
     * @param newState the state we're entering, which MUST be one of the 
     *  constants defined in Downloader
     * @param time the time we expect to state in this state, in 
     *  milliseconds. 
     */
    private void setState(int newState, long time) {
        synchronized (this) {
            this.state=newState;
            this.stateTime=System.currentTimeMillis()+time;
        }
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
        if (dloaders.size()==0) {
			if (allFiles.length > 0)
                return allFiles[0].getSize();
			else 
				return -1;
        } else 
            //Could also use currentFileSize, but this works.
            return ((HTTPDownloader)dloaders.get(0))
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

    public synchronized String getQueuePosition() {
        if(getState() != REMOTE_QUEUED)
            return "";
        return queuePosition;
    }
    
    public synchronized int getNumDownloaders() {
        return dloaders.size() + queuedThreads.size();
    }

    private final Iterator getHosts(boolean chattableOnly) {
        List /* of Endpoint */ buf=new LinkedList();
        for (Iterator iter=dloaders.iterator(); iter.hasNext(); ) {
            HTTPDownloader dloader=(HTTPDownloader)iter.next();            
            if (chattableOnly ? dloader.chatEnabled() : true) {                
                buf.add(new Endpoint(dloader.getInetAddress().getHostAddress(),
                                     dloader.getPort()));
            }
        }
        return buf.iterator();
    }
    
    public synchronized String getVendor() {
        if ( dloaders.size() > 0 ) {
            HTTPDownloader dl = (HTTPDownloader)dloaders.get(0);
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
        Iterator iter = dloaders.iterator();
        while(iter.hasNext()) {
            c = true;
            BandwidthTracker dloader = (BandwidthTracker)iter.next();
            dloader.measureBandwidth();
			currentTotal += dloader.getAverageBandwidth();
		}
		if ( c )
		    averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
		                    / ++numMeasures;
    }
    
    public synchronized float getMeasuredBandwidth() {
        float retVal = 0f;
        Iterator iter = dloaders.iterator();
        while(iter.hasNext()) {
            BandwidthTracker dloader = (BandwidthTracker)iter.next();
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
