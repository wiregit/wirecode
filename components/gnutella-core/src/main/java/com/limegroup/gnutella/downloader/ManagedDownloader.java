package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.settings.ThemeSettings;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.sun.java.util.collections.*;
import java.util.Date;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.io.*;
import java.net.*;

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
     *  elements of type RemoteFileDesc and URLRemoteFileDesc--no 
     *  RemoteFileDesc2's are stored in the array. */
    private RemoteFileDesc[] allFiles;

    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=300;
    /** The max number of push tries to make, per host. */
    private static final int PUSH_TRIES=2;
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
		SettingsManager.instance().getMaxDownstreamBytesPerSec() < 8 ? 
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
    /** The number of times to requery the network. 
     *  We are getting rid of ALL requeries, so the new value is one such that
     *  a Requery can never happen.
     */
    private static final int REQUERY_ATTEMPTS = -1;
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
     * The IntervalSet of intervals within the file which have not been
     * allocated to any downloader yet.  This set of intervals represents
     * the "white" region of the file we are downloading.
     *
     * An IntervalSet is used to ensure that chunks readded to it are
     * coalesced together.
     * LOCKING: synchronize on this
     */
    private IntervalSet needed;
    /**List of RemoteFileDesc which were busy when we tried to connect to them.
     * To be used when we have run out of other options.*/
    private List /* of RemoteFileDesc2 */ busy;
    /** List of RemoteFileDesc to which we actively connect and request parts
     * of the file.*/
    private List /*of RemoteFileDesc2 */ files;
    /** keeps a count of worker threads that are queued on uploader, useful 
     * for setting the state correctly*/
    private volatile int queuedCount;
	
    /**
     * The collection of alternate locations we successfully downloaded from
     * somthing from. We will never use this data-structure until the very end,
     * when we have become active uploaders of the file.
     */
	private AlternateLocationCollection validAlts; 

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
    /** Lock used to communicate between addDownload and tryAllDownloads.
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
    private static final boolean RECORD_STATS = !CommonUtils.isJava118();

    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * does not start until initialize(..) is called, nor is it safe to call
     * any other methods until that point.
     * @param files the list of files to get.  This stops after ANY of the
     *  files is downloaded.
     * @param ifc the repository of incomplete files for resuming
     */
    public ManagedDownloader(RemoteFileDesc[] files,IncompleteFileManager ifc) {
		if(files == null) {
			throw new NullPointerException("null RFDS");
		}
		if(ifc == null) {
			throw new NullPointerException("null incomplete file manager");
		}
        this.allFiles = files;
        this.incompleteFileManager = ifc;
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
        this.dloaderManagerThread=new Thread() {
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
    protected void initializeIncompleteFile(File incompleteFile) {
        if (this.incompleteFile!=null)
            return;
        this.incompleteFile=incompleteFile;
        this.commonOutFile=incompleteFileManager.getEntry(incompleteFile);
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
            long size = incompleteFileManager.getCompletedSize(incompleteFile);
            // Find any matching file-desc for this URN.
            FileDesc fd = fileManager.getFileDescForUrn(hash);
            if( fd != null ) {
                // Retrieve the alternate locations (without adding ourself)
                validAlts = fd.getAlternateLocationCollectionWithoutSelf();
                Iterator iter = validAlts.iterator();
                synchronized(validAlts) {
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
    public boolean conflicts(File incompleteFile) {
        synchronized (this) {
            //TODO3: this is stricter than necessary.  What if a location has
            //been removed?  Tricky without global variables.  At the least we
            //should return false if in COULDNT_DOWNLOAD state.
            for (int i=0; i<allFiles.length; i++) {
                RemoteFileDesc rfd=(RemoteFileDesc)allFiles[i];
                File thisFile=incompleteFileManager.getFile(rfd);
                if (thisFile.equals(incompleteFile))
                    return true;
            }
        }
        return false;
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
     * @param numRequeries the number of requeries that have already happened
     * @exception CantResumeException if this doesn't know what to search for 
	 * @return a new <tt>QueryRequest</tt> for making the requery
     */
    protected synchronized QueryRequest newRequery(int numRequeries) 
		throws CantResumeException {
        if (allFiles.length<0)                  //TODO: what filename?
            throw new CantResumeException("");  //      maybe another exception?

		if(allFiles[0].getSHA1Urn() == null) {
			return QueryRequest.createRequery(extractQueryString());
		}
		return QueryRequest.createRequery(allFiles[0].getSHA1Urn());
    }


    /** Returns the URNs for requery, i.e., the union of all requeries 
     *  (within reason).
     *  @return a Set of URN */
    private final Set /* of URN */ extractUrns() {
        final int MAX_URNS=2;
        Set ret=new HashSet(MAX_URNS);
        for (int i=0; i<allFiles.length && ret.size()<MAX_URNS; i++) {
            URN urn=allFiles[i].getSHA1Urn();
            if (urn!=null)
                ret.add(urn);
        }
        return ret;
    }

    /** Returns the keywords for a requery, i.e., the keywords found in all
     *  filenames.  REQUIRES: allFiles.length MUST be greater than 0. */
    private final synchronized String extractQueryString() {
        //Intersect words(allFiles[i]), for all i.
        Assert.that(allFiles.length>0, "Precondition violated");
        Set intersection=keywords(allFiles[0].getFileName());
//          for (int i=1; i<allFiles.length; i++) {
//              intersection.retainAll(
//                  keywords(allFiles[i].getFileName()));
//          }

        //Put the keywords into a string.
        StringBuffer sb = new StringBuffer();
        for (Iterator keys=intersection.iterator(); keys.hasNext(); ) {
            sb.append(keys.next());
            if (keys.hasNext())
                sb.append(" ");
        }        
        return sb.toString();
    }

    /** Returns the canonicalized non-trivial search keywords in fileName. */
    private static final Set keywords(String fileName) {
        //Remove extension
        fileName=ripExtension(fileName);
        
        //Separate by whitespace and _, etc.
        Set ret=new HashSet();
        StringTokenizer st = new StringTokenizer(fileName, FileManager.DELIMETERS);
        while (st.hasMoreTokens()) {
            final String currToken = st.nextToken().toLowerCase();;
            try {                
                //Ignore if a number.
                Double d = new Double(currToken);
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

        debug("MD.namesClose(): one = " + one);
        debug("MD.namesClose(): two = " + two);
        debug("MD.namesClose(): retVal = " + retVal);
            
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
        if (! allowAddition(rfd))
            return false;
        return addDownloadForced(rfd, cache);
    }

    /** Like addDownload, but doesn't call allowAddition(..). 
     *  @return true, since download always allowed */
    protected final synchronized boolean addDownloadForced(RemoteFileDesc rfd,
                                                           boolean cache) {
        //Ignore if this was already added.  This includes existing downloaders
        //as well as busy lists.
        for (int i=0; i<allFiles.length; i++) {
            if (rfd.equals(allFiles[i]))
                return true;          
        }  

        //System.out.println("Adding "+rfd);
        //Add to buckets (will be seen because buckets exposes
        //representation)...
        
        if (buckets != null)
            buckets.add(rfd);
        if(cache) {
            //...append to allFiles for resume purposes...
            RemoteFileDesc[] newAllFiles=new RemoteFileDesc[allFiles.length+1];
            System.arraycopy(allFiles, 0, newAllFiles, 0, allFiles.length);
            newAllFiles[newAllFiles.length-1]=rfd;
            allFiles=newAllFiles;
            //...and notify manager to look for new workers.  You might be
            //tempted to just call dloaderManagerThread.interrupt(), but that
            //causes spurious interrupts to happen when establishing connections
            //(push or otherwise).  So instead we target the two cases we're
            //interested: waiting for downloaders to complete (by waiting on
            //this) or waiting for retry (by sleeping).
        }
        if ((state==Downloader.WAITING_FOR_RETRY) ||
            (state==Downloader.WAITING_FOR_RESULTS) || 
            (state==Downloader.GAVE_UP))
            reqLock.release();
        else
            this.notify();                      //see tryAllDownloads3
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

    private synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
        AlternateLocation loc = null;
        try {
            loc = AlternateLocation.create(rfd);
        } catch(IOException iox) {
            return;
        }
        for(Iterator iter=dloaders.iterator(); iter.hasNext();) {
            HTTPDownloader httpDloader = (HTTPDownloader)iter.next();
            if(good)
                httpDloader.addSuccessfulAltLoc(loc);
            else
                httpDloader.addFailedAltLoc(loc);
        }

        if(!good)
            validAlts.remove(loc);
    }

    public boolean resume() throws AlreadyDownloadingException {
        //Ignore request if already in the download cycle.
        synchronized (this) {
            if (! (state==WAITING_FOR_RETRY || state==GAVE_UP || 
                   state==ABORTED || state==WAITING_FOR_RESULTS))
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
                    (dloaderManagerThread.isAlive()))
                    // if the dloaderManagerThread is simply waiting on reqLock,
                    // then just release him.  calling initialize will 'do the 
                    // right thing' but will cause a memory leak due to threads
                    // not being cleaned up.  Alternatively, we could have
                    // called stop() and then initialize.
                    reqLock.release();
                else
                    //This stopped because all hosts were tried.  (Note that this
                    //couldn't have been user aborted.)  Therefore no threads are
                    //running in this and it may be safely resumed.
                    initialize(this.manager, this.fileManager, this.callback, 
                               false);
            } else if (state==WAITING_FOR_RETRY) {
                //Interrupt any waits.
                if (dloaderManagerThread!=null)
                    dloaderManagerThread.interrupt();
            } else if (state==WAITING_FOR_RESULTS) {
                // wake up the requerier...
                reqLock.release();
            }
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


    /** 
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.  
     * @param deserialized True if this downloader was deserialized from disk,
     * false if it was newly constructed.
     */
    protected void tryAllDownloads(boolean deserialized) {     
        // the number of requeries i've done...
        int numRequeries = 0;
        // the amount of time i've spent waiting for results or any other
        // special state as dictated by subclasses (getFailedState)
        long timeSpentWaiting = 0;
        // the time to next requery.  We don't want to send the first requery
        // until a few minutes after the initial download attempt--after all,
        // the user just started a query.  Hence initialize nextRequeryTime to
        // System.currentTimeMillis() plus a few minutes.
        long nextRequeryTime = nextRequeryTime(numRequeries);

        synchronized (this) {
            buckets=new RemoteFileDescGrouper(allFiles,incompleteFileManager);
            // if this was read from the disk, read the alternate locations
            // that may be stored in the IncompleteFileDesc.
            // This call is necessary now to allow buckets to be built up
            // correctly, as the RFDs built from alternate locations are not
            // added to allFiles, so the RFDGrouper won't add them into
            // buckets.
            if(deserialized) {
                initializeAlternateLocations();
            }
        }
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
                queuedCount=0;
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

                // should i send a requery?
                final long currTime = System.currentTimeMillis();
                if ((currTime >= nextRequeryTime) &&
                    (numRequeries < REQUERY_ATTEMPTS)) {
					waitForStableConnections();
                    // yeah, it is about time and i've not sent too many...
                    try {
                        if (manager.sendQuery(this, newRequery(numRequeries)))
                            numRequeries++;
                    } catch (CantResumeException ignore) { }
                    // set time for next requery...
                    nextRequeryTime = nextRequeryTime(numRequeries);
                }


                // FLOW:
                // 1.  If there is a retry to try (at least 1), then sleep for
                // the time you should sleep to wait for busy hosts.  Also do
                // some counting to let the GUI know how many guys you are
                // waiting on.  Be sure to use the RequestLock, so then you can
                // be waken up early to service a new QR
                // 2. If there is no retry, then we have the following options:
                //    A.  If you are waiting for results, set up the GUI
                //        correctly.  Note that the condition to enter this
                //        branch will be violated when the last requery wait
                //        time is reached but we've incremented past the number
                //        of requeries allowed.
                //    B.  Else, give up.
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
                } else {
                    if (numRequeries <= REQUERY_ATTEMPTS) {
                        final long waitTime = 
                        nextRequeryTime - System.currentTimeMillis();
                        if (waitTime > 0) {
                            setState(WAITING_FOR_RESULTS, waitTime);
                            reqLock.lock(waitTime);
                        }
                    }
                    else {
                        // first delegate to subclass - see if we should set
                        // the state and or wait for a certain amount of time
                        long[] instructions = getFailedState(deserialized, 
                                                            timeSpentWaiting);
                        // if the subclass has told me to do some special waiting
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
    private static void waitForStableConnections() 
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
			Thread.sleep(CONNECTION_DELAY); 
		}
    }


    /** Returns the next system time that we can requery.  Subclasses may
     *  override to customize this behavior.  Note that this is still 
     *  subject to global requery limits in DownloadManager.
     *  @param requeries the number of requeries that have happened so far
     *  @return an absolute system time of the next allowed requery */
    protected long nextRequeryTime(int requeries) {
        return System.currentTimeMillis()+TIME_BETWEEN_REQUERIES;
    }

    /** Returns the amount of time to wait in milliseconds before retrying,
     *  based on tries.  This is also the time to wait for * incoming pushes to
     *  arrive, so it must not be too small.  A value of * tries==0 represents
     *  the first try.  */
    private long calculateWaitTime() {
        //60 seconds: same as BearShare.
        return 60*1000;
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
            // if we're downloading a theme file, save in the theme dir.
            if( fileName.toLowerCase().endsWith("." + ThemeSettings.EXTENSION))
                saveDir = ThemeSettings.THEME_DIR_FILE;
            else 
                saveDir=SettingsManager.instance().getSaveDirectory();
            completeFile=new File(saveDir, fileName);
            String savePath = saveDir.getCanonicalPath();		
            String completeFileParentPath = 
            new File(completeFile.getParent()).getCanonicalPath();
            if (!savePath.equals(completeFileParentPath))
                throw new InvalidPathException();  
        } catch (IOException e) {
            return COULDNT_MOVE_TO_LIBRARY;
        }

		boolean firstSHA1RFD = true;
		RemoteFileDesc tempRFD;
		String rfdStr;
		URL    rfdURL;
		
		// Create a new AlternateLocationCollection (if needed).
		// The resulting collection's SHA1 is based off the
		// the SHA1 of the first RFD that has a SHA1.
		// If an AlternateLocationCollection already existed with that
		// SHA1, it reuses it.  Otherwise, it creates it new.
        synchronized (this) {
            tempRFD = (RemoteFileDesc)files.get(0);
        }        
        URN sha1 = tempRFD.getSHA1Urn();
				 
        // If no alternate location collection existed already, or one existed
        // but this is the first new RFD, and current SHA1 is different than it,
        // create a new collection.
        if( sha1!=null && 
            (validAlts == null || !validAlts.getSHA1Urn().equals(sha1)) )
            validAlts  = AlternateLocationCollection.create(sha1);
        
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
                    debug("hash verification problem, fileHash="+
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
        //System.out.println("MANAGER: completed");
        //Delete target.  If target doesn't exist, this will fail silently.
        completeFile.delete();
        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileManager, though this is not strictly necessary
        //because IFM.purge() is called frequently in DownloadManager.
        if (!incompleteFile.renameTo(completeFile))
            if (! CommonUtils.copy(incompleteFile, completeFile))
                return COULDNT_MOVE_TO_LIBRARY;
        incompleteFileManager.removeEntry(incompleteFile);

        //Add file to library.
        // first check if it conflicts with the saved dir....
        if (fileExists(completeFile))
            fileManager.removeFileIfShared(completeFile);
        boolean fileAdded = 
		    fileManager.addFileIfShared(completeFile, getXMLDocuments());  

		// Add the alternate locations to the newly saved local file
		if(validAlts != null && fileAdded) {
			FileDesc fileDesc = 
			    fileManager.getFileDescMatching(completeFile);  
			// making this call now is necessary to avoid writing the 
			// same alternate locations back to the requester as they sent 
			// in their original headers
			if (fileDesc != null && 
              fileDesc.getSHA1Urn().equals(validAlts.getSHA1Urn())) { 
                fileDesc.addAll(validAlts);
				//tell the library we have alternate locations
				callback.handleSharedFileUpdate(completeFile);
                HashSet set = null;
                synchronized(this) {
                    set = new HashSet(files);
                }
//  				HeadRequester requester = 
//  			        new HeadRequester(set, fileHash, fileDesc, 
//                                        fileDesc.getAlternateLocationCollection());
//  				Thread headThread = new Thread(requester, "HEAD Request Thread");
//  				headThread.setDaemon(true);
//  				headThread.start();
			}
		}
		
        return COMPLETE;
    }   

    /** @return True returned if the File exists in the Save directory....
     */
    private boolean fileExists(File f) {
        boolean retVal = false;
        try {
            File downloadDir = SettingsManager.instance().getSaveDirectory();
            String filename=f.getName();
            File completeFile = 
				new File(downloadDir, CommonUtils.convertFileName(filename));  
            if ( completeFile.exists() ) 
                retVal = true;
        }
        catch (Exception e) { }
        return retVal;
    }

    /** Removes all entries for incompleteFile from incompleteFileManager 
     *  and attempts to rename incompleteFile to "CORRUPT-i-...".  Deletes
     *  incompleteFile if rename fails. */
    private void cleanupCorrupt(File incompleteFile, String name) {
        corruptFileBytes=getAmountRead();        
        incompleteFileManager.removeEntry(incompleteFile);

        //Try to rename the incomplete file to a new corrupt file in the same
        //directory (INCOMPLETE_DIRECTORY).
        boolean renamed = false;
        for (int i=0; i<10 && !renamed; i++) {
            corruptFile=new File(incompleteFile.getParent(),
                                 "CORRUPT-"+i+"-"+name);
            if (corruptFile.exists())
                continue;
            renamed=incompleteFile.renameTo(corruptFile);
        }

        //Could not rename after ten attempts?  Delete.
        if(!renamed) {
            incompleteFile.delete();
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
        //The parts of the file we still need to download.
        //INVARIANT: all intervals are disjoint and non-empty
        synchronized(this) {
            needed=new IntervalSet(); 
            {//all variables in this block have limited scope
                RemoteFileDesc rfd=(RemoteFileDesc)files.get(0);
                File incompleteFile=incompleteFileManager.getFile(rfd);
                synchronized (incompleteFileManager) {
                    if( commonOutFile != null )
                        commonOutFile.clearManagedDownloader();
                    //get VerifyingFile
                    commonOutFile=
                    incompleteFileManager.getEntry(incompleteFile);
                }
                if(commonOutFile==null) {//no entry in incompleteFM
                    debug("creating a verifying file");
                    commonOutFile = new VerifyingFile(true);
                    //we must add an entry for this in IncompleteFileManager
                    incompleteFileManager.
                                   addEntry(incompleteFile,commonOutFile);
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
                Iterator iter=commonOutFile.getFreeBlocks(rfd.getSize());
                while (iter.hasNext())
                    addToNeeded((Interval)iter.next());
            }
        }

        //The locations that were busy, for trying later.
        busy=new LinkedList();
        int size = -1;
        int connectTo = -1;
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
                        debug("MANAGER: terminating because of stop");
                        throw new InterruptedException();
                    } else if (dloaders.size()==0 && needed.isEmpty()) {
                        // Verify the commonOutFile is all done.
                        int doneSize =
                            (int)incompleteFileManager.getCompletedSize(
                                incompleteFile);
                        Assert.that(
                            !commonOutFile.getFreeBlocks(doneSize).hasNext(),
                            "file is incomplete, but needed.isEmpty()" );
                            
                        //Finished. Interrupt all worker threads
                        for(int i=threads.size();i>0;i--) {
                            Thread t = (Thread)threads.get(i-1);
                            t.interrupt();
                        }
                        
                        debug("MANAGER: terminating because of completion");
                        return COMPLETE;
                    } else if (threads.size()==0
                               && files.size()==0) {
                        //No downloaders worth living for.
                        if (busy.size()>0) {
                            debug("MANAGER: terminating with busy");
                            files.addAll(busy);
                            return WAITING_FOR_RETRY;
                        } else {
                            debug("MANAGER: terminating w/o hope");
                            return GAVE_UP;
                        }
                    }
                    size = files.size();
                    connectTo = getNumAllowedDownloads();
                }
            }
        
            //OK. We are going to create a thread for each RFD, 
            for(int i=0; i<connectTo && i<size; i++) {
                final RemoteFileDesc rfd = removeBest(files);
                Thread connectCreator = new Thread() {
                    public void run() {
                        boolean iterate = false;
                        try {
                            iterate = connectAndDownload(rfd);
                        } catch (Throwable e) {
                            //This is a "firewall" for reporting unhandled
                            //errors.  We don't really try to recover at this
                            //point, but we do attempt to display the error in
                            //the GUI for debugging purposes.
                            ErrorService.error(e);
                        } finally {
                            synchronized (ManagedDownloader.this) { 
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
    
        //Note: http11 is true or false depending on what we think thevalue
        //should be for rfd is at the start, before connecting. We may later
        //find that the we are wrong, in which case we update the rfd's http11
        //value. But while we are in connectAndDownload we continue to use this
        //local variable because the code is incapable of handling a change in
        //http11 status while inside connectAndDownload.
        boolean http11 = true;//must enter the loop
        
        while(http11) {
            //Step 2. OK. Wr have established TCP Connection. This 
            //downloader should choose a part of the file to download
            //and send the appropriate HTTP hearders
            //Note: 0=disconnected,1=tcp-connected,2=http-connected
            boolean wasQueued = false;
            int connected;
            http11 = rfd.isHTTP11();
            while(true) { //while queued, connect and sleep if we queued
                int[] a = {-1};//reset the sleep value
                connected = assignAndRequest(dloader,a,http11);
                
                //an uploader we want to stay connected with
                if(connected == 4)
                    continue; // and has partial ranges
                if(connected!=1)
                    break;
                if(!wasQueued) {
                    synchronized(this) {queuedCount++;}
                    wasQueued = true;
                }
                try {
                    if(a[0] > 0)
                        Thread.sleep(a[0]);//value from QueuedException
                } catch (InterruptedException ix) {
                    debug("worker: interrupted while asleep in queue" +
                          dloader);
                    synchronized(this) { queuedCount--; }
                    dloader.stop();//close connection
                    // notifying will make no diff, coz the next iteration
                    // will throw interrupted exception.
                    return true;
                }
            }
            if(wasQueued) //we have been given a slot, after being queued
                synchronized(this) { queuedCount--; }

            //Now, connected is either 0 or 2
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
        
        //came out of the while loop, http1.0 spawn a thread
        return true;
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

        File incompleteFile = incompleteFileManager.getFile(rfd);
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
                && getState()!=HASHING && getState()!=SAVING && queuedCount==0)
                setState(CONNECTING, 
                         needsPush ? PUSH_CONNECT_TIME : NORMAL_CONNECT_TIME);
        }

        debug("WORKER: attempting connect to "
              + rfd.getHost() + ":" + rfd.getPort());        
        
        if(RECORD_STATS)
            DownloadStat.CONNECTION_ATTEMPTS.incrementStat();

        // for multicast replies, try pushes first
        // and then try direct connects.
        // this is because newer clients work better with pushes,
        // but older ones didn't understand them
        if( rfd.isReplyToMulticast() ) {
            try {
                ret = connectWithPush(rfd, incompleteFile);
            } catch(IOException e) {
                try {
                    ret = connectDirectly(rfd, incompleteFile);
                } catch(IOException e2) {
                    informMesh(rfd,false);
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
                ret = connectDirectly(rfd, incompleteFile);
                return ret;
            } catch(IOException e) {
                informMesh(rfd, false);
                // oh well, fall through to the push.
            }
        }
        
        try {
            ret = connectWithPush(rfd, incompleteFile);
            return ret;
        } catch(IOException e) {
            // even the push failed :(
        }
        informMesh(rfd, false);
        // if we're here, everything failed.
        return null;
    }
        

    /**
     * Attempts to directly connect through TCP to the remote end.
     */
    private HTTPDownloader connectDirectly(RemoteFileDesc rfd, 
      File incompleteFile) throws IOException {
        HTTPDownloader ret;
        //Establish normal downloader.              
        ret = new HTTPDownloader(rfd, incompleteFile);
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
      File incompleteFile) throws IOException {
      
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
        ret = new HTTPDownloader(pushSocket, rfd, incompleteFile);
        
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
     * @param refSleepTime this parameter is used for pass by reference, this
     * method puts the minPollTime as the 0th element of this array.
     * @return 0 if (the server is not giving us the file)
     * TryAgainLater, FileNotFound, NotSharing, Stopped, Misc IOE
     * otherwise if queued return 1
     * otherwise if connected successfully return 2
     * otherwise if NoSuchElement( we have no areas to steal) return 3
     * otherwise if rfd was partial uploader and gave us ranges to try return 4
     */
    private int assignAndRequest(HTTPDownloader dloader,int[] refSleepTime, 
                                                              boolean http11) {
        synchronized(stealLock) {
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
                debug("nsex thrown in assingAndRequest "+dloader);
                synchronized(this) {
                    // Add to files, keep checking for stalled uploader with
                    // this rfd
                    files.add(dloader.getRemoteFileDesc());
                }
                return 3;
            } catch (NoSuchRangeException nsrx) { 
                if(RECORD_STATS)
                    DownloadStat.NSR_EXCEPTION.incrementStat();
                debug("nsrx thrown in assignAndRequest"+dloader);
                synchronized(this) {
                    RemoteFileDesc rfd = dloader.getRemoteFileDesc();
                    //forget the ranges we are pretending uploader is busy.
                    rfd.setAvailableRanges(null);
                    busy.add(rfd);
                }
                return 0;                
            } catch(TryAgainLaterException talx) {
                if(RECORD_STATS)
                    DownloadStat.TAL_EXCEPTION.incrementStat();
                debug("talx thrown in assignAndRequest"+dloader);
                synchronized(this) {
                    busy.add(dloader.getRemoteFileDesc());//try this rfd later
                }
                return 0;
            } catch(RangeNotAvailableException rnae) {
                if(RECORD_STATS)
                    DownloadStat.RNA_EXCEPTION.incrementStat();
                debug("rnae thrown in assignAndRequest"+dloader);
                informMesh(dloader.getRemoteFileDesc(), true);
                return 4; //no need to add to files or busy we keep iterating
            } catch (FileNotFoundException fnfx) {
                if(RECORD_STATS)
                    DownloadStat.FNF_EXCEPTION.incrementStat();
                debug("fnfx thrown in assignAndRequest "+dloader);
                informMesh(dloader.getRemoteFileDesc(),false);
                return 0;//discard the rfd of dloader
            } catch (NotSharingException nsx) {
                if(RECORD_STATS)
                    DownloadStat.NS_EXCEPTION.incrementStat();
                debug("nsx thrown in assignAndRequest "+dloader);
                informMesh(dloader.getRemoteFileDesc(),false);
                return 0;//discard the rfd of dloader
            } catch (QueuedException qx) { 
                if(RECORD_STATS)
                    DownloadStat.Q_EXCEPTION.incrementStat();
                debug("queuedEx thrown in AssignAndRequest sleeping.."+dloader);
                //The extra time to sleep can be tuned. For now it's 1 S.
                refSleepTime[0] = qx.getMinPollTime()*/*S->mS*/1000+1000;
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
                return 1;
            } catch(ProblemReadingHeaderException prhe) {
                if(RECORD_STATS)
                    DownloadStat.PRH_EXCEPTION.incrementStat();
                debug("prhe thrown in assignAndRequest "+dloader);
                informMesh(dloader.getRemoteFileDesc(),false);
                return 0; //discard the rfd of dloader
            } catch(UnknownCodeException uce) {
                if(RECORD_STATS)
                    DownloadStat.UNKNOWN_CODE_EXCEPTION.incrementStat();
                debug("uce (" + uce.getCode() + ") thrown in assignAndRequest "
                      + dloader);
                informMesh(dloader.getRemoteFileDesc(),false);
                return 0; //discard the rfd of dloader
            } catch (IOException iox) {
                if(RECORD_STATS)
                    DownloadStat.IO_EXCEPTION.incrementStat();
                debug("iox thrown in assignAndRequest "+dloader);
                informMesh(dloader.getRemoteFileDesc(),false);
                return 0; //discard the rfd of dloader
            } finally {
                //add alternate locations, which we could have gotten from 
                //the downloader
                AlternateLocationCollection c = dloader.getAltLocsReceived();
                if(c!=null) {
                    synchronized(c) { 
                        Iterator iter = c.iterator();
                        while(iter.hasNext()) {
                            AlternateLocation al=(AlternateLocation)iter.next();
                            RemoteFileDesc rfd = al.createRemoteFileDesc
                            (dloader.getRemoteFileDesc().getSize());
                            addDownload(rfd, false);//dont cache
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
            synchronized(this) {
                setState(DOWNLOADING);
            }
            if (stopped) {
                debug("Stopped in assignAndRequest");
                updateNeeded(dloader); //give back a white area
                synchronized(this) {
                    files.add(dloader.getRemoteFileDesc());
                }
                return 0;//throw new InterruptedException();
            }
            synchronized(this) {
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
     * Attempts to add the given location to this.  If the location
     * is accepted for future swarming, this returns true.  Otherwise,
     * this returns false.
     */
    private void addAlternateLocation(AlternateLocation alt) {
        // create the collection with the SHA1 of this alt if it hasn't
        // been created yet.
        if(validAlts == null)
            validAlts =
            AlternateLocationCollection.create(alt.getSHA1Urn());
                
        boolean added = validAlts.add(alt);
        
        // if the location didn't accept the new one.
        if(!added)
            DownloadStat.ALTERNATE_NOT_ADDED.incrementStat();
        
        // everything worked, add it (without caching)
        if( RECORD_STATS )
            DownloadStat.ALTERNATE_COLLECTED.incrementStat();
    }
	
	/**
	 * Returns the number of alternate locations that this download is using.
	 */
	public int getNumberOfAlternateLocations() {
	    if ( validAlts == null ) return 0;
	    return validAlts.getAltLocsSize();
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
        debug("WORKER: picking white "+interval+" to "+dloader);
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
        //to confusing, too many problems, etc...
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
                HTTPDownloader h=(HTTPDownloader)iter.next();
                if (biggest==null 
                    || h.getAmountToRead()>biggest.getAmountToRead())
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
            float bandwidth = -1;//initialize
            try {
                bandwidth = biggest.getMeasuredBandwidth();
            } catch (InsufficientDataException ide) {
                throw new NoSuchElementException();
            }
            if(bandwidth < MIN_ACCEPTABLE_SPEED) {
                //replace (bad boy) biggest if possible
                int start=
                biggest.getInitialReadingPoint()+amountRead;
                int stop=
                biggest.getInitialReadingPoint()+biggest.getAmountToRead();
                //Note: we are not interested in being queued at this point this
                //line could throw a bunch of exceptions (not queuedException)
                dloader.connectHTTP(getOverlapOffset(start), stop, false);
                dloader.stopAt(stop);
                debug("WORKER: picking stolen grey "
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
            debug("WORKER: assigning split grey "
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
        debug("WORKER: about to start downloading "+downloader);
        boolean problem = false;
        try {
            downloader.doDownload(commonOutFile);
            if(RECORD_STATS) {
                if(http11)
                    DownloadStat.SUCCESFULL_HTTP11.incrementStat();
                else
                    DownloadStat.SUCCESFULL_HTTP10.incrementStat();
            }
            return true;
        } catch (IOException e) {
            if(RECORD_STATS) {
                if(http11)
                    DownloadStat.FAILED_HTTP11.incrementStat();
                else
                    DownloadStat.FAILED_HTTP10.incrementStat();
             }
            problem = true;
            informMesh(downloader.getRemoteFileDesc(),false);
			chatList.removeHost(downloader);
            browseList.removeHost(downloader);
            return false;
            //e.printStackTrace();
        }
        finally {
            int stop=downloader.getInitialReadingPoint()
                        +downloader.getAmountRead();
            debug("    WORKER: terminating from "+downloader+" at "+stop+ 
                  " error? "+problem);
            //In order to reuse this location again, we need to know the
            //RemoteFileDesc.  TODO2: use measured speed if possible.
            RemoteFileDesc rfd=downloader.getRemoteFileDesc();            
            synchronized (this) {
                if (problem) {
                    updateNeeded(downloader);
                    downloader.stop();
                }
                
                dloaders.remove(downloader);
                //no need to add http11 dloader to files                
                if(!problem && !http11)
                    files.add(rfd);
                if(!problem && rfd.isAltLocCapable()) {
                    AlternateLocation loc=null;
                    try {
                        loc = AlternateLocation.create(rfd);
                    } catch (Exception e) {}
                    if(loc!=null) {
                        informMesh(rfd,true);
                        FileDesc[] descs = 
                        fileManager.getIncompleteFileDescriptors();
                        //Sumeet:TODO1:get from FileManager rather than iterate
                        for(int i=0; i<descs.length; i++) {
                            if(this.incompleteFile.equals(descs[i].getFile()))
                                descs[i].add(loc);
                        }
                        //Sumeet:TODO1:IncompleteFileDesc and this should share
                        //a AlternateLocationCollection
                        addAlternateLocation(loc);
                    }
                }
            }
        }
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
            debug("Updating needed. Adding interval "+in+" from "+dloader);
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
        int capacity=SettingsManager.instance().getConnectionSpeed();
        if (capacity<=SpeedConstants.MODEM_SPEED_INT)
            //Modems get 2 hosts at most...be safe and return positives
            return Math.max(2-downloads,0);
        else if (capacity<=SpeedConstants.T1_SPEED_INT)
            //DSL, Cable, and "T1" can swarm from up to 6 locations.
            return Math.max(6-downloads,0);
        else 
            //Wow you are fast, try 8
            return Math.max(8-downloads,0);
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
        //  Primary key: whether the file has a hash (avoid corruptions)
        //  Secondary key: the quality of the results (avoid dud locations)
        //  Ternary key: the speed of the result (avoid slow downloads)
        while (iter.hasNext()) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            //rfd.hash > ret.hash?
            if (rfd.getSHA1Urn()!=null && ret.getSHA1Urn()==null)
                ret=rfd;
            else if ((rfd.getSHA1Urn()==null) == (ret.getSHA1Urn()==null)) {
                //rfd.quality > ret.quality?
                if (rfd.getQuality() > ret.getQuality())
                    ret=rfd;
                else if (rfd.getQuality() == ret.getQuality()) {
                    //rfd.speed > ret.speed?
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
        if (rfd.isPrivate())
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
    private synchronized LimeXMLDocument[] getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?  How about only those in this
        //bucket?
        LimeXMLDocument[] retArray = null;
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

        if (allDocs.size() > 0) {
			retArray = (LimeXMLDocument[])allDocs.toArray(new LimeXMLDocument[0]);
        }
        else
            retArray = null;

        return retArray;
    }

    private void cleanup() {
        miniRFDToLock.clear();
        threadLockToSocket.clear();
        if(needed != null) //it's null while before we try first bucket
            needed.clear();
        busy = null;
        files = null;
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
        return dloaders.size() + queuedCount;
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
     *  wake up the tryAllDownloads thread with a release().  
     *  WARNING:  THIS IS VERY SPECIFIC SYNCHRONIZATION.  IT WAS NOT MEANT TO 
     *  WORK WITH MORE THAN ONE PRODUCER OR ONE CONSUMER.
     */
    private class RequeryLock extends Object {
        private boolean shouldWait = true;
        public synchronized void release() {
            shouldWait = false;
            this.notifyAll();
        }

        public synchronized void lock(long waitTime) 
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
        }

    }

    private final boolean debugOn = true;
    private final boolean log = true;
    PrintWriter writer = null;
    private final void debug(String out) {
        if (debugOn) {
            if(log) {
                if(writer== null) {
                    try {
                        writer=new 
                        PrintWriter(new FileOutputStream("log.log",true));
                    }catch (IOException ioe) {
                        System.out.println("could not create log file");
                    }
                }
                writer.println(out);
                writer.flush();
            }
            else
                System.out.println(out);
        }
    }
    private final void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }
}
