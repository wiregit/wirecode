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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.AssertFailure;
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
import com.limegroup.gnutella.altlocs.AltLocListener;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
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
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to DownloadWorker threads.  Does retries and resumes automatically.
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
public class ManagedDownloader implements Downloader, MeshHandler, AltLocListener, Serializable {
    /*
      IMPLEMENTATION NOTES: The basic idea behind swarmed (multisource)
      downloads is to download one file in parallel from multiple servers.  For
      example, one might simultaneously download the first half of a file from
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
      
      ManagedDownloader delegates to multiple DownloadWorker instances, one for
      each HTTP connection.  They use a shared VerifyingFile object that keeps
      track of which blocks have been written to disk.  
      
      ManagedDownloader uses one thread to control the smart downloads plus one
      thread per DownloadWorker instance.  The call flow of ManagedDownloader's
      "master" thread is as follows:

       performDownload:
           initializeDownload    
           fireDownloadWorkers (asynchronously start workers)    
           verifyAndSave

      The core downloading loop is done by fireDownloadWorkers.Currently the 
      desired parallelism is fixed at 2 for modem users, 6 for cable/T1/DSL, 
      and 8 for T3 and above.
      
      DownloadManager notifies a ManagedDownloader when it should start
      performDownload.  An inactive download (waiting for a busy host,
      waiting for a user to requery, waiting for GUESS responses, etc..)
      is essentially a state-machine, pumped forward by DownloadManager.
      The 'master thread' of a ManagedDownloader is recreated every time
      DownloadManager moves the download from inactive to active.
      
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
          j) DISK_PROBLEM (limewire couldn't the file)
          k) CORRUPT_FILE (the file was corrupt)

     There are a few intermediary states:
          l) HASHING
          m) SAVING
     HASHING & SAVING are seen by the GUI, and are used just prior to COMPLETE,
     to let the user know what is currently happening in the closing states of
     the download.  RECOVERY_FAILED is used as an indicator that we no longer want
     to retry the download, because we've tried and recovered from corruption
     too many times.
     
     How corruption is handled:
     There are two general cases where corruption can be discovered - during a download
     or after the download has finished.
     
     During the download, each worker thread checks periodically whether the amount of 
     data lost to corruption exceeds 10% of the completed file size.  Whenever that 
     happens, the worker thread asks the user whether the download should be terminated.
     If the user chooses to delete the file, the downloader is stopped asynchronously and
     _corruptState is set to CORRUPT_STOP_STATE.  The master download thread is interrupted,
     it checks _corruptState and either discards or removes the file.
     
     After the download, if the sha1 does not match the expected, the master download thread
     propmts the user whether they want to keep the file or discard it.  If we did not have a
     tree during the download we remove the file from partial sharing, otherwise we keep it
     until the user asnswers the prompt (which may take a very long time for overnight downloads).
     The tree itself is purged.
     
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
    /** The complete Set of files passed to the constructor.  Must be
     *  maintained in memory to support resume.  allFiles may only contain
     *  elements of type RemoteFileDesc and URLRemoteFileDesc */
    private Set cachedRFDs;
	
	/**
	 * The ranker used to select the next host we should connect to
	 */
	private SourceRanker ranker;

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

    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private volatile Thread dloaderManagerThread;
    /** True iff this has been forcibly stopped. */
    private volatile boolean stopped;
    /** True iff this has been paused.  */
    private volatile boolean paused;

    
    /** 
     * The connections we're using for the current attempts.
     * LOCKING: copy on write on this 
     * 
     */    
    private volatile List /* of DownloadWorker */ _activeWorkers;
    
    /**
     * A List of worker threads in progress.  Used to make sure that we do
     * not terminate in fireDownloadWorkers without hope if threads are
     * connecting to hosts but not have not yet been added to _activeWorkers.
     * 
     * Also, if the download completes and any of the threads are sleeping 
     * because it has been queued by the uploader, those threads need to be 
     * killed.
     * LOCKING: synchronize on this
     */
    private List /*of DownloadWorker*/ _workers;

    /**
     * Stores the queued threads and the corresponding queue position
     * LOCKING: copy on write on this
     */
    private volatile Map /*DownloadWorker -> Integer*/ queuedWorkers;

    /**
     * Set of RFDs where we store rfds we are currently connected to or
     * trying to connect to.
     */
    private Set /*of RemoteFileDesc */ currentRFDs;
    
    /**
     * The SHA1 hash of the file that this ManagedDownloader is controlling.
     */
    protected URN downloadSHA1;
	
    /**
     * The collection of alternate locations we successfully downloaded from
     * somthing from.
     */
	private Set validAlts; 
	
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
    protected File incompleteFile;
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
    private volatile int corruptState;
    private Object corruptStateLock;

    /**
     * Locking object to be used for accessing all alternate locations.
     * LOCKING: never try to obtain monitor on this if you hold the monitor on
     * altLock 
     */
    private Object altLock;

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
    
    protected Map propertiesMap;
    
    protected static final String DEFAULT_FILENAME = "defaultFileName";
    protected static final String FILE_SIZE = "fileSize";


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
        this.cachedRFDs = new HashSet();
		if (files != null) {
			cachedRFDs.addAll(Arrays.asList(files));
            if (files.length > 0) 
                initPropertiesMap(files[0]);
            else
                propertiesMap = Collections.EMPTY_MAP;
        }
        this.incompleteFileManager = ifc;
        this.originalQueryGUID = originalQueryGUID;
        this.deserializedFromDisk = false;
    }

    protected synchronized void initPropertiesMap(RemoteFileDesc rfd) {
        propertiesMap = new HashMap();
        propertiesMap.put(DEFAULT_FILENAME,rfd.getFileName());
        propertiesMap.put(FILE_SIZE,new Integer(rfd.getSize()));
        propertiesMap = Collections.unmodifiableMap(propertiesMap);
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
        
        stream.writeObject(cachedRFDs);
        
        //Blocks can be written to incompleteFileManager from other threads
        //while this downloader is being serialized, so lock is needed.
        synchronized (incompleteFileManager) {
            stream.writeObject(incompleteFileManager);
        }
        
        stream.writeObject(propertiesMap);
    }

    /** See note on serialization at top of file.  You must call initialize on
     *  this!  
     * Also see note in writeObjects about why we are not using 
     * BandwidthTrackerImpl after reading from the stream
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        deserializedFromDisk = true;
		
        Object next = stream.readObject();
        
        // old format
        if (next instanceof RemoteFileDesc[]) {
            RemoteFileDesc [] rfds=(RemoteFileDesc[])next;
            if (rfds != null && rfds.length > 0) 
                initPropertiesMap(rfds[0]);
            cachedRFDs = new HashSet(Arrays.asList(rfds));
        } else {
            // new format
            cachedRFDs = (Set) next;
            if (cachedRFDs.size() > 0) {
                RemoteFileDesc any = (RemoteFileDesc)cachedRFDs.iterator().next();
                initPropertiesMap(any); // in case the map isn't serialized
            }
        }
		
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
        
        Object map = stream.readObject();
        if (map instanceof Map) 
            propertiesMap = (Map)map;
        else if (propertiesMap == null)
            propertiesMap = Collections.EMPTY_MAP; 
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
        currentRFDs = new HashSet();
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
        corruptState=NOT_CORRUPT_STATE;
        corruptStateLock=new Object();
        altLock = new Object();
        numMeasures = 0;
        averageBandwidth = 0f;
        queuePosition=Integer.MAX_VALUE;
        queuedVendor = "";
        triedLocatingSources = false;
		ranker = SourceRanker.getAppropriateRanker();
        ranker.setMeshHandler(this);
        // get the SHA1 if we can.
        if(cachedRFDs.size() > 0 && downloadSHA1 == null) {
            for(Iterator iter = cachedRFDs.iterator();
			iter.hasNext() && downloadSHA1 == null;) {
				RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
				downloadSHA1 = rfd.getSHA1Urn();
                RouterService.getAltlocManager().addListener(downloadSHA1,this);
            }
        }
		
		// make sure all rfds have the same sha1
        verifyAllFiles();
		
        validAlts = new HashSet();
        // stores up to 1000 locations for up to an hour each
        invalidAlts = new FixedSizeExpiringSet(1000,60*60*1000L);
        // stores up to 10 locations for up to 10 minutes
        recentInvalidAlts = new FixedSizeExpiringSet(10, 10*60*1000L);
        synchronized (this) {
            if(shouldInitAltLocs(deserializedFromDisk)) {
                initializeAlternateLocations();
            }
        }
        
        try {
            initializeFilesAndFolders();
            initializeIncompleteFile();
            initializeVerifyingFile();
        }catch(IOException bad) {
            setState(DISK_PROBLEM);
            return;
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
    private void verifyAllFiles() {
        if(downloadSHA1 == null)
            return ;
        
		for (Iterator iter = cachedRFDs.iterator(); iter.hasNext();) {
			RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
			if (rfd.getSHA1Urn() != null && !downloadSHA1.equals(rfd.getSHA1Urn()))
				iter.remove();
		}
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
                    int status = performDownload();
                    completeDownload(status);
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
    private void completeDownload(int status) {
        
        boolean complete;
        // If TAD2 gave a completed state, set the state correctly & exit.
        // Otherwise...
        // If we manually stopped then set to ABORTED, else set to the 
        // appropriate state (either a busy host or no hosts to try).
        synchronized(this) {
            switch(status) {
            case COMPLETE:
            case DISK_PROBLEM:
            case CORRUPT_FILE:
                setState(status);
                break;
            case WAITING_FOR_RETRY:
            case GAVE_UP:
                if(stopped)
                    setState(ABORTED);
                else if(paused)
                    setState(PAUSED);
                else
                    setState(status);
                break;
            default:
                Assert.that(false, "Bad status from tad2: "+status);
            }
            
            complete = isCompleted();
            if (downloadSHA1 != null && complete)
                RouterService.getAltlocManager().removeListener(downloadSHA1, this);
            
            ranker.stop();
        }
        
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
            setState(WAITING_FOR_RETRY, ranker.calculateWaitTime());
        
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
        case DISK_PROBLEM:
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
     * reloads any previously busy hosts in the ranker, as well as other
     * hosts that we know about 
     */
    private synchronized void initializeRanker() {
        ranker.setMeshHandler(this);
        ranker.addToPool(cachedRFDs);
    }
    
    /**
     * initializes the verifying file if the incompleteFile is initialized.
     */
    private void initializeVerifyingFile() throws IOException {

        if (incompleteFile == null)
            return;
        
        //get VerifyingFile
        commonOutFile= incompleteFileManager.getEntry(incompleteFile);

        if(commonOutFile==null) {//no entry in incompleteFM
            
            int completedSize = 
                (int)IncompleteFileManager.getCompletedSize(incompleteFile);
            
            commonOutFile = new VerifyingFile(completedSize);
            try {
                //we must add an entry in IncompleteFileManager
                incompleteFileManager.
                           addEntry(incompleteFile,commonOutFile);
            } catch(IOException ioe) {
                ErrorService.error(ioe, "file: " + incompleteFile);
                throw ioe;
            }
        }        
    }
    
    protected void initializeIncompleteFile() throws IOException {
        if (incompleteFile != null || cachedRFDs == null || cachedRFDs.isEmpty())
            return;
        
        if (downloadSHA1 != null)
            incompleteFile = incompleteFileManager.getFileForUrn(downloadSHA1);
        
        if (incompleteFile == null) { 
            incompleteFile = 
                incompleteFileManager.getFile(getFileName(),downloadSHA1,getContentLength());
        }
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
                addLocationsToDownload(RouterService.getAltlocManager().getDirect(hash),
                        RouterService.getAltlocManager().getPush(hash,false),
                        RouterService.getAltlocManager().getPush(hash,true),
                        (int)size);
                
            }
        }
    }
    
    /**
     * Adds the alternate locations from the collections as possible
     * download sources.
     */
    private void addLocationsToDownload(AlternateLocationCollection direct,
            AlternateLocationCollection push,
            AlternateLocationCollection fwt,
                                        int size) {
        List locs = new ArrayList(direct.getAltLocsSize()+push.getAltLocsSize()+fwt.getAltLocsSize());
        // always add the direct alt locs.
        synchronized(direct) {
            for (Iterator iter = direct.iterator(); iter.hasNext();) {
                AlternateLocation loc = (AlternateLocation) iter.next();
                locs.add(loc.createRemoteFileDesc(size));
            }
        }
        
        synchronized(push) {
            for (Iterator iter = push.iterator(); iter.hasNext();) {
                AlternateLocation loc = (AlternateLocation) iter.next();
                locs.add(loc.createRemoteFileDesc(size));
            }
        }
        
        synchronized(fwt) {
            for (Iterator iter = fwt.iterator(); iter.hasNext();) {
                AlternateLocation loc = (AlternateLocation) iter.next();
                locs.add(loc.createRemoteFileDesc(size));
            }
        }
                
        addPossibleSources(locs);
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
            for (Iterator iter = cachedRFDs.iterator();iter.hasNext();) {
                RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
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
		    
        String queryString = StringUtils.createQueryString(getFileName());
        if(queryString == null || queryString.equals(""))
            throw new CantResumeException(getFileName());
        else
            return QueryRequest.createQuery(queryString);
            
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

        if (RouterService.acceptedIncomingConnection() ||
                !other.isFirewalled() ||
                (other.supportsFWTransfer() && RouterService.canDoFWT())) {
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
        return false;
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
            
            // compare to previously cached rfds
            for (Iterator iter = cachedRFDs.iterator();iter.hasNext();) {
                // get current info....
                RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
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
     * notifies this downloader that an alternate location has been added.
     */
    public synchronized void locationAdded(AlternateLocation loc) {
        Assert.that(loc.getSHA1Urn().equals(getSHA1Urn()));
        addDownload(loc.createRemoteFileDesc(getContentLength()),false);
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
    
    public synchronized boolean addDownload(Collection c, boolean cache) {
        if (stopped)
            return false;
        
        List l = new ArrayList(c.size());
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (hostIsAllowed(rfd) && allowAddition(rfd))
                l.add(rfd);
        }
        
        return addDownloadForced(l,cache);
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

        // DO NOT DOWNLOAD FROM YOURSELF.
        if( rfd.isMe() )
            return true;
        
        // already downloading from the host
        if (currentRFDs.contains(rfd))
            return true;
        
        prepareRFD(rfd,cache);
        
        //...and notify manager to look for new workers.  The two cases we're
        //interested: waiting for downloaders to complete (by waiting on
        //this) or waiting for retry (handled by DownloadManager).
        
        if (ranker.addToPool(rfd)){
            if(LOG.isTraceEnabled())
                LOG.trace("added rfd: " + rfd);
            if(isInactive() || dloaderManagerThread == null)
                receivedNewSources = true;
        }
        
        return true;
    }
    
    protected synchronized final boolean addDownloadForced(Collection c, boolean cache) {
        // remove any rfds we're currently downloading from 
        c.removeAll(currentRFDs);
        
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isMe()) {
                iter.remove();
                continue;
            }
            prepareRFD(rfd,cache);
            if(LOG.isTraceEnabled())
                LOG.trace("added rfd: " + rfd);
        }
        
        if ( ranker.addToPool(c) ) {
            if(isInactive() || dloaderManagerThread == null)
                receivedNewSources = true;
        }
        
        return true;
    }
    
    private void prepareRFD(RemoteFileDesc rfd, boolean cache) {
        rfd.setDownloading(true);
        if(downloadSHA1 == null) {
            downloadSHA1 = rfd.getSHA1Urn();
            RouterService.getAltlocManager().addListener(downloadSHA1,this);
        }

        //add to allFiles for resume purposes if caching...
        if(cache) 
            cachedRFDs.add(rfd);        
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
            killAllWorkers();
            
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
     * Kills all workers.
     */    
    private synchronized void killAllWorkers() {
        for (Iterator iter = _workers.iterator(); iter.hasNext();) {
            DownloadWorker doomed = (DownloadWorker) iter.next();
            doomed.interrupt();
        }
    }
    
    /**
     * Callback from workers to inform the managing thread that
     * a disk problem has occured.
     */
    synchronized void diskProblemOccured() {
        setState(DISK_PROBLEM);
        stop();
    }

    /**
     * Notifies all existing HTTPDownloaders about this RFD.
     * If good is true, it notifies them of a succesful alternate location,
     * otherwise it notifies them of a failed alternate location.
     * The internal validAlts is also updated if good is true,
     * and invalidAlts is updated if good is false.
     * The IncompleteFileDesc is also notified of new locations for this
     * file.
     * If we successfully downloaded from this host, cache it for future resume.
     */
    public synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
        if (LOG.isDebugEnabled())
            LOG.debug("informing mesh that "+rfd+" is "+good);
        
        if (good)
            cachedRFDs.add(rfd);
        
        if(!rfd.isAltLocCapable())
            return;
        
        // Verify that this download has a hash.  If it does not,
        // we should not have been getting locations in the first place.
        Assert.that(downloadSHA1 != null, "null hash.");
        
        Assert.that(downloadSHA1.equals(rfd.getSHA1Urn()), "wrong loc SHA1");
        
        AlternateLocation loc;
        try {
            loc = AlternateLocation.create(rfd);
        } catch(IOException iox) {
            return;
        }

        for(Iterator iter=getActiveWorkers().iterator(); iter.hasNext();) {
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
        
        // add to the local collections
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
                }
            }  else {
                    if(rfd.isFromAlternateLocation() )
                        if(loc instanceof PushAltLoc)
                                DownloadStat.PUSH_ALTERNATE_NOT_ADDED.incrementStat();
                        else
                                DownloadStat.ALTERNATE_NOT_ADDED.incrementStat();
                    
                    validAlts.remove(loc);
                    invalidAlts.add(rfd.getRemoteHostData());
                    recentInvalidAlts.add(loc);
            }
        }
        
        // if this is a pushloc, update the proxies accordingly
        if (loc instanceof PushAltLoc) {

            // Note: we update the proxies of a clone in order not to lose the
            // original proxies
            loc = loc.createClone();
            PushAltLoc ploc = (PushAltLoc)loc; 
            ploc.updateProxies(good);
        }
        
        // and to the global collection
        if (good)
            RouterService.getAltlocManager().add(loc, this);
        else
            RouterService.getAltlocManager().remove(loc, this);
    }

    public synchronized void addPossibleSources(Collection c) {
        addDownload(c,false);
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
        
        // if any guys were busy, reduce their retry time to 0,
        // since the user really wants to resume right now.
        for(Iterator i = cachedRFDs.iterator(); i.hasNext(); )
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
    
    public URN getSHA1Urn() {
        return downloadSHA1;
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
        if(cachedRFDs != null) {
            for (Iterator iter = cachedRFDs.iterator(); iter.hasNext();) {
				RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
				rfd.setDownloading(false);
			}
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
    protected int performDownload() {
        if(checkHosts()) {//files is global
            setState(GAVE_UP);
            return GAVE_UP;
        }

        // 1. initialize the download
        int status = initializeDownload();
        if ( status == CONNECTING) {
            try {
                //2. Do the download
                try {
                    status = fireDownloadWorkers();//Exception may be thrown here.
                }finally {
                    //3. Close the file controlled by commonOutFile.
                    commonOutFile.close();
                }
                
                // 4. if all went well, save
                if (status == COMPLETE) 
                    status = verifyAndSave();
                else if(LOG.isDebugEnabled())
                    LOG.debug("stopping early with status: " + status); 
                
            } catch (InterruptedException e) {
                
                // nothing should interrupt except for a stop
                if (!stopped && !paused)
                    ErrorService.error(e);
                else
                    status = GAVE_UP;
                
                // if we were stopped due to corrupt download, cleanup
                if (corruptState == CORRUPT_STOP_STATE) {
                    cleanupCorrupt(incompleteFile, completeFile.getName());
                    status = CORRUPT_FILE;
                }
            }
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("MANAGER: TAD2 returned: " + status);
                   
        return status;
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
     * Tries to initialize the download location and the verifying file. 
     * @return GAVE_UP if we had no sources, DISK_PROBLEM if such occured, 
     * CONNECTING if we're ready to connect
     */
    protected int initializeDownload() {
        
        synchronized (this) {
            if (cachedRFDs.size()==0 && !ranker.hasMore()) 
                return GAVE_UP;
        }
        
        try {
            initializeIncompleteFile();
            initializeVerifyingFile();
            openVerifyingFile();
        } catch (IOException iox) {
            return DISK_PROBLEM;
        }

        // Create a new validAlts for this sha1.
        // initialize the HashTree
        if( downloadSHA1 != null ) 
            initializeHashTree();
        
        // load up the ranker with the hosts we know about
        initializeRanker();
        
        return CONNECTING;
    }
    
    /**
     * Verifies the completed file against the SHA1 hash and saves it.  If
     * there is corruption, it asks the user whether to discard or keep the file 
     * @return COMPLETE if all went fine, DISK_PROBLEM if not.
     * @throws InterruptedException if we get interrupted while waiting for user
     * response.
     */
    private int verifyAndSave() throws InterruptedException{
        
        // Find out the hash of the file and verify that its the same
        // as our hash.
        URN fileHash = scanForCorruption();
        if (corruptState == CORRUPT_STOP_STATE) {
            cleanupCorrupt(incompleteFile, completeFile.getName());
            return CORRUPT_FILE;
        }
        
        // Save the file to disk.
        return saveFile(fileHash);
    }
    
    /**
     * Waits indefinitely for a response to the corrupt message prompt, if
     * such was displayed.
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
        
        if(LOG.isWarnEnabled()) {
            LOG.warn("hash verification problem, fileHash="+
                           fileHash+", ourHash="+downloadSHA1);
        }

        // unshare the file if we didn't have a tree
        // otherwise we will have shared only the parts that verified
        if (commonOutFile.getHashTree() == null) 
            fileManager.removeFileIfShared(incompleteFile);
        
        // purge the tree
        TigerTreeCache.instance().purgeTree(downloadSHA1);
        commonOutFile.setHashTree(null);

        // ask what to do next 
        promptAboutCorruptDownload();
        waitForCorruptResponse();
        
        return fileHash;        
    }

    /**
     * initialize the directory where the file is to be saved.
     */
    private void initializeFilesAndFolders() throws IOException{
        
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
            saveDir = SharingSettings.getSaveDirectory();
            completeFile = new File(saveDir, fileName);
            String savePath = FileUtils.getCanonicalPath(saveDir);
            String completeFileParentPath = 
                FileUtils.getCanonicalPath(completeFile.getAbsoluteFile().getParentFile());
            if (!savePath.equals(completeFileParentPath))
                throw new IOException();
        } catch (IOException e) {
            ErrorService.error(e);
            throw e;
        }
    }
    
    /**
     * checks the TT cache and if a good tree is present loads it 
     */
    private void initializeHashTree() {
		HashTree tree = TigerTreeCache.instance().getHashTree(downloadSHA1); 
	    
		// if we have a valid tree, update our chunk size and disable overlap checking
		if (tree != null && tree.isDepthGoodEnough()) {
				commonOutFile.setHashTree(tree);
		}
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
            return DISK_PROBLEM;
            
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
            
            // save the trees!
            if (downloadSHA1 != null && downloadSHA1.equals(fileHash) && commonOutFile.getHashTree() != null) {
                TigerTreeCache.instance(); 
                TigerTreeCache.addHashTree(downloadSHA1,commonOutFile.getHashTree());
            }
        }

        FileDesc fileDesc = 
		    fileManager.addFileIfShared(completeFile, getXMLDocuments());  

		return COMPLETE;
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
    private void openVerifyingFile() throws IOException {

        //need to get the VerifyingFile ready to write
        try {
            commonOutFile.open(incompleteFile);
        } catch(IOException e) {
            if(!IOUtils.handleException(e, "DOWNLOAD"))
                ErrorService.error(e);
            throw e;
        }
    }
    
    /**
     * Starts a new Worker thread for the given RFD.
     */
    private void startWorker(final RemoteFileDesc rfd) {
        DownloadWorker worker = new DownloadWorker(this,rfd,commonOutFile,stealLock);
        Thread connectCreator = new ManagedThread(worker);
        
        connectCreator.setName("DownloadWorker");
        
        synchronized(this) {
            _workers.add(worker);
            currentRFDs.add(rfd);
        }

        connectCreator.start();
    }        
    
    /**
     * Callback that the specified worker has finished.
     */
    synchronized void workerFinished(DownloadWorker finished) {
        if (LOG.isDebugEnabled())
            LOG.debug("worker "+finished+" finished.");
        removeWorker(finished); 
        notify();
    }
    
    synchronized void workerStarted(DownloadWorker worker) {
        if (LOG.isDebugEnabled())
            LOG.debug("worker "+worker + " started.");
        setState(ManagedDownloader.DOWNLOADING);
        addActiveWorker(worker);
        chatList.addHost(worker.getDownloader());
        browseList.addHost(worker.getDownloader());
    }
    
    void workerFailed(DownloadWorker failed) {
        chatList.removeHost(failed.getDownloader());
        browseList.removeHost(failed.getDownloader());
    }
    
    synchronized void removeWorker(DownloadWorker worker) {
        removeActiveWorker(worker);
        _workers.remove(worker);
    }
    
    synchronized void removeActiveWorker(DownloadWorker worker) {
        currentRFDs.remove(worker.getRFD());
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

    synchronized String getWorkersInfo() {
        String workerState = "";
        for (Iterator iter = _workers.iterator(); iter.hasNext();) {
            DownloadWorker worker = (DownloadWorker) iter.next();
            workerState+=worker.getInfo();
        }
        return workerState;
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
     * @return The alternate locations we have failed to downloaded from
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
    private int fireDownloadWorkers() throws InterruptedException {
        LOG.trace("MANAGER: entered fireDownloadWorkers");

        int size = -1;
        //Assert.that(threads.size()==0,
        //            "wrong threads size: " + threads.size());

        //While there is still an unfinished region of the file...
        while (true) {
            if (stopped || paused) {
                LOG.warn("MANAGER: terminating because of stop|pause");
                throw new InterruptedException();
            } 
            
            // are we just about to finish downloading the file?
            
            LOG.debug("About to wait for pending if needed");
            
            try {            
                commonOutFile.waitForPendingIfNeeded();
            } catch(DiskException dio) {
                if (stopped || paused) {
                    LOG.warn("MANAGER: terminating because of stop|pause");
                    throw new InterruptedException();
                }
                stop();
                return DISK_PROBLEM;
            }
            
            LOG.debug("Finished waiting for pending");
            
            
            // Finished.
            if (commonOutFile.isComplete()) {
                killAllWorkers();
                
                LOG.trace("MANAGER: terminating because of completion");
                return COMPLETE;
            }
            
            synchronized(this) { 
                // if everybody we know about is busy (or we don't know about anybody)
                // and we're not downloading from anybody - terminate the download.
                if (_workers.size() == 0 && !ranker.hasNonBusy())   {                        
                    if ( ranker.calculateWaitTime() > 0) {
                        LOG.trace("MANAGER: terminating with busy");
                        return WAITING_FOR_RETRY;
                    } else {
                        LOG.trace("MANAGER: terminating w/o hope");
                        return GAVE_UP;
                    }
                }
                
                if(LOG.isDebugEnabled())
                    LOG.debug("MANAGER: kicking off workers, dloadsCount: " + 
                            _activeWorkers.size() + ", threads: " + _workers.size());
                
                //OK. We are going to create a thread for each RFD. The policy for
                //the worker threads is to have one more thread than the max swarm
                //limit, which if successfully starts downloading or gets a better
                //queued slot than some other worker kills the lowest worker in some
                //remote queue.
                if (shouldStartWorker()){
                    // see if we need to update our ranker
                    ranker = SourceRanker.getAppropriateRanker(ranker);
                    
                    if (ranker.hasMore()) {
                        
                        // get the best host
                        RemoteFileDesc rfd = ranker.getBest();
                        
                        // If the rfd was busy, that means all possible RFDs
                        // are busy - store for later
                        if( rfd.isBusy() ) 
                            addRFD(rfd);
                         else 
                            startWorker(rfd);
                    }
                    
                } else if (LOG.isDebugEnabled())
                    LOG.debug("no blocks but can't steal - sleeping");
                
                //wait for a notification before we continue.
                try {
                    //if no workers notify in a while, iterate. This is a problem
                    //for stalled downloaders which will never notify. So if we
                    //wait without a timeout, we could wait forever.
                    this.wait(DownloadSettings.WORKER_INTERVAL.getValue()); // note that this relinquishes the lock
                } catch (InterruptedException ignored) {}
            }
        }//end of while
    }
    
    /**
     * @return if we should start another worker - means we have more to download,
     * have not reached our swarm capacity and the ranker has something to offer
     * or we have some rfds to re-try
     */
    private boolean shouldStartWorker() {
        return (commonOutFile.hasFreeBlocksToAssign() > 0 || stealingCanHappen() ) &&
             ((_workers.size() - queuedWorkers.size()) < getSwarmCapacity()) &&
             ranker.hasMore();
    }
    
    /**
     * @return true if we have more than one worker or the last one is slow
     */
    private boolean stealingCanHappen() {
        if (_workers.size() < 1)
            return false;
        else if (_workers.size() > 1)
            return true;
            
        DownloadWorker lastOne = (DownloadWorker)_workers.get(0);
        // with larger chunk sizes we may end up with slower last downloader
        return lastOne.isSlow(); 
    }
	
	synchronized void addRFD(RemoteFileDesc rfd) {
        ranker.addToPool(rfd);
	}
    
	/**
	 * Returns the number of alternate locations that this download is using.
	 */
	public int getNumberOfAlternateLocations() {
	    if ( validAlts == null ) return 0;
        synchronized(altLock) {
            return validAlts.size();
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
        return (ranker == null ? 0 : ranker.getNumKnownHosts());
    }
    
    public synchronized int getBusyHostCount() {
        return ranker.getNumBusyHosts();
    }

    public synchronized int getQueuedHostCount() {
        return queuedWorkers.size();
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
     * Asks the user if we should continue or discard this download.
     */
    void promptAboutCorruptDownload() {
        synchronized(corruptStateLock) {
            if(corruptState == NOT_CORRUPT_STATE) {
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
     */
    public void discardCorruptDownload(final boolean delete) {
        
        // offload this from the swing thread since it will require
        // access to the verifying file.
        Runnable r = new Runnable() {
            public void run() {
                synchronized(corruptStateLock) {
                    if(delete) {
                        corruptState = CORRUPT_STOP_STATE;
                    } else {
                        corruptState = CORRUPT_CONTINUE_STATE;
                    }
                }

                if (delete)
                    stop();
                else 
                    commonOutFile.setDiscardUnverified(false);
                
                synchronized(corruptStateLock) {
                    corruptStateLock.notify();
                }
            }
        };
        
        RouterService.schedule(r,0,0);

    }
            

    /**
     * Returns the union of all XML metadata documents from all hosts.
     */
    private synchronized List getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?
        List allDocs = new ArrayList();

        // get all docs possible
        for (Iterator iter = cachedRFDs.iterator();iter.hasNext();) {
			RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
			LimeXMLDocument doc = rfd.getXMLDoc();
			if(doc != null) {
				allDocs.add(doc);
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
            return  (int)Math.ceil(Math.max(remaining, 0)/1000f);
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

        String fileName = (String)propertiesMap.get(DEFAULT_FILENAME); 
        if ( fileName == null) {
            Assert.that(false,"defaultFileName is null, "+
                        "subclass may have not overridden getFileName");
        }
        return CommonUtils.convertFileName(fileName);
    }


	/**
     *  Certain subclasses would like to know whether we have at least one good
	 *  RFD.
     */
	protected synchronized boolean hasRFD() {
        return ( cachedRFDs != null && !cachedRFDs.isEmpty());
	}
	

    public synchronized int getContentLength() {
        Integer i = (Integer)propertiesMap.get(FILE_SIZE);
        return i != null ? i.intValue() : 0;
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
    public int getAmountRead() {
        VerifyingFile ourFile;
        synchronized(this) {
            if ( state == CORRUPT_FILE )
                return corruptFileBytes;
            else if ( state == HASHING ) {
                if ( incompleteFile == null )
                    return 0;
                else
                    return URN.getHashingProgress(incompleteFile);
            } else {
                ourFile = commonOutFile;
            }
        }
        
        return ourFile == null ? 0 : ourFile.getBlockSize();                
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
    
    synchronized List getAllWorkers() {
        //CoR because it will be used only while stealing
        return new ArrayList(_workers);
    }
    
    void removeQueuedWorker(DownloadWorker unQueued) {
        if (getQueuedWorkers().containsKey(unQueued)) {
            synchronized(this) {
                Map m = new HashMap(getQueuedWorkers());
                m.remove(unQueued);
                queuedWorkers = Collections.unmodifiableMap(m);
            }
        }
    }
    
    private synchronized void addQueuedWorker(DownloadWorker queued, int position) {
        if (LOG.isDebugEnabled())
            LOG.debug("adding queued worker " + queued +" at position "+position+
                    " current queued workers:\n"+queuedWorkers);
        
        if ( position < queuePosition ) {
            queuePosition = position;
            queuedVendor = queued.getDownloader().getVendor();
        }
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
    
    /**
     * Interrupts a remotely queued thread if we this status is connected,
     * or if the status is queued and our queue position is better than
     * an existing queued status.
     *
     * @param status The ConnectionStatus of this downloader.
     *
     * @return true if this thread should be kept around, false otherwise --
     * explicitly, there is no need to kill any threads, or if the currentThread
     * is already in the queuedWorkers, or if we did kill a thread worse than
     * this thread.  
     */
    synchronized boolean killQueuedIfNecessary(DownloadWorker worker, int queuePos) {
        if (LOG.isDebugEnabled())
            LOG.debug("deciding whether to queue worker "+worker+ " at position "+queuePos);
        
        //Either I am queued or downloading, find the highest queued thread
        DownloadWorker doomed = null;
        
        // No replacement required?...
        if(getNumDownloaders() <= getSwarmCapacity() && queuePos == -1) {
            return true;
        } 

        // Already Queued?...
        if(queuedWorkers.containsKey(worker) && queuePos > -1) {
            // update position
            addQueuedWorker(worker,queuePos);
            return true;
        }

        if (getNumDownloaders() >= getSwarmCapacity()) {
            // Search for the queued thread with a slot worse than ours.
            int highest = queuePos; // -1 if we aren't queued.            
            for(Iterator i = queuedWorkers.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry current = (Map.Entry)i.next();
                int currQueue = ((Integer)current.getValue()).intValue();
                if(currQueue > highest) {
                    doomed = (DownloadWorker)current.getKey();
                    highest = currQueue;
                }
            }
            
            // No one worse than us?... kill us.
            if(doomed == null) {
                LOG.debug("not queueing myself");
                return false;
            } else if (LOG.isDebugEnabled())
                LOG.debug(" will replace "+doomed);
            
            //OK. let's kill this guy 
            doomed.interrupt();
        }
        
        //OK. I should add myself to queuedWorkers if I am queued
        if(queuePos > -1)
            addQueuedWorker(worker, queuePos);
        
        return true;
                
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
        List active = getActiveWorkers();
        if ( active.size() > 0 ) {
            HTTPDownloader dl = ((DownloadWorker)active.get(0)).getDownloader();
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
        Iterator iter = getActiveWorkers().iterator();
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

	public int getAmountVerified() {
        VerifyingFile ourFile;
        synchronized(this) {
            ourFile = commonOutFile;
        }
		return ourFile == null? 0 : ourFile.getVerifiedBlockSize();
	}
	
	public int getAmountLost() {
        VerifyingFile ourFile;
        synchronized(this) {
            ourFile = commonOutFile;
        }
		return ourFile == null ? 0 : ourFile.getAmountLost();
	}
    
    public int getChunkSize() {
        VerifyingFile ourFile;
        synchronized(this) {
            ourFile = commonOutFile;
        }
        return ourFile != null ? ourFile.getChunkSize() : VerifyingFile.DEFAULT_CHUNK_SIZE;
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

interface MeshHandler {
    void informMesh(RemoteFileDesc rfd, boolean good);
    void addPossibleSources(Collection hosts);
}