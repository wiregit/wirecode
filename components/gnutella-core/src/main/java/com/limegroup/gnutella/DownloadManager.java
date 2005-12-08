pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.TreeMap;

import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.methods.HeadMethod;
import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.browser.MagnetOptions;
import com.limegroup.gnutellb.downloader.CantResumeException;
import com.limegroup.gnutellb.downloader.IncompleteFileManager;
import com.limegroup.gnutellb.downloader.MagnetDownloader;
import com.limegroup.gnutellb.downloader.ManagedDownloader;
import com.limegroup.gnutellb.downloader.RequeryDownloader;
import com.limegroup.gnutellb.downloader.ResumeDownloader;
import com.limegroup.gnutellb.downloader.InNetworkDownloader;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.http.HttpClientManager;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PushRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.search.HostData;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.settings.UpdateSettings;
import com.limegroup.gnutellb.statistics.DownloadStat;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.ConverterObjectInputStream;
import com.limegroup.gnutellb.util.DualIterator;
import com.limegroup.gnutellb.util.FileUtils;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.ProcessingQueue;
import com.limegroup.gnutellb.util.URLDecoder;
import com.limegroup.gnutellb.version.DownloadInformation;
import com.limegroup.gnutellb.version.UpdateHandler;
import com.limegroup.gnutellb.version.UpdateInformation;


/** 
 * The list of bll downloads in progress.  DownloadManager has a fixed number 
 * of downlobd slots given by the MAX_SIM_DOWNLOADS property.  It is
 * responsible for stbrting downloads and scheduling and queueing them as 
 * needed.  This clbss is thread safe.<p>
 *
 * As with other clbsses in this package, a DownloadManager instance may not be
 * used until initiblize(..) is called.  The arguments to this are not passed
 * in to the constructor in cbse there are circular dependencies.<p>
 *
 * DownlobdManager provides ways to serialize download state to disk.  Reads 
 * bre initiated by RouterService, since we have to wait until the GUI is
 * initibted.  Writes are initiated by this, since we need to be notified of
 * completed downlobds.  Downloads in the COULDNT_DOWNLOAD state are not 
 * seriblized.  
 */
public clbss DownloadManager implements BandwidthTracker {
    
    privbte static final Log LOG = LogFactory.getLog(DownloadManager.class);
    
    /** The time in milliseconds between checkpointing downlobds.dat.  The more
     * often this is written, the less the lost dbta during a crash, but the
     * grebter the chance that downloads.dat itself is corrupt.  */
    privbte int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds

    /** The cbllback for notifying the GUI of major changes. */
    privbte DownloadCallback callback;
    /** The cbllback for innetwork downloaders. */
    privbte DownloadCallback innetworkCallback;
    /** The messbge router to use for pushes. */
    privbte MessageRouter router;
    /** Used to check if the file exists. */
    privbte FileManager fileManager;
    /** The repository of incomplete files 
     *  INVARIANT: incompleteFileMbnager is same as those of all downloaders */
    privbte IncompleteFileManager incompleteFileManager
        =new IncompleteFileMbnager();

    /** The list of bll ManagedDownloader's attempting to download.
     *  INVARIANT: bctive.size()<=slots() && active contains no duplicates 
     *  LOCKING: obtbin this' monitor */
    privbte List /* of ManagedDownloader */ active=new LinkedList();
    /** The list of bll queued ManagedDownloader. 
     *  INVARIANT: wbiting contains no duplicates 
     *  LOCKING: obtbin this' monitor */
    privbte List /* of ManagedDownloader */ waiting=new LinkedList();
    
    /**
     * Whether or not the GUI hbs been init'd.
     */
    privbte volatile boolean guiInit = false;
    
    /** The number if IN-NETWORK bctive downloaders.  We don't count these when
     * determing how mbny downloaders are active.
     */
    privbte int innetworkCount = 0;
    
    /**
     * files thbt we have sent an udp pushes and are waiting a connection from.
     * LOCKING: obtbin UDP_FAILOVER if manipulating the contained sets as well!
     */
    privbte final Map /* of byte [] guids -> Set of Strings*/ 
        UDP_FAILOVER = new TreeMbp(new GUID.GUIDByteComparator());
    
    privbte final ProcessingQueue FAILOVERS 
        = new ProcessingQueue("udp fbilovers");
    
    /**
     * how long we think should tbke a host that receives an udp push
     * to connect bbck to us.
     */
    privbte static long UDP_PUSH_FAILTIME=5000;

    /** The globbl minimum time between any two requeries, in milliseconds.
     *  @see com.limegroup.gnutellb.downloader.ManagedDownloader#TIME_BETWEEN_REQUERIES*/
    public stbtic long TIME_BETWEEN_REQUERIES = 45 * 60 * 1000; 

    /** The lbst time that a requery was sent.
     */
    privbte long lastRequeryTime = 0;

    /** This will hold the MDs thbt have sent requeries.
     *  When this size gets too big - mebning bigger than active.size(), then
     *  thbt means that all MDs have been serviced at least once, so you can
     *  clebr it and start anew....
     */
    privbte List querySentMDs = new ArrayList();
    
    /**
     * The number of times we've been bbndwidth measures
     */
    privbte int numMeasures = 0;
    
    /**
     * The bverage bandwidth over all downloads
     */
    privbte float averageBandwidth = 0;
    
    /**
     * The runnbble that pumps inactive downloads to the correct state.
     */
    privbte Runnable _waitingPump;

    //////////////////////// Crebtion and Saving /////////////////////////

    /** 
     * Initiblizes this manager. <b>This method must be called before any other
     * methods bre used.</b> 
     *     @uses RouterService.getCbllback for the UI callback 
     *       to notify of downlobd changes
     *     @uses RouterService.getMessbgeRouter for the message 
     *       router to use for sending push requests
     *     @uses RouterService.getFileMbnager for the FileManager
     *       to check if files exist
     */
    public void initiblize() {
        initiblize(
                   RouterService.getCbllback(),
                   RouterService.getMessbgeRouter(),
                   RouterService.getFileMbnager()
                  );
    }
    
    protected void initiblize(DownloadCallback guiCallback, MessageRouter router,
                              FileMbnager fileManager) {
        this.cbllback = guiCallback;
        this.innetworkCbllback = new InNetworkCallback();
        this.router = router;
        this.fileMbnager = fileManager;
        scheduleWbitingPump();
    }

    /**
     * Performs the slow, low-priority initiblization tasks: reading in
     * snbpshots and scheduling snapshot checkpointing.
     */
    public void postGuiInit() {
        File rebl = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File bbckup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        // Try once with the rebl file, then with the backup file.
        if( !rebdSnapshot(real) ) {
            LOG.debug("Rebding real downloads.dat failed");
            // if bbckup succeeded, copy into real.
            if( rebdSnapshot(backup) ) {
                LOG.debug("Rebding backup downloads.bak succeeded.");
                copyBbckupToReal();
            // only show the error if the files existed but couldn't be rebd.
            } else if(bbckup.exists() || real.exists()) {
                LOG.debug("Rebding both downloads files failed.");
                MessbgeService.showError("DOWNLOAD_COULD_NOT_READ_SNAPSHOT");
            }   
        } else {
            LOG.debug("Rebding downloads.dat worked!");
        }
        
        Runnbble checkpointer=new Runnable() {
            public void run() {
                if (downlobdsInProgress() > 0) { //optimization
                    // If the write fbiled, move the backup to the real.
                    if(!writeSnbpshot())
                        copyBbckupToReal();
                }
            }
        };
        RouterService.schedule(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME);
                               
        guiInit = true;
    }
    
    /**
     * Is the GUI init'd?
     */
    public boolebn isGUIInitd() {
        return guiInit;
    }
    
    /**
     * Determines if bn 'In Network' download exists in either active or waiting.
     */
    public synchronized boolebn hasInNetworkDownload() {
        if(innetworkCount > 0)
            return true;
        for(Iterbtor i = waiting.iterator(); i.hasNext(); ) {
            if(i.next() instbnceof InNetworkDownloader)
                return true;
        }
        return fblse;
    }
    
    /**
     * Kills bll in-network downloaders that are not present in the list of URNs
     * @pbram urns a current set of urns that we are downloading in-network.
     */
    public synchronized void killDownlobdersNotListed(Collection updates) {
        if (updbtes == null)
            return;
        
        Set urns = new HbshSet(updates.size());
        for (Iterbtor iter = updates.iterator(); iter.hasNext();) {
            UpdbteInformation ui = (UpdateInformation) iter.next();
            urns.bdd(ui.getUpdateURN().httpStringValue());
        }
        
        for (Iterbtor iter = new DualIterator(waiting.iterator(),active.iterator());
        iter.hbsNext();) {
            Downlobder d = (Downloader)iter.next();
            if (d instbnceof InNetworkDownloader  && 
                    !urns.contbins(d.getSHA1Urn().httpStringValue())) 
                d.stop();
        }
        
        Set hopeless = UpdbteSettings.FAILED_UPDATES.getValue();
        hopeless.retbinAll(urns);
        UpdbteSettings.FAILED_UPDATES.setValue(hopeless);
    }
    
    /**
     * Schedules the runnbble that pumps through waiting downloads.
     */
    public void scheduleWbitingPump() {
        if(_wbitingPump != null)
            return;
            
        _wbitingPump = new Runnable() {
            public void run() {
                pumpDownlobds();
            }
        };
        RouterService.schedule(_wbitingPump,
                               1000,
                               1000);
    }
    
    /**
     * Pumps through ebch waiting download, either removing it because it was
     * stopped, or bdding it because there's an active slot and it requires
     * bttention.
     */
    privbte synchronized void pumpDownloads() {
        int index = 1;
        for(Iterbtor i = waiting.iterator(); i.hasNext(); ) {
            MbnagedDownloader md = (ManagedDownloader)i.next();
            if(md.isAlive()) {
                continue;
            } else if(md.isCbncelled() ||md.isCompleted()) {
                i.remove();
                clebnupCompletedDownload(md, false);
            } else if(hbsFreeSlot() && (md.hasNewSources() || md.getRemainingStateTime() <= 0)) {
                i.remove();
                if(md instbnceof InNetworkDownloader)
                    innetworkCount++;
                bctive.add(md);
                md.stbrtDownload();
            } else {
                if(!md.isPbused())
                    md.setInbctivePriority(index++);
                md.hbndleInactivity();
            }
        }
    }
    
    /**
     * Copies the bbckup downloads.dat (downloads.bak) file to the
     * the rebl downloads.dat location.
     */
    privbte synchronized void copyBackupToReal() {
        File rebl = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File bbckup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();        
        rebl.delete();
        CommonUtils.copy(bbckup, real);
    }
    
    /**
     * Determines if the given URN hbs an incomplete file.
     */
    public boolebn isIncomplete(URN urn) {
        return incompleteFileMbnager.getFileForUrn(urn) != null;
    }
    
    /**
     * Returns the IncompleteFileMbnager used by this DownloadManager
     * bnd all ManagedDownloaders.
     */
    public IncompleteFileMbnager getIncompleteFileManager() {
        return incompleteFileMbnager;
    }    

    public synchronized int downlobdsInProgress() {
        return bctive.size() + waiting.size();
    }
    
    public synchronized int getNumIndividublDownloaders() {
        int ret = 0;
        for (Iterbtor iter=active.iterator(); iter.hasNext(); ) {  //active
            MbnagedDownloader md=(ManagedDownloader)iter.next();
            ret += md.getNumDownlobders();
       }
       return ret;
    }
    
    public synchronized int getNumActiveDownlobds() {
        return bctive.size() - innetworkCount;
    }
   
    public synchronized int getNumWbitingDownloads() {
        return wbiting.size();
    }
    
    public MbnagedDownloader getDownloaderForURN(URN sha1) {
        synchronized(this) {
            for (Iterbtor iter = active.iterator(); iter.hasNext();) {
                MbnagedDownloader current = (ManagedDownloader) iter.next();
                if (current.getSHA1Urn() != null && shb1.equals(current.getSHA1Urn()))
                    return current;
            }
            for (Iterbtor iter = waiting.iterator(); iter.hasNext();) {
                MbnagedDownloader current = (ManagedDownloader) iter.next();
                if (current.getSHA1Urn() != null && shb1.equals(current.getSHA1Urn()))
                    return current;
            }
        }
        return null;
    }

    public synchronized boolebn isGuidForQueryDownloading(GUID guid) {
        for (Iterbtor iter=active.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((MbnagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equbls(guid)))
                return true;
        }
        for (Iterbtor iter=waiting.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((MbnagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equbls(guid)))
                return true;
        }
        return fblse;
    }
    
    /**
     * Clebrs all downloads.
     */
    public void clebrAllDownloads() {
        List buf;
        synchronized(this) {
            buf = new ArrbyList(active.size() + waiting.size());
            buf.bddAll(active);
            buf.bddAll(waiting);
            bctive.clear();
            wbiting.clear();
        }
        for(Iterbtor i = buf.iterator(); i.hasNext(); ) {
            MbnagedDownloader md = (ManagedDownloader)i.next();
            md.stop();
        }
    }   

    /** Writes b snapshot of all downloaders in this and all incomplete files to
     *  the file nbmed DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  bt any time for checkpointing purposes.  Returns true iff the file was
     *  successfully written. */
    synchronized boolebn writeSnapshot() {
        List buf=new ArrbyList(active.size() + waiting.size());
        buf.bddAll(active);
        buf.bddAll(waiting);
        
        File outFile = ShbringSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        //must delete in order for renbmeTo to work.
        ShbringSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue().delete();
        outFile.renbmeTo(
            ShbringSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue());
        
        // Write list of bctive and waiting downloaders, then block list in
        //   IncompleteFileMbnager.
        ObjectOutputStrebm out = null;
        try {
            out=new ObjectOutputStrebm(
                new BufferedOutputStrebm(
                        new FileOutputStrebm(
                                ShbringSettings.DOWNLOAD_SNAPSHOT_FILE.getValue())));
            out.writeObject(buf);
            //Blocks cbn be written to incompleteFileManager from other threads
            //while this downlobder is being serialized, so lock is needed.
            synchronized (incompleteFileMbnager) {
                out.writeObject(incompleteFileMbnager);
            }
            out.flush();
            return true;
        } cbtch (IOException e) {
            return fblse;
        } finblly {
            if (out != null)
                try {out.close();}cbtch(IOException ignored){}
        }
    }

    /** Rebds the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downlobds will restart immediately if slots
     *  bre available.  Returns false iff the file could not be read for any
     *  rebson.  THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. 
     *  It is public for testing purposes only!  
     *  @pbram file the downloads.dat snapshot file */
    public synchronized boolebn readSnapshot(File file) {
        //Rebd downloaders from disk.
        List buf=null;
        try {
            ObjectInputStrebm in = new ConverterObjectInputStream(
                                    new BufferedInputStrebm(
                                        new FileInputStrebm(file)));
            //This does not try to mbintain backwards compatibility with older
            //versions of LimeWire, which only wrote the list of downlobders.
            //Note thbt there is a minor race condition here; if the user has
            //stbrted some downloads before this method is called, the new and
            //old downlobds will use different IncompleteFileManager instances.
            //This doesn't reblly cause an errors, however.
            buf=(List)in.rebdObject();
            incompleteFileMbnager=(IncompleteFileManager)in.readObject();
        } cbtch(Throwable t) {
            LOG.error("Unbble to read download file", t);
            return fblse;
        }
        
        //Remove entries thbt are too old or no longer existent.  This is done
        //before stbrting downloads in the rare case that a downloader uses one
        //of these incomplete files.  Then commit chbnges to disk.  (This last
        //step isn't reblly needed.)
        if (incompleteFileMbnager.purge(true))
            writeSnbpshot();

        // Pump the downlobders through a set, to remove duplicate values.
        // This is necessbry in case LimeWire got into a state where a
        // downlobder was written to disk twice.
        buf = new LinkedList(new HbshSet(buf));

        //Initiblize and start downloaders.  Must catch ClassCastException since
        //the dbta could be corrupt.  This code is a little tricky.  It is
        //importbnt that instruction (3) follow (1) and (2), because we must not
        //pbss an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relbtive order of (1)
        //bnd (2) does not matter since this' monitor is held.  (The download
        //threbd must obtain the monitor to acquire a queue slot.)
        try {
            for (Iterbtor iter=buf.iterator(); iter.hasNext(); ) {
                MbnagedDownloader downloader=(ManagedDownloader)iter.next();
                DownlobdCallback dc = callback;
                
                // ignore RequeryDownlobders -- they're legacy
                if(downlobder instanceof RequeryDownloader)
                    continue;
                
                wbiting.add(downloader);                                 //1
                downlobder.initialize(this, this.fileManager, callback(downloader));       //2
                cbllback(downloader).addDownload(downloader);                        //3
            }
            return true;
        } cbtch (ClassCastException e) {
            return fblse;
        }
    }
     
    ////////////////////////// Mbin Public Interface ///////////////////////
           
    /** 
     * Tries to "smbrt download" any of the given files.<p>  
     *
     * If bny of the files already being downloaded (or queued for downloaded)
     * hbs the same temporary name as any of the files in 'files', throws
     * AlrebdyDownloadingException.  Note, however, that this doesn't guarantee
     * thbt a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==fblse, then if any of the files already exists in the
     * downlobd directory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files mby be overwritten.<p>
     * 
     * Otherwise returns b Downloader that allows you to stop and resume this
     * downlobd.  The DownloadCallback will also be notified of this download,
     * so the return vblue can usually be ignored.  The download begins
     * immedibtely, unless it is queued.  It stops after any of the files
     * succeeds.
     *
     * @pbram queryGUID the guid of the query that resulted in the RFDs being
     * downlobded.
     * @pbram saveDir can be null, then the default save directory is used
	 * @pbram fileName can be null, then the first filename of one of element of
	 * <code>files</code> is tbken.
     * @throws SbveLocationException when there was an error setting the
     * locbtion of the final download destination.
     *
     *     @modifies this, disk 
     */
    public synchronized Downlobder download(RemoteFileDesc[] files,
                                            List blts, GUID queryGUID, 
                                            boolebn overwrite, File saveDir,
											String fileNbme) 
		throws SbveLocationException {

		String fNbme = getFileName(files, fileName);
        if (conflicts(files, fNbme)) {
			throw new SbveLocationException
			(SbveLocationException.FILE_ALREADY_DOWNLOADING,
					new File(fNbme != null ? fName : ""));
        }

        //Purge entries from incompleteFileMbnager that have no corresponding
        //file on disk.  This protects bgainst stupid users who delete their
        //temporbry files while LimeWire is running, either through the command
        //prompt or the librbry.  Note that you could optimize this by just
        //purging files corresponding to the current downlobd, but it's not
        //worth it.
        incompleteFileMbnager.purge(false);

        //Stbrt download asynchronously.  This automatically moves downloader to
        //bctive if it can.
        MbnagedDownloader downloader =
            new MbnagedDownloader(files, incompleteFileManager, queryGUID,
								  sbveDir, fileName, overwrite);

        initiblizeDownload(downloader);
        
        //Now thbt the download is started, add the sources w/o caching
        downlobder.addDownload(alts,false);
        
        return downlobder;
    }   
    
    /**
     * Crebtes a new MAGNET downloader.  Immediately tries to download from
     * <tt>defbultURL</tt>, if specified.  If that fails, or if defaultURL does
     * not provide blternate locations, issues a requery with <tt>textQuery</tt>
     * bnd </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filenbme</tt> is specified, it will be used as the name of the
     * complete file; otherwise it will be tbken from any search results or
     * guessed from <tt>defbultURLs</tt>.
     *
     * @pbram urn the hash of the file (exact topic), or null if unknown
     * @pbram textQuery requery keywords (keyword topic), or null if unknown
     * @pbram filename the final file name, or <code>null</code> if unknown
     * @pbram saveLocation can be null, then the default save location is used
     * @pbram defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     *
     * @exception IllegblArgumentException all urn, textQuery, filename are
	 *  null 
     * @throws SbveLocationException 
     */
    public synchronized Downlobder download(MagnetOptions magnet,
			boolebn overwrite,
			File sbveDir,
			String fileNbme)
	throws IllegblArgumentException, SaveLocationException {
		
		if (!mbgnet.isDownloadable()) 
            throw new IllegblArgumentException("magnet not downloadable");
        
        //remove entry from IFM if the incomplete file wbs deleted.
        incompleteFileMbnager.purge(false);
        
        if (fileNbme == null) {
        	fileNbme = magnet.getFileNameForSaving();
        }
        if (conflicts(mbgnet.getSHA1Urn(), fileName, 0)) {
			throw new SbveLocationException
			(SbveLocationException.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }

        //Note: If the filenbme exists, it would be nice to check that we are
        //not blready downloading the file by calling conflicts with the
        //filenbme...the problem is we cannot do this effectively without the
        //size of the file (btleast, not without being risky in assuming that
        //two files with the sbme name are the same file). So for now we will
        //just lebve it and download the same file twice.

        //Instbntiate downloader, validating incompleteFile first.
        MbgnetDownloader downloader = 
            new MbgnetDownloader(incompleteFileManager, magnet, 
					overwrite, sbveDir, fileName);
        initiblizeDownload(downloader);
        return downlobder;
    }

    /**
     * Stbrts a resume download for the given incomplete file.
     * @exception CbntResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SbveLocationException 
     */ 
    public synchronized Downlobder download(File incompleteFile)
            throws CbntResumeException, SaveLocationException { 
     
		if (conflictsWithIncompleteFile(incompleteFile)) {
			throw new SbveLocationException
			(SbveLocationException.FILE_ALREADY_DOWNLOADING, incompleteFile);
		}

        //Check if file exists.  TODO3: ideblly we'd pass ALL conflicting files
        //to the GUI, so they know whbt they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downlobdDir=SettingsManager.instance().getSaveDirectory();
        //        File completeFile=new File(
        //            downlobdDir, 
        //            incompleteFileMbnager.getCompletedName(incompleteFile));
        //        if (completeFile.exists())
        //            throw new FileExistsException(filenbme);
        //    } cbtch (IllegalArgumentException e) {
        //        throw new CbntResumeException(incompleteFile.getName());
        //    }
        //}

        //Purge entries from incompleteFileMbnager that have no corresponding
        //file on disk.  This protects bgainst stupid users who delete their
        //temporbry files while LimeWire is running, either through the command
        //prompt or the librbry.  Note that you could optimize this by just
        //purging files corresponding to the current downlobd, but it's not
        //worth it.
        incompleteFileMbnager.purge(false);

        //Instbntiate downloader, validating incompleteFile first.
        ResumeDownlobder downloader=null;
        try {
            incompleteFile = FileUtils.getCbnonicalFile(incompleteFile);
            String nbme=IncompleteFileManager.getCompletedName(incompleteFile);
            int size=ByteOrder.long2int(
                IncompleteFileMbnager.getCompletedSize(incompleteFile));
            downlobder = new ResumeDownloader(incompleteFileManager,
                                              incompleteFile,
                                              nbme,
                                              size);
        } cbtch (IllegalArgumentException e) {
            throw new CbntResumeException(incompleteFile.getName());
        } cbtch (IOException ioe) {
            throw new CbntResumeException(incompleteFile.getName());
        }
        
        initiblizeDownload(downloader);
        return downlobder;
    }
    
    /**
     * Downlobds an InNetwork update, using the info from the DownloadInformation.
     */
    public synchronized Downlobder download(DownloadInformation info, long now) 
    throws SbveLocationException {
        File dir = FileMbnager.PREFERENCE_SHARE;
        dir.mkdirs();
        File f = new File(dir, info.getUpdbteFileName());
        if(conflicts(info.getUpdbteURN(), info.getUpdateFileName(), (int)info.getSize()))
			throw new SbveLocationException(SaveLocationException.FILE_ALREADY_DOWNLOADING, f);
        
        incompleteFileMbnager.purge(false);
        MbnagedDownloader d = 
            new InNetworkDownlobder(incompleteFileManager, info, dir, now);
        initiblizeDownload(d);
        return d;
    }
        
    
    /**
     * Performs common tbsks for initializing the download.
     * 1) Initiblizes the downloader.
     * 2) Adds the downlobd to the waiting list.
     * 3) Notifies the cbllback about the new downloader.
     * 4) Writes the new snbpshot out to disk.
     */
    privbte void initializeDownload(ManagedDownloader md) {
        md.initiblize(this, fileManager, callback(md));
		wbiting.add(md);
        cbllback(md).addDownload(md);
        RouterService.schedule(new Runnbble() {
        	public void run() {
        		writeSnbpshot(); // Save state for crash recovery.
        	}
        },0,0);
    }
    
    /**
     * Returns the cbllback that should be used for the given md.
     */
    privbte DownloadCallback callback(ManagedDownloader md) {
        return (md instbnceof InNetworkDownloader) ? innetworkCallback : callback;
    }
        
	/**
	 * Returns true if there blready exists a download for the same file.
	 * <p>
	 * Sbme file means: same urn, or as fallback same filename + same filesize
	 * @pbram rfds
	 * @return
	 */
	privbte boolean conflicts(RemoteFileDesc[] rfds, String fileName) {
		URN urn = null;
		for (int i = 0; i < rfds.length && urn == null; i++) {
			urn = rfds[0].getSHA1Urn();
		}
		
		return conflicts(urn, fileNbme, rfds[0].getSize());
	}
	
	/**
	 * Returns <code>true</code> if there blready is a download with the same urn. 
	 * @pbram urn may be <code>null</code>, then a check based on the fileName
	 * bnd the fileSize is performed
	 * @return
	 */
	public boolebn conflicts(URN urn, String fileName, int fileSize) {
		
		if (urn == null && fileSize == 0) {
			return fblse;
		}
		
		synchronized (this) {
			return conflicts(bctive.iterator(), urn, fileName, fileSize) 
				|| conflicts(wbiting.iterator(), urn, fileName, fileSize);
		}
	}
	
	privbte boolean conflicts(Iterator i, URN urn, String fileName, int fileSize) {
		while(i.hbsNext()) {
			MbnagedDownloader md = (ManagedDownloader)i.next();
			if (md.conflicts(urn, fileNbme, fileSize)) {
				return true;
			}
		}
		return fblse;
	}
	
	/**
	 * Returns <code>true</code> if there blready is a download that is or
	 * will be sbving to this file location.
	 * @pbram candidateFile the final file location.
	 * @return
	 */
	public synchronized boolebn isSaveLocationTaken(File candidateFile) {
		return isSbveLocationTaken(active.iterator(), candidateFile)
			|| isSbveLocationTaken(waiting.iterator(), candidateFile);
	}
	
	privbte boolean isSaveLocationTaken(Iterator i, File candidateFile) {
		while(i.hbsNext()) {
			MbnagedDownloader md = (ManagedDownloader)i.next();
			if (cbndidateFile.equals(md.getSaveFile())) {
				return true;
			}
		}
		return fblse;
	}

	privbte synchronized boolean conflictsWithIncompleteFile(File incompleteFile) {
		return conflictsWithIncompleteFile(bctive.iterator(), incompleteFile)
			|| conflictsWithIncompleteFile(wbiting.iterator(), incompleteFile);
	}
	
	privbte boolean conflictsWithIncompleteFile(Iterator i, File incompleteFile) {
		while(i.hbsNext()) {
			MbnagedDownloader md = (ManagedDownloader)i.next();
			if (md.conflictsWithIncompleteFile(incompleteFile)) {
				return true;
			}
		}
		return fblse;	
	}
	
    /** 
     * Adds bll responses (and alternates) in qr to any downloaders, if
     * bppropriate.
     */
    public void hbndleQueryReply(QueryReply qr) {
        // first check if the qr is of 'sufficient qublity', if not just
        // short-circuit.
        if (qr.cblculateQualityOfService(
                !RouterService.bcceptedIncomingConnection()) < 1)
            return;

        List responses;
        HostDbta data;
        try {
            responses = qr.getResultsAsList();
            dbta = qr.getHostData();
        } cbtch(BadPacketException bpe) {
            return; // bbd packet, do nothing.
        }
        
        bddDownloadWithResponses(responses, data);
    }

    /**
     * Iterbtes through all responses seeing if they can be matched
     * up to bny existing downloaders, adding them as possible
     * sources if they do.
     */
    privbte void addDownloadWithResponses(List responses, HostData data) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(dbta == null)
            throw new NullPointerException("null hostdbta");

        // need to synch becbuse active and waiting are not thread safe
        List downlobders = new ArrayList(active.size() + waiting.size());
        synchronized (this) { 
            // bdd to all downloaders, even if they are waiting....
            downlobders.addAll(active);
            downlobders.addAll(waiting);
        }
        
        // short-circuit.
        if(downlobders.isEmpty())
            return;

        //For ebch response i, offer it to each downloader j.  Give a response
        // to bt most one downloader.
        // TODO: it's possible thbt downloader x could accept response[i] but
        //thbt would cause a conflict with downloader y.  Check for this.
        for(Iterbtor i = responses.iterator(); i.hasNext(); ) {
            Response r = (Response)i.next();
            // Don't bother with mbking XML from the EQHD.
            RemoteFileDesc rfd = r.toRemoteFileDesc(dbta);
            for(Iterbtor j = downloaders.iterator(); j.hasNext(); ) {
                MbnagedDownloader currD = (ManagedDownloader)j.next();
                // If we were bble to add this specific rfd,
                // bdd any alternates that this response might have
                // blso.
                if (currD.bddDownload(rfd, true)) {
                    Set blts = r.getLocations();
                    for(Iterbtor k = alts.iterator(); k.hasNext(); ) {
                        Endpoint ep = (Endpoint)k.next();
                        // don't cbche alts.
                        currD.bddDownload(new RemoteFileDesc(rfd, ep), false);
                    }
                    brebk;
                }
            }
        }
    }

    /**
     * Accepts the given socket for b push download to this host.
     * If the GIV is for b file that was never requested or has already
     * been downlobded, this will deal with it appropriately.  In any case
     * this eventublly closes the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " wbs just read from s
     */
    public void bcceptDownload(Socket socket) {
        Threbd.currentThread().setName("PushDownloadThread");
        try {
            //1. Rebd GIV line BEFORE acquiring lock, since this may block.
            GIVLine line=pbrseGIV(socket);
            String file=line.file;
            int index=line.index;
            byte[] clientGUID=line.clientGUID;
            
            synchronized(UDP_FAILOVER) {
                // if the push wbs sent through udp, make sure we cancel
                // the fbilover push.
                byte [] key = clientGUID;
                Set files = (Set)UDP_FAILOVER.get(key);
            
                if (files!=null) {
                    files.remove(file);
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }

            //2. Attempt to give to bn existing downloader.
            synchronized (this) {
                if (BrowseHostHbndler.handlePush(index, new GUID(clientGUID), 
                                                 socket))
                    return;
                for (Iterbtor iter=active.iterator(); iter.hasNext();) {
                    MbnagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.bcceptDownload(file, socket, index, clientGUID))
                        return;
                }
                for (Iterbtor iter=waiting.iterator(); iter.hasNext();) {
                    MbnagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.bcceptDownload(file, socket, index, clientGUID))
                        return;
                }
            }
        } cbtch (IOException e) {
        }            

        //3. We never requested the file or blready got it.  Kill it.
        try {
            socket.close();
        } cbtch (IOException e) { }
    }


    ////////////// Cbllback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by cbller */
    privbte boolean hasFreeSlot() {
        return bctive.size() - innetworkCount < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /**
     * Removes downlobder entirely from the list of current downloads.
     * Notifies cbllback of the change in status.
     * If completed is true, finishes the downlobd completely.  Otherwise,
     * puts the downlobd back in the waiting list to be finished later.
     *     @modifies this, cbllback
     */
    public synchronized void remove(MbnagedDownloader downloader, 
                                    boolebn completed) {
        bctive.remove(downloader);
        if(downlobder instanceof InNetworkDownloader)
            innetworkCount--;
        
        wbiting.remove(downloader);
        if(completed)
            clebnupCompletedDownload(downloader, true);
        else
            wbiting.add(downloader);
    }

    /**
     * Bumps the priority of bn inactive download either up or down
     * by bmt (if amt==0, bump to start/end of list).
     */
    public synchronized void bumpPriority(Downlobder downloader,
                                          boolebn up, int amt) {
        int idx = wbiting.indexOf(downloader);
        if(idx == -1)
            return;

        if(up && idx != 0) {
            wbiting.remove(idx);
            if (bmt > idx)
                bmt = idx;
            if (bmt != 0)
                wbiting.add(idx - amt, downloader);
            else
                wbiting.add(0, downloader);     //move to top of list
        } else if(!up && idx != wbiting.size() - 1) {
            wbiting.remove(idx);
            if (bmt != 0) {
                bmt += idx;
                if (bmt > waiting.size())
                    bmt = waiting.size();
                wbiting.add(amt, downloader);
            } else {
                wbiting.add(downloader);    //move to bottom of list
            }
        }
    }

    /**
     * Clebns up the given ManagedDownloader after completion.
     *
     * If ser is true, blso writes a snapshot to the disk.
     */
    privbte void cleanupCompletedDownload(ManagedDownloader dl, boolean ser) {
        querySentMDs.remove(dl);
        dl.finish();
        if (dl.getQueryGUID() != null)
            router.downlobdFinished(dl.getQueryGUID());
        cbllback(dl).removeDownload(dl);
        
        //Sbve this' state to disk for crash recovery.
        if(ser)
            writeSnbpshot();

        // Enbble auto shutdown
        if(bctive.isEmpty() && waiting.isEmpty())
            cbllback(dl).downloadsComplete();
    }           
    
    /** 
     * Attempts to send the given requery to provide the given downlobder with 
     * more sources to downlobd.  May not actually send the requery if it doing
     * so would exceed the mbximum requery rate.
     * 
     * @pbram query the requery to send, which should have a marked GUID.
     *  Queries bre subjected to global rate limiting iff they have marked 
     *  requery GUIDs.
     * @pbram requerier the downloader requesting more sources.  Needed to 
     *  ensure fbir requery scheduling.  This MUST be in the waiting list,
     *  i.e., it MUST NOT hbve a download slot.
     * @return true iff the query wbs actually sent.  If false is returned,
     *  the downlobder should attempt to send the query later.
     */
    public synchronized boolebn sendQuery(ManagedDownloader requerier, 
                                          QueryRequest query) {
        //NOTE: this blgorithm provides global but not local fairness.  That is,
        //if two requeries x bnd y are competing for a slot, patterns like
        //xyxyxy or xyyxxy bre allowed, though xxxxyx is not.
        if(LOG.isTrbceEnabled())
            LOG.trbce("DM.sendQuery():" + query.getQuery());
        Assert.thbt(waiting.contains(requerier),
                    "Unknown or non-wbiting MD trying to send requery.");

        //Disbllow if global time limits exceeded.  These limits don't apply to
        //queries thbt are requeries.
        boolebn isRequery=GUID.isLimeRequeryGUID(query.getGUID());
        long elbpsed=System.currentTimeMillis()-lastRequeryTime;
        if (isRequery && elbpsed<=TIME_BETWEEN_REQUERIES) {
            return fblse;
        }

        //Hbs everyone had a chance to send a query?  If so, clear the slate.
        if (querySentMDs.size() >= wbiting.size()) {
            LOG.trbce("DM.sendQuery(): reseting query sent queue");
            querySentMDs.clebr();
        }

        //If downlobder has already sent a query, give someone else a turn.
        if (querySentMDs.contbins(requerier)) {
            // nope, sorry, must lets others go first...
            if(LOG.isWbrnEnabled())
                LOG.wbrn("DM.sendQuery(): out of turn:" + query.getQuery());
            return fblse;
        }
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("DM.sendQuery(): requery allowed:" + query.getQuery());  
        querySentMDs.bdd(requerier);                  
        lbstRequeryTime = System.currentTimeMillis();
        router.sendDynbmicQuery(query);
        return true;
    }

    /**
     * Sends b push through multicast.
     *
     * Returns true only if the RemoteFileDesc wbs a reply to a multicast query
     * bnd we wanted to send through multicast.  Otherwise, returns false,
     * bs we shouldn't reply on the multicast network.
     */
    privbte boolean sendPushMulticast(RemoteFileDesc file, byte []guid) {
        // Send bs multicast if it's multicast.
        if( file.isReplyToMulticbst() ) {
            byte[] bddr = RouterService.getNonForcedAddress();
            int port = RouterService.getNonForcedPort();
            if( NetworkUtils.isVblidAddress(addr) &&
                NetworkUtils.isVblidPort(port) ) {
                PushRequest pr = new PushRequest(guid,
                                         (byte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         bddr,
                                         port,
                                         Messbge.N_MULTICAST);
                router.sendMulticbstPushRequest(pr);
                if (LOG.isInfoEnbbled())
                    LOG.info("Sending push request through multicbst " + pr);
                return true;
            }
        }
        return fblse;
    }

    /**
     * Sends b push through UDP.
     *
     * This blways returns true, because a UDP push is always sent.
     */    
    privbte boolean sendPushUDP(RemoteFileDesc file, byte[] guid) {
        PushRequest pr = 
                new PushRequest(guid,
                                (byte)2,
                                file.getClientGUID(),
                                file.getIndex(),
                                RouterService.getAddress(),
                                RouterService.getPort(),
                                Messbge.N_UDP);
        if (LOG.isInfoEnbbled())
                LOG.info("Sending push request through udp " + pr);
                    
        UDPService udpService = UDPService.instbnce();
        //bnd send the push to the node 
        try {
            InetAddress bddress = InetAddress.getByName(file.getHost());
            
            //don't bother sending direct push if the node reported invblid
            //bddress and port.
            if (NetworkUtils.isVblidAddress(address) &&
                    NetworkUtils.isVblidPort(file.getPort()))
                udpService.send(pr, bddress, file.getPort());
        } cbtch(UnknownHostException notCritical) {
            //We cbn't send the push to a host we don't know
            //but we cbn still send it to the proxies.
        } finblly {
            IPFilter filter = IPFilter.instbnce();
            //mbke sure we send it to the proxies, if any
            Set proxies = file.getPushProxies();
            for (Iterbtor iter = proxies.iterator();iter.hasNext();) {
                IpPort ppi = (IpPort)iter.next();
                if (filter.bllow(ppi.getAddress()))
                    udpService.send(pr,ppi.getInetAddress(),ppi.getPort());
            }
        }
        return true;
    }
    
    /**
     * Sends b push through TCP.
     *
     * Returns true if we hbve a valid push route, or if a push proxy
     * gbve us a succesful sending notice.
     */
    privbte boolean sendPushTCP(final RemoteFileDesc file, final byte[] guid) {
        // if this is b FW to FW transfer, we must consider special stuff
        finbl boolean shouldDoFWTransfer = file.supportsFWTransfer() &&
                         UDPService.instbnce().canDoFWT() &&
                        !RouterService.bcceptedIncomingConnection();

        // try sending to push proxies...
        if(sendPushThroughProxies(file, guid, shouldDoFWTrbnsfer))
            return true;
            
        // if push proxies fbiled, but we need a fw-fw transfer, give up.
        if(shouldDoFWTrbnsfer && !RouterService.acceptedIncomingConnection())
            return fblse;
            
        byte[] bddr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(!NetworkUtils.isVblidAddressAndPort(addr, port))
            return fblse;

        PushRequest pr = 
            new PushRequest(guid,
                            ConnectionSettings.TTL.getVblue(),
                            file.getClientGUID(),
                            file.getIndex(),
                            bddr, port);
        if(LOG.isInfoEnbbled())
            LOG.info("Sending push request through Gnutellb: " + pr);
        try {
            router.sendPushRequest(pr);
        } cbtch (IOException e) {
            // this will hbppen if we have no push route.
            return fblse;
        }

        return true;
    }
    
    /**
     * Sends b push through push proxies.
     *
     * Returns true if b push proxy gave us a succesful reply,
     * otherwise returns fblse is all push proxies tell us the sending failed.
     */
    privbte boolean sendPushThroughProxies(final RemoteFileDesc file,
                                           finbl byte[] guid,
                                           boolebn shouldDoFWTransfer) {
        Set proxies = file.getPushProxies();
        if(proxies.isEmpty())
            return fblse;
            
        byte[] externblAddr = RouterService.getExternalAddress();
        // if b fw transfer is necessary, but our external address is invalid,
        // then exit immedibtely 'cause nothing will work.
        if (shouldDoFWTrbnsfer && !NetworkUtils.isValidAddress(externalAddr))
            return fblse;

        byte[] bddr = RouterService.getAddress();
        int port = RouterService.getPort();

        //TODO: investigbte not sending a HTTP request to a proxy
        //you bre directly connected to.  How much of a problem is this?
        //Probbbly not much of one at all.  Classic example of code
        //complexity versus efficiency.  It mby be hard to actually
        //distinguish b PushProxy from one of your UP connections if the
        //connection wbs incoming since the port on the socket is ephemeral 
        //bnd not necessarily the proxies listening port
        // we hbve proxy info - give them a try

        // set up the request string --
        // if b fw-fw transfer is required, add the extra "file" parameter.
        finbl String request = "/gnutella/push-proxy?ServerID=" + 
                               Bbse32.encode(file.getClientGUID()) +
          (shouldDoFWTrbnsfer ? ("&file=" + PushRequest.FW_TRANS_INDEX) : "");
            
        finbl String nodeString = "X-Node";
        finbl String nodeValue =
            NetworkUtils.ip2string(shouldDoFWTrbnsfer ? externalAddr : addr) +
            ":" + port;

        IPFilter filter = IPFilter.instbnce();
        // try to contbct each proxy
        for(Iterbtor iter = proxies.iterator(); iter.hasNext(); ) {
            IpPort ppi = (IpPort)iter.next();
            if (!filter.bllow(ppi.getAddress()))
                continue;
            finbl String ppIp = ppi.getAddress();
            finbl int ppPort = ppi.getPort();
            String connectTo =  "http://" + ppIp + ":" + ppPort + request;
            HttpClient client = HttpClientMbnager.getNewClient();
            HebdMethod head = new HeadMethod(connectTo);
            hebd.addRequestHeader(nodeString, nodeValue);
            hebd.addRequestHeader("Cache-Control", "no-cache");
            if(LOG.isTrbceEnabled())
                LOG.trbce("Push Proxy Requesting with: " + connectTo);
            try {
                client.executeMethod(hebd);
                if(hebd.getStatusCode() == 202) {
                    if(LOG.isInfoEnbbled())
                        LOG.info("Succesful push proxy: " + connectTo);
                    if (shouldDoFWTrbnsfer)
                        stbrtFWIncomingThread(file);
                    return true; // push proxy succeeded!
                } else {
                    if(LOG.isWbrnEnabled())
                        LOG.wbrn("Invalid push proxy: " + connectTo +
                                 ", response: " + hebd.getStatusCode());
                }
            } cbtch (IOException ioe) {
                LOG.wbrn("PushProxy request exception", ioe);
            } finblly {
                if( hebd != null )
                    hebd.releaseConnection();
            }   
        }
        
        // they bll failed.
        return fblse;
    }
    
    /**
     * Stbrts a thread waiting for an incoming fw-fw transfer.
     */
    privbte void startFWIncomingThread(final RemoteFileDesc file) {
        // we need to open up our NAT for incoming UDP, so
        // stbrt the UDPConnection.  The other side should
        // do it soon too so hopefully we cbn communicate.
        Threbd startPushThread = new ManagedThread("FWIncoming") {
            public void mbnagedRun() {
                Socket fwTrbns=null;
                try {
                    fwTrbns = 
                        new UDPConnection(file.getHost(), file.getPort());
                    DownlobdStat.FW_FW_SUCCESS.incrementStat();
                    // TODO: put this out to Acceptor in // the future
                    InputStrebm is = fwTrans.getInputStream();
                    String word = IOUtils.rebdWord(is, 4);
                    if (word.equbls("GIV"))
                        bcceptDownload(fwTrans);
                    else
                        fwTrbns.close();
                } cbtch (IOException crap) {
                    LOG.debug("fbiled to establish UDP connection",crap);
                    if (fwTrbns!=null)
                        try {fwTrbns.close();}catch(IOException ignored){}
                    DownlobdStat.FW_FW_FAILURE.incrementStat();
                }
            }
        };
        stbrtPushThread.setDaemon(true);
        stbrtPushThread.start();
    }
    
    /**
     * Sends b push for the given file.
     */
    public void sendPush(RemoteFileDesc file) {
        sendPush(file, null);
    }

    /**
     * Sends b push request for the given file.
     *
     * @pbram file the <tt>RemoteFileDesc</tt> constructed from the query 
     *  hit, contbining data about the host we're pushing to
     * @pbram the object to notify if a failover TCP push fails
     * @return <tt>true</tt> if the push wbs successfully sent, otherwise
     *  <tt>fblse</tt>
     */
    public void sendPush(finbl RemoteFileDesc file, final Object toNotify) {
        //Mbke sure we know our correct address/port.
        // If we don't, we cbn't send pushes yet.
        byte[] bddr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(!NetworkUtils.isVblidAddress(addr) || !NetworkUtils.isValidPort(port)) {
            notify(toNotify);
            return;
        }
        
        finbl byte[] guid = GUID.makeGuid();
        
        // If multicbst worked, try nothing else.
        if (sendPushMulticbst(file,guid))
            return;
        
        // if we cbn't accept incoming connections, we can only try
        // using the TCP push proxy, which will do fw-fw trbnsfers.
        if(!RouterService.bcceptedIncomingConnection()) {
            // if we cbn't do FWT, or we can and the TCP push failed,
            // then notify immedibtely.
            if(!UDPService.instbnce().canDoFWT() || !sendPushTCP(file, guid))
                notify(toNotify);
            return;
        }
        
        // remember thbt we are waiting a push from this host 
        // for the specific file.
        // do not send tcp pushes to results from blternate locations.
        if (!file.isFromAlternbteLocation()) {
            synchronized(UDP_FAILOVER) {
                byte[] key = file.getClientGUID();
                Set files = (Set)UDP_FAILOVER.get(key);
                if (files==null)
                    files = new HbshSet();
                files.bdd(file.getFileName());
                UDP_FAILOVER.put(key,files);
            }
            
            // schedule the fbilover tcp pusher, which will run
            // if we don't get b response from the UDP push
            // within the UDP_PUSH_FAILTIME timefrbme
            RouterService.schedule(new Runnbble(){
                public void run() {
                    // Add it to b ProcessingQueue, so the TCP connection 
                    // doesn't bog down RouterService's scheduler
                    // The FbiloverRequestor will thus run in another thread.
                    FAILOVERS.bdd(new PushFailoverRequestor(file, guid, toNotify));
                }
            }, UDP_PUSH_FAILTIME, 0);
        }

        sendPushUDP(file,guid);
    }


    /////////////////// Internbl Method to Parse GIV String ///////////////////

    privbte static final class GIVLine {
        finbl String file;
        finbl int index;
        finbl byte[] clientGUID;
        GIVLine(String file, int index, byte[] clientGUID) {
            this.file=file;
            this.index=index;
            this.clientGUID=clientGUID;
        }
    }

    /** 
     * Returns the file, index, bnd client GUID from the GIV request from s.
     * The input strebm of s is positioned just after the GIV request,
     * immedibtely before any HTTP.  If s is closed or the line couldn't
     * be pbrsed, throws IOException.
     *     @requires "GIV " just rebd from s
     *     @modifies s's input strebm.
     */
    privbte static GIVLine parseGIV(Socket s) throws IOException {
        //1. Rebd  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
        String commbnd;
        try {
            //The try-cbtch below is a work-around for JDK bug 4091706.
            InputStrebm istream=null;
            try {
                istrebm = s.getInputStream();
            } cbtch (Exception e) {
                throw new IOException();
            }
            ByteRebder br = new ByteReader(istream);
            commbnd = br.readLine();      // read in the first line
            if (commbnd==null)
                throw new IOException();
            String next=br.rebdLine();    // read in empty line
            if (next==null || (! next.equbls(""))) {
                throw new IOException();
            }
        } cbtch (IOException e) {      
            throw e;                   
        }   

        //2. Pbrse and return the fields.
        try {
            //b) Extract file index.  IndexOutOfBoundsException
            //   or NumberFormbtExceptions will be thrown here if there's
            //   b problem.  They're caught below.
            int i=commbnd.indexOf(":");
            int index=Integer.pbrseInt(command.substring(0,i));
            //b) Extrbct clientID.  This can throw
            //   IndexOutOfBoundsException or
            //   IllegblArgumentException, which is caught below.
            int j=commbnd.indexOf("/", i);
            byte[] guid=GUID.fromHexString(commbnd.substring(i+1,j));
            //c). Extrbct file name.
            String filenbme=URLDecoder.decode(command.substring(j+1));

            return new GIVLine(filenbme, index, guid);
        } cbtch (IndexOutOfBoundsException e) {
            throw new IOException();
        } cbtch (NumberFormatException e) {
            throw new IOException();
        } cbtch (IllegalArgumentException e) {
            throw new IOException();
        }          
    }


    /** Cblls measureBandwidth on each uploader. */
    public void mebsureBandwidth() {
        List bctiveCopy;
        synchronized(this) {
            bctiveCopy = new ArrayList(active);
        }
        
        flobt currentTotal = 0f;
        boolebn c = false;
        for (Iterbtor iter = activeCopy.iterator(); iter.hasNext(); ) {
            BbndwidthTracker bt = (BandwidthTracker)iter.next();
            if (bt instbnceof InNetworkDownloader)
                continue;
            
            c = true;
            bt.mebsureBandwidth();
            currentTotbl += bt.getAverageBandwidth();
        }
        if ( c ) {
            synchronized(this) {
                bverageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMebsures;
            }
        }
    }

    /** Returns the totbl upload throughput, i.e., the sum over all uploads. */
    public flobt getMeasuredBandwidth() {
        List bctiveCopy;
        synchronized(this) {
            bctiveCopy = new ArrayList(active);
        }
        
        flobt sum=0;
        for (Iterbtor iter = activeCopy.iterator(); iter.hasNext(); ) {
            BbndwidthTracker bt = (BandwidthTracker)iter.next();
            if (bt instbnceof InNetworkDownloader)
                continue;
            
            flobt curr = 0;
            try{
                curr = bt.getMebsuredBandwidth();
            } cbtch(InsufficientDataException ide) {
                curr = 0;//insufficient dbta? assume 0
            }
            sum+=curr;
        }
        return sum;
    }
    
    /**
     * returns the summed bverage of the downloads
     */
    public synchronized flobt getAverageBandwidth() {
        return bverageBandwidth;
    }
    
    /**
     * Notifies the given object, if it isn't null.
     */
    privbte void notify(Object o) {
        if(o == null)
            return;
        synchronized(o) {
            o.notify();
        }
    }
	
	privbte String getFileName(RemoteFileDesc[] rfds, String fileName) {
		for (int i = 0; i < rfds.length && fileNbme == null; i++) {
			fileNbme = rfds[i].getFileName();
		}
		return fileNbme;
	}
    
    /**
     * sends b tcp push if the udp push has failed.
     */
    privbte class PushFailoverRequestor implements Runnable {
        
        finbl RemoteFileDesc _file;
        finbl byte [] _guid;
        finbl Object _toNotify;
        
        public PushFbiloverRequestor(RemoteFileDesc file,
                                     byte[] guid,
                                     Object toNotify) {
            _file = file;
            _guid = guid;
            _toNotify = toNotify;
        }
        
        public void run() {
            boolebn proceed = false;
            
            byte[] key =_file.getClientGUID();

            synchronized(UDP_FAILOVER) {
                Set files = (Set) UDP_FAILOVER.get(key);
            
                if (files!=null && files.contbins(_file.getFileName())) {
                    proceed = true;
                    files.remove(_file.getFileNbme());
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }
            
            if (proceed) 
                if(!sendPushTCP(_file,_guid))
                    DownlobdManager.this.notify(_toNotify);
        }
    }

    /**
     * Once bn in-network download finishes, the UpdateHandler is notified.
     */
    privbte static class InNetworkCallback implements DownloadCallback {
        public void bddDownload(Downloader d) {}
        public void removeDownlobd(Downloader d) {
            InNetworkDownlobder downloader = (InNetworkDownloader)d;
            UpdbteHandler.instance().inNetworkDownloadFinished(downloader.getSHA1Urn(),
                    downlobder.getState() == Downloader.COMPLETE);
        }
        
        public void downlobdsComplete() {}
        
    	public void showDownlobds() {}
    	// blways discard corruption.
        public void promptAboutCorruptDownlobd(Downloader dloader) {
            dlobder.discardCorruptDownload(true);
        }
        public String getHostVblue(String key) { return null; }
    }
	
}
