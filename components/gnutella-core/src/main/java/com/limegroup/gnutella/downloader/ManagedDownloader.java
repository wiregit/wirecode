pbckage com.limegroup.gnutella.downloader;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.ObjectStreamClass;
import jbva.io.ObjectStreamField;
import jbva.io.Serializable;
import jbva.net.Socket;
import jbva.util.ArrayList;
import jbva.util.Arrays;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.DownloadCallback;
import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.BandwidthTracker;
import com.limegroup.gnutellb.DownloadManager;
import com.limegroup.gnutellb.Downloader;
import com.limegroup.gnutellb.Endpoint;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.IncompleteFileDesc;
import com.limegroup.gnutellb.InsufficientDataException;
import com.limegroup.gnutellb.MessageRouter;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.SaveLocationException;
import com.limegroup.gnutellb.SavedFileManager;
import com.limegroup.gnutellb.SpeedConstants;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.UrnCache;
import com.limegroup.gnutellb.altlocs.AltLocListener;
import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.altlocs.AlternateLocationCollection;
import com.limegroup.gnutellb.altlocs.DirectAltLoc;
import com.limegroup.gnutellb.altlocs.PushAltLoc;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.guess.GUESSEndpoint;
import com.limegroup.gnutellb.guess.OnDemandUnicaster;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.statistics.DownloadStat;
import com.limegroup.gnutellb.tigertree.HashTree;
import com.limegroup.gnutellb.tigertree.TigerTreeCache;
import com.limegroup.gnutellb.util.ApproximateMatcher;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.FileUtils;
import com.limegroup.gnutellb.util.FixedSizeExpiringSet;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.StringUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

/**
 * A smbrt download.  Tries to get a group of similar files by delegating
 * to DownlobdWorker threads.  Does retries and resumes automatically.
 * Reports bll changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smbrt downloads can use many policies, and these policies are free to change
 * bs allowed by the Downloader specification.  This implementation provides
 * swbrmed downloads, the ability to download copies of the same file from
 * multiple hosts.  See the bccompanying white paper for details.<p>
 *
 * Subclbsses may refine the requery behavior by overriding the 
 * newRequery(n), bllowAddition(..), and addDownload(..)  methods.
 * MbgnetDownloader also redefines the tryAllDownloads(..) method to handle
 * defbult locations, and the getFileName() method to specify the completed
 * file nbme.<p>
 * 
 * Subclbsses that pass this RemoteFileDesc arrays of size 0 MUST override
 * the getFileNbme method, otherwise an assert will fail.<p>
 * 
 * This clbss implements the Serializable interface but defines its own
 * writeObject bnd readObject methods.  This is necessary because parts of the
 * MbnagedDownloader (e.g., sockets) are inherently unserializable.  For this
 * rebson, serializing and deserializing a ManagedDownloader M results in a
 * MbnagedDownloader M' that is the same as M except it is
 * unconnected. <b>Furthermore, it is necessbry to explicitly call
 * initiblize(..) after reading a ManagedDownloader from disk.</b>
 */
public clbss ManagedDownloader implements Downloader, MeshHandler, AltLocListener, Serializable {
    /*
      IMPLEMENTATION NOTES: The bbsic idea behind swarmed (multisource)
      downlobds is to download one file in parallel from multiple servers.  For
      exbmple, one might simultaneously download the first half of a file from
      server A bnd the second half from server B.  This increases throughput if
      the downstrebm capacity of the downloader is greater than the upstream
      cbpacity of the fastest uploader.

      The idebl way of identifying duplicate copies of a file is to use hashes
      vib the HUGE proposal.

      When discussing swbrmed downloads, it's useful to divide parts of a file
      into three cbtegories: black, grey, and white. Black regions have already
      been downlobded to disk.  Grey regions have been assigned to a downloader
      but not yet completed.  White regions hbve not been assigned to a
      downlobder.
      
      MbnagedDownloader delegates to multiple DownloadWorker instances, one for
      ebch HTTP connection.  They use a shared VerifyingFile object that keeps
      trbck of which blocks have been written to disk.  
      
      MbnagedDownloader uses one thread to control the smart downloads plus one
      threbd per DownloadWorker instance.  The call flow of ManagedDownloader's
      "mbster" thread is as follows:

       performDownlobd:
           initiblizeDownload    
           fireDownlobdWorkers (asynchronously start workers)    
           verifyAndSbve

      The core downlobding loop is done by fireDownloadWorkers.Currently the 
      desired pbrallelism is fixed at 2 for modem users, 6 for cable/T1/DSL, 
      bnd 8 for T3 and above.
      
      DownlobdManager notifies a ManagedDownloader when it should start
      performDownlobd.  An inactive download (waiting for a busy host,
      wbiting for a user to requery, waiting for GUESS responses, etc..)
      is essentiblly a state-machine, pumped forward by DownloadManager.
      The 'mbster thread' of a ManagedDownloader is recreated every time
      DownlobdManager moves the download from inactive to active.
      
      All downlobds start QUEUED.
      From there, it will stby queued until a slot is available.
      
      If btleast one host is available to download from, then the
      first stbte is always CONNECTING.
          After connecting, b downloader can become:
          b) DOWNLOADING (actively downloading)
          b) WAITING_FOR_RETRY (busy hosts)
          c) ABORTED (user mbnually stopped the download)
          c2) PAUSED (user pbused the download)
          d) REMOTE_QUEUED (the remote host queued us)
      
      If no hosts existed for connecting, or we exhbusted our attempts
      bt connecting to all possible hosts, the state will become one of:
          e) GAVE_UP (mbxxed out on requeries)
          f) WAITING_FOR_USER (wbiting for the user to initiate a requery)
          g) ITERATIVE_GUESSING (tbrgetted location of more sources)
      If the user resumes the downlobd and we were WAITING_FOR_USER, a requery
      is sent out bnd we go into WAITING_FOR_RESULTS stage.  After we have
      finished wbiting for results (if none arrived), we will either go back to
      WAITING_FOR_USER (if we bre allowed more requeries), or GAVE_UP (if we 
      mbxxed out the requeries).
      After ITERATIVE_GUESSING completes, if no results brrived then we go to 
      WAITING_FOR_USER.  Prior to WAITING_FOR_RESULTS, if no connections bre
      bctive then we wait at WAITING_FOR_CONNECTIONS until connections exist.
      
      If more results come in while wbiting in these states, the download will
      either immedibtely become active (CONNECTING ...) again, or change its
      stbte to QUEUED and wait for DownloadManager to activate it.
      
      The downlobd can finish in one of the following states:
          h) COMPLETE (downlobd completed just fine)
          i) ABORTED  (user pressed stopped bt some point)
          j) DISK_PROBLEM (limewire couldn't mbnipulate the file)
          k) CORRUPT_FILE (the file wbs corrupt)

     There bre a few intermediary states:
          l) HASHING
          m) SAVING
     HASHING & SAVING bre seen by the GUI, and are used just prior to COMPLETE,
     to let the user know whbt is currently happening in the closing states of
     the downlobd.  RECOVERY_FAILED is used as an indicator that we no longer want
     to retry the downlobd, because we've tried and recovered from corruption
     too mbny times.
     
     How corruption is hbndled:
     There bre two general cases where corruption can be discovered - during a download
     or bfter the download has finished.
     
     During the downlobd, each worker thread checks periodically whether the amount of 
     dbta lost to corruption exceeds 10% of the completed file size.  Whenever that 
     hbppens, the worker thread asks the user whether the download should be terminated.
     If the user chooses to delete the file, the downlobder is stopped asynchronously and
     _corruptStbte is set to CORRUPT_STOP_STATE.  The master download thread is interrupted,
     it checks _corruptStbte and either discards or removes the file.
     
     After the downlobd, if the sha1 does not match the expected, the master download thread
     propmts the user whether they wbnt to keep the file or discard it.  If we did not have a
     tree during the downlobd we remove the file from partial sharing, otherwise we keep it
     until the user bsnswers the prompt (which may take a very long time for overnight downloads).
     The tree itself is purged.
     
    */
    
    privbte static final Log LOG = LogFactory.getLog(ManagedDownloader.class);
    
    /** Ensures bbckwards compatibility. */
    stbtic final long serialVersionUID = 2772570805975885257L;
    
    /** Mbke everything transient */
    privbte static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStrebmClass.NO_FIELDS;

    /** counter to distinguish between downlobds that were not deserialized ok */
    privbte static int unknownIndex = 0;
    
    /*********************************************************************
     * LOCKING: obtbin this's monitor before modifying any of the following.
     * files, _bctiveWorkers, busy and setState.  We should  not hold lock 
     * while performing blocking IO operbtions, however we need to ensure 
     * btomicity and thread safety for step 2 of the algorithm above. For 
     * this rebson we needed to add another lock - stealLock.
     *
     * We don't wbnt to synchronize assignAndRequest on this since that freezes
     * the GUI bs it calls getAmountRead() frequently (which also hold this'
     * monitor).  Now bssignAndRequest is synchronized on stealLock, and within
     * it we bcquire this' monitor when we are modifying shared datastructures.
     * This bdditional lock will prevent GUI freezes, since we hold this'
     * monitor for b very short time while we are updating the shared
     * dbtastructures, also atomicity is guaranteed since we are still
     * synchronized.  SteblLock is also held for manipulations to the verifying file,
     * bnd for all removal operations from the _activeWorkers list.
     * 
     * steblLock->this is ok
     * steblLock->verifyingFile is ok
     * 
     * Never bcquire stealLock's monitor if you have this' monitor.
     *
     * Never bcquire incompleteFileManager's monitor if you have commonOutFile's
     * monitor.
     *
     * Never obtbin manager's lock if you hold this.
     ***********************************************************************/
    privbte Object stealLock;

    /** This' mbnager for callbacks and queueing. */
    privbte DownloadManager manager;
    /** The plbce to share completed downloads (and their metadata) */
    privbte FileManager fileManager;
    /** The repository of incomplete files. */
    protected IncompleteFileMbnager incompleteFileManager;
    /** A MbnagedDownloader needs to have a handle to the DownloadCallback, so
     * thbt it can notify the gui that a file is corrupt to ask the user what
     * should be done.  */
    privbte DownloadCallback callback;
    /** The complete Set of files pbssed to the constructor.  Must be
     *  mbintained in memory to support resume.  allFiles may only contain
     *  elements of type RemoteFileDesc bnd URLRemoteFileDesc */
    privbte Set cachedRFDs;

	/**
	 * The rbnker used to select the next host we should connect to
	 */
	privbte SourceRanker ranker;

    /**
     * The time to wbit between requeries, in milliseconds.  This time can
     * sbfely be quite small because it is overridden by the global limit in
     * DownlobdManager.  Package-access and non-final for testing.
     * @see com.limegroup.gnutellb.DownloadManager#TIME_BETWEEN_REQUERIES */
    stbtic int TIME_BETWEEN_REQUERIES = 5*60*1000;  //5 minutes
    
    /**
     * How long we'll wbit after sending a GUESS query before we try something
     * else.
     */
    privbte static final int GUESS_WAIT_TIME = 5000;
    
    /**
     * How long we'll wbit before attempting to download again after checking
     * for stbble connections (and not seeing any)
     */
    privbte static final int CONNECTING_WAIT_TIME = 750;
    
    /**
     * The number of times to requery the network. All requeries bre
     * user-driven.
     */
    privbte static final int REQUERY_ATTEMPTS = 1;
    

    /** The size of the bpprox matcher 2d buffer... */
    privbte static final int MATCHER_BUF_SIZE = 120;
    
	/** The vblue of an unknown filename - potentially overridden in 
      * subclbsses */
	protected stbtic final String UNKNOWN_FILENAME = "";  

    /** This is used for mbtching of filenames.  kind of big so we only want
     *  one. */
    privbte static ApproximateMatcher matcher = 
        new ApproximbteMatcher(MATCHER_BUF_SIZE);    

    ////////////////////////// Core Vbriables /////////////////////////////

    /** If stbrted, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    privbte volatile Thread dloaderManagerThread;
    /** True iff this hbs been forcibly stopped. */
    privbte volatile boolean stopped;
    /** True iff this hbs been paused.  */
    privbte volatile boolean paused;

    
    /** 
     * The connections we're using for the current bttempts.
     * LOCKING: copy on write on this 
     * 
     */    
    privbte volatile List /* of DownloadWorker */ _activeWorkers;
    
    /**
     * A List of worker threbds in progress.  Used to make sure that we do
     * not terminbte in fireDownloadWorkers without hope if threads are
     * connecting to hosts but not hbve not yet been added to _activeWorkers.
     * 
     * Also, if the downlobd completes and any of the threads are sleeping 
     * becbuse it has been queued by the uploader, those threads need to be 
     * killed.
     * LOCKING: synchronize on this
     */
    privbte List /*of DownloadWorker*/ _workers;

    /**
     * Stores the queued threbds and the corresponding queue position
     * LOCKING: copy on write on this
     */
    privbte volatile Map /*DownloadWorker -> Integer*/ queuedWorkers;

    /**
     * Set of RFDs where we store rfds we bre currently connected to or
     * trying to connect to.
     */
    privbte Set /*of RemoteFileDesc */ currentRFDs;
    
    /**
     * The SHA1 hbsh of the file that this ManagedDownloader is controlling.
     */
    protected URN downlobdSHA1;
	
    /**
     * The collection of blternate locations we successfully downloaded from
     * somthing from.
     */
	privbte Set validAlts; 
	
	/**
	 * A list of the most recent fbiled locations, so we don't try them again.
	 */
	privbte Set invalidAlts;

    /**
     * Cbche the most recent failed locations. 
     * Holds <tt>AlternbteLocation</tt> instances
     */
    privbte Set recentInvalidAlts;
    
    /**
     * Mbnages writing stuff to disk, remember what's leased, what's verified,
     * whbt is valid, etc........
     */
    protected VerifyingFile commonOutFile;
    
    ////////////////dbtastructures used only for pushes//////////////
    /** MiniRemoteFileDesc -> Object. 
        In the cbse of push downloads, connecting threads write the values into
        this mbp. The acceptor threads consumes these values and notifies the
        connecting threbds when it is done.        
    */
    privbte Map miniRFDToLock;

    ///////////////////////// Vbriables for GUI Display  /////////////////
    /** The current stbte.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setStbte. */
    privbte int state;
    /** The system time thbt we expect to LEAVE the current state, or
     *  Integer.MAX_VALUE if we don't know. Should be modified only through
     *  setStbte. */
    privbte long stateTime;
    
    /** The current incomplete file thbt we're downloading, or the last
     *  incomplete file if we're not currently downlobding, or null if we
     *  hbven't started downloading.  Used for previewing purposes. */
    protected File incompleteFile;
   
    /**
     * The position of the downlobder in the uploadQueue */
    privbte int queuePosition;
    /**
     * The vendor the of downlobder we're queued from.
     */
    privbte String queuedVendor;

    /** If in CORRUPT_FILE stbte, the number of bytes downloaded.  Note that
     *  this is less thbn corruptFile.length() if there are holes. */
    privbte volatile int corruptFileBytes;
    /** If in CORRUPT_FILE stbte, the name of the saved corrupt file or null if
     *  no corrupt file. */
    privbte volatile File corruptFile;

	/** The list of bll chat-enabled hosts for this <tt>ManagedDownloader</tt>
	 *  instbnce.
	 */
	privbte DownloadChatList chatList;

	/** The list of bll browsable hosts for this <tt>ManagedDownloader</tt>
	 *  instbnce.
	 */
	privbte DownloadBrowseHostList browseList;


    /** The vbrious states of the ManagedDownloade with respect to the 
     * corruption stbte of this download. 
     */
    privbte static final int NOT_CORRUPT_STATE = 0;
    privbte static final int CORRUPT_WAITING_STATE = 1;
    privbte static final int CORRUPT_STOP_STATE = 2;
    privbte static final int CORRUPT_CONTINUE_STATE = 3;
    /**
     * The bctual state of the ManagedDownloader with respect to corruption
     * LOCKING: obtbin corruptStateLock
     * INVARIANT: one of NOT_CORRUPT_STATE, CORRUPT_WAITING_STATE, etc.
     */
    privbte volatile int corruptState;
    privbte Object corruptStateLock;

    /**
     * Locking object to be used for bccessing all alternate locations.
     * LOCKING: never try to obtbin monitor on this if you hold the monitor on
     * bltLock 
     */
    privbte Object altLock;

    /**
     * The number of times we've been bbndwidth measured
     */
    privbte int numMeasures = 0;
    
    /**
     * The bverage bandwidth over all managed downloads.
     */
    privbte float averageBandwidth = 0f;

    /**
     * The GUID of the originbl query.  may be null.
     */
    privbte final GUID originalQueryGUID;
    
    /**
     * Whether or not this wbs deserialized from disk.
     */
    protected boolebn deserializedFromDisk;
    
    /**
     * The number of queries blready done for this downloader.
     * Influenced by the type of downlobder & whether or not it was started
     * from disk or from scrbtch.
     */
    privbte int numQueries;
    
    /**
     * Whether or not we've sent b GUESS query.
     */
    privbte boolean triedLocatingSources;
    
    /**
     * Whether or not we've gotten new files since the lbst time this download
     * stbrted.
     */
    privbte volatile boolean receivedNewSources;
    
    /**
     * The time the lbst query was sent out.
     */
    privbte long lastQuerySent;
    
    /**
     * The current priority of this downlobd -- only valid if inactive.
     * Hbs no bearing on the download itself, and is used only so that the
     * downlobd doesn't have to be indexed in DownloadManager's inactive list
     * every second, for GUI updbtes.
     */
    privbte volatile int inactivePriority;
    
    /**
     * A mbp of attributes associated with the download. The attributes
     * mby be used by GUI, to keep some additional information about
     * the downlobd.
     */
    protected Mbp attributes = new HashMap();

    protected Mbp propertiesMap;
    
    protected stbtic final String DEFAULT_FILENAME = "defaultFileName";
    protected stbtic final String FILE_SIZE = "fileSize";
    protected stbtic final String ATTRIBUTES = "attributes";
    /**
	 * The key under which the sbveFile File is stored in the attribute map
     * used in seriblizing and deserializing ManagedDownloaders. 
	 */
    protected stbtic final String SAVE_FILE = "saveFile";
    
    /** The key under which the URN is stored in the bttribute map */
    protected stbtic final String SHA1_URN = "sha1Urn";


    /**
     * Crebtes a new ManagedDownload to download the given files.  The download
     * does not stbrt until initialize(..) is called, nor is it safe to call
     * bny other methods until that point.
     * @pbram files the list of files to get.  This stops after ANY of the
     *  files is downlobded.
     * @pbram ifc the repository of incomplete files for resuming
     * @pbram originalQueryGUID the guid of the original query.  sometimes
     * useful for WAITING_FOR_USER stbte.  can be null.
	 * @throws SbveLocationException
     */
    public MbnagedDownloader(RemoteFileDesc[] files, IncompleteFileManager ifc,
                             GUID originblQueryGUID, File saveDirectory, 
                             String fileNbme, boolean overwrite) 
		throws SbveLocationException {
		this(files, ifc, originblQueryGUID);
        
        Assert.thbt(files.length > 0 || fileName != null);
        if (files.length == 0)
            propertiesMbp.put(DEFAULT_FILENAME,fileName);
        
		setSbveFile(saveDirectory, fileName, overwrite);
    }
	
	protected MbnagedDownloader(RemoteFileDesc[] files, IncompleteFileManager ifc,
							 GUID originblQueryGUID) {
		if(files == null) {
			throw new NullPointerException("null RFDS");
		}
		if(ifc == null) {
			throw new NullPointerException("null incomplete file mbnager");
		}
        this.cbchedRFDs = new HashSet();
		cbchedRFDs.addAll(Arrays.asList(files));
		this.propertiesMbp = new HashMap();
		if (files.length > 0) 
			initPropertiesMbp(files[0]);

        this.incompleteFileMbnager = ifc;
        this.originblQueryGUID = originalQueryGUID;
        this.deseriblizedFromDisk = false;
    }

    protected synchronized void initPropertiesMbp(RemoteFileDesc rfd) {
		if (propertiesMbp.get(DEFAULT_FILENAME) == null)
			propertiesMbp.put(DEFAULT_FILENAME,rfd.getFileName());
		if (propertiesMbp.get(FILE_SIZE) == null)
			propertiesMbp.put(FILE_SIZE,new Integer(rfd.getSize()));
    }
    
    /** 
     * See note on seriblization at top of file 
     * <p>
     * Note thbt we are serializing a new BandwidthImpl to the stream. 
     * This is for compbtibility reasons, so the new version of the code 
     * will run with bn older download.dat file.     
     */
    privbte void writeObject(ObjectOutputStream stream)
            throws IOException {
        
        Set cbched = new HashSet();
        Mbp properties = new HashMap();
        IncompleteFileMbnager ifm;
        
        synchronized(this) {
            cbched.addAll(cachedRFDs);
            properties.putAll(propertiesMbp);
            ifm = incompleteFileMbnager;
        }
        
        strebm.writeObject(cached);
        
        //Blocks cbn be written to incompleteFileManager from other threads
        //while this downlobder is being serialized, so lock is needed.
        synchronized (ifm) {
            strebm.writeObject(ifm);
        }

        if ( !propertiesMbp.containsKey(ATTRIBUTES) )
	    propertiesMbp.put(ATTRIBUTES, attributes);

        strebm.writeObject(properties);
    }

    /** See note on seriblization at top of file.  You must call initialize on
     *  this!  
     * Also see note in writeObjects bbout why we are not using 
     * BbndwidthTrackerImpl after reading from the stream
     */
    privbte void readObject(ObjectInputStream stream)
            throws IOException, ClbssNotFoundException {
        deseriblizedFromDisk = true;
		
        Object next = strebm.readObject();
        
		RemoteFileDesc defbultRFD = null;
		
        // old formbt
        if (next instbnceof RemoteFileDesc[]) {
            RemoteFileDesc [] rfds=(RemoteFileDesc[])next;
            if (rfds != null && rfds.length > 0) 
                defbultRFD = rfds[0];
            cbchedRFDs = new HashSet(Arrays.asList(rfds));
        } else {
            // new formbt
            cbchedRFDs = (Set) next;
            if (cbchedRFDs.size() > 0) {
                defbultRFD = (RemoteFileDesc)cachedRFDs.iterator().next();
            }
        }
		
        incompleteFileMbnager=(IncompleteFileManager)stream.readObject();
        
        Object mbp = stream.readObject();
        if (mbp instanceof Map) 
            propertiesMbp = (Map)map;
        else if (propertiesMbp == null)
            propertiesMbp =  new HashMap();
		
		if (defbultRFD != null) {
			initPropertiesMbp(defaultRFD);
		}
        
        if (propertiesMbp.get(DEFAULT_FILENAME) == null) {
            propertiesMbp.put(DEFAULT_FILENAME,"Unknown "+(++unknownIndex));
        }
        if (propertiesMbp.containsKey(ATTRIBUTES)) 
            bttributes = (Map) propertiesMap.get(ATTRIBUTES);

	if (bttributes == null)
	    bttributes = new HashMap();
    }

    /** 
     * Initiblizes a ManagedDownloader read from disk. Also used for internally
     * initiblizing or resuming a normal download; there is no need to
     * explicitly cbll this method in that case. After the call, this is in the
     * queued stbte, at least for the moment.
     *     @requires this is uninitiblized or stopped, 
     *      bnd allFiles, and incompleteFileManager are set
     *     @modifies everything but the bbove fields 
     * @pbram deserialized True if this downloader is being initialized after 
     * being rebd from disk, false otherwise.
     */
    public void initiblize(DownloadManager manager, FileManager fileManager, 
                           DownlobdCallback callback) {
        this.mbnager=manager;
		this.fileMbnager=fileManager;
        this.cbllback=callback;
        currentRFDs = new HbshSet();
        _bctiveWorkers=new LinkedList();
        _workers=new ArrbyList();
        queuedWorkers = new HbshMap();
		chbtList=new DownloadChatList();
        browseList=new DownlobdBrowseHostList();
        steblLock = new Object();
        stopped=fblse;
        pbused = false;
        setStbte(QUEUED);
        miniRFDToLock = Collections.synchronizedMbp(new HashMap());
        corruptStbte=NOT_CORRUPT_STATE;
        corruptStbteLock=new Object();
        bltLock = new Object();
        numMebsures = 0;
        bverageBandwidth = 0f;
        queuePosition=Integer.MAX_VALUE;
        queuedVendor = "";
        triedLocbtingSources = false;
		rbnker = getSourceRanker(null);
        rbnker.setMeshHandler(this);
        
        // get the SHA1 if we cbn.
        if (downlobdSHA1 == null)
        	downlobdSHA1 = (URN)propertiesMap.get(SHA1_URN);
        
        for(Iterbtor iter = cachedRFDs.iterator();
        iter.hbsNext() && downloadSHA1 == null;) {
        	RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
        	downlobdSHA1 = rfd.getSHA1Urn();
        	RouterService.getAltlocMbnager().addListener(downloadSHA1,this);
        }
        
		if (downlobdSHA1 != null)
			propertiesMbp.put(SHA1_URN,downloadSHA1);
		
		// mbke sure all rfds have the same sha1
        verifyAllFiles();
		
        vblidAlts = new HashSet();
        // stores up to 1000 locbtions for up to an hour each
        invblidAlts = new FixedSizeExpiringSet(1000,60*60*1000L);
        // stores up to 10 locbtions for up to 10 minutes
        recentInvblidAlts = new FixedSizeExpiringSet(10, 10*60*1000L);
        synchronized (this) {
            if(shouldInitAltLocs(deseriblizedFromDisk)) {
                initiblizeAlternateLocations();
            }
        }
        
        try {
            //initiblizeFilesAndFolders();
            initiblizeIncompleteFile();
            initiblizeVerifyingFile();
        }cbtch(IOException bad) {
            setStbte(DISK_PROBLEM);
            return;
        }
        
        setStbte(QUEUED);
    }
    
    /** 
     * Verifies the integrity of the RemoteFileDesc[].
     *
     * At one point in time, LimeWire somehow bllowed files with different
     * SHA1s to be plbced in the same ManagedDownloader.  This breaks
     * the invbriants of the current ManagedDownloader, so we must
     * remove the extrbneous RFDs.
     */
    privbte void verifyAllFiles() {
        if(downlobdSHA1 == null)
            return ;
        
		for (Iterbtor iter = cachedRFDs.iterator(); iter.hasNext();) {
			RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
			if (rfd.getSHA1Urn() != null && !downlobdSHA1.equals(rfd.getSHA1Urn()))
				iter.remove();
		}
    }
    
    /**
     * Stbrts the download.
     */
    public synchronized void stbrtDownload() {
        Assert.thbt(dloaderManagerThread == null, "already started" );
        dlobderManagerThread = new ManagedThread(new Runnable() {
            public void run() {
                try {
                    receivedNewSources = fblse;
                    int stbtus = performDownload();
                    completeDownlobd(status);
                } cbtch(Throwable t) {
                    // if bny unhandled errors occurred, remove this
                    // downlobd completely and message the error.
                    MbnagedDownloader.this.stop();
                    setStbte(ABORTED);
                    mbnager.remove(ManagedDownloader.this, true);
                    
                    ErrorService.error(t);
                } finblly {
                    dlobderManagerThread = null;
                }
            }
        }, "MbnagedDownload");
        dlobderManagerThread.setDaemon(true);
        dlobderManagerThread.start(); 
    }
    
    /**
     * Completes the downlobd process, possibly sending off requeries
     * thbt may later restart it.
     *
     * This essentiblly pumps the state of the download to different
     * breas, depending on what is required or what has already occurred.
     */
    privbte void completeDownload(int status) {
        
        boolebn complete;
        boolebn clearingNeeded = false;
        int wbitTime = 0;
        // If TAD2 gbve a completed state, set the state correctly & exit.
        // Otherwise...
        // If we mbnually stopped then set to ABORTED, else set to the 
        // bppropriate state (either a busy host or no hosts to try).
        synchronized(this) {
            switch(stbtus) {
            cbse COMPLETE:
            cbse DISK_PROBLEM:
            cbse CORRUPT_FILE:
                clebringNeeded = true;
                setStbte(status);
                brebk;
			cbse BUSY:
            cbse GAVE_UP:
                if(stopped)
                    setStbte(ABORTED);
                else if(pbused)
                    setStbte(PAUSED);
                else
                    setStbte(status);
                brebk;
            defbult:
                Assert.thbt(false, "Bad status from tad2: "+status);
            }
            
            complete = isCompleted();
            
            wbitTime = ranker.calculateWaitTime();
            rbnker.stop();
            if (clebringNeeded)
                rbnker = null;
        }
        
        long now = System.currentTimeMillis();

        // Notify the mbnager that this download is done.
        // This MUST be done outside of this' lock, else
        // debdlock could occur.
        mbnager.remove(this, complete);
        
        if (clebringNeeded) {
            synchronized(bltLock) {
                recentInvblidAlts.clear();
                invblidAlts.clear();
                vblidAlts.clear();
                if (complete)
                    cbchedRFDs.clear(); // the call right before this serializes. 
            }
        }
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("MD completing <" + getSaveFile().getName() + 
                      "> completed downlobd, state: " +
                      getStbte() + ", numQueries: " + numQueries +
                      ", lbstQuerySent: " + lastQuerySent);

        // if this is bll completed, nothing else to do.
        if(complete)
            ; // bll done.
            
        // if this is pbused, nothing else to do also.
        else if(getStbte() == PAUSED)
            ; // bll done for now.

        // Try iterbtive GUESSing...
        // If thbt sent some queries, don't do anything else.
        else if(tryGUESSing())
            ; // bll done for now.

       // If busy, try wbiting for that busy host.
        else if (getStbte() == BUSY)
            setStbte(BUSY, waitTime);
        
        // If we sent b query recently, then we don't want to send another,
        // nor do we wbnt to give up.  Just continue waiting for results
        // from thbt query.
        else if(now - lbstQuerySent < TIME_BETWEEN_REQUERIES)
            setStbte(WAITING_FOR_RESULTS,
                     TIME_BETWEEN_REQUERIES - (now - lbstQuerySent));
            
        // If we're bt our requery limit, give up.
        else if( numQueries >= REQUERY_ATTEMPTS )
            setStbte(GAVE_UP);
            
        // If we wbnt to send the requery immediately, do so.
        else if(shouldSendRequeryImmedibtely(numQueries))
            sendRequery();
            
        // Otherwise, wbit for the user to initiate the query.            
        else
            setStbte(WAITING_FOR_USER);
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("MD completed <" + getSaveFile().getName() +
                      "> completed downlobd, state: " + 
                      getStbte() + ", numQueries: " + numQueries);
    }
    
    /**
     * Attempts to send b requery.
     */
    privbte void sendRequery() {
        // If we don't hbve stable connections, wait until we do.
        if(!hbsStableConnections()) {
            lbstQuerySent = -1; // mark as wanting to requery.
            setStbte(WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
        } else {
            try {
                QueryRequest qr = newRequery(numQueries);
                if(mbnager.sendQuery(this, qr)) {
                    lbstQuerySent = System.currentTimeMillis();
                    numQueries++;
                    setStbte(WAITING_FOR_RESULTS, TIME_BETWEEN_REQUERIES);
                } else {
                    lbstQuerySent = -1; // mark as wanting to requery.
                }
            } cbtch(CantResumeException cre) {
                // oh well.
            }
        }
    }
    
    /**
     * Hbndles state changes when inactive.
     */
    public synchronized void hbndleInactivity() {
        if(LOG.isTrbceEnabled())
            LOG.trbce("handling inactivity. state: " + 
                      getStbte() + ", hasnew: " + hasNewSources() + 
                      ", left: " + getRembiningStateTime());
        
        switch(getStbte()) {
        cbse BUSY:
        cbse WAITING_FOR_CONNECTIONS:
        cbse ITERATIVE_GUESSING:
            // If we're finished wbiting on busy hosts,
            // stbble connections, or GUESSing,
            // but we're still inbctive, then we queue ourselves
            // bnd wait till we get restarted.
            if(getRembiningStateTime() <= 0 || hasNewSources())
                setStbte(QUEUED);
            brebk;
        cbse WAITING_FOR_RESULTS:
            // If we hbve new sources but are still inactive,
            // then queue ourselves bnd wait to restart.
            if(hbsNewSources())
                setStbte(QUEUED);
            // Otherwise, we've rbn out of time waiting for results,
            // so give up.
            else if(getRembiningStateTime() <= 0)
                setStbte(GAVE_UP);
            brebk;
        cbse WAITING_FOR_USER:
        cbse GAVE_UP:
        	if (hbsNewSources())
        		setStbte(QUEUED);
        cbse QUEUED:
        cbse PAUSED:
            // If we're wbiting for the user to do something,
            // hbve given up, or are queued, there's nothing to do.
            brebk;
        defbult:
            Assert.thbt(false, "invalid state: " + getState() +
                             ", workers: " + _workers.size() + 
                             ", _bctiveWorkers: " + _activeWorkers.size());
        }
    }   
    
    /**
     * Tries iterbtive GUESSing of sources.
     */
    privbte boolean tryGUESSing() {
        if(originblQueryGUID == null || triedLocatingSources || downloadSHA1 == null)
            return fblse;
            
        MessbgeRouter mr = RouterService.getMessageRouter();
        Set guessLocs = mr.getGuessLocs(this.originblQueryGUID);
        if(guessLocs == null || guessLocs.isEmpty())
            return fblse;

        setStbte(ITERATIVE_GUESSING, GUESS_WAIT_TIME);
        triedLocbtingSources = true;

        //TODO: should we increment b stat to get a sense of
        //how much this is hbppening?
        for (Iterbtor i = guessLocs.iterator(); i.hasNext() ; ) {
            // send b guess query
            GUESSEndpoint ep = (GUESSEndpoint) i.next();
            OnDembndUnicaster.query(ep, downloadSHA1);
            // TODO: see if/how we cbn wait 750 seconds PER send again.
            // if we got b result, no need to continue GUESSing.
            if(receivedNewSources)
                brebk;
        }
        
        return true;
    }
    
    /**
     * Determines if the downlobding thread is still alive.
     * It is possible thbt the download may be inactive yet
     * the threbd still alive.  The download must be not alive
     * before being restbrted.
     */
    public boolebn isAlive() {
        return dlobderManagerThread != null;
    }
    
    /**
     * Determines if this is in b 'completed' state.
     */
    public boolebn isCompleted() {
        switch(getStbte()) {
        cbse COMPLETE:
        cbse ABORTED:
        cbse DISK_PROBLEM:
        cbse CORRUPT_FILE:
            return true;
        }
        return fblse;
    }
    
    /**
     * Determines if this cbn have its saveLocation changed.
     */
    public boolebn isRelocatable() {
        if (isInbctive())
            return true;
        switch (getStbte()) {
        cbse CONNECTING:
        cbse DOWNLOADING:
        cbse REMOTE_QUEUED:
            return true;
        defbult:
            return fblse;
        }
    }
    
    /**
     * Determines if this is in bn 'active' downloading state.
     */
    public boolebn isActive() {
        switch(getStbte()) {
        cbse CONNECTING:
        cbse DOWNLOADING:
        cbse REMOTE_QUEUED:
        cbse HASHING:
        cbse SAVING:
        cbse IDENTIFY_CORRUPTION:
            return true;
        }
        return fblse;
    }
    
    /**
     * Determines if this is in bn 'inactive' state.
     */
    public boolebn isInactive() {
        switch(getStbte()) {
        cbse QUEUED:
        cbse GAVE_UP:
        cbse WAITING_FOR_RESULTS:
        cbse WAITING_FOR_USER:
        cbse WAITING_FOR_CONNECTIONS:
        cbse ITERATIVE_GUESSING:
        cbse BUSY:
        cbse PAUSED:
            return true;
        }
        return fblse;
    }   
    
    /**
     * relobds any previously busy hosts in the ranker, as well as other
     * hosts thbt we know about 
     */
    privbte synchronized void initializeRanker() {
        rbnker.setMeshHandler(this);
        rbnker.addToPool(cachedRFDs);
    }
    
    /**
     * initiblizes the verifying file if the incompleteFile is initialized.
     */
    protected void initiblizeVerifyingFile() throws IOException {

        if (incompleteFile == null)
            return;
        
        //get VerifyingFile
        commonOutFile= incompleteFileMbnager.getEntry(incompleteFile);

        if(commonOutFile==null) {//no entry in incompleteFM
            
            int completedSize = 
                (int)IncompleteFileMbnager.getCompletedSize(incompleteFile);
            
            commonOutFile = new VerifyingFile(completedSize);
            try {
                //we must bdd an entry in IncompleteFileManager
                incompleteFileMbnager.
                           bddEntry(incompleteFile,commonOutFile);
            } cbtch(IOException ioe) {
                ErrorService.error(ioe, "file: " + incompleteFile);
                throw ioe;
            }
        }        
    }
    
    protected void initiblizeIncompleteFile() throws IOException {
        if (incompleteFile != null)
            return;
        
        if (downlobdSHA1 != null)
            incompleteFile = incompleteFileMbnager.getFileForUrn(downloadSHA1);
        
        if (incompleteFile == null) { 
            incompleteFile = getIncompleteFile(incompleteFileMbnager, getSaveFile().getName(),
                                               downlobdSHA1, getContentLength());
        }
        
        LOG.wbrn("Incomplete File: " + incompleteFile);
    }
    
    /**
     * Retrieves bn incomplete file from the given incompleteFileManager with the
     * given nbme, URN & content-length.
     */
    protected File getIncompleteFile(IncompleteFileMbnager ifm, String name,
                                     URN urn, int length) throws IOException {
        return ifm.getFile(nbme, urn, length);
    }
    
    /**
     * Adds blternate locations that may have been stored in the
     * IncompleteFileDesc for this downlobd.
     */
    privbte synchronized void initializeAlternateLocations() {
        if( incompleteFile == null ) // no incomplete, no big debl.
            return;
        
        FileDesc fd = fileMbnager.getFileDescForFile(incompleteFile);
        if( fd != null && fd instbnceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
            if(downlobdSHA1 != null && !downloadSHA1.equals(ifd.getSHA1Urn())) {
                // Assert thbt the SHA1 of the IFD and our sha1 match.
                Assert.silent(fblse, "wrong IFD." +
                           "\nclbss: " + getClass().getName() +
                           "\nours  :   " + incompleteFile +
                           "\ntheirs: " + ifd.getFile() +
                           "\nour hbsh    : " + downloadSHA1 +
                           "\ntheir hbshes: " + ifd.getUrns()+
                           "\nifm.hbshes : "+incompleteFileManager.dumpHashes());
                fileMbnager.removeFileIfShared(incompleteFile);
            }
        }
        
        // Locbte the hash for this incomplete file, to retrieve the 
        // IncompleteFileDesc.
        URN hbsh = incompleteFileManager.getCompletedHash(incompleteFile);
        if( hbsh != null ) {
            long size = IncompleteFileMbnager.getCompletedSize(incompleteFile);
            //crebte validAlts
            bddLocationsToDownload(RouterService.getAltlocManager().getDirect(hash),
                    RouterService.getAltlocMbnager().getPush(hash,false),
                    RouterService.getAltlocMbnager().getPush(hash,true),
                    (int)size);
        }
    }
    
    /**
     * Adds the blternate locations from the collections as possible
     * downlobd sources.
     */
    privbte void addLocationsToDownload(AlternateLocationCollection direct,
            AlternbteLocationCollection push,
            AlternbteLocationCollection fwt,
                                        int size) {
        List locs = new ArrbyList(direct.getAltLocsSize()+push.getAltLocsSize()+fwt.getAltLocsSize());
        // blways add the direct alt locs.
        synchronized(direct) {
            for (Iterbtor iter = direct.iterator(); iter.hasNext();) {
                AlternbteLocation loc = (AlternateLocation) iter.next();
                locs.bdd(loc.createRemoteFileDesc(size));
            }
        }
        
        synchronized(push) {
            for (Iterbtor iter = push.iterator(); iter.hasNext();) {
                AlternbteLocation loc = (AlternateLocation) iter.next();
                locs.bdd(loc.createRemoteFileDesc(size));
            }
        }
        
        synchronized(fwt) {
            for (Iterbtor iter = fwt.iterator(); iter.hasNext();) {
                AlternbteLocation loc = (AlternateLocation) iter.next();
                locs.bdd(loc.createRemoteFileDesc(size));
            }
        }
                
        bddPossibleSources(locs);
    }

    /**
     * Returns true if this downlobder is using (or could use) the given incomplete file.
     * @pbram incFile an incomplete file, which SHOULD be the return
     * vblue of IncompleteFileManager.getFile
     * <p>
     * Follows the sbme order as {@link #initializeIncompleteFile()}.
     */
    public boolebn conflictsWithIncompleteFile(File incFile) {
		File iFile = incompleteFile;
		if (iFile != null) {
			return iFile.equbls(incFile);
		}
		URN urn = downlobdSHA1;
		if (urn != null) {
			iFile = incompleteFileMbnager.getFileForUrn(urn);
		}
		if (iFile != null) {
			return iFile.equbls(incFile);
		}
	
		RemoteFileDesc rfd = null;
		synchronized (this) {
			if (!hbsRFD()) {
				return fblse;
			}
			rfd = (RemoteFileDesc)cbchedRFDs.iterator().next();
		}
		if (rfd != null) {
			try {
				File thisFile = incompleteFileMbnager.getFile(rfd);
				return thisFile.equbls(incFile);
			} cbtch(IOException ioe) {
				return fblse;
			}
		}
		return fblse;
    }

	/**
	 * Returns <code>true</code> if this downlobder's urn matches the given urn
	 * or if b downloader started for the triple (urn, fileName, fileSize) would
	 * write to the sbme incomplete file as this downloader does.  
	 * @pbram urn can be <code>null</code>, then the check is based upon fileName
	 * bnd fileSize
	 * @pbram fileName, must not be <code>null</code>
	 * @pbram fileSize, can be 0
	 * @return
	 */
	public boolebn conflicts(URN urn, String fileName, int fileSize) {
		if (urn != null && downlobdSHA1 != null) {
			return urn.equbls(downloadSHA1);
		}
		if (fileSize > 0) {
			try {
				File file = incompleteFileMbnager.getFile(fileName, null, fileSize);
				return conflictsWithIncompleteFile(file);
			} cbtch (IOException e) {
			}
		}
		return fblse;
	}
	

    /////////////////////////////// Requery Code ///////////////////////////////

    /** 
     * Returns b new QueryRequest for requery purposes.  Subclasses may wish to
     * override this to be more or less specific.  Note thbt the requery will
     * not be sent if globbl limits are exceeded.<p>
     *
     * Since there bre no more AUTOMATIC requeries, subclasses are advised to
     * stop using crebteRequery(...).  All attempts to 'requery' the network is
     * spbwned by the user, so use createQuery(...) .  The reason we need to
     * use crebteQuery is because DownloadManager.sendQuery() has a global
     * limit on the number of requeries sent by LW (bs IDed by the guid), but
     * it bllows normal queries to always be sent.
     *
     * @pbram numRequeries the number of requeries that have already happened
     * @exception CbntResumeException if this doesn't know what to search for 
	 * @return b new <tt>QueryRequest</tt> for making the requery
     */
    protected synchronized QueryRequest newRequery(int numRequeries)
      throws CbntResumeException {
		    
        String queryString = StringUtils.crebteQueryString(getDefaultFileName());
        if(queryString == null || queryString.equbls(""))
            throw new CbntResumeException(getSaveFile().getName());
        else
            return QueryRequest.crebteQuery(queryString);
            
    }


    /**
     * Determines if we should send b requery immediately, or wait for user
     * input.
     *
     * 'lbstQuerySent' being equal to -1 indicates that the user has already
     * clicked resume, so we do wbnt to send immediately.
     */
    protected boolebn shouldSendRequeryImmediately(int numRequeries) {
        if(lbstQuerySent == -1)
            return true;
        else
            return fblse;
    }

    /** Subclbsses should override this method when necessary.
     *  If you return fblse, then AltLocs are not initialized from the
     *  incomplete file upon invocbtion of tryAllDownloads.
     *  The true cbse can be used when the partial file is being shared
     *  through PFS bnd we've learned about AltLocs we want to use.
     */
    protected boolebn shouldInitAltLocs(boolean deserializedFromDisk) {
        return fblse;
    }
    
    /**
     * Determines if the specified host is bllowed to download.
     */
    protected boolebn hostIsAllowed(RemoteFileDesc other) {
         // If this host is bbnned, don't add.
        if ( !IPFilter.instbnce().allow(other.getHost()) )
            return fblse;            

        if (RouterService.bcceptedIncomingConnection() ||
                !other.isFirewblled() ||
                (other.supportsFWTrbnsfer() && RouterService.canDoFWT())) {
            // See if we hbve already tried and failed with this location
            // This is only done if the locbtion we're trying is an alternate..
            synchronized(bltLock) {
                if (other.isFromAlternbteLocation() && 
                        invblidAlts.contains(other.getRemoteHostData())) {
                    return fblse;
                }
            }
            
            return true;
        }
        return fblse;
    }
              


    privbte static boolean initDone = false; // used to init

    /**
     * Returns true if 'other' should be bccepted as a new download location.
     */
    protected boolebn allowAddition(RemoteFileDesc other) {
        if (!initDone) {
            synchronized (mbtcher) {
                mbtcher.setIgnoreCase(true);
                mbtcher.setIgnoreWhitespace(true);
                mbtcher.setCompareBackwards(true);
            }
            initDone = true;
        }

        // before doing expensive stuff, see if connection is even possible...
        if (other.getQublity() < 1) // I only want 2,3,4 star guys....
            return fblse;        

        // get other info...
		finbl URN otherUrn = other.getSHA1Urn();
        finbl String otherName = other.getFileName();
        finbl long otherLength = other.getFileSize();

        synchronized (this) {
            int ourLength = getContentLength();
            
            if (ourLength != -1 && ourLength != otherLength) 
                return fblse;
            
            if (otherUrn != null && downlobdSHA1 != null) 
                return otherUrn.equbls(downloadSHA1);
            
            // compbre to previously cached rfds
            for (Iterbtor iter = cachedRFDs.iterator();iter.hasNext();) {
                // get current info....
                RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
                finbl String thisName = rfd.getFileName();
                finbl long thisLength = rfd.getFileSize();
				
                // if they bre similarly named and same length
                // do length check first, much less expensive.....
                if (otherLength == thisLength) 
                    if (nbmesClose(otherName, thisName)) 
                        return true;                
            }
        }
        return fblse;
    }

    privbte final boolean namesClose(final String one, 
                                     finbl String two) {
        boolebn retVal = false;

        // copied from TbbleLine...
        //Filenbmes close?  This is the most expensive test, so it should go
        //lbst.  Allow 10% edit difference in filenames or 6 characters,
        //whichever is smbller.
        int bllowedDifferences=Math.round(Math.min(
             0.10f*((flobt)(StringUtils.ripExtension(one)).length()),
             0.10f*((flobt)(StringUtils.ripExtension(two)).length())));
        bllowedDifferences=Math.min(allowedDifferences, 6);

        synchronized (mbtcher) {
            retVbl = matcher.matches(matcher.process(one),
                                     mbtcher.process(two),
                                     bllowedDifferences);
        }

        if(LOG.isDebugEnbbled()) {
            LOG.debug("MD.nbmesClose(): one = " + one);
            LOG.debug("MD.nbmesClose(): two = " + two);
            LOG.debug("MD.nbmesClose(): retVal = " + retVal);
        }
            
        return retVbl;
    }

    /**
     * notifies this downlobder that an alternate location has been added.
     */
    public synchronized void locbtionAdded(AlternateLocation loc) {
        Assert.thbt(loc.getSHA1Urn().equals(getSHA1Urn()));
        bddDownload(loc.createRemoteFileDesc(getContentLength()),false);
    }
    
    /** 
     * Attempts to bdd the given location to this.  If rfd is accepted, this
     * will terminbte after downloading rfd or any of the other locations in
     * this.  This mby swarm some file from rfd and other locations.<p>
     * 
     * This method only bdds rfd if allowAddition(rfd).  Subclasses may
     * wish to override this protected method to control the behbvior.
     * 
     * @pbram rfd a new download candidate.  Typically rfd will be similar or
     *  sbme to some entry in this, but that is not required.  
     * @return true if rfd hbs been added.  In this case, the caller should
     *  not offer rfd to bnother ManagedDownloaders.
     */
    public synchronized boolebn addDownload(RemoteFileDesc rfd, boolean cache) {
        // never bdd to a stopped download.
        if(stopped || isCompleted())
            return fblse;
        
        if (!bllowAddition(rfd))
            return fblse;
        
        rfd.setDownlobding(true);
        
        if(!hostIsAllowed(rfd))
            return fblse;
        
        return bddDownloadForced(rfd, cache);
    }
    
    public synchronized boolebn addDownload(Collection c, boolean cache) {
        if (stopped || isCompleted())
            return fblse;
        
        List l = new ArrbyList(c.size());
        for (Iterbtor iter = c.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (hostIsAllowed(rfd) && bllowAddition(rfd))
                l.bdd(rfd);
        }
        
        return bddDownloadForced(l,cache);
    }

    /**
     * Like bddDownload, but doesn't call allowAddition(..).
     *
     * If cbche is false, the RFD is not added to allFiles, but is
     * bdded to 'files', the list of RFDs we will connect to.
     *
     * If the RFD mbtches one already in allFiles, the new one is
     * NOT bdded to allFiles, but IS added to the list of RFDs to connect to
     * if bnd only if a matching RFD is not currently in that list.
     *
     * This ALWAYS returns true, becbuse the download is either allowed
     * or silently ignored (becbuse we're already downloading or going to
     * bttempt to download from the host described in the RFD).
     */
    protected synchronized boolebn addDownloadForced(RemoteFileDesc rfd,
                                                           boolebn cache) {

        // DO NOT DOWNLOAD FROM YOURSELF.
        if( rfd.isMe() )
            return true;
        
        // blready downloading from the host
        if (currentRFDs.contbins(rfd))
            return true;
        
        prepbreRFD(rfd,cache);
        
        if (rbnker.addToPool(rfd)){
            if(LOG.isTrbceEnabled())
                LOG.trbce("added rfd: " + rfd);
            receivedNewSources = true;
        }
        
        return true;
    }
    
    protected synchronized finbl boolean addDownloadForced(Collection c, boolean cache) {
        // remove bny rfds we're currently downloading from 
        c.removeAll(currentRFDs);
        
        for (Iterbtor iter = c.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isMe()) {
                iter.remove();
                continue;
            }
            prepbreRFD(rfd,cache);
            if(LOG.isTrbceEnabled())
                LOG.trbce("added rfd: " + rfd);
        }
        
        if ( rbnker.addToPool(c) ) {
            if(LOG.isTrbceEnabled())
                LOG.trbce("added rfds: " + c);
            receivedNewSources = true;
        }
        
        return true;
    }
    
    privbte void prepareRFD(RemoteFileDesc rfd, boolean cache) {
        if(downlobdSHA1 == null) {
            downlobdSHA1 = rfd.getSHA1Urn();
            RouterService.getAltlocMbnager().addListener(downloadSHA1,this);
        }

        //bdd to allFiles for resume purposes if caching...
        if(cbche) 
            cbchedRFDs.add(rfd);        
    }
    
    /**
     * Returns true if we hbve received more possible source since the last
     * time we went inbctive.
     */
    public boolebn hasNewSources() {
        return !pbused && receivedNewSources;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Accepts b push download.  If this chooses to download the given file
     * (with given index bnd clientGUID) from socket, returns true.  In this
     * cbse, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns fblse without modifying this or socket.
     * If this could hbs problems with the socket, throws IOException.  In this
     * cbse the caller should close the socket.  Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (bnd nothing else) has been read from socket
     */
    public boolebn acceptDownload(
            String file, Socket socket, int index, byte[] clientGUID)
            throws IOException {
        
        MiniRemoteFileDesc mrfd=new MiniRemoteFileDesc(file,index,clientGUID);
        DownlobdWorker worker =  (DownloadWorker) miniRFDToLock.get(mrfd);
        
        if(worker == null) //not in mbp. Not intended for me
            return fblse;
        
        worker.setPushSocket(socket);
        
        return true;
    }
    
    void registerPushWbiter(DownloadWorker worker, MiniRemoteFileDesc mrfd) {
        miniRFDToLock.put(mrfd,worker);
    }
    
    void unregisterPushWbiter(MiniRemoteFileDesc mrfd) {
        miniRFDToLock.remove(mrfd);
    }
    
    /**
     * Determines if this downlobd was cancelled.
     */
    public boolebn isCancelled() {
        return stopped;
    }
    
    /**
     * Pbuses this download.
     */
    public synchronized void pbuse() {
        // do not pbuse if already stopped.
        if(!stopped && !isCompleted()) {
            stop();
            stopped = fblse;
            pbused = true;
            // if we're blready inactive, mark us as paused immediately.
            if(isInbctive())
                setStbte(PAUSED);
        }
    }
    
    /**
     * Determines if this downlobd is paused.
     *
     * If isPbused == true but getState() != PAUSED then this download
     * is in the process of pbusing itself.
     */
    public boolebn isPaused() {
        return pbused == true;
    }
    
    /**
     * Stops this downlobd.
     */
    public void stop() {
    
        if(pbused) {
            stopped = true;
            pbused = false;
        }

        // mbke redundant calls to stop() fast
        // this chbnge is pretty safe because stopped is only set in two
        // plbces - initialized and here.  so long as this is true, we know
        // this is sbfe.
        if (stopped || pbused)
            return;

        LOG.debug("STOPPING MbnagedDownloader");

        //This method is tricky.  Look cbrefully at run.  The most important
        //thing is to set the stopped flbg.  That guarantees run will terminate
        //eventublly.
        stopped=true;
        
        synchronized(this) {
            killAllWorkers();
            
            // must cbpture in local variable so the value doesn't become null
            // between if & contents of if.
            Threbd dlMan = dloaderManagerThread;
            if(dlMbn != null)
                dlMbn.interrupt();
            else
                LOG.wbrn("MANAGER: no thread to interrupt");
        }
    }

    /**
     * Kills bll workers.
     */    
    privbte synchronized void killAllWorkers() {
        for (Iterbtor iter = _workers.iterator(); iter.hasNext();) {
            DownlobdWorker doomed = (DownloadWorker) iter.next();
            doomed.interrupt();
        }
    }
    
    /**
     * Cbllback from workers to inform the managing thread that
     * b disk problem has occured.
     */
    synchronized void diskProblemOccured() {
        setStbte(DISK_PROBLEM);
        stop();
    }

    /**
     * Notifies bll existing HTTPDownloaders about this RFD.
     * If good is true, it notifies them of b succesful alternate location,
     * otherwise it notifies them of b failed alternate location.
     * The internbl validAlts is also updated if good is true,
     * bnd invalidAlts is updated if good is false.
     * The IncompleteFileDesc is blso notified of new locations for this
     * file.
     * If we successfully downlobded from this host, cache it for future resume.
     */
    public synchronized void informMesh(RemoteFileDesc rfd, boolebn good) {
        if (LOG.isDebugEnbbled())
            LOG.debug("informing mesh thbt "+rfd+" is "+good);
        
        if (good)
            cbchedRFDs.add(rfd);
        
        if(!rfd.isAltLocCbpable())
            return;
        
        // Verify thbt this download has a hash.  If it does not,
        // we should not hbve been getting locations in the first place.
        Assert.thbt(downloadSHA1 != null, "null hash.");
        
        Assert.thbt(downloadSHA1.equals(rfd.getSHA1Urn()), "wrong loc SHA1");
        
        AlternbteLocation loc;
        try {
            loc = AlternbteLocation.create(rfd);
        } cbtch(IOException iox) {
            return;
        }
        
        AlternbteLocation local;
        
        // if this is b pushloc, update the proxies accordingly
        if (loc instbnceof PushAltLoc) {
            
            // Note: we updbte the proxies of a clone in order not to lose the
            // originbl proxies
            locbl = loc.createClone();
            PushAltLoc ploc = (PushAltLoc)loc;
            
            // no need to notify mesh bbout pushlocs w/o any proxies
            if (ploc.getPushAddress().getProxies().isEmpty())
                return;
            
            ploc.updbteProxies(good);
        } else
            locbl = loc;
        
        // bnd to the global collection
        if (good)
            RouterService.getAltlocMbnager().add(loc, this);
        else
            RouterService.getAltlocMbnager().remove(loc, this);

        // bdd to the downloaders
        for(Iterbtor iter=getActiveWorkers().iterator(); iter.hasNext();) {
            HTTPDownlobder httpDloader = ((DownloadWorker)iter.next()).getDownloader();
            RemoteFileDesc r = httpDlobder.getRemoteFileDesc();
            
            // no need to tell uplobder about itself and since many firewalled
            // downlobds may have the same port and host, we also check their
            // push endpoints
            if(! (locbl instanceof PushAltLoc) ? 
                    (r.getHost().equbls(rfd.getHost()) && r.getPort()==rfd.getPort()) :
                    r.getPushAddr()!=null && r.getPushAddr().equbls(rfd.getPushAddr()))
                continue;
            
            //no need to send push bltlocs to older uploaders
            if (locbl instanceof DirectAltLoc || httpDloader.wantsFalts()) {
            	if (good)
            		httpDlobder.addSuccessfulAltLoc(local);
            	else
            		httpDlobder.addFailedAltLoc(local);
            }
        }
        
        // bdd to the local collections
        synchronized(bltLock) {
            if(good) {
                //check if vblidAlts contains loc to avoid duplicate stats, and
                //spurious count increments in the locbl
                //AlternbteLocationCollections
                if(!vblidAlts.contains(local)) {
                    if(rfd.isFromAlternbteLocation() )
                        if (rfd.needsPush())
                            DownlobdStat.PUSH_ALTERNATE_WORKED.incrementStat();
                        else
                            DownlobdStat.ALTERNATE_WORKED.incrementStat(); 
                    vblidAlts.add(local);
                }
            }  else {
                    if(rfd.isFromAlternbteLocation() )
                        if(locbl instanceof PushAltLoc)
                                DownlobdStat.PUSH_ALTERNATE_NOT_ADDED.incrementStat();
                        else
                                DownlobdStat.ALTERNATE_NOT_ADDED.incrementStat();
                    
                    vblidAlts.remove(local);
                    invblidAlts.add(rfd.getRemoteHostData());
                    recentInvblidAlts.add(local);
            }
        }
    }

    public synchronized void bddPossibleSources(Collection c) {
        bddDownload(c,false);
    }
    
    /**
     * Requests this downlobd to resume.
     *
     * If the downlobd is not inactive, this does nothing.
     * If the downlobder was waiting for the user, a requery is sent.
     */
    public synchronized boolebn resume() {
        //Ignore request if blready in the download cycle.
        if (!isInbctive())
            return fblse;

        // if we were wbiting for the user to start us,
        // then try to send the requery.
        if(getStbte() == WAITING_FOR_USER)
            lbstQuerySent = -1; // inform requerying that we wanna go.
        
        // if bny guys were busy, reduce their retry time to 0,
        // since the user reblly wants to resume right now.
        for(Iterbtor i = cachedRFDs.iterator(); i.hasNext(); )
            ((RemoteFileDesc)i.next()).setRetryAfter(0);

        if(pbused) {
            pbused = false;
            stopped = fblse;
        }
            
        // queue ourselves so we'll try bnd become active immediately
        setStbte(QUEUED);

        return true;
    }
    
    /**
     * Returns the incompleteFile or the completeFile, if the is complete.
     */
    public File getFile() {
        if(incompleteFile == null)
            return null;
            
        if(stbte == COMPLETE)
            return getSbveFile();
        else
            return incompleteFile;
    }
    
    public URN getSHA1Urn() {
        return downlobdSHA1;
    }
    
    /**
     * Returns the first frbgment of the incomplete file,
     * copied to b new file, or the completeFile if the download
     * is complete, or the corruptFile if the downlobd is corrupted.
     */
    public File getDownlobdFragment() {
        //We hbven't started yet.
        if (incompleteFile==null)
            return null;
        
        //b) Special case for saved corrupt fragments.  We don't worry about
        //removing holes.
        if (stbte==CORRUPT_FILE) 
            return corruptFile; //mby be null
        //b) If the file is being downlobded, create *copy* of first
        //block of incomplete file.  The copy is needed becbuse some
        //progrbms, notably Windows Media Player, attempt to grab
        //exclusive file locks.  If the downlobd hasn't started, the
        //incomplete file mby not even exist--not a problem.
        else if (stbte!=COMPLETE) {
            File file=new File(incompleteFile.getPbrent(),
                               IncompleteFileMbnager.PREVIEW_PREFIX
                                   +incompleteFile.getNbme());
            //Get the size of the first block of the file.  (Remember
            //thbt swarmed downloads don't always write in order.)
            int size=bmountForPreview();
            if (size<=0)
                return null;
            //Copy first block, returning if nothing wbs copied.
            if (CommonUtils.copy(incompleteFile, size, file)<=0) 
                return null;
            return file;
        }
        //c) Otherwise, choose completed file.
        else {
            return getSbveFile();
        }
    }


    /** 
     * Returns the bmount of the file written on disk that can be safely
     * previewed. 
     */
    privbte synchronized int amountForPreview() {
        //And find the first block.
        if (commonOutFile == null)
            return 0; // trying to preview before incomplete file crebted
        synchronized (commonOutFile) {
            for (Iterbtor iter=commonOutFile.getBlocks();iter.hasNext() ; ) {
                Intervbl interval=(Interval)iter.next();
                if (intervbl.low==0)
                    return intervbl.high;
            }
        }
        return 0;//Nothing to preview!
    }

    /**
	 * Sets the file nbme and directory where the download will be saved once
	 * complete.
     * 
     * @pbram overwrite true if overwriting an existing file is allowed
     * @throws IOException if FileUtils.isRebllyParent(testParent, testChild) throws IOException
     */
    public void setSbveFile(File saveDirectory, String fileName,
							boolebn overwrite) 
		throws SbveLocationException {
        if (sbveDirectory == null)
            sbveDirectory = SharingSettings.getSaveDirectory();
        if (fileNbme == null)
            fileNbme = getDefaultFileName();
        
        if (!sbveDirectory.isDirectory()) {
            if (sbveDirectory.exists())
                throw new SbveLocationException(SaveLocationException.NOT_A_DIRECTORY, saveDirectory);
            throw new SbveLocationException(SaveLocationException.DIRECTORY_DOES_NOT_EXIST, saveDirectory);
        }
        
        File cbndidateFile = new File(saveDirectory, fileName);
        try {
            if (!FileUtils.isRebllyParent(saveDirectory, candidateFile))
                throw new SbveLocationException(SaveLocationException.SECURITY_VIOLATION, candidateFile);
        } cbtch (IOException e) {
            throw new SbveLocationException(SaveLocationException.FILESYSTEM_ERROR, candidateFile);
        }
		
        if (! FileUtils.setWritebble(saveDirectory))    
            throw new SbveLocationException(SaveLocationException.DIRECTORY_NOT_WRITEABLE,saveDirectory);
		
        if (cbndidateFile.exists()) {
            if (!cbndidateFile.isFile())
                throw new SbveLocationException(SaveLocationException.FILE_NOT_REGULAR, candidateFile);
            if (!overwrite)
                throw new SbveLocationException(SaveLocationException.FILE_ALREADY_EXISTS, candidateFile);
        }
		
		// check if bnother existing download is being saved to this download
		// we ignore the overwrite flbg on purpose in this case
		if (RouterService.getDownlobdManager().isSaveLocationTaken(candidateFile)) {
			throw new SbveLocationException(SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO, candidateFile);
		}
         
        // Pbssed sanity checks, so save file
        synchronized (this) {
            if (!isRelocbtable())
                throw new SbveLocationException(SaveLocationException.FILE_ALREADY_SAVED, candidateFile);
            propertiesMbp.put(SAVE_FILE, candidateFile);
        }
    }
   
    /** 
     * This method is used to determine where the file will be sbved once downloaded.
     *
     * @return A File representbtion of the directory or regular file where this file will be saved.  null indicates the program-wide default save directory.
     */
    public synchronized File getSbveFile() {
        Object sbveFile = propertiesMap.get(SAVE_FILE);
		if (sbveFile != null) {
			return (File)sbveFile;
		}
        
        return new File(ShbringSettings.getSaveDirectory(), getDefaultFileName());
    }  
    
    //////////////////////////// Core Downlobding Logic /////////////////////

    /**
     * Clebns up information before this downloader is removed from memory.
     */
    public synchronized void finish() {
        if (downlobdSHA1 != null)
            RouterService.getAltlocMbnager().removeListener(downloadSHA1, this);
        
        if(cbchedRFDs != null) {
            for (Iterbtor iter = cachedRFDs.iterator(); iter.hasNext();) {
				RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
				rfd.setDownlobding(false);
			}
        }       
    }

    /** 
     * Actublly does the download, finding duplicate files, trying all
     * locbtions, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to sbve directory and adding
     * file to the librbry.  Called from dloadManagerThread.  
     * @pbram deserialized True if this downloader was deserialized from disk,
     * fblse if it was newly constructed.
     */
    protected int performDownlobd() {
        if(checkHosts()) {//files is globbl
            setStbte(GAVE_UP);
            return GAVE_UP;
        }

        // 1. initiblize the download
        int stbtus = initializeDownload();
        if ( stbtus == CONNECTING) {
            try {
                //2. Do the downlobd
                try {
                    stbtus = fireDownloadWorkers();//Exception may be thrown here.
                }finblly {
                    //3. Close the file controlled by commonOutFile.
                    commonOutFile.close();
                }
                
                // 4. if bll went well, save
                if (stbtus == COMPLETE) 
                    stbtus = verifyAndSave();
                else if(LOG.isDebugEnbbled())
                    LOG.debug("stopping ebrly with status: " + status); 
                
            } cbtch (InterruptedException e) {
                
                // nothing should interrupt except for b stop
                if (!stopped && !pbused)
                    ErrorService.error(e);
                else
                    stbtus = GAVE_UP;
                
                // if we were stopped due to corrupt downlobd, cleanup
                if (corruptStbte == CORRUPT_STOP_STATE) {
                    // TODO is this reblly what cleanupCorrupt expects?
                    clebnupCorrupt(incompleteFile, getSaveFile().getName());
                    stbtus = CORRUPT_FILE;
                }
            }
        }
        
        if(LOG.isDebugEnbbled())
            LOG.debug("MANAGER: TAD2 returned: " + stbtus);
                   
        return stbtus;
    }

	privbte static final int MIN_NUM_CONNECTIONS      = 2;
	privbte static final int MIN_CONNECTION_MESSAGES  = 6;
	privbte static final int MIN_TOTAL_MESSAGES       = 45;
    stbtic boolean   NO_DELAY				  = false; // For testing

    /**
     *  Determines if we hbve any stable connections to send a requery down.
     */
    privbte boolean hasStableConnections() {
		if ( NO_DELAY )
		    return true;  // For Testing without network connection

		// TODO: Note thbt on a private network, these conditions might
		//       be too strict.
		
		// Wbit till your connections are stable enough to get the minimum 
		// number of messbges
		return RouterService.countConnectionsWithNMessbges(MIN_CONNECTION_MESSAGES) 
			        >= MIN_NUM_CONNECTIONS &&
               RouterService.getActiveConnectionMessbges() >= MIN_TOTAL_MESSAGES;
    }

    /**
     * Tries to initiblize the download location and the verifying file. 
     * @return GAVE_UP if we hbd no sources, DISK_PROBLEM if such occured, 
     * CONNECTING if we're rebdy to connect
     */
    protected int initiblizeDownload() {
        
        synchronized (this) {
            if (cbchedRFDs.size()==0 && !ranker.hasMore()) 
                return GAVE_UP;
        }
        
        try {
            initiblizeIncompleteFile();
            initiblizeVerifyingFile();
            openVerifyingFile();
        } cbtch (IOException iox) {
            return DISK_PROBLEM;
        }

        // Crebte a new validAlts for this sha1.
        // initiblize the HashTree
        if( downlobdSHA1 != null ) 
            initiblizeHashTree();
        
        // lobd up the ranker with the hosts we know about
        initiblizeRanker();
        
        return CONNECTING;
    }
    
    /**
     * Verifies the completed file bgainst the SHA1 hash and saves it.  If
     * there is corruption, it bsks the user whether to discard or keep the file 
     * @return COMPLETE if bll went fine, DISK_PROBLEM if not.
     * @throws InterruptedException if we get interrupted while wbiting for user
     * response.
     */
    privbte int verifyAndSave() throws InterruptedException{
        
        // Find out the hbsh of the file and verify that its the same
        // bs our hash.
        URN fileHbsh = scanForCorruption();
        if (corruptStbte == CORRUPT_STOP_STATE) {
            // TODO is this whbt cleanup Corrupt expects?
            clebnupCorrupt(incompleteFile, getSaveFile().getName());
            return CORRUPT_FILE;
        }
        
        // Sbve the file to disk.
        return sbveFile(fileHash);
    }
    
    /**
     * Wbits indefinitely for a response to the corrupt message prompt, if
     * such wbs displayed.
     */
    privbte void waitForCorruptResponse() {
        if(corruptStbte != NOT_CORRUPT_STATE) {
            synchronized(corruptStbteLock) {
                try {
                    while(corruptStbte==CORRUPT_WAITING_STATE)
                        corruptStbteLock.wait();
                } cbtch(InterruptedException ignored) {}
            }
        }
    }  
    
    /**
     * Scbns the file for corruption, returning the hash of the file on disk.
     */
    privbte URN scanForCorruption() {
        // if we blready were told to stop, then stop.
        if (corruptStbte==CORRUPT_STOP_STATE)
            return null;
        
        //if the user hbs not been asked before.               
        URN fileHbsh=null;
        try {
            // let the user know we're hbshing the file
            setStbte(HASHING);
            fileHbsh = URN.createSHA1Urn(incompleteFile);
        }
        cbtch(IOException ignored) {}
        cbtch(InterruptedException ignored) {}
        
        // If we hbve no hash, we can't check at all.
        if(downlobdSHA1 == null)
            return fileHbsh;

        // If they're equbl, everything's fine.
        //if fileHbsh == null, it will be a mismatch
        if(downlobdSHA1.equals(fileHash))
            return fileHbsh;
        
        if(LOG.isWbrnEnabled()) {
            LOG.wbrn("hash verification problem, fileHash="+
                           fileHbsh+", ourHash="+downloadSHA1);
        }

        // unshbre the file if we didn't have a tree
        // otherwise we will hbve shared only the parts that verified
        if (commonOutFile.getHbshTree() == null) 
            fileMbnager.removeFileIfShared(incompleteFile);
        
        // purge the tree
        TigerTreeCbche.instance().purgeTree(downloadSHA1);
        commonOutFile.setHbshTree(null);

        // bsk what to do next 
        promptAboutCorruptDownlobd();
        wbitForCorruptResponse();
        
        return fileHbsh;        
    }

    /**
     * checks the TT cbche and if a good tree is present loads it 
     */
    privbte void initializeHashTree() {
		HbshTree tree = TigerTreeCache.instance().getHashTree(downloadSHA1); 
	    
		// if we hbve a valid tree, update our chunk size and disable overlap checking
		if (tree != null && tree.isDepthGoodEnough()) {
				commonOutFile.setHbshTree(tree);
		}
    }
	
    /**
     * Sbves the file to disk.
     */
    privbte int saveFile(URN fileHash){
        // let the user know we're sbving the file...
        setStbte( SAVING );
        
        //4. Move to librbry.
        // Mbke sure we can write into the complete file's directory.
        if (!FileUtils.setWritebble(getSaveFile().getParentFile()))
            return DISK_PROBLEM;
        File sbveFile = getSaveFile();
        //Delete tbrget.  If target doesn't exist, this will fail silently.
        sbveFile.delete();

        //Try moving file.  If we couldn't move the file, i.e., becbuse
        //someone is previewing it or it's on b different volume, try copy
        //instebd.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileMbnager, though this is not strictly necessary
        //becbuse IFM.purge() is called frequently in DownloadManager.
        
        // First bttempt to rename it.
        boolebn success = FileUtils.forceRename(incompleteFile,saveFile);

        incompleteFileMbnager.removeEntry(incompleteFile);
        
        // If thbt didn't work, we're out of luck.
        if (!success)
            return DISK_PROBLEM;
            
        //Add file to librbry.
        // first check if it conflicts with the sbved dir....
        if (sbveFile.exists())
            fileMbnager.removeFileIfShared(saveFile);

        //Add the URN of this file to the cbche so that it won't
        //be hbshed again when added to the library -- reduces
        //the time of the 'Sbving File' state.
        if(fileHbsh != null) {
            Set urns = new HbshSet(1);
            urns.bdd(fileHash);
            File file = sbveFile;
            try {
                file = FileUtils.getCbnonicalFile(saveFile);
            } cbtch(IOException ignored) {}
            // Alwbys cache the URN, so results can lookup to see
            // if the file exists.
            UrnCbche.instance().addUrns(file, urns);
            // Notify the SbvedFileManager that there is a new saved
            // file.
            SbvedFileManager.instance().addSavedFile(file, urns);
            
            // sbve the trees!
            if (downlobdSHA1 != null && downloadSHA1.equals(fileHash) && commonOutFile.getHashTree() != null) {
                TigerTreeCbche.instance(); 
                TigerTreeCbche.addHashTree(downloadSHA1,commonOutFile.getHashTree());
            }
        }

        
		if (ShbringSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue())
			fileMbnager.addFileAlways(getSaveFile(), getXMLDocuments());
		else
		    fileMbnager.addFileIfShared(getSaveFile(), getXMLDocuments());

		return COMPLETE;
    }

    /** Removes bll entries for incompleteFile from incompleteFileManager 
     *  bnd attempts to rename incompleteFile to "CORRUPT-i-...".  Deletes
     *  incompleteFile if renbme fails. */
    privbte void cleanupCorrupt(File incFile, String name) {
        corruptFileBytes=getAmountRebd();        
        incompleteFileMbnager.removeEntry(incFile);

        //Try to renbme the incomplete file to a new corrupt file in the same
        //directory (INCOMPLETE_DIRECTORY).
        boolebn renamed = false;
        for (int i=0; i<10 && !renbmed; i++) {
            corruptFile=new File(incFile.getPbrent(),
                                 "CORRUPT-"+i+"-"+nbme);
            if (corruptFile.exists())
                continue;
            renbmed=incFile.renameTo(corruptFile);
        }

        //Could not renbme after ten attempts?  Delete.
        if(!renbmed) {
            incFile.delete();
            this.corruptFile=null;
        }
    }
    
    /**
     * Initiblizes the verifiying file.
     */
    privbte void openVerifyingFile() throws IOException {

        //need to get the VerifyingFile rebdy to write
        try {
            commonOutFile.open(incompleteFile);
        } cbtch(IOException e) {
            if(!IOUtils.hbndleException(e, "DOWNLOAD"))
                ErrorService.error(e);
            throw e;
        }
    }
    
    /**
     * Stbrts a new Worker thread for the given RFD.
     */
    privbte void startWorker(final RemoteFileDesc rfd) {
        DownlobdWorker worker = new DownloadWorker(this,rfd,commonOutFile,stealLock);
        Threbd connectCreator = new ManagedThread(worker);
        
        connectCrebtor.setName("DownloadWorker");
        
        synchronized(this) {
            _workers.bdd(worker);
            currentRFDs.bdd(rfd);
        }

        connectCrebtor.start();
    }        
    
    /**
     * Cbllback that the specified worker has finished.
     */
    synchronized void workerFinished(DownlobdWorker finished) {
        if (LOG.isDebugEnbbled())
            LOG.debug("worker "+finished+" finished.");
        removeWorker(finished); 
        notify();
    }
    
    synchronized void workerStbrted(DownloadWorker worker) {
        if (LOG.isDebugEnbbled())
            LOG.debug("worker "+worker + " stbrted.");
        setStbte(ManagedDownloader.DOWNLOADING);
        bddActiveWorker(worker);
        chbtList.addHost(worker.getDownloader());
        browseList.bddHost(worker.getDownloader());
    }
    
    void workerFbiled(DownloadWorker failed) {
        HTTPDownlobder downloader = failed.getDownloader();
        if (downlobder != null) {
            chbtList.removeHost(downloader);
            browseList.removeHost(downlobder);
        }
    }
    
    synchronized void removeWorker(DownlobdWorker worker) {
        removeActiveWorker(worker);
        workerFbiled(worker); // make sure its out of the chat list & browse list
        _workers.remove(worker);
    }
    
    synchronized void removeActiveWorker(DownlobdWorker worker) {
        currentRFDs.remove(worker.getRFD());
        List l = new ArrbyList(getActiveWorkers());
        l.remove(worker);
        _bctiveWorkers = Collections.unmodifiableList(l);
    }
    
    synchronized void bddActiveWorker(DownloadWorker worker) {
        // only bdd if not already added.
        if(!getActiveWorkers().contbins(worker)) {
            List l = new ArrbyList(getActiveWorkers());
            l.bdd(worker);
            _bctiveWorkers = Collections.unmodifiableList(l);
        }
    }

    synchronized String getWorkersInfo() {
        String workerStbte = "";
        for (Iterbtor iter = _workers.iterator(); iter.hasNext();) {
            DownlobdWorker worker = (DownloadWorker) iter.next();
            workerStbte+=worker.getInfo();
        }
        return workerStbte;
    }
    /**
     * @return The blternate locations we have successfully downloaded from
     */
    Set getVblidAlts() {
        synchronized(bltLock) {
            Set ret;
            
            if (vblidAlts != null) {
                ret = new HbshSet();
                for (Iterbtor iter = validAlts.iterator();iter.hasNext();)
                    ret.bdd(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /**
     * @return The blternate locations we have failed to downloaded from
     */
    Set getInvblidAlts() {
        synchronized(bltLock) {
            Set ret;
            
            if (invblidAlts != null) {
                ret = new HbshSet();
                for (Iterbtor iter = recentInvalidAlts.iterator();iter.hasNext();)
                    ret.bdd(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /** 
     * Like tryDownlobds2, but does not deal with the library, cleaning
     * up corrupt files, etc.  Cbller should look at corruptState to
     * determine if the file is corrupted; b return value of COMPLETE
     * does not mebn no corruptions where encountered.
     *
     * @return COMPLETE if b file was successfully downloaded
     *         WAITING_FOR_RETRY if no file wbs downloaded, but it makes sense 
     *             to try bgain later because some hosts reported busy.
     *             The cbller should usually wait before retrying.
     *         GAVE_UP the downlobd attempt failed, and there are 
     *             no more locbtions to try.
     *         COULDNT_MOVE_TO_LIBRARY couldn't write the incomplete file
     * @exception InterruptedException if the someone stop()'ed this downlobd.
     *  stop() wbs called either because the user killed the download or
     *  b corruption was detected and they chose to kill and discard the
     *  downlobd.  Calls to resume() do not result in InterruptedException.
     */
    privbte int fireDownloadWorkers() throws InterruptedException {
        LOG.trbce("MANAGER: entered fireDownloadWorkers");

        //While there is still bn unfinished region of the file...
        while (true) {
            if (stopped || pbused) {
                LOG.wbrn("MANAGER: terminating because of stop|pause");
                throw new InterruptedException();
            } 
            
            // bre we just about to finish downloading the file?
            
            LOG.debug("About to wbit for pending if needed");
            
            try {            
                commonOutFile.wbitForPendingIfNeeded();
            } cbtch(DiskException dio) {
                if (stopped || pbused) {
                    LOG.wbrn("MANAGER: terminating because of stop|pause");
                    throw new InterruptedException();
                }
                stop();
                return DISK_PROBLEM;
            }
            
            LOG.debug("Finished wbiting for pending");
            
            
            // Finished.
            if (commonOutFile.isComplete()) {
                killAllWorkers();
                
                LOG.trbce("MANAGER: terminating because of completion");
                return COMPLETE;
            }
            
            synchronized(this) { 
                // if everybody we know bbout is busy (or we don't know about anybody)
                // bnd we're not downloading from anybody - terminate the download.
                if (_workers.size() == 0 && !rbnker.hasNonBusy())   {
                    
                    receivedNewSources = fblse;
                    
                    if ( rbnker.calculateWaitTime() > 0) {
                        LOG.trbce("MANAGER: terminating with busy");
                        return BUSY;
                    } else {
                        LOG.trbce("MANAGER: terminating w/o hope");
                        return GAVE_UP;
                    }
                }
                
                if(LOG.isDebugEnbbled())
                    LOG.debug("MANAGER: kicking off workers, dlobdsCount: " + 
                            _bctiveWorkers.size() + ", threads: " + _workers.size());
                
                //OK. We bre going to create a thread for each RFD. The policy for
                //the worker threbds is to have one more thread than the max swarm
                //limit, which if successfully stbrts downloading or gets a better
                //queued slot thbn some other worker kills the lowest worker in some
                //remote queue.
                if (shouldStbrtWorker()){
                    // see if we need to updbte our ranker
                    rbnker = getSourceRanker(ranker);
                    
                    RemoteFileDesc rfd = rbnker.getBest();
                    
                    if (rfd != null) {
                        // If the rfd wbs busy, that means all possible RFDs
                        // bre busy - store for later
                        if( rfd.isBusy() ) 
                            bddRFD(rfd);
                         else 
                            stbrtWorker(rfd);
                    }
                    
                } else if (LOG.isDebugEnbbled())
                    LOG.debug("no blocks but cbn't steal - sleeping");
                
                //wbit for a notification before we continue.
                try {
                    //if no workers notify in b while, iterate. This is a problem
                    //for stblled downloaders which will never notify. So if we
                    //wbit without a timeout, we could wait forever.
                    this.wbit(DownloadSettings.WORKER_INTERVAL.getValue()); // note that this relinquishes the lock
                } cbtch (InterruptedException ignored) {}
            }
        }//end of while
    }
    
    /**
     * Retrieves the bppropriate source ranker (or returns the current one).
     */
    protected SourceRbnker getSourceRanker(SourceRanker ranker) {
        return SourceRbnker.getAppropriateRanker(ranker);
    }
    
    /**
     * @return if we should stbrt another worker - means we have more to download,
     * hbve not reached our swarm capacity and the ranker has something to offer
     * or we hbve some rfds to re-try
     */
    privbte boolean shouldStartWorker() {
        return (commonOutFile.hbsFreeBlocksToAssign() > 0 || stealingCanHappen() ) &&
             ((_workers.size() - queuedWorkers.size()) < getSwbrmCapacity()) &&
             rbnker.hasMore();
    }
    
    /**
     * @return true if we hbve more than one worker or the last one is slow
     */
    privbte boolean stealingCanHappen() {
        if (_workers.size() < 1)
            return fblse;
        else if (_workers.size() > 1)
            return true;
            
        DownlobdWorker lastOne = (DownloadWorker)_workers.get(0);
        // with lbrger chunk sizes we may end up with slower last downloader
        return lbstOne.isSlow(); 
    }
	
	synchronized void bddRFD(RemoteFileDesc rfd) {
        if (rbnker != null)
            rbnker.addToPool(rfd);
	}
    
    synchronized void forgetRFD(RemoteFileDesc rfd) {
        if (cbchedRFDs.remove(rfd) && cachedRFDs.isEmpty()) {
            // remember our lbst RFD
            rfd.setSeriblizeProxies();
            cbchedRFDs.add(rfd);
        }
    }
    
	/**
	 * Returns the number of blternate locations that this download is using.
	 */
	public int getNumberOfAlternbteLocations() {
	    if ( vblidAlts == null ) return 0;
        synchronized(bltLock) {
            return vblidAlts.size();
        }
    }

    /**
     * Returns the number of invblid alternate locations that this download is
     * using.
     */
    public int getNumberOfInvblidAlternateLocations() {
        if ( invblidAlts == null ) return 0;
        synchronized(bltLock) {
            return invblidAlts.size();
        }
    }
    
    /**
     * Returns the bmount of other hosts this download can possibly use.
     */
    public synchronized int getPossibleHostCount() {
        return rbnker == null ? 0 : ranker.getNumKnownHosts();
    }
    
    public synchronized int getBusyHostCount() {
        return rbnker == null ? 0 : ranker.getNumBusyHosts();
    }

    public synchronized int getQueuedHostCount() {
        return queuedWorkers.size();
    }

    int getSwbrmCapacity() {
        int cbpacity = ConnectionSettings.CONNECTION_SPEED.getValue();
        if(cbpacity <= SpeedConstants.MODEM_SPEED_INT) //modems swarm = 2
            return SpeedConstbnts.MODEM_SWARM;
        else if (cbpacity <= SpeedConstants.T1_SPEED_INT) //DSL, Cable, T1 = 6
            return SpeedConstbnts.T1_SWARM;
        else // T3
            return SpeedConstbnts.T3_SWARM;
    }

    /**
     * Asks the user if we should continue or discbrd this download.
     */
    void promptAboutCorruptDownlobd() {
        synchronized(corruptStbteLock) {
            if(corruptStbte == NOT_CORRUPT_STATE) {
                corruptStbte = CORRUPT_WAITING_STATE;
                //Note:We bre going to inform the user. The GUI will notify us
                //when the user hbs made a decision. Until then the corruptState
                //is set to wbiting. We are not going to move files unless we
                //bre out of this state
                sendCorruptCbllback();
                //Note2:ActivityCbllback is going to ask a message to be show to
                //the user bsynchronously
            }
        }
    }
    
    /**
     * Hook for sending b corrupt callback.
     */
    protected void sendCorruptCbllback() {
        cbllback.promptAboutCorruptDownload(this);
    }

    /**
     * Informs this downlobder about how to handle corruption.
     */
    public void discbrdCorruptDownload(final boolean delete) {
        if (LOG.isDebugEnbbled())
            LOG.debug("User chose to delete corrupt "+delete);
        
        // offlobd this from the swing thread since it will require
        // bccess to the verifying file.
        Runnbble r = new Runnable() {
            public void run() {
                synchronized(corruptStbteLock) {
                    if(delete) {
                        corruptStbte = CORRUPT_STOP_STATE;
                    } else {
                        corruptStbte = CORRUPT_CONTINUE_STATE;
                    }
                }

                if (delete)
                    stop();
                else 
                    commonOutFile.setDiscbrdUnverified(false);
                
                synchronized(corruptStbteLock) {
                    corruptStbteLock.notify();
                }
            }
        };
        
        RouterService.schedule(r,0,0);

    }
            

    /**
     * Returns the union of bll XML metadata documents from all hosts.
     */
    privbte synchronized List getXMLDocuments() {
        //TODO: we don't bctually union here.  Also, should we only consider
        //those locbtions that we download from?
        List bllDocs = new ArrayList();

        // get bll docs possible
        for (Iterbtor iter = cachedRFDs.iterator();iter.hasNext();) {
			RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
			LimeXMLDocument doc = rfd.getXMLDocument();
			if(doc != null) {
				bllDocs.add(doc);
			}
        }

        return bllDocs;
    }

    /////////////////////////////Displby Variables////////////////////////////

    /** Sbme as setState(newState, Integer.MAX_VALUE). */
    synchronized void setStbte(int newState) {
        this.stbte=newState;
        this.stbteTime=Long.MAX_VALUE;
    }

    /** 
     * Sets this' stbte.
     * @pbram newState the state we're entering, which MUST be one of the 
     *  constbnts defined in Downloader
     * @pbram time the time we expect to state in this state, in 
     *  milliseconds. 
     */
    synchronized void setStbte(int newState, long time) {
            this.stbte=newState;
            this.stbteTime=System.currentTimeMillis()+time;
    }
    
    /**
     * Sets the inbctive priority of this download.
     */
    public void setInbctivePriority(int priority) {
        inbctivePriority = priority;
    }
    
    /**
     * Gets the inbctive priority of this download.
     */
    public int getInbctivePriority() {
        return inbctivePriority;
    }


    /*************************************************************************
     * Accessors thbt delegate to dloader. Synchronized because dloader can
     * chbnge.
     *************************************************************************/

    /** @return the GUID of the query thbt spawned this downloader.  may be null.
     */
    public GUID getQueryGUID() {
        return this.originblQueryGUID;
    }

    public synchronized int getStbte() {
        return stbte;
    }

    public synchronized int getRembiningStateTime() {
        long rembining;
        switch (stbte) {
        cbse CONNECTING:
        cbse BUSY:
        cbse WAITING_FOR_RESULTS:
        cbse ITERATIVE_GUESSING:
        cbse WAITING_FOR_CONNECTIONS:
            rembining=stateTime-System.currentTimeMillis();
            return  (int)Mbth.ceil(Math.max(remaining, 0)/1000f);
        cbse QUEUED:
            return 0;
        defbult:
            return Integer.MAX_VALUE;
        }
    }

    /**
	 * Returns the vblue for the key {@link #DEFAULT_FILENAME} from
	 * the properties mbp.
	 * <p>
	 * Subclbsses should put the name into the map or overriede this
	 * method.
	 */
    protected synchronized String getDefbultFileName() {       
        String fileNbme = (String)propertiesMap.get(DEFAULT_FILENAME); 
         if (fileNbme == null) {
             Assert.thbt(false,"defaultFileName is null, "+
                         "subclbss may have not overridden getDefaultFileName");
         }
		 return CommonUtils.convertFileNbme(fileName);
    }


	/**
     *  Certbin subclasses would like to know whether we have at least one good
	 *  RFD.
     */
	protected synchronized boolebn hasRFD() {
        return ( cbchedRFDs != null && !cachedRFDs.isEmpty());
	}
	
	/**
	 * Return -1 if the file size is not known yet, i.e. is not stored in the
	 * properties mbp under {@link #FILE_SIZE}.
	 */
    public synchronized int getContentLength() {
        Integer i = (Integer)propertiesMbp.get(FILE_SIZE);
        return i != null ? i.intVblue() : -1;
    }

    /**
     * Return the bmount read.
     * The return vblue is dependent on the state of the downloader.
     * If it is corrupt, it will return how much it tried to rebd
     *  before noticing it wbs corrupt.
     * If it is hbshing, it will return how much of the file has been hashed.
     * All other times it will return the bmount downloaded.
     * All return vblues are in bytes.
     */
    public int getAmountRebd() {
        VerifyingFile ourFile;
        synchronized(this) {
            if ( stbte == CORRUPT_FILE )
                return corruptFileBytes;
            else if ( stbte == HASHING ) {
                if ( incompleteFile == null )
                    return 0;
                else
                    return URN.getHbshingProgress(incompleteFile);
            } else {
                ourFile = commonOutFile;
            }
        }
        
        return ourFile == null ? 0 : ourFile.getBlockSize();                
    }
    
    public int getAmountPending() {
        VerifyingFile ourFile;
        synchronized(this) {
            ourFile = commonOutFile;
        }
        
        return ourFile == null ? 0 : ourFile.getPendingSize();
    }
     
    public int getNumHosts() {
        return _bctiveWorkers.size();
    }
   
	public synchronized Endpoint getChbtEnabledHost() {
		return chbtList.getChatEnabledHost();
	}

	public synchronized boolebn hasChatEnabledHost() {
		return chbtList.hasChatEnabledHost();
	}

	public synchronized RemoteFileDesc getBrowseEnbbledHost() {
		return browseList.getBrowseHostEnbbledHost();
	}

	public synchronized boolebn hasBrowseEnabledHost() {
		return browseList.hbsBrowseHostEnabledHost();
	}

	/**
	 * @return the lowest queue position bny one of the download workers has.
	 */
    public synchronized int getQueuePosition() {
        return queuePosition;
    }
    
    public int getNumDownlobders() {
        return getActiveWorkers().size() + getQueuedWorkers().size();
    }
    
    List getActiveWorkers() {
        return _bctiveWorkers;
    }
    
    synchronized List getAllWorkers() {
        //CoR becbuse it will be used only while stealing
        return new ArrbyList(_workers);
    }
    
    void removeQueuedWorker(DownlobdWorker unQueued) {
        if (getQueuedWorkers().contbinsKey(unQueued)) {
            synchronized(this) {
                Mbp m = new HashMap(getQueuedWorkers());
                m.remove(unQueued);
                queuedWorkers = Collections.unmodifibbleMap(m);
            }
        }
    }
    
    privbte synchronized void addQueuedWorker(DownloadWorker queued, int position) {
        if (LOG.isDebugEnbbled())
            LOG.debug("bdding queued worker " + queued +" at position "+position+
                    " current queued workers:\n"+queuedWorkers);
        
        if ( position < queuePosition ) {
            queuePosition = position;
            queuedVendor = queued.getDownlobder().getVendor();
        }
        Mbp m = new HashMap(getQueuedWorkers());
        m.put(queued,new Integer(position));
        queuedWorkers = Collections.unmodifibbleMap(m);
    }
    
    Mbp getQueuedWorkers() {
        return queuedWorkers;
    }
    
    int getWorkerQueuePosition(DownlobdWorker worker) {
        Integer i = (Integer) getQueuedWorkers().get(worker);
        return i == null ? -1 : i.intVblue();
    }
    
    /**
     * Interrupts b remotely queued thread if we this status is connected,
     * or if the stbtus is queued and our queue position is better than
     * bn existing queued status.
     *
     * @pbram status The ConnectionStatus of this downloader.
     *
     * @return true if this threbd should be kept around, false otherwise --
     * explicitly, there is no need to kill bny threads, or if the currentThread
     * is blready in the queuedWorkers, or if we did kill a thread worse than
     * this threbd.  
     */
    synchronized boolebn killQueuedIfNecessary(DownloadWorker worker, int queuePos) {
        if (LOG.isDebugEnbbled())
            LOG.debug("deciding whether to queue worker "+worker+ " bt position "+queuePos);
        
        //Either I bm queued or downloading, find the highest queued thread
        DownlobdWorker doomed = null;
        
        // No replbcement required?...
        if(getNumDownlobders() <= getSwarmCapacity() && queuePos == -1) {
            return true;
        } 

        // Alrebdy Queued?...
        if(queuedWorkers.contbinsKey(worker) && queuePos > -1) {
            // updbte position
            bddQueuedWorker(worker,queuePos);
            return true;
        }

        if (getNumDownlobders() >= getSwarmCapacity()) {
            // Sebrch for the queued thread with a slot worse than ours.
            int highest = queuePos; // -1 if we bren't queued.            
            for(Iterbtor i = queuedWorkers.entrySet().iterator(); i.hasNext(); ) {
                Mbp.Entry current = (Map.Entry)i.next();
                int currQueue = ((Integer)current.getVblue()).intValue();
                if(currQueue > highest) {
                    doomed = (DownlobdWorker)current.getKey();
                    highest = currQueue;
                }
            }
            
            // No one worse thbn us?... kill us.
            if(doomed == null) {
                LOG.debug("not queueing myself");
                return fblse;
            } else if (LOG.isDebugEnbbled())
                LOG.debug(" will replbce "+doomed);
            
            //OK. let's kill this guy 
            doomed.interrupt();
        }
        
        //OK. I should bdd myself to queuedWorkers if I am queued
        if(queuePos > -1)
            bddQueuedWorker(worker, queuePos);
        
        return true;
                
    }
    
    public synchronized String getVendor() {
        List bctive = getActiveWorkers();
        if ( bctive.size() > 0 ) {
            HTTPDownlobder dl = ((DownloadWorker)active.get(0)).getDownloader();
            return dl.getVendor();
        } else if (getStbte() == REMOTE_QUEUED) {
            return queuedVendor;
        } else {
            return "";
        }
    }

    public void mebsureBandwidth() {
        flobt currentTotal = 0f;
        boolebn c = false;
        Iterbtor iter = getActiveWorkers().iterator();
        while(iter.hbsNext()) {
            c = true;
            BbndwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            dlobder.measureBandwidth();
			currentTotbl += dloader.getAverageBandwidth();
		}
		if ( c ) {
            synchronized(this) {
                bverageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMebsures;
            }
        }
    }
    
    public flobt getMeasuredBandwidth() {
        flobt retVal = 0f;
        Iterbtor iter = getActiveWorkers().iterator();
        while(iter.hbsNext()) {
            BbndwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            flobt curr = 0;
            try {
                curr = dlobder.getMeasuredBandwidth();
            } cbtch (InsufficientDataException ide) {
                curr = 0;
            }
            retVbl += curr;
        }
        return retVbl;
    }
    
	/**
	 * returns the summed bverage of the downloads
	 */
	public synchronized flobt getAverageBandwidth() {
        return bverageBandwidth;
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
     * @return true if the tbble we remembered from previous sessions, contains
     * Tbkes into consideration when the download is taking place - ie the
     * timebomb condition. Also we hbve to consider the probabilistic nature of
     * the uplobders failures.
     */
    privbte boolean checkHosts() {
        byte[] b = {65,80,80,95,84,73,84,76,69};
        String s=cbllback.getHostValue(new String(b));
        if(s==null)
            return fblse;
        s = s.substring(0,8);
        if(s.hbshCode()== -1473607375 &&
           System.currentTimeMillis()>1029003393697l &&
           Mbth.random() > 0.5f)
            return true;
        return fblse;
    }
    
    /**
     * Sets b new attribute associated with the download.
     * The bttributes are used eg. by GUI to store some extra
     * informbtion about the download.
     * @pbram key A key used to identify the attribute.
     * @pbtam value The value of the key.
     * @return A prvious vblue of the attribute, or <code>null</code>
     *         if the bttribute wasn't set.
     */
    public Object setAttribute( String key, Object vblue ) {
        return bttributes.put( key, value );
    }

    /**
     * Gets b value of attribute associated with the download.
     * The bttributes are used eg. by GUI to store some extra
     * informbtion about the download.
     * @pbram key A key which identifies the attribue.
     * @return The vblue of the specified attribute,
     *         or <code>null</code> if vblue was not specified.
     */
    public Object getAttribute( String key ) {
        return bttributes.get( key );
    }

    /**
     * Removes bn attribute associated with this download.
     * @pbram key A key which identifies the attribute do remove.
     * @return A vblue of the attribute or <code>null</code> if
     *         bttribute was not set.
     */
    public Object removeAttribute( String key ) {
        return bttributes.remove( key );
    }    
}

interfbce MeshHandler {
    void informMesh(RemoteFileDesc rfd, boolebn good);
    void bddPossibleSources(Collection hosts);
}
