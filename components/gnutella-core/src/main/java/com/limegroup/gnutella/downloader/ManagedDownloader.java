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

import java.io.*;
import java.net.*;

import java.util.StringTokenizer;

import com.sun.java.util.collections.*;

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
 * Subclasses may refine the requery behavior by overriding the nextRequeryTime,
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
      via the HUGE proposal.  Until that is more widely adopted, LimeWire
      considers two files with the same name and file size to be duplicates.

      When discussing swarmed downloads, it's useful to divide parts of a file
      into three categories: black, grey, and white. Black regions have already
      been downloaded to disk.  Grey regions have been assigned to a downloader
      but not yet completed.  White regions have not been assigned to a
      downloader.
      
      LimeWire uses the following algorithm for swarming.  First all download
      locations are divided into buckets of "same" files. Then the following is
      done for each bucket:

      while the file has not been downloaded, and we have not given up
           try to increasing parallelism, to our maximum capacity
               
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
                             /      |
                         bucket   tryAllDownloads2
                                    |
                                  tryAllDownloads3
                                    | 
                               (asynchronously)
                                    |
                              connectAndDownload
                          /           |             \
        establishConnection     assignAndRequest       doDownload
             |                        |                        \
       HTTPDownloader.connectTCP  assignWhite/assignGrey        \
                                      |           HTTPDownlaoder.doDownload
                               HTTPDownloader.connectHTTP

      tryAllDownloads does the bucketing of files, as well as waiting for
      retries.  The core downloading loop is done by tryAllDownloads3.
      connectAndDownload (which is started asynchronously in tryAllDownloads3),
      does the three step ennumerated above.

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
     * files, dloaders, needed, busy and setState.  We should  not hold lock 
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
     *
     * Also, always hold stealLock's monitor before REMOVING elements from 
     * needed. As always, we must obtain this' monitor before modifying needed.
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
    private static final int PUSH_CONNECT_TIME=16000;  //16 seconds
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
    private static final int OVERLAP_BYTES=10;

    /** The time to wait between requeries, in milliseconds.  This time can
     *  safely be quite small because it is overridden by the global limit in
     *  DownloadManager.  Package-access and non-final for testing.
     *  @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_REQUERIES */
    static int TIME_BETWEEN_REQUERIES = 5*60*1000;  //5 minutes
    /** The number of times to requery the network. All requeries are
     *  user-driven.
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
    /** The buckets of "same" files, each a list of RemoteFileDesc. One by one,
     *  we will try to swarm each bucket.  Buckets are passed to the
     *  tryAllDownloadsX, removeBest, and tryOneDownload methods, which can
     *  mutate them.  Also, existing buckets can be modified and new buckets
     *  added by the addDownload method.  Therefore great care must be taken to
     *  synchronize on this when modifying buckets, as well as avoiding
     *  ConcurrentModificationException. 
     * 
     *  INVARIANT: buckets contains a subset of allFiles.  (Remember that we
     *  remove locations from buckets if they don't pan out.  We don't remove
     *  them from allFiles to support resumes.)
     */
    private RemoteFileDescGrouper buckets;
    
    /**
     * The current RFDs that this ManagedDownloader is connecting to.
     * This is necessary to store so that when an RFD is removed from files,
     * we can check in this datastructure to ensure that an RFD is not
     * connected to twice.
     *
     * Initialized in tryAllDownloads3.
     */
    private List currentRFDs;
    
    /**
     * The index of the bucket we are trying to download from. We use it
     * to find the hash of the bucket we are downloading from - so that 
     * we can verify that we downloaded the correct file, once the download is
     * complete
     */
    private int bucketNumber;

    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private Thread dloaderManagerThread;
    /** True iff this has been forcibly stopped. */
    private boolean stopped;

    
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

    /**
     * The IntervalSet of intervals within the file which have not been
     * allocated to any downloader yet.  This set of intervals represents
     * the "white" region of the file we are downloading.
     *
     * An IntervalSet is used to ensure that chunks readded to it are
     * coalesced together.
     * LOCKING: synchronize on this
     */
    private IntervalSet needed;
    /** List of RemoteFileDesc to which we actively connect and request parts
     * of the file.*/
    private List /*of RemoteFileDesc */ files;
	
    /**
     * The collection of alternate locations we successfully downloaded from
     * somthing from. We will never use this data-structure until the very end,
     * when we have become active uploaders of the file.
     */
	private AlternateLocationCollection validAlts; 
	
	/**
	 * A list of the most recent failed locations, so we don't try them again.
	 *
	 * TODO: Use a Set and FixedsizeForgetfulHashSet
	 */
	private Map invalidAlts;

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
    /** If in the wait state, the number of retries we're waiting for.
     *  Otherwise undefined. */
    private int retriesWaiting;
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
    /** Lock used to communicate between addDownload and tryAllDownloads, and
     *  pauseForRequery and resume.
     *  The RequeryLock is only meant for one producer and consumer.  The code
     *  may it look like there are multiple producers and consumers, but if you
     *  follow the code flow you'll see that there is only one of each.
     */
    private RequeryLock reqLock = new RequeryLock();

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
     * Whether or not to record stats.
     */
    static final boolean RECORD_STATS = !CommonUtils.isJava118();

    /**
     * The GUID of the original query.  may be null.
     */
    private final GUID originalQueryGUID;


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
        allFiles=(RemoteFileDesc[])stream.readObject();
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
		//Old versions used to read BandwidthTrackerImpl here.  Now we just use
		//one as a place holder.
        stream.readObject();

        //The following is needed to prevent NullPointerException when reading
        //serialized object from disk. This can't be done in the constructor or
        //initializer statements, as they're not executed.  Nor should it be
        //done in initialize, as that could cause problems in resume().
        reqLock=new RequeryLock();
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
                           ActivityCallback callback, 
                           final boolean deserialized) {
        this.manager=manager;
		this.fileManager=fileManager;
        this.callback=callback;
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
        numMeasures = 0;
        averageBandwidth = 0f;
        invalidAlts = new FixedsizeForgetfulHashMap(500);
        synchronized (this) {
            buckets=new RemoteFileDescGrouper(allFiles,incompleteFileManager);
            if(shouldInitAltLocs(deserialized)) {
                initializeAlternateLocations();
            }
        }
        this.dloaderManagerThread=new Thread("ManagedDownload") {
            public void run() {
                try { 
                    tryAllDownloads(deserialized);
                } catch (Throwable e) {
                    //This is a "firewall" for reporting unhandled errors.  We
                    //don't really try to recover at this point, but we do
                    //attempt to display the error in the GUI for debugging
                    //purposes.
                    ErrorService.error(e);
                }
            }
        };
        dloaderManagerThread.setDaemon(true);
        dloaderManagerThread.start();       
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
            }
        }
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this. In
     * other words, if this.conflicts(other)==true, no other ManagedDownloader
     * should attempt to download other.  
     */
    public boolean conflicts(RemoteFileDesc other) {
        File otherFile=incompleteFileManager.getFile(other);
        return conflicts(otherFile);
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
                File thisFile=incompleteFileManager.getFile(rfd);
                if (thisFile.equals(incFile))
                    return true;
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
     * The default implementation includes all non-trivial keywords found in all
     * RemoteFileDesc's in this, i.e., the INTERSECTION of all file names.  A
     * keyword is "non-trivial" if it is not a number of a common English
     * article (e.g., "the"), a number (e.g., "2"), or the file extension.  The
     * query also includes all hashes for all RemoteFileDesc's, i.e., the UNION
     * of all hashes.
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
        if (allFiles.length<0)                  //TODO: what filename?
            throw new CantResumeException("");  //      maybe another exception?

		if(allFiles[0].getSHA1Urn() == null) {
			return QueryRequest.createQuery(extractQueryString());
		}
        return QueryRequest.createQuery(extractQueryString());
    }

    /** We need to offer this to subclasses to override because they might
     *  have specific behavior when deserialized from disk.  for example,
     *  RequeryDowloader should return a count of 0 upon deserialization, but 1
     *  if started from scratch.
     */
    protected int getQueryCount(boolean deserializedFromDisk) {
        // MDs, whether started from scratch or from disk, always
        // start with 0 query attempts.  subclasses should override as
        // necessary
        return 0; 
    }


    /**
     * This dictates whether this downloader should wait for user input before
     * spawning a Requery.  Subclasses should override with desired behavior as
     * necessary.
     * @return true if we the pause was broken because of new results.  false
     * if the user woke us up.
     * @param numRequeries The number of requeries sent so far.
     * @param deserializedFromDisk If the downloader was deserialized from a 
     * snapshot.  May be useful for subclasses.
     */
    protected boolean pauseForRequery(int numRequeries, 
                                      boolean deserializedFromDisk) 
        throws InterruptedException {
        // if you've sent too many requeries jump out immediately....
        if (numRequeries >= REQUERY_ATTEMPTS)
            return false;
        // MD's never want to requery without user input.
        boolean retVal = false;
        synchronized (reqLock) {
            setState(WAITING_FOR_USER);
            try {
                retVal = reqLock.lock(0);  // wait indefinitely
            }
            catch (InterruptedException stopException) {
                // must have been killed!!
                if (!stopped)
                    ErrorService.error(stopException);
                else
                    throw stopException;
            }
            // state will be set by tryAllDownloads()
        }
        return retVal;
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


    /** Returns the keywords for a requery, i.e., the keywords found in all
     *  filenames.  REQUIRES: allFiles.length MUST be greater than 0. */
    private final synchronized String extractQueryString() {
        Assert.that(allFiles.length>0, "Precondition violated");

        final int MAX_LEN = 30;

        // Put the keywords into a string, up to MAX_LEN
        Set intersection=keywords(allFiles[0].getFileName());
        if (intersection.size() < 1) // nothing to extract!
            return StringUtils.truncate(allFiles[0].getFileName(), 30);
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
        
        //TODO: one small problem - if every keyword in the filename is greater
        //than MAX_LEN, then the string returned will be empty.  we won't
        //handle this case as it is HIGHLY improbable.
        
        String retString = sb.toString();
        Assert.that(retString.length() <= MAX_LEN);
        return I18NConvert.instance().getNorm(retString);
    }

    /** Returns the canonicalized non-trivial search keywords in fileName. */
    private static final Set keywords(String fileName) {
        //Remove extension
        fileName=ripExtension(fileName);
        
        //Separate by whitespace and _, etc.
        Set ret=new HashSet();
        StringTokenizer st = new StringTokenizer(fileName, 
                                                 FileManager.DELIMETERS);
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


    private boolean initDone = false; // used to init

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
            
        // If this host is banned, don't add.
        if ( !IPFilter.instance().allow(other.getHost()) )
            return false;            
            
        // See if we have already tried and failed with this location
        // This is only done if the location we're trying is an alternate..
        if (other.isFromAlternateLocation() && invalidAlts.containsKey(other))
            return false;
        

        // get other info...
		final URN otherUrn = other.getSHA1Urn();
        final String otherName = other.getFileName();
        final long otherLength = other.getSize();

        synchronized (this) {
            // compare to allFiles....
            for (int i=0; i<allFiles.length; i++) {
                // get current info....
                RemoteFileDesc rfd = (RemoteFileDesc) allFiles[i];
				final URN urn = rfd.getSHA1Urn();
				if(otherUrn != null && urn != null) {
					return otherUrn.equals(urn);
				}
                final String thisName = rfd.getFileName();
                final long thisLength = rfd.getSize();
				
                // if they are similarly named and are close in length....
                // do sizeClose() first, much less expensive.....
                if (sizeClose(otherLength, thisLength))
                    if (namesClose(otherName, thisName)) 
                        return true;                
            }
        }
        return false;
    }

    private final long SIXTY_KB = 60000;
    private final boolean sizeClose(long one, long two) {
        boolean retVal = false;
		// if the sizes match exactly, we are good to go....
		if (one == two)
            retVal = true;
        else {
            //Similar file size (within 60k)?  This value was determined 
            //empirically to optimize grouping aggressiveness with minimal 
            //performance cost.
            long sizeDiff = Math.abs(one - two);
            if (sizeDiff <= SIXTY_KB) 
                retVal = true;
        }
        return retVal;
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
        if (! allowAddition(rfd)) {
            return false;
        }
        return addDownloadForced(rfd, cache);
    }

    /**
     * Like addDownload, but doesn't call allowAddition(..).
     *
     * If cache is false, the RFD is not added to allFiles, but is
     * added to the appropriate bucket.
     *
     * If the RFD matches one already in allFiles, the new one is
     * NOT added to allFiles, but IS added the appropriate bucket
     * if and only if a matching RFD is not currently in the bucket.
     *
     * If the file is ultimately added to buckets, either reqLock is released
     * or this is notified.
     *
     * This ALWAYS returns true, because the download is either allowed
     * or silently ignored (because we're already downloading or going to
     * attempt to download from the host described in the RFD).
     */
    protected final synchronized boolean addDownloadForced(RemoteFileDesc rfd,
                                                           boolean cache) {
                                                            
        // DO NOT DOWNLOAD FROM YOURSELF.
        if( NetworkUtils.isMe(rfd.getHost(), rfd.getPort()) )
            return true;
        
        // If this already exists in allFiles, DO NOT ADD IT AGAIN.
        // However, when we add it to buckets, we have to make sure
        // it didn't already exist in the specified bucket.
        // If cache is already false, there is no need to look.
        if (cache) {
            for (int i=0; i<allFiles.length; i++) {
                if (rfd.equals(allFiles[i])) {
                    cache = false; // do not store in allFiles.
                    break;
                }
            }
        }
        
        boolean added = false;
        //Add to buckets (will be seen because buckets exposes representation)
        //if we don't already contain this RFD.
        if (shouldAllowRFD(rfd)) {
            // We must always check to see if this RFD was already added to
            // the buckets now that we add downloads before adding to alt locs.
            // (Previously altloccollection filtered out already-seen ones)
            added = (buckets.add(rfd, true) != -1);
        }
        
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
            if ((state==Downloader.WAITING_FOR_RETRY) ||
                (state==Downloader.WAITING_FOR_RESULTS) || 
                (state==Downloader.GAVE_UP) || 
                (state==Downloader.WAITING_FOR_USER))
                reqLock.releaseDueToNewResults();
            else
                this.notify();                      //see tryAllDownloads3
        }
        
        return true;
    }
    
    private synchronized boolean shouldAllowRFD(RemoteFileDesc rfd) {
        if( buckets == null)
            return false;
        if( currentRFDs != null && currentRFDs.contains(rfd))
            return false;
        return true;
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

    public synchronized void stop() {
        // make redundant calls to stop() fast
        // this change is pretty safe because stopped is only set in two
        // places - initialized and here.  so long as this is true, we know
        // this is safe.
        if (stopped)
            return;

        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
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
            

        //Interrupt thread if waiting to retry.  This is actually just an
        //optimization since the thread will terminate upon exiting wait.  In
        //fact, this may not do anything at all if the thread is just about to
        //enter the wait!  Still this is nice in case the thread is waiting for
        //a long time.
        if (dloaderManagerThread!=null)
            dloaderManagerThread.interrupt();
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
        URN bucketHash = null;
        IncompleteFileDesc ifd = null;
        //TODO3: Until IncompleteFileDesc and ManagedDownloader share a copy
        // of the AlternateLocationCollection, they must use seperate
        // AlternateLocation objects.
        AlternateLocation loc = null;
        AlternateLocation forFD = null;
        
        if(!rfd.isAltLocCapable())
            return;
            
        // Verify that the bucket itself has a hash.  If it does not,
        // we should not have been getting locations in the first place.
        bucketHash = buckets.getURNForBucket(bucketNumber);
        Assert.that(bucketHash != null, "null bucketHash.");
        
        // Now verify that the SHA1 of the RFD matches the SHA1 of the
        // bucket hash.
        Assert.that(bucketHash.equals(rfd.getSHA1Urn()), "wrong loc SHA1");
        
        // If a validAlts collection wasn't created already
        // (which would only be possible if the initial set of
        // RFDs did not have a hash, but subsequent searches
        // produced RFDs with hashes), create the collection.
        if( validAlts == null )
            validAlts = AlternateLocationCollection.create(bucketHash);
        
        try {
            loc = AlternateLocation.create(rfd);
            forFD = AlternateLocation.create(rfd);
        } catch(IOException iox) {
            return;
        }
        for(Iterator iter=dloaders.iterator(); iter.hasNext();) {
            HTTPDownloader httpDloader = (HTTPDownloader)iter.next();
            RemoteFileDesc r = httpDloader.getRemoteFileDesc();
            if(r.getHost()==rfd.getHost() && r.getPort()==rfd.getPort()) 
                continue;//no need to tell uploader about itself
            if(good)
                httpDloader.addSuccessfulAltLoc(loc);
            else
                httpDloader.addFailedAltLoc(loc);
        }

        FileDesc fd = fileManager.getFileDescForFile(incompleteFile);
        if( fd != null && fd instanceof IncompleteFileDesc) {
            ifd = (IncompleteFileDesc)fd;
            if(!bucketHash.equals(ifd.getSHA1Urn())) {
                // Assert that the SHA1 of the IFD and the bucketHash match.
                Assert.silent(false, "wrong IFD.\n" +
                           "ours  :   " + incompleteFile +
                           "\ntheirs: " + ifd.getFile() +
                           "\nour hash    : " + bucketHash +
                           "\ntheir hashes: " +
                           DataUtils.listSet(ifd.getUrns())+
                          "\nifm.hashes : "+incompleteFileManager.dumpHashes());
                fileManager.removeFileIfShared(incompleteFile);
                ifd = null; // do not use, it's bad.
            }
        }

        if(good) {
            //check if validAlts contains loc to avoid duplicate stats, and
            //spurious count increments in the local
            //AlternateLocationCollections
            if(!validAlts.contains(loc)) {
                if( RECORD_STATS && rfd.isFromAlternateLocation() )
                    DownloadStat.ALTERNATE_WORKED.incrementStat(); 
                validAlts.add(loc);
                if( ifd != null )
                    ifd.addVerified(forFD);
            }
        } else {
            if( RECORD_STATS && rfd.isFromAlternateLocation() )
                DownloadStat.ALTERNATE_NOT_ADDED.incrementStat();
            validAlts.remove(loc);
            if( ifd != null )
                ifd.remove(forFD);
            invalidAlts.put(rfd, rfd);
        }
    }

    public boolean resume() throws AlreadyDownloadingException {
        //Ignore request if already in the download cycle.
        synchronized (this) {
            if (! (state==WAITING_FOR_RETRY || state==GAVE_UP || 
                   state==ABORTED || state==WAITING_FOR_USER))
                return false;
        }

        //Sometimes a resume can cause a conflict.  So we check.  Do not hold
        //this' lock during this step, as that can cause deadlock.  There is a
        //small chance that a conflicting file could be added to this after the
        //check.
        String conflict=this.manager.conflicts(allFiles, this);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);        

        //Do actual download.
        synchronized (this) {
            if (state==GAVE_UP || state==ABORTED) {
                if ((state==GAVE_UP) &&
                    (dloaderManagerThread!=null) && 
                    (dloaderManagerThread.isAlive())) {
                    // We can be sure all available RFDs have been tried, so
                    // buckets is empty. We should allow the user to retry
                    // the original RFDs he tried to download by pushing
                    // the resume button, so we create a new 'buckets'
                    // This is a quick and easy way of possibly restarting
                    // the download that uses zero network resources.
                    synchronized (this) {
                        buckets=new RemoteFileDescGrouper(
                            allFiles, incompleteFileManager);
                    }
                    // if the dloaderManagerThread is simply waiting on reqLock,
                    // then just release him.  calling initialize will 'do the 
                    // right thing' but will cause a memory leak due to threads
                    // not being cleaned up.  Alternatively, we could have
                    // called stop() and then initialize.
                    reqLock.releaseDueToNewResults();
                } else
                    //This stopped because all hosts were tried.  (Note that
                    //this couldn't have been user aborted.)  Therefore no
                    //threads are running in this and it may be safely resumed.
                    initialize(this.manager, this.fileManager, this.callback, 
                               false);
            } else if (state==WAITING_FOR_RETRY) {
                //Interrupt any waits.
                if (dloaderManagerThread!=null)
                    dloaderManagerThread.interrupt();
            } else if (state==WAITING_FOR_USER) 
                reqLock.releaseDueToRequery();
            return true;
        }
    }

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
     * This method is called when
     * 1) all downloads sources failed
     * 2) there are no busy hosts
     * 3) there is no room for a requery
     * Subclasses should override this method if they want to enforce special
     * behavior before going to the GAVE_UP state.
     * NOTE: Only the following states are can be preemptively woken up due to
     * new results - WAITING_FOR_RETRY, WAITING_FOR_RESULTS, and GAVE_UP.
     * @return two longs - long[0] is the state the downloader should go in.
     * long[1] is the time the downloader should spend in state long[0].  if
     * long[1] < 1, this return value is ignored.
     * @param deserialized true if this downloader was initialized from disk, 
     * false if it is brand new.
     * @param timeSpentWaiting the millisecond time that the downloader has 
     * spent in the failed state.
     */
    protected long[] getFailedState(boolean deserialized, 
                                   long timeSpentWaiting) {
        // no special states
        return new long[2];
    }
    
    /**
     * Cleans up information before this downloader is removed from memory.
     */
    public void finish() {
        if( commonOutFile != null )
            commonOutFile.clearManagedDownloader();
    }

    /** @return either the URN of the file that was downloaded the most so far
     *  or the URN of the bucket with the most sources.
     */
    private URN getBestURN() {
        URN retURN = null;

        // Iterate through all available URNs and attempt to get one with the
        // biggest size
        List urns = buckets.getURNs();
        int currBigSize = 0;
        Iterator iter = urns.iterator();
        while (iter.hasNext()) {
            URN currURN = (URN) iter.next();
            if(currURN == null) continue;
            File incomplete = incompleteFileManager.getFileForUrn(currURN);
            if (incomplete == null) continue;
            VerifyingFile vF =incompleteFileManager.getEntry(incomplete);
            if (vF == null) continue;
            if ((retURN == null) || (vF.getBlockSize() > currBigSize)) {
                currBigSize = vF.getBlockSize();
                retURN = currURN;
            }
        }

        // if we haven't downloaded anything, just get the most redundant URN
        if (retURN == null) retURN = buckets.getBestURN();
        return retURN;
    }

    /** 
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.  
     * @param deserialized True if this downloader was deserialized from disk,
     * false if it was newly constructed.
     */
    protected void tryAllDownloads(final boolean deserializedFromDisk) {     
        // the number of queries i've done for this downloader - this is
        // influenced by the type of downloader i am and if i was started from
        // disk or from scratch
        int numQueries = getQueryCount(deserializedFromDisk);
        // set this up in case you need to wait for results.  we'll always wait
        // TIME_BETWEEN_REQUERIES long after a query
        long timeQuerySent = System.currentTimeMillis();
        // the amount of time i've spent waiting for results or any other
        // special state as dictated by subclasses (getFailedState)
        long timeSpentWaiting = 0;
        // only query GUESS sources once
        boolean triedLocatingSources = false;

        //While not success and still busy...
        while (true) {
            try {
                //Try each b, bucket returning on success.  Note that buckets
                //may be added while the iterator is in use; these changes will
                //be reflected in the iteration.  The children of
                //tryAllDownloads call setState(CONNECTING) and
                //setState(DOWNLOADING) as appropriate.
                setState(QUEUED);  
                queuePosition="";//initialize
                queuedVendor="";//initialize
                manager.waitForSlot(this);
                boolean waitForRetry=false;
                bucketNumber = 0;//reset
                try {
                    for (Iterator iter=buckets.buckets(); iter.hasNext(); 
                                                              bucketNumber++) {
                        //when are are done tyring with a bucket cleanup
                        cleanup();
                        files =(List)iter.next();
                        if(checkHosts()) {//files is global
                            setState(GAVE_UP);
                            return;
                        }
                        if (files.size() <= 0)
                            continue;
                        int status=tryAllDownloads2();
                        if (status==COMPLETE) {
                            //Success!
                            setState(COMPLETE);
                            manager.remove(this, true);
                            return;
                        } else if (status==COULDNT_MOVE_TO_LIBRARY) {
                            setState(COULDNT_MOVE_TO_LIBRARY);
                            manager.remove(this, false);
                            return;
                        } else if (status==CORRUPT_FILE) {
                            setState(CORRUPT_FILE);
                            manager.remove(this, false);
                            return;
                        } else if (status==WAITING_FOR_RETRY) {
                            waitForRetry=true;
                        } else {
                            Assert.that(status==GAVE_UP,
                                        "Bad status from tad2: "+status);
                        }                    
                    }
                } catch (InterruptedException e) {
                    //check that each waitForSlot is paired with yieldSlot,
                    //unless we're aborting
                    if (!stopped)
                        ErrorService.error(e);
                }
                manager.yieldSlot(this);
                if (stopped) {
                    setState(ABORTED);
                    manager.remove(this, false);
                    return;
                }

                // sanity checks
                Assert.that(getState() != GAVE_UP);
                Assert.that(getState() != COMPLETE);
                Assert.that(getState() != COULDNT_MOVE_TO_LIBRARY);
                Assert.that(getState() != CORRUPT_FILE);
                
                // try to do iterative guessing here
                if ((this.originalQueryGUID != null) && !triedLocatingSources) { 
                    MessageRouter mr = RouterService.getMessageRouter();
                    Set guessLocs = mr.getGuessLocs(this.originalQueryGUID);
                    
                    if ((guessLocs != null) && !guessLocs.isEmpty()) {
                        setState(ITERATIVE_GUESSING);
                        triedLocatingSources = true;
                        boolean areThereNewResults = false;
                        URN bestURN = getBestURN();
                        for (Iterator i = guessLocs.iterator(); i.hasNext() ; ) {
                            // send a guess query
                            GUESSEndpoint ep = (GUESSEndpoint) i.next();
                            OnDemandUnicaster.query(ep, bestURN);
                            // wait a while for a result
                            if (!areThereNewResults)
                                areThereNewResults = reqLock.lock(750);
                            // else don't wait at all, we want to process that
                            // new result(s) ASAP
                        }
                        if (areThereNewResults)
                            continue;
                    }
                }

                if (stopped) {
                    setState(ABORTED);
                    manager.remove(this, false);
                    return;
                }

                final long currTime = System.currentTimeMillis();

                // FLOW:
                // 1.  If there is a retry to try (at least 1), then sleep for
                // the time you should sleep to wait for busy hosts.  Also do
                // some counting to let the GUI know how many guys you are
                // waiting on.  Be sure to use the RequestLock, so then you can
                // be waken up early to service a new QR
                // 2. If there is no retry, then we have the following options:
                //    A.  If you are waiting for results, set up the GUI
                //        correctly.  You know if you are waiting if
                //        numQueries is positive and we still have to wait.
                //    B.  Else, stall the download and see if the user ever
                //        wants to relaunch the query.  Note that the stalled
                //        download could be resumed because relevant results
                //        came in (they were later than TIME_BETWEEN_REQUERIES)
                //    C.  Else, see if the subclass has any special 'give up'
                //        instructions, else just give up and wait passively
                //        for results

                // 1.
                if (waitForRetry) {
                    synchronized (this) {
                        retriesWaiting=0;
                        for (Iterator iter=buckets.buckets();iter.hasNext();) {
                            List /* of RemoteFileDesc */ bucket=
                                                            (List)iter.next();
                            retriesWaiting+=bucket.size();
                        }
                    }
                    long time=calculateWaitTime();
                    setState(WAITING_FOR_RETRY, time);
                    // wait for a retry, but if you get a new result in earlier
                    // feel free to wake up early and try it....
                    reqLock.lock(time); 
                } 
                // 2.
                else {
                    
                    boolean areThereNewResults = false;
                    final long timeToWait = TIME_BETWEEN_REQUERIES - 
                        (System.currentTimeMillis() - timeQuerySent);
                    // 2A) we've sent a requery and never received results, 
                    // so wait for results...
                    if ((numQueries > 0) && (timeToWait > 0)) {
                        setState(WAITING_FOR_RESULTS, timeToWait);
                        areThereNewResults = reqLock.lock(timeToWait);
                    }

                    if (!areThereNewResults) {
                        // 2B) should we wait for the user to respawn a query?
                        // pauseForRequery delegates to subclasses when
                        // necessary, it returns if it was woken up due to new
                        // results.  so if new results, go up top and try and
                        // get them, else the user woke us up so send another
                        // query
                        if (pauseForRequery(numQueries, deserializedFromDisk)) 
                            continue;
                        if (numQueries < REQUERY_ATTEMPTS) {
                            waitForStableConnections();
                            // yeah, it is about time & i've not sent too many...
                            try {
                                if (manager.sendQuery(this, 
                                                      newRequery(numQueries)))
                                    numQueries++;
                                // reset wait time for results
                                timeQuerySent = System.currentTimeMillis();
                            } catch (CantResumeException ignore) { }
                        }
                        else {
                            // 2C) delegate to subclasses and follow
                            // instructions, or just 'give up' and wait for
                            // results.
                            // first delegate to subclass - see if we should set
                            // the state and or wait for a certain amount of time
                            long[] instructions = 
                                getFailedState(deserializedFromDisk, 
                                               timeSpentWaiting);
                            // if the subclass has told me to do some special
                            // waiting
                            if (instructions[1] > 0) {
                                setState((int) instructions[0], instructions[1]);
                                reqLock.lock(instructions[1]);
                                timeSpentWaiting += 
                                    System.currentTimeMillis() - currTime;
                            }
                            else {
                                // now just give up and hope for matching results
                                setState(GAVE_UP);
                                // wait indefn. for matching results
                                reqLock.lock(0);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                if (stopped) { 
                    setState(ABORTED);
                    manager.remove(this, false);
                    return;
                }
            }
        }
    }


	private static final int MIN_NUM_CONNECTIONS      = 2;
	private static final int MIN_CONNECTION_MESSAGES  = 6;
	private static final int MIN_TOTAL_MESSAGES       = 45;
	private static final int CONNECTION_DELAY         = 500;
	        static boolean   NO_DELAY				  = false; // For testing
    /**
     *  Try to wait for good, stable, connections with some amount of reach
	 *  or message flow.
     */
    private void waitForStableConnections() 
      throws InterruptedException {

		if ( NO_DELAY )  return;  // For Testing without network connection

		// TODO: Note that on a private network, these conditions might
		//       be too strict.

		// Wait till your connections are stable enough to get the minimum 
		// number of messages
		while 
		( (RouterService.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
			  < MIN_NUM_CONNECTIONS) &&
		  (RouterService.getActiveConnectionMessages() < MIN_TOTAL_MESSAGES) 
        ) {
            setState(WAITING_FOR_CONNECTIONS);
			Thread.sleep(CONNECTION_DELAY); 
		}
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
        //file of the bucket; see case (b) of getFileName() and
        //MagnetDownloader.getFileName().
        //    incompleteFile is picked using an arbitrary RFD from the bucket, since
        //IncompleteFileManager guarantees that any "same" files will get the
        //same temporary file.
        incompleteFile=incompleteFileManager.getFile(
                                                  (RemoteFileDesc)files.get(0));
        File saveDir;
        String fileName = getFileName();
        try {
            saveDir = SharingSettings.getSaveDirectory();
            completeFile=new File(saveDir, fileName);
            String savePath = saveDir.getCanonicalPath();		
            String completeFileParentPath = 
            new File(completeFile.getParent()).getCanonicalPath();
            if (!savePath.equals(completeFileParentPath))
                throw new InvalidPathException();  
        } catch (IOException e) {
            return COULDNT_MOVE_TO_LIBRARY;
        }

        // Create a new validAlts for this sha1.
		URN sha1 = buckets.getURNForBucket(bucketNumber);
		if( sha1 != null )
		    validAlts = AlternateLocationCollection.create(sha1);
        
        //2. Do the download
        int status = -1;  //TODO: is this equal to COMPLETE etc?
        try {
            status=tryAllDownloads3();//Exception may be thrown here. 
        } catch (InterruptedException e) { }

        //Close the file controlled by commonOutFile.
        commonOutFile.close();
        
        if(corruptState != NOT_CORRUPT_STATE) {//it is corrupt
            synchronized(corruptStateLock) {
                try{
                    while(corruptState==CORRUPT_WAITING_STATE) {
                        corruptStateLock.wait();
                    }
                } catch(InterruptedException ignored) {
                    //interruped while waiting for user. do nothing
                }
            }
            if (corruptState==CORRUPT_STOP_STATE) {
                cleanupCorrupt(incompleteFile, completeFile.getName());
                return CORRUPT_FILE;
            } 
            else if (corruptState==CORRUPT_CONTINUE_STATE) {
                ;//do nothing. 
                //Just fall through and and behave as normal complete
            }
        }
        
        if (status==-1) //InterruptedException was thrown in tryAllDownloads3
            throw new InterruptedException();
        if (status!=COMPLETE)
            return status;
        
        //3. Find out the hash of the file and verify that its the same
        // as the hash of the bucket it was downloaded from.
        //If the hash is different, we should ask the user about corruption
        //if the user has not been asked before.               
        URN bucketHash = buckets.getURNForBucket(bucketNumber);
        URN fileHash=null;
        try {
            // let the user know we're hashing the file
            setState(HASHING);
            fileHash = URN.createSHA1Urn(incompleteFile);
        } catch(IOException ignored) {}
        if(bucketHash!=null) { //if bucketHash==null, we cannot check
            //if fileHash == null, it will be a mismatch
            synchronized(corruptStateLock) {
                if(!bucketHash.equals(fileHash)) {
                    // immediately set as corrupt,
                    // will change to non-corrupt later if user ignores
                    setState(CORRUPT_FILE);
                    promptAboutCorruptDownload();
                    if(LOG.isWarnEnabled())
                        LOG.warn("hash verification problem, fileHash="+
                                       fileHash+", bucketHash="+bucketHash);
                }
                try {
                    while(corruptState==CORRUPT_WAITING_STATE)
                        corruptStateLock.wait();
                } catch(InterruptedException ignored2) {
                    //interrupted while waiting for user. do nothing.
                }
            }
            if (corruptState==CORRUPT_STOP_STATE) {
                cleanupCorrupt(incompleteFile, completeFile.getName());
                return CORRUPT_FILE;
            } 
        }
        
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
        boolean success = incompleteFile.renameTo(completeFile);
        
        // If that fails, try killing any partial uploads we may have
        // to unlock the file, and then rename it.
        if (!success) {
            FileDesc fd = RouterService.getFileManager().getFileDescForFile(
                incompleteFile);
            if( fd != null ) {
                UploadManager upMan = RouterService.getUploadManager();
                // This must all be synchronized so that a new upload
                // doesn't lock the file before we rename it.
                synchronized(upMan) {
                    if( upMan.killUploadsForFileDesc(fd) )
                        success = incompleteFile.renameTo(completeFile);
                }
            }
        }
        
        // If that didn't work, try copying the file.
        if (!success)
            success = CommonUtils.copy(incompleteFile, completeFile);
            
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
            // Only cache if we're going to share it.
            if(fileManager.isFileInSharedDirectories(file))
                UrnCache.instance().addUrns(file, urns);
        }

        FileDesc fileDesc = 
		    fileManager.addFileIfShared(completeFile, getXMLDocuments());  

		// Add the alternate locations to the newly saved local file
		if(validAlts != null && 
		   fileDesc!=null && 
		   fileDesc.getSHA1Urn().equals(validAlts.getSHA1Urn())) {
		    LOG.trace("MANAGER: adding valid alts to FileDesc");
			// making this call now is necessary to avoid writing the 
			// same alternate locations back to the requester as they sent 
			// in their original headers
            fileDesc.addAll(validAlts);
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
                               new Thread(requester, "HEAD Request Thread");
                headThread.setDaemon(true);
                headThread.start();
            }
        }
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
    private int tryAllDownloads3() throws InterruptedException {
        LOG.trace("MANAGER: entered tryAllDownloads3");
        
        //The parts of the file we still need to download.
        //INVARIANT: all intervals are disjoint and non-empty
        int completedSize = -1;
        synchronized(this) {
            needed=new IntervalSet();
            {//all variables in this block have limited scope
                Assert.that(incompleteFile != null);
                synchronized (incompleteFileManager) {
                    if( commonOutFile != null )
                        commonOutFile.clearManagedDownloader();
                    //get VerifyingFile
                    commonOutFile=
                    incompleteFileManager.getEntry(incompleteFile);
                }
                if(commonOutFile==null) {//no entry in incompleteFM
                    LOG.trace("creating a verifying file");
                    commonOutFile = new VerifyingFile(true);
                    //we must add an entry for this in IncompleteFileManager
                    incompleteFileManager.
                                   addEntry(incompleteFile,commonOutFile);
                    {//debugging block
                      FileDesc f=fileManager.getFileDescForFile(incompleteFile);
                      URN bucketHash = buckets.getURNForBucket(bucketNumber);
                      if(bucketHash != null && f!=null &&
                         !bucketHash.equals(f.getSHA1Urn())) {
                            Assert.silent(false,
                                        "IncompleteFileManager wrong fd");
                          //dont fail later
                          fileManager.removeFileIfShared(incompleteFile);
                      }
                    }
                }
                //need to get the VerifyingFile ready to write
                try {
                    commonOutFile.open(incompleteFile,this);
                } catch(IOException e) {
                    // This is a serious problem if it happens.
                    // TODO: add better checking to make sure it's possible
                    //  to write this file, possibly at startup
                    ErrorService.error(e);
                    
                    //Ideally we should show the user some sort of message here.
                    return COULDNT_MOVE_TO_LIBRARY;
                }
                //update needed
                completedSize =
                   (int)IncompleteFileManager.getCompletedSize(incompleteFile);
                Iterator iter=commonOutFile.getFreeBlocks(completedSize);
                while (iter.hasNext())
                    addToNeeded((Interval)iter.next());
            }
        }

        //The current RFDs that are being connected to.
        currentRFDs = new LinkedList();
        int size = -1;
        int connectTo = -1;
        int dloadsCount = -1;
        Assert.that(threads.size()==0);

        //While there is still an unfinished region of the file...
        while (true) {
            synchronized (stealLock) {//Must obtain stealLock before this
                synchronized(this) {
                    //Note: This block which causes exiting from
                    //tryAllDownloads3(), needs to be synchronized on stealLock
                    //(and this - state and datastructures) because a worker
                    //thread could take out a block from needed, and not yet
                    //have added a downloader to dloaders while the manager
                    //thread is executing this block. In that case the manager
                    //thread will exit thinking the download is complete
                    if (stopped) {
                        LOG.warn("MANAGER: terminating because of stop");
                        throw new InterruptedException();
                    } 
                    
                    if (dloaders.size()==0 && needed.isEmpty()) {
                        // Verify the commonOutFile is all done.
                        int doneSize =
                            (int)IncompleteFileManager.getCompletedSize(
                                incompleteFile);
                        Assert.that( completedSize == doneSize,
                            "incomplete files (or size!) changed!");
                        Iterator freeBlocks =
                            commonOutFile.getFreeBlocks(doneSize);

                        // An odd bug, but we can recover from it.
                        if(freeBlocks.hasNext()) {
                            while(freeBlocks.hasNext())
                                addToNeeded((Interval)freeBlocks.next());
                            Assert.silent(false, 
                                "file is incomplete, but needed.isEmpty." +
                                " left: " + needed);
                        } else {
                            //The normal correct case.
                            //Finished. Interrupt all worker threads
                            for(int i=threads.size();i>0;i--) {
                                Thread t = (Thread)threads.get(i-1);
                                t.interrupt();
                            }
                        
                            LOG.trace("MANAGER: terminating because of completion");
                            return COMPLETE;
                        }
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
                }
            }
        
            //OK. We are going to create a thread for each RFD. The policy for
            //the worker threads is to have one more thread than the max swarm
            //limit, which if successfully starts downloading or gets a better
            //queued slot than some other worker kills the lowest worker in some
            //remote queue.
            for(int i=0; i< (connectTo+1) && i<size && 
                              dloadsCount < getSwarmCapacity(); i++) {
                final RemoteFileDesc rfd;
                synchronized(this) {
                    rfd = removeBest(files);
                    // If the rfd was busy, that means all possible RFDs
                    // are busy, so just put it back in files and exit.
                    if( rfd.isBusy() ) {
                        files.add(rfd);
                        break;
                    }
                    // else...
                    currentRFDs.add(rfd);
                }
                Thread connectCreator = new Thread("DownloadWorker") {
                    public void run() {
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
                synchronized (this) { threads.add(connectCreator); }
                connectCreator.start();
            }//end of for 
            //wait for a notification before we continue.
            synchronized(this) {
                try {
                    //if no workers notify in 4 secs, iterate. This is a problem
                    //for stalled downloaders which will never notify. So if we
                    //wait without a timeout, we could wait forever.
                    this.wait(4000);
                } catch (InterruptedException ee ) {
                    //ee.printStackTrace();
                }
            }
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
            synchronized(validAlts) {
                Iterator iter = validAlts.iterator();
                int count = 0;
                while(iter.hasNext() && count < 10) {
                    dloader.addSuccessfulAltLoc((AlternateLocation)iter.next());
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
            int connected;
            http11 = rfd.isHTTP11();
            while(true) { //while queued, connect and sleep if we queued
                int[] qInfo ={-1,-1};//reset the sleep value, and queue position
                connected = assignAndRequest(dloader,qInfo,http11);
                boolean addQueued = killQueuedIfNecessary(connected,qInfo[1]);
                //an uploader we want to stay connected with
                if(connected == 4)
                    continue; // and has partial ranges
                if(connected!=1)
                    break;
                
                if(connected==1 && !addQueued) {
                    //I'm queued, but no thread was killed for me. die!
                    return true; //manager! keep churning more threads
                }
                if(connected==1) {//we have a queued thread, sleep
                    Assert.that(qInfo[0]>-1&&qInfo[1]>-1,
                                                    "inconsistent queue data");
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
                        Thread.sleep(qInfo[0]);//value from QueuedException
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
                else
                    Assert.that(false,"this should never happen");
            }
            //we have been given a slot remove this thread from queuedThreads
            synchronized(this) {
                //no problem even if this thread is  not in there
                queuedThreads.remove(Thread.currentThread()); 
            } 

            //Now, connected is either 0, 2 or 3.
            Assert.that(connected==0 || connected==2 || connected==3,
                        "invalid return from assignAndRequest "+connected);
            if(connected==0) { // File Not Found, Try Again Later, etc...
                dloader.stop(); // close the connection for now.
                return true;
            }
            else if(connected==3) { // Nothing more to download
                // close the connection since we're finished.
                dloader.stop();
                return false;
            }            
            //Step 3. OK, we have successfully connected, start saving the
            // file to disk
            boolean downloadOK = doDownload(dloader,http11);
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
        
        //came out of the while loop, http1.0 spawn a thread
        return true;
    }
    
    /**
     * @param connectCode 0 means no connection, 1 means connection queued, 2
     * means connection made, 3 means no connection required, 4 means partial
     * range available
     * @param queuePos the position of this downloader in the remote queue, MUST
     * be equal to -1 unless connectCode == 1 
     * <P>
     * Interrupts a remotely queued thread if the value of connectCode is 2
     * (meaning queuePos is -1) or connectCode is 1(meaning queuePos is the the
     * remote position of this thread) AND a thread has a worse position than
     * queuePos.  
     * @return true if this thread should be kept around, false otherwise --
     * explicitly, there is no need to kill any threads, or if the currentThread
     * is already in the queuedThreads, or if we did kill a thread worse than
     * this thread.  
     */
    private boolean killQueuedIfNecessary(int connectCode, int queuePos) {
        //check integrity constriants first
        Assert.that(connectCode>=0 && connectCode <=4,"Invalid connectCode");
        if(connectCode==2)
            Assert.that(queuePos==-1,"inconsistnet parameter queuePos");
        if(queuePos > -1)
            Assert.that(connectCode==1,"inconsistnet parameter connectCode");

        if(connectCode==0) //no need to replace a thread, server not available
            return false;
        if(connectCode==3) //no need to kill a thread for NoSuchElement
            return false;
        if(connectCode==4) //Partial ranges, don't kill now, defer the decision
            return false;
        //Either I am queued or downloading, find the highest queued thread
        Thread killThread = null;
        Thread currentThread = Thread.currentThread();
        synchronized(this) {            
            if(getNumDownloaders() < getSwarmCapacity()) {//need not kill anyone
                if(connectCode==1) //queued thread with no replacement required
                    queuedThreads.put(currentThread,new Integer(queuePos));
                return true;
            }
            if(queuedThreads.containsKey(currentThread)) {
                //we are already in there, update position if we're still queued
                if(connectCode==1)
                    queuedThreads.put(currentThread,new Integer(queuePos));
                return true;
            }
            Iterator iter = queuedThreads.keySet().iterator();
            int highest = queuePos;
            while(iter.hasNext()) {
                Object o = iter.next();
                int currQueue = ((Integer)queuedThreads.get(o)).intValue();
                if(currQueue > highest) { //queuePos==-1 for downloading threads
                    killThread=(Thread)o;
                    highest = currQueue;
                }
            }
            if(killThread == null) //no kill candidate
                return false;
            //OK. let's kill this guy
            killThread.interrupt();
            //OK. I should add myself to queuedThreads if I am queued
            if(connectCode == 1)
                queuedThreads.put(currentThread,new Integer(queuePos));
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
        boolean needsPush = needsPush(rfd);
        
        synchronized (this) {
            currentLocation=rfd.getHost();
            //If we're just increasing parallelism, stay in DOWNLOADING
            //state.  Otherwise the following call is needed to restart
            //the timer.
            if (dloaders.size()==0 && getState()!=COMPLETE && 
                getState()!=ABORTED && getState()!=GAVE_UP && 
                getState()!=COULDNT_MOVE_TO_LIBRARY && getState()!=CORRUPT_FILE 
                && getState()!=HASHING && getState()!=SAVING && 
                queuedThreads.size()==0)
                setState(CONNECTING, 
                         needsPush ? PUSH_CONNECT_TIME : NORMAL_CONNECT_TIME);
        }

        if(LOG.isDebugEnabled())
            LOG.debug("WORKER: attempting connect to "
              + rfd.getHost() + ":" + rfd.getPort());        
        
        if(RECORD_STATS)
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
        
        // must notify that we cannot connect directly.
        informMesh(rfd, false);

        try {
            ret = connectWithPush(rfd, incFile);
            return ret;
        } catch(IOException e) {
            // even the push failed :(
        }

        // if we're here, everything failed.
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
            if(RECORD_STATS)
                DownloadStat.CONNECT_DIRECT_SUCCESS.incrementStat();
        } catch(IOException iox) {
            if(RECORD_STATS)
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
            pushSent = manager.sendPush(rfd);
            if ( pushSent ) {
                //No loop is actually needed here, assuming spurious
                //notify()'s don't occur.  (They are not allowed by the Java
                //Language Specifications.)  Look at acceptDownload for
                //details.
                try {
                    threadLock.wait(PUSH_CONNECT_TIME);  
                } catch(InterruptedException e) {
                    if(RECORD_STATS)
                        DownloadStat.PUSH_FAILURE_INTERRUPTED.incrementStat();
                    throw new IOException("push interupted.");
                }
            }
        }
        
        //Done waiting or were notified.
        Socket pushSocket = (Socket)threadLockToSocket.remove(threadLock);
        if (pushSocket==null) {
            if(RECORD_STATS) {
                if( !pushSent )
                    DownloadStat.PUSH_FAILURE_NO_ROUTE.incrementStat();
                else
                    DownloadStat.PUSH_FAILURE_NO_RESPONSE.incrementStat();
            }
            throw new IOException("push socket is null");
        }
        
        miniRFDToLock.remove(mrfd);//we are not going to use it after this
        ret = new HTTPDownloader(pushSocket, rfd, incFile);
        
        //Socket.getInputStream() throws IOX if the connection is closed.
        //So this connectTCP *CAN* throw IOX.
        try {
            ret.connectTCP(0);//just initializes the byteReader in this case
            if(RECORD_STATS)
                DownloadStat.CONNECT_PUSH_SUCCESS.incrementStat();
        } catch(IOException iox) {
            if(RECORD_STATS)
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
     * @param refQueueInfo this parameter is used for pass by reference, this
     * method puts the minPollTime as the 0th element of this array, and the
     * remote queue postion of the downloader in the 1st element
     * @return 0 if (the server is not giving us the file)
     * TryAgainLater, FileNotFound, NotSharing, Stopped, Misc IOE
     * otherwise if queued return 1
     * otherwise if connected successfully return 2
     * otherwise if NoSuchElement( we have no areas to steal) return 3
     * otherwise if rfd was partial uploader and gave us ranges to try return 4 */
    private int assignAndRequest(HTTPDownloader dloader,int[] refQueueInfo, 
                                                              boolean http11) {
        synchronized(stealLock) {
            RemoteFileDesc rfd = dloader.getRemoteFileDesc();
            boolean updateNeeded = true;
            try {
                if (!needed.isEmpty()) {
                    assignWhite(dloader,http11);
                } else {
                    updateNeeded = false;      //1. See comment in finally
                    assignGrey(dloader,http11); 
                }
                updateNeeded = false;          //2. See comment in finally
            } catch(NoSuchElementException nsex) {
                if(RECORD_STATS)
                    DownloadStat.NSE_EXCEPTION.incrementStat();
                //thrown in assignGrey.The downloader we were trying to steal
                //from is not mutated.  DO NOT CALL updateNeeded() here!
                Assert.that(updateNeeded == false,
                            "updateNeeded not false in assignAndRequest");
                if(LOG.isDebugEnabled())            
                    LOG.debug("nsex thrown in assingAndRequest "+dloader,nsex);
                synchronized(this) {
                    // Add to files, keep checking for stalled uploader with
                    // this rfd
                    files.add(rfd);
                }
                return 3;
            } catch (NoSuchRangeException nsrx) { 
                if(RECORD_STATS)
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
                return 0;                
            } catch(TryAgainLaterException talx) {
                if(RECORD_STATS)
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
                return 0;
            } catch(RangeNotAvailableException rnae) {
                if(RECORD_STATS)
                    DownloadStat.RNA_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("rnae thrown in assignAndRequest "+dloader,rnae);
                rfd.resetFailedCount();                
                informMesh(rfd, true);
                return 4; //no need to add to files or busy we keep iterating
            } catch (FileNotFoundException fnfx) {
                if(RECORD_STATS)
                    DownloadStat.FNF_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("fnfx thrown in assignAndRequest"+dloader, fnfx);
                informMesh(rfd, false);
                return 0;//discard the rfd of dloader
            } catch (NotSharingException nsx) {
                if(RECORD_STATS)
                    DownloadStat.NS_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("nsx thrown in assignAndRequest "+dloader, nsx);
                informMesh(rfd, false);
                return 0;//discard the rfd of dloader
            } catch (QueuedException qx) { 
                if(RECORD_STATS)
                    DownloadStat.Q_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("qx thrown in assignAndRequest "+dloader, qx);
                //The extra time to sleep can be tuned. For now it's 1 S.
                refQueueInfo[0] = qx.getMinPollTime()*/*S->mS*/1000+1000;
                refQueueInfo[1] = qx.getQueuePosition();
                synchronized(this) {
                    if(dloaders.size()==0) {
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
                return 1;
            } catch(ProblemReadingHeaderException prhe) {
                if(RECORD_STATS)
                    DownloadStat.PRH_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("prhe thrown in assignAndRequest "+dloader,prhe);
                informMesh(rfd, false);
                return 0; //discard the rfd of dloader
            } catch(UnknownCodeException uce) {
                if(RECORD_STATS)
                    DownloadStat.UNKNOWN_CODE_EXCEPTION.incrementStat();
                if(LOG.isDebugEnabled())
                    LOG.debug("uce (" + uce.getCode() +
                              ") thrown in assignAndRequest " +
                              dloader, uce);
                informMesh(rfd, false);
                return 0; //discard the rfd of dloader
            } catch (IOException iox) {
                if(RECORD_STATS)
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
                return 0;
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
                //Update the needed list unless any of the following happened: 
                // 1. We tried to assign a grey region - which means needed 
                //    was never modified since needed.size()==0
                // 2. NoSuchElementException was thrown in 
                //    assignWhite or assignGrey
                // 3. We completed normally.
                //
                //Equivalent: update the needed list IF we assigned white and 
                //            we weren't able to start the download
                if(updateNeeded)
                    updateNeeded(dloader);
            }
            
            //did not throw exception? OK. we are downloading
            if(RECORD_STATS && rfd.getFailedCount() > 0)
                DownloadStat.RETRIED_SUCCESS.incrementStat();    
            
            rfd.resetFailedCount();

            synchronized(this) {
                setState(DOWNLOADING);
            }
            if (stopped) {
                LOG.trace("Stopped in assignAndRequest");
                updateNeeded(dloader); //give back a white area
                synchronized(this) {
                    files.add(rfd);
                }
                return 0;//throw new InterruptedException();
            }
            synchronized(this) {
                // only add if not already added.
                if(!dloaders.contains(dloader))
                    dloaders.add(dloader);
                chatList.addHost(dloader);
                browseList.addHost(dloader);
            }
            if(RECORD_STATS)
                DownloadStat.RESPONSE_OK.incrementStat();            
            return 2;
        }
    }
	
	/**
	 * Returns the number of alternate locations that this download is using.
	 */
	public int getNumberOfAlternateLocations() {
	    if ( validAlts == null ) return 0;
	    return validAlts.getAltLocsSize();
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
    NotSharingException , QueuedException, NoSuchRangeException {
        //Assign "white" (unclaimed) interval to new downloader.
        //TODO2: assign to existing downloader if possible, without
        //      increasing parallelis
        Interval interval = null;
        
        // Note that the retrieval from needed & return to needed
        // in fixIntervalForChunk MUST be atomic, otherwise
        // another downloader could attempt to retrieve needed
        // before we've put a chunk back in it.
        
        // If it's not a partial source, take the first chunk.
        // Then, if it's HTTP11, reduce the chunk up to CHUNK_SIZE.
        if( !dloader.getRemoteFileDesc().isPartialSource() ) {
            synchronized(this) {
                interval = needed.removeFirst();
                if(http11)
                    interval = fixIntervalForChunk(interval);
            }
        }
        // If it is a partial source, extract the first needed/available range
        // Then, if it's HTTP11, reduce the chunk up to CHUNK_SIZE.
        else {
            synchronized(this) {
                // May throw NoSuchElementException
                interval = getNeededPartialRange(dloader);
                if(http11)
                    interval = fixIntervalForChunk(interval);
            }
        }
        
        //Intervals from the needed set are INCLUSIVE on the high end, but
        //intervals passed to HTTPDownloader are EXCLUSIVE.  Hence the +1 in the
        //code below.  Note connectHTTP can throw several exceptions.  Also, the
        //call to stopAt does not appear necessary, but we're leaving it in just
        //in case.
        dloader.connectHTTP(getOverlapOffset(interval.low), interval.high+1,
                            true);
        dloader.stopAt(interval.high+1);
        if(LOG.isDebugEnabled())
            LOG.debug("WORKER: picking white "+interval+" to "+dloader);
    }

    /**
     * Steals a grey area from the biggesr HHTPDownloader and gives it to
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
        //TODO3: account for speed
        //TODO3: there is a minor race condition where biggest and 
        //      dloader could write to the same region of the file
        //      I think it's ok, though it could result in >100% in the GUI
        HTTPDownloader biggest=null;
        synchronized (this) {
            for (Iterator iter=dloaders.iterator(); iter.hasNext();) {
                HTTPDownloader h = (HTTPDownloader)iter.next();
                if (h.isActive() && (biggest==null ||
                  h.getAmountToRead() > biggest.getAmountToRead()))
                    biggest=h;
            }                
        }
        if (biggest==null) {//Not using downloader...but RFD maybe useful
            throw new NoSuchElementException();
        }
        //Note that getAmountToRead() and getInitialReadingPoint() are
        //constant.  getAmountRead() is not, so we "capture" it into a
        //variable.
        int amountRead=biggest.getAmountRead();
        int left=biggest.getAmountToRead()-amountRead;
        //check if we need to steal the last chunk from a slow downloader.
        //TODO4: Should we check left < CHUNK_SIZE+OVERLAP_BYTES
        if ((http11 && left<CHUNK_SIZE) || (!http11 && left < MIN_SPLIT_SIZE)){ 
            float bandwidthVictim = -1;
            float bandwidthStealer = -1;
            
            try {
                bandwidthVictim = biggest.getAverageBandwidth();
                biggest.getMeasuredBandwidth(); // trigger IDE.
            } catch (InsufficientDataException ide) {
                LOG.debug("victim does not have datapoints", ide);
                bandwidthVictim = -1;
            }
            try {
                bandwidthStealer = dloader.getAverageBandwidth();
                dloader.getMeasuredBandwidth(); // trigger IDE.
            } catch(InsufficientDataException ide) {
                LOG.debug("stealer does not have datapoints", ide);
                bandwidthStealer = -1;
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER: " + dloader + " attempting to steal from " + 
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
                int start=
                biggest.getInitialReadingPoint()+amountRead;
                int stop=
                biggest.getInitialReadingPoint()+biggest.getAmountToRead();
                //Note: we are not interested in being queued at this point this
                //line could throw a bunch of exceptions (not queuedException)
                dloader.connectHTTP(getOverlapOffset(start), stop, false);
                dloader.stopAt(stop);
                if(LOG.isDebugEnabled())
                    LOG.debug("WORKER: picking stolen grey "
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
            if(http11) //steal CHUNK_SIZE bytes from the end
                start = biggest.getInitialReadingPoint()+
                        biggest.getAmountToRead()-CHUNK_SIZE +1;
            else 
                start=biggest.getInitialReadingPoint()+amountRead+left/2;
            int stop=
            biggest.getInitialReadingPoint()+biggest.getAmountToRead();
            //this line could throw a bunch of exceptions
            dloader.connectHTTP(getOverlapOffset(start), stop,true);
            dloader.stopAt(stop);
            biggest.stopAt(start);//we know that biggest must be http1.0
            if(LOG.isDebugEnabled())
                LOG.debug("WORKER: assigning split grey "
                  +start+"-"+stop+" from "+biggest+" to "+dloader);
        }
    }

    /**
     * Extracts the first needed partial range from an HTTPDownloader.
     * May throw NoSuchElementException if no elements are needed.
     * Requires this' monitor is held.
     *
     * @return the first needed partial range from an HTTP Downloader.
     * @throws NoSuchElementException if it has no elements that are neded
     */
    private synchronized Interval getNeededPartialRange(HTTPDownloader dloader)
      throws NoSuchRangeException {
        List availableRanges =
            dloader.getRemoteFileDesc().getAvailableRanges();
        Interval ret = null;

        // go through the list of needed ranges and match each one
        // against the list of available ranges.
        // We use an iterator for iterating over the list, despite
        // the fact that we call addToNeeded (which adds an element
        // to the iterator's list).  Normally, this is not allowed because
        // the next iteration would throw a ConcurrentModificationException.
        // However, once we reach the point where we remove the element (using
        // the iterator's remove) and re-add elements back to the list,
        // we are finished using the iterator (as evidenced by the two breaks).
        for (Iterator i = needed.getAllIntervals(); i.hasNext();) {
            // this is the interval we are going to match against
            // the available ranges now
            Interval need = (Interval)i.next();
            // go through the list of available ranges
            for (int k = 0; k < availableRanges.size(); k++) {
                // test this available range now but make sure
                // to account for the OVERLAP_BYTES requested by
                // the ManagedDownloader.
                Interval available = (Interval)availableRanges.get(k);
                available = addOverlap(available);
                
                // when the overlap was added, the range wasn't large
                // enough, or the two ranges don't overlap...
                if( available == null || !need.overlaps(available) )
                    continue;

                ret = need;
                i.remove();
                
                //check if high end needs truncation
                if (ret.high > available.high ) {
                    ret = new Interval(ret.low, available.high );
                    addToNeeded(new Interval(ret.high+1,need.high));
                }
                //check if low end needs trucncation
                if ( ret.low < available.low ) {
                    ret = new Interval(available.low, ret.high);
                    addToNeeded(new Interval(need.low, ret.low-1));
                }

                break;//found a match, exit loop
            } //end of inner

            // We found a match, so exit.
            // We MUST exit here instead of iterating,
            // or the Iterator will throw a ConcurrentModificationException.
            if (ret != null)
                break;

        } //end of outer
        
        if( ret == null )
            throw new NoSuchRangeException("no partial range is needed");
            
        return ret;
    }
    
    /**
     * Returns a new chunk that is less than CHUNK_SIZE.
     * This reduces the high value of the interval.
     * Adds what was cut off to needed.
     * Requires this' monitor is held.
     *
     * @return a new (smaller) interval up to CHUNK_SIZE.
     * @require this' monitor is held
     */
    private Interval fixIntervalForChunk(Interval temp) {
        Interval interval;
        if((temp.high-temp.low+1) > CHUNK_SIZE) {
            int max = temp.low+CHUNK_SIZE-1;
            interval = new Interval(temp.low, max);
            temp = new Interval(max+1,temp.high);
            addToNeeded(temp);
        } 
        else { //temp's size <= CHUNK_SIZE
            interval = temp;
        }
        
        return interval;
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
            if(RECORD_STATS) {
                if(http11)
                    DownloadStat.SUCCESFULL_HTTP11.incrementStat();
                else
                    DownloadStat.SUCCESFULL_HTTP10.incrementStat();
            }
        } catch (IOException e) {
            if(RECORD_STATS) {
                if(http11)
                    DownloadStat.FAILED_HTTP11.incrementStat();
                else
                    DownloadStat.FAILED_HTTP10.incrementStat();
             }
            problem = true;
			chatList.removeHost(downloader);
            browseList.removeHost(downloader);
        } finally {
            int stop=downloader.getInitialReadingPoint()
                        +downloader.getAmountRead();
            if(LOG.isDebugEnabled())
                LOG.debug("    WORKER: terminating from "+downloader+" at "+stop+ 
                  " error? "+problem);
            synchronized (this) {
                if (problem) {
                    updateNeeded(downloader);
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
     * adds an interval to needed, if dloader was not able to download a part
     * assigned to it.  
     */
    private synchronized void updateNeeded(HTTPDownloader dloader) {
        //TODO2: call repOK() at the begining and end of this method.
        //repOK():
        //   -no elements of needed overlap by at most 500 bytes
        //   -no elements of needed overlap any of the downloaders (500)
        //   -no downloaders overlap each other (500)
        int low=dloader.getInitialReadingPoint()+dloader.getAmountRead();
        int high = dloader.getInitialReadingPoint()+dloader.getAmountToRead()-1;
        //note: the high'th byte will also be downloaded.
        if( (high-low)>0) {//dloader failed to download a part assigned to it?
            Interval in = new Interval(low,high);
            if(LOG.isDebugEnabled())
                LOG.debug("Updating needed. Adding interval "
                          +in+" from "+dloader);
            addToNeeded(in);
        }
    }
    
    /**
     * Adds an Interval into the needed list.
     */
    private synchronized void addToNeeded(Interval val) {
        needed.add(val);
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
    private synchronized RemoteFileDesc removeBest(List filesLeft) {
        //Lock is needed here because filesLeft can be modified by
        //tryOneDownload in worker thread.
        Iterator iter=filesLeft.iterator();
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
            
        filesLeft.remove(ret);
        return ret;
    }

    /** Returns true iff rfd should be attempted by push download, either 
     *  because it is a private address or was unreachable in the past. */
    private static boolean needsPush(RemoteFileDesc rfd) {
        // if replying to multicast, do a push.
        if ( rfd.isReplyToMulticast() )
            return true;
        //Return true if rfd is private or unreachable
        if (rfd.isPrivate()) {
            // Don't do a push for magnets in case you are in a private network.
            // Note to Sam: This doesn't mean that isPrivate should be true.
            if (rfd instanceof URLRemoteFileDesc) 
                return false;
            else  // Otherwise obey push rule for private rfds.
                return true;
        }
        else if (!NetworkUtils.isValidPort(rfd.getPort()))
            return true;
        else
            return false;
    }

    /**
     * package access. Passes the call up to the activity callback
     */
    void promptAboutCorruptDownload() {
        synchronized(corruptStateLock) {
            //If we are corrupt, we want to stop sharing the incomplete file,
            //as it is not going to generate the same SHA1 anymore.
            RouterService.getFileManager().removeFileIfShared(incompleteFile);
            
            //For any other state we don't do anything
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

    public void discardCorruptDownload(boolean delete) {
        if(delete) {
            corruptState = CORRUPT_STOP_STATE;
            stop();
        }
        else 
            corruptState = CORRUPT_CONTINUE_STATE;
        synchronized(corruptStateLock) {
            corruptStateLock.notify();
        }
    }
            

    /**
     * Returns the union of all XML metadata documents from all hosts.
     */
    private synchronized List getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?  How about only those in this
        //bucket?
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

    private void cleanup() {
        miniRFDToLock.clear();
        threadLockToSocket.clear();
        if(needed != null) //it's null while before we try first bucket
            needed.clear();
        files = null;
        validAlts = null;
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
            remaining=stateTime-System.currentTimeMillis();
            return (int)Math.max(remaining, 0)/1000;
        case WAITING_FOR_RESULTS:
            remaining=stateTime-System.currentTimeMillis();
            return (int)Math.max(remaining, 0)/1000;
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

    public synchronized int getRetriesWaiting() {
        return retriesWaiting;
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

    /** Synchronization Primitive for auto-requerying....
     *  Can be understood as follows:
     *  -- The tryAllDownloads thread does a lock(), which will cause it to
     *  wait() for up to waitTime.  it may be woken up earlier if it gets
     *  a requery result. moreover, it won't wait if it has a result already...
     *  -- The addDownload method, upon getting a result that matches, will
     *  wake up the tryAllDownloads thread with a release...().  
     *  -- The tryAllDownloads method may release the lock due to a user-driven
     *  query.
     *  WARNING:  THIS IS VERY SPECIFIC SYNCHRONIZATION.  IT WAS NOT MEANT TO 
     *  WORK WITH MORE THAN ONE PRODUCER OR ONE CONSUMER.
     */
    private class RequeryLock extends Object {
        private volatile boolean shouldWait = true;
        // returned from lock to signify the reason for exit
        private volatile boolean newResults = false;

        public synchronized void releaseDueToNewResults() {
            shouldWait = false;
            newResults = true;
            this.notifyAll();
        }

        public synchronized void releaseDueToRequery() {
            // we want shouldWait to stay what it is - we don't want to 'queue'
            // (queue size = 1) user requests 
            this.notifyAll();
        }

        private synchronized boolean getAndClearNewResult() {
            boolean retVal = newResults;
            newResults = false;
            return retVal;
        }

        /** @exception InterruptedException if you are interrupted, you were
         *  probably stopped.
         */
        public synchronized boolean lock(long waitTime) 
            throws InterruptedException {
            try {
                // max waitTime i'll wait, best case i'll get
                // interrupted 
                if (shouldWait) 
                    this.wait(waitTime);
            }
            catch (InterruptedException ie) {
                // if interrupted, make sure to reset shouldWait...
                shouldWait = true;
                throw ie;
            }
            shouldWait = true;
            return getAndClearNewResult();
        }

    }
}
