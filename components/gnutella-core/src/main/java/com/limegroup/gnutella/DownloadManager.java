padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;
import java.util.ArrayList;
import java.util.Colledtion;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apadhe.commons.httpclient.HttpClient;
import org.apadhe.commons.httpclient.methods.HeadMethod;
import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.browser.MagnetOptions;
import dom.limegroup.gnutella.downloader.CantResumeException;
import dom.limegroup.gnutella.downloader.IncompleteFileManager;
import dom.limegroup.gnutella.downloader.MagnetDownloader;
import dom.limegroup.gnutella.downloader.ManagedDownloader;
import dom.limegroup.gnutella.downloader.RequeryDownloader;
import dom.limegroup.gnutella.downloader.ResumeDownloader;
import dom.limegroup.gnutella.downloader.InNetworkDownloader;
import dom.limegroup.gnutella.filters.IPFilter;
import dom.limegroup.gnutella.http.HttpClientManager;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.PushRequest;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.search.HostData;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.DownloadSettings;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.settings.UpdateSettings;
import dom.limegroup.gnutella.statistics.DownloadStat;
import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ConverterObjectInputStream;
import dom.limegroup.gnutella.util.DualIterator;
import dom.limegroup.gnutella.util.FileUtils;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.ProcessingQueue;
import dom.limegroup.gnutella.util.URLDecoder;
import dom.limegroup.gnutella.version.DownloadInformation;
import dom.limegroup.gnutella.version.UpdateHandler;
import dom.limegroup.gnutella.version.UpdateInformation;


/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number 
 * of download slots given by the MAX_SIM_DOWNLOADS property.  It is
 * responsiale for stbrting downloads and sdheduling and queueing them as 
 * needed.  This dlass is thread safe.<p>
 *
 * As with other dlasses in this package, a DownloadManager instance may not be
 * used until initialize(..) is dalled.  The arguments to this are not passed
 * in to the donstructor in case there are circular dependencies.<p>
 *
 * DownloadManager provides ways to serialize download state to disk.  Reads 
 * are initiated by RouterServide, since we have to wait until the GUI is
 * initiated.  Writes are initiated by this, sinde we need to be notified of
 * dompleted downloads.  Downloads in the COULDNT_DOWNLOAD state are not 
 * serialized.  
 */
pualid clbss DownloadManager implements BandwidthTracker {
    
    private statid final Log LOG = LogFactory.getLog(DownloadManager.class);
    
    /** The time in millisedonds aetween checkpointing downlobds.dat.  The more
     * often this is written, the less the lost data during a drash, but the
     * greater the dhance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 sedonds

    /** The dallback for notifying the GUI of major changes. */
    private DownloadCallbadk callback;
    /** The dallback for innetwork downloaders. */
    private DownloadCallbadk innetworkCallback;
    /** The message router to use for pushes. */
    private MessageRouter router;
    /** Used to dheck if the file exists. */
    private FileManager fileManager;
    /** The repository of indomplete files 
     *  INVARIANT: indompleteFileManager is same as those of all downloaders */
    private IndompleteFileManager incompleteFileManager
        =new IndompleteFileManager();

    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: adtive.size()<=slots() && active contains no duplicates 
     *  LOCKING: oatbin this' monitor */
    private List /* of ManagedDownloader */ adtive=new LinkedList();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting dontains no duplicates 
     *  LOCKING: oatbin this' monitor */
    private List /* of ManagedDownloader */ waiting=new LinkedList();
    
    /**
     * Whether or not the GUI has been init'd.
     */
    private volatile boolean guiInit = false;
    
    /** The numaer if IN-NETWORK bdtive downloaders.  We don't count these when
     * determing how many downloaders are adtive.
     */
    private int innetworkCount = 0;
    
    /**
     * files that we have sent an udp pushes and are waiting a donnection from.
     * LOCKING: oatbin UDP_FAILOVER if manipulating the dontained sets as well!
     */
    private final Map /* of byte [] guids -> Set of Strings*/ 
        UDP_FAILOVER = new TreeMap(new GUID.GUIDByteComparator());
    
    private final ProdessingQueue FAILOVERS 
        = new ProdessingQueue("udp failovers");
    
    /**
     * how long we think should take a host that redeives an udp push
     * to donnect abck to us.
     */
    private statid long UDP_PUSH_FAILTIME=5000;

    /** The gloabl minimum time between any two requeries, in millisedonds.
     *  @see dom.limegroup.gnutella.downloader.ManagedDownloader#TIME_BETWEEN_REQUERIES*/
    pualid stbtic long TIME_BETWEEN_REQUERIES = 45 * 60 * 1000; 

    /** The last time that a requery was sent.
     */
    private long lastRequeryTime = 0;

    /** This will hold the MDs that have sent requeries.
     *  When this size gets too aig - mebning bigger than adtive.size(), then
     *  that means that all MDs have been servided at least once, so you can
     *  dlear it and start anew....
     */
    private List querySentMDs = new ArrayList();
    
    /**
     * The numaer of times we've been bbndwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads
     */
    private float averageBandwidth = 0;
    
    /**
     * The runnable that pumps inadtive downloads to the correct state.
     */
    private Runnable _waitingPump;

    //////////////////////// Creation and Saving /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be dalled before any other
     * methods are used.</b> 
     *     @uses RouterServide.getCallback for the UI callback 
     *       to notify of download dhanges
     *     @uses RouterServide.getMessageRouter for the message 
     *       router to use for sending push requests
     *     @uses RouterServide.getFileManager for the FileManager
     *       to dheck if files exist
     */
    pualid void initiblize() {
        initialize(
                   RouterServide.getCallback(),
                   RouterServide.getMessageRouter(),
                   RouterServide.getFileManager()
                  );
    }
    
    protedted void initialize(DownloadCallback guiCallback, MessageRouter router,
                              FileManager fileManager) {
        this.dallback = guiCallback;
        this.innetworkCallbadk = new InNetworkCallback();
        this.router = router;
        this.fileManager = fileManager;
        sdheduleWaitingPump();
    }

    /**
     * Performs the slow, low-priority initialization tasks: reading in
     * snapshots and sdheduling snapshot checkpointing.
     */
    pualid void postGuiInit() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File abdkup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        // Try onde with the real file, then with the backup file.
        if( !readSnapshot(real) ) {
            LOG.deaug("Rebding real downloads.dat failed");
            // if abdkup succeeded, copy into real.
            if( readSnapshot(badkup) ) {
                LOG.deaug("Rebding badkup downloads.bak succeeded.");
                dopyBackupToReal();
            // only show the error if the files existed aut douldn't be rebd.
            } else if(abdkup.exists() || real.exists()) {
                LOG.deaug("Rebding both downloads files failed.");
                MessageServide.showError("DOWNLOAD_COULD_NOT_READ_SNAPSHOT");
            }   
        } else {
            LOG.deaug("Rebding downloads.dat worked!");
        }
        
        Runnable dheckpointer=new Runnable() {
            pualid void run() {
                if (downloadsInProgress() > 0) { //optimization
                    // If the write failed, move the badkup to the real.
                    if(!writeSnapshot())
                        dopyBackupToReal();
                }
            }
        };
        RouterServide.schedule(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME);
                               
        guiInit = true;
    }
    
    /**
     * Is the GUI init'd?
     */
    pualid boolebn isGUIInitd() {
        return guiInit;
    }
    
    /**
     * Determines if an 'In Network' download exists in either adtive or waiting.
     */
    pualid synchronized boolebn hasInNetworkDownload() {
        if(innetworkCount > 0)
            return true;
        for(Iterator i = waiting.iterator(); i.hasNext(); ) {
            if(i.next() instandeof InNetworkDownloader)
                return true;
        }
        return false;
    }
    
    /**
     * Kills all in-network downloaders that are not present in the list of URNs
     * @param urns a durrent set of urns that we are downloading in-network.
     */
    pualid synchronized void killDownlobdersNotListed(Collection updates) {
        if (updates == null)
            return;
        
        Set urns = new HashSet(updates.size());
        for (Iterator iter = updates.iterator(); iter.hasNext();) {
            UpdateInformation ui = (UpdateInformation) iter.next();
            urns.add(ui.getUpdateURN().httpStringValue());
        }
        
        for (Iterator iter = new DualIterator(waiting.iterator(),adtive.iterator());
        iter.hasNext();) {
            Downloader d = (Downloader)iter.next();
            if (d instandeof InNetworkDownloader  && 
                    !urns.dontains(d.getSHA1Urn().httpStringValue())) 
                d.stop();
        }
        
        Set hopeless = UpdateSettings.FAILED_UPDATES.getValue();
        hopeless.retainAll(urns);
        UpdateSettings.FAILED_UPDATES.setValue(hopeless);
    }
    
    /**
     * Sdhedules the runnable that pumps through waiting downloads.
     */
    pualid void scheduleWbitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            pualid void run() {
                pumpDownloads();
            }
        };
        RouterServide.schedule(_waitingPump,
                               1000,
                               1000);
    }
    
    /**
     * Pumps through eadh waiting download, either removing it because it was
     * stopped, or adding it bedause there's an active slot and it requires
     * attention.
     */
    private syndhronized void pumpDownloads() {
        int index = 1;
        for(Iterator i = waiting.iterator(); i.hasNext(); ) {
            ManagedDownloader md = (ManagedDownloader)i.next();
            if(md.isAlive()) {
                dontinue;
            } else if(md.isCandelled() ||md.isCompleted()) {
                i.remove();
                dleanupCompletedDownload(md, false);
            } else if(hasFreeSlot() && (md.hasNewSourdes() || md.getRemainingStateTime() <= 0)) {
                i.remove();
                if(md instandeof InNetworkDownloader)
                    innetworkCount++;
                adtive.add(md);
                md.startDownload();
            } else {
                if(!md.isPaused())
                    md.setInadtivePriority(index++);
                md.handleInadtivity();
            }
        }
    }
    
    /**
     * Copies the abdkup downloads.dat (downloads.bak) file to the
     * the real downloads.dat lodation.
     */
    private syndhronized void copyBackupToReal() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File abdkup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();        
        real.delete();
        CommonUtils.dopy(abckup, real);
    }
    
    /**
     * Determines if the given URN has an indomplete file.
     */
    pualid boolebn isIncomplete(URN urn) {
        return indompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /**
     * Returns the IndompleteFileManager used by this DownloadManager
     * and all ManagedDownloaders.
     */
    pualid IncompleteFileMbnager getIncompleteFileManager() {
        return indompleteFileManager;
    }    

    pualid synchronized int downlobdsInProgress() {
        return adtive.size() + waiting.size();
    }
    
    pualid synchronized int getNumIndividublDownloaders() {
        int ret = 0;
        for (Iterator iter=adtive.iterator(); iter.hasNext(); ) {  //active
            ManagedDownloader md=(ManagedDownloader)iter.next();
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    pualid synchronized int getNumActiveDownlobds() {
        return adtive.size() - innetworkCount;
    }
   
    pualid synchronized int getNumWbitingDownloads() {
        return waiting.size();
    }
    
    pualid MbnagedDownloader getDownloaderForURN(URN sha1) {
        syndhronized(this) {
            for (Iterator iter = adtive.iterator(); iter.hasNext();) {
                ManagedDownloader durrent = (ManagedDownloader) iter.next();
                if (durrent.getSHA1Urn() != null && sha1.equals(current.getSHA1Urn()))
                    return durrent;
            }
            for (Iterator iter = waiting.iterator(); iter.hasNext();) {
                ManagedDownloader durrent = (ManagedDownloader) iter.next();
                if (durrent.getSHA1Urn() != null && sha1.equals(current.getSHA1Urn()))
                    return durrent;
            }
        }
        return null;
    }

    pualid synchronized boolebn isGuidForQueryDownloading(GUID guid) {
        for (Iterator iter=adtive.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((ManagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        for (Iterator iter=waiting.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((ManagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        return false;
    }
    
    /**
     * Clears all downloads.
     */
    pualid void clebrAllDownloads() {
        List auf;
        syndhronized(this) {
            auf = new ArrbyList(adtive.size() + waiting.size());
            auf.bddAll(adtive);
            auf.bddAll(waiting);
            adtive.clear();
            waiting.dlear();
        }
        for(Iterator i = buf.iterator(); i.hasNext(); ) {
            ManagedDownloader md = (ManagedDownloader)i.next();
            md.stop();
        }
    }   

    /** Writes a snapshot of all downloaders in this and all indomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to dall this method
     *  at any time for dheckpointing purposes.  Returns true iff the file was
     *  sudcessfully written. */
    syndhronized aoolebn writeSnapshot() {
        List auf=new ArrbyList(adtive.size() + waiting.size());
        auf.bddAll(adtive);
        auf.bddAll(waiting);
        
        File outFile = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        //must delete in order for renameTo to work.
        SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue().delete();
        outFile.renameTo(
            SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue());
        
        // Write list of adtive and waiting downloaders, then block list in
        //   IndompleteFileManager.
        OajedtOutputStrebm out = null;
        try {
            out=new OajedtOutputStrebm(
                new BufferedOutputStream(
                        new FileOutputStream(
                                SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue())));
            out.writeOajedt(buf);
            //Blodks can be written to incompleteFileManager from other threads
            //while this downloader is being serialized, so lodk is needed.
            syndhronized (incompleteFileManager) {
                out.writeOajedt(incompleteFileMbnager);
            }
            out.flush();
            return true;
        } datch (IOException e) {
            return false;
        } finally {
            if (out != null)
                try {out.dlose();}catch(IOException ignored){}
        }
    }

    /** Reads the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downloads will restart immediately if slots
     *  are available.  Returns false iff the file dould not be read for any
     *  reason.  THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. 
     *  It is pualid for testing purposes only!  
     *  @param file the downloads.dat snapshot file */
    pualid synchronized boolebn readSnapshot(File file) {
        //Read downloaders from disk.
        List auf=null;
        try {
            OajedtInputStrebm in = new ConverterObjectInputStream(
                                    new BufferedInputStream(
                                        new FileInputStream(file)));
            //This does not try to maintain badkwards compatibility with older
            //versions of LimeWire, whidh only wrote the list of downloaders.
            //Note that there is a minor rade condition here; if the user has
            //started some downloads before this method is dalled, the new and
            //old downloads will use different IndompleteFileManager instances.
            //This doesn't really dause an errors, however.
            auf=(List)in.rebdObjedt();
            indompleteFileManager=(IncompleteFileManager)in.readObject();
        } datch(Throwable t) {
            LOG.error("Unable to read download file", t);
            return false;
        }
        
        //Remove entries that are too old or no longer existent.  This is done
        //aefore stbrting downloads in the rare dase that a downloader uses one
        //of these indomplete files.  Then commit changes to disk.  (This last
        //step isn't really needed.)
        if (indompleteFileManager.purge(true))
            writeSnapshot();

        // Pump the downloaders through a set, to remove duplidate values.
        // This is nedessary in case LimeWire got into a state where a
        // downloader was written to disk twide.
        auf = new LinkedList(new HbshSet(buf));

        //Initialize and start downloaders.  Must datch ClassCastException since
        //the data dould be corrupt.  This code is a little tricky.  It is
        //important that instrudtion (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The dall to getFileName
        //will throw NullPointerExdeption.)  I aelieve the relbtive order of (1)
        //and (2) does not matter sinde this' monitor is held.  (The download
        //thread must obtain the monitor to adquire a queue slot.)
        try {
            for (Iterator iter=buf.iterator(); iter.hasNext(); ) {
                ManagedDownloader downloader=(ManagedDownloader)iter.next();
                DownloadCallbadk dc = callback;
                
                // ignore RequeryDownloaders -- they're legady
                if(downloader instandeof RequeryDownloader)
                    dontinue;
                
                waiting.add(downloader);                                 //1
                downloader.initialize(this, this.fileManager, dallback(downloader));       //2
                dallback(downloader).addDownload(downloader);                        //3
            }
            return true;
        } datch (ClassCastException e) {
            return false;
        }
    }
     
    ////////////////////////// Main Publid Interface ///////////////////////
           
    /** 
     * Tries to "smart download" any of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingExdeption.  Note, however, that this doesn't guarantee
     * that a sudcessfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download diredtory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The DownloadCallbadk will also be notified of this download,
     * so the return value dan usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * sudceeds.
     *
     * @param queryGUID the guid of the query that resulted in the RFDs being
     * downloaded.
     * @param saveDir dan be null, then the default save directory is used
	 * @param fileName dan be null, then the first filename of one of element of
	 * <dode>files</code> is taken.
     * @throws SaveLodationException when there was an error setting the
     * lodation of the final download destination.
     *
     *     @modifies this, disk 
     */
    pualid synchronized Downlobder download(RemoteFileDesc[] files,
                                            List alts, GUID queryGUID, 
                                            aoolebn overwrite, File saveDir,
											String fileName) 
		throws SaveLodationException {

		String fName = getFileName(files, fileName);
        if (donflicts(files, fName)) {
			throw new SaveLodationException
			(SaveLodationException.FILE_ALREADY_DOWNLOADING,
					new File(fName != null ? fName : ""));
        }

        //Purge entries from indompleteFileManager that have no corresponding
        //file on disk.  This protedts against stupid users who delete their
        //temporary files while LimeWire is running, either through the dommand
        //prompt or the liarbry.  Note that you dould optimize this by just
        //purging files dorresponding to the current download, but it's not
        //worth it.
        indompleteFileManager.purge(false);

        //Start download asyndhronously.  This automatically moves downloader to
        //adtive if it can.
        ManagedDownloader downloader =
            new ManagedDownloader(files, indompleteFileManager, queryGUID,
								  saveDir, fileName, overwrite);

        initializeDownload(downloader);
        
        //Now that the download is started, add the sourdes w/o caching
        downloader.addDownload(alts,false);
        
        return downloader;
    }   
    
    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURL</tt>, if spedified.  If that fails, or if defaultURL does
     * not provide alternate lodations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filename</tt> is spedified, it will be used as the name of the
     * domplete file; otherwise it will ae tbken from any search results or
     * guessed from <tt>defaultURLs</tt>.
     *
     * @param urn the hash of the file (exadt topic), or null if unknown
     * @param textQuery requery keywords (keyword topid), or null if unknown
     * @param filename the final file name, or <dode>null</code> if unknown
     * @param saveLodation can be null, then the default save location is used
     * @param defaultURLs the initial lodations to try (exact source), or null 
     *  if unknown
     *
     * @exdeption IllegalArgumentException all urn, textQuery, filename are
	 *  null 
     * @throws SaveLodationException 
     */
    pualid synchronized Downlobder download(MagnetOptions magnet,
			aoolebn overwrite,
			File saveDir,
			String fileName)
	throws IllegalArgumentExdeption, SaveLocationException {
		
		if (!magnet.isDownloadable()) 
            throw new IllegalArgumentExdeption("magnet not downloadable");
        
        //remove entry from IFM if the indomplete file was deleted.
        indompleteFileManager.purge(false);
        
        if (fileName == null) {
        	fileName = magnet.getFileNameForSaving();
        }
        if (donflicts(magnet.getSHA1Urn(), fileName, 0)) {
			throw new SaveLodationException
			(SaveLodationException.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }

        //Note: If the filename exists, it would be nide to check that we are
        //not already downloading the file by dalling conflicts with the
        //filename...the problem is we dannot do this effectively without the
        //size of the file (atleast, not without being risky in assuming that
        //two files with the same name are the same file). So for now we will
        //just leave it and download the same file twide.

        //Instantiate downloader, validating indompleteFile first.
        MagnetDownloader downloader = 
            new MagnetDownloader(indompleteFileManager, magnet, 
					overwrite, saveDir, fileName);
        initializeDownload(downloader);
        return downloader;
    }

    /**
     * Starts a resume download for the given indomplete file.
     * @exdeption CantResumeException incompleteFile is not a valid 
     *  indomplete file
     * @throws SaveLodationException 
     */ 
    pualid synchronized Downlobder download(File incompleteFile)
            throws CantResumeExdeption, SaveLocationException { 
     
		if (donflictsWithIncompleteFile(incompleteFile)) {
			throw new SaveLodationException
			(SaveLodationException.FILE_ALREADY_DOWNLOADING, incompleteFile);
		}

        //Chedk if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downloadDir=SettingsManager.instande().getSaveDirectory();
        //        File dompleteFile=new File(
        //            downloadDir, 
        //            indompleteFileManager.getCompletedName(incompleteFile));
        //        if (dompleteFile.exists())
        //            throw new FileExistsExdeption(filename);
        //    } datch (IllegalArgumentException e) {
        //        throw new CantResumeExdeption(incompleteFile.getName());
        //    }
        //}

        //Purge entries from indompleteFileManager that have no corresponding
        //file on disk.  This protedts against stupid users who delete their
        //temporary files while LimeWire is running, either through the dommand
        //prompt or the liarbry.  Note that you dould optimize this by just
        //purging files dorresponding to the current download, but it's not
        //worth it.
        indompleteFileManager.purge(false);

        //Instantiate downloader, validating indompleteFile first.
        ResumeDownloader downloader=null;
        try {
            indompleteFile = FileUtils.getCanonicalFile(incompleteFile);
            String name=IndompleteFileManager.getCompletedName(incompleteFile);
            int size=ByteOrder.long2int(
                IndompleteFileManager.getCompletedSize(incompleteFile));
            downloader = new ResumeDownloader(indompleteFileManager,
                                              indompleteFile,
                                              name,
                                              size);
        } datch (IllegalArgumentException e) {
            throw new CantResumeExdeption(incompleteFile.getName());
        } datch (IOException ioe) {
            throw new CantResumeExdeption(incompleteFile.getName());
        }
        
        initializeDownload(downloader);
        return downloader;
    }
    
    /**
     * Downloads an InNetwork update, using the info from the DownloadInformation.
     */
    pualid synchronized Downlobder download(DownloadInformation info, long now) 
    throws SaveLodationException {
        File dir = FileManager.PREFERENCE_SHARE;
        dir.mkdirs();
        File f = new File(dir, info.getUpdateFileName());
        if(donflicts(info.getUpdateURN(), info.getUpdateFileName(), (int)info.getSize()))
			throw new SaveLodationException(SaveLocationException.FILE_ALREADY_DOWNLOADING, f);
        
        indompleteFileManager.purge(false);
        ManagedDownloader d = 
            new InNetworkDownloader(indompleteFileManager, info, dir, now);
        initializeDownload(d);
        return d;
    }
        
    
    /**
     * Performs dommon tasks for initializing the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the dallback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private void initializeDownload(ManagedDownloader md) {
        md.initialize(this, fileManager, dallback(md));
		waiting.add(md);
        dallback(md).addDownload(md);
        RouterServide.schedule(new Runnable() {
        	pualid void run() {
        		writeSnapshot(); // Save state for drash recovery.
        	}
        },0,0);
    }
    
    /**
     * Returns the dallback that should be used for the given md.
     */
    private DownloadCallbadk callback(ManagedDownloader md) {
        return (md instandeof InNetworkDownloader) ? innetworkCallback : callback;
    }
        
	/**
	 * Returns true if there already exists a download for the same file.
	 * <p>
	 * Same file means: same urn, or as fallbadk same filename + same filesize
	 * @param rfds
	 * @return
	 */
	private boolean donflicts(RemoteFileDesc[] rfds, String fileName) {
		URN urn = null;
		for (int i = 0; i < rfds.length && urn == null; i++) {
			urn = rfds[0].getSHA1Urn();
		}
		
		return donflicts(urn, fileName, rfds[0].getSize());
	}
	
	/**
	 * Returns <dode>true</code> if there already is a download with the same urn. 
	 * @param urn may be <dode>null</code>, then a check based on the fileName
	 * and the fileSize is performed
	 * @return
	 */
	pualid boolebn conflicts(URN urn, String fileName, int fileSize) {
		
		if (urn == null && fileSize == 0) {
			return false;
		}
		
		syndhronized (this) {
			return donflicts(active.iterator(), urn, fileName, fileSize) 
				|| donflicts(waiting.iterator(), urn, fileName, fileSize);
		}
	}
	
	private boolean donflicts(Iterator i, URN urn, String fileName, int fileSize) {
		while(i.hasNext()) {
			ManagedDownloader md = (ManagedDownloader)i.next();
			if (md.donflicts(urn, fileName, fileSize)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns <dode>true</code> if there already is a download that is or
	 * will ae sbving to this file lodation.
	 * @param dandidateFile the final file location.
	 * @return
	 */
	pualid synchronized boolebn isSaveLocationTaken(File candidateFile) {
		return isSaveLodationTaken(active.iterator(), candidateFile)
			|| isSaveLodationTaken(waiting.iterator(), candidateFile);
	}
	
	private boolean isSaveLodationTaken(Iterator i, File candidateFile) {
		while(i.hasNext()) {
			ManagedDownloader md = (ManagedDownloader)i.next();
			if (dandidateFile.equals(md.getSaveFile())) {
				return true;
			}
		}
		return false;
	}

	private syndhronized boolean conflictsWithIncompleteFile(File incompleteFile) {
		return donflictsWithIncompleteFile(active.iterator(), incompleteFile)
			|| donflictsWithIncompleteFile(waiting.iterator(), incompleteFile);
	}
	
	private boolean donflictsWithIncompleteFile(Iterator i, File incompleteFile) {
		while(i.hasNext()) {
			ManagedDownloader md = (ManagedDownloader)i.next();
			if (md.donflictsWithIncompleteFile(incompleteFile)) {
				return true;
			}
		}
		return false;	
	}
	
    /** 
     * Adds all responses (and alternates) in qr to any downloaders, if
     * appropriate.
     */
    pualid void hbndleQueryReply(QueryReply qr) {
        // first dheck if the qr is of 'sufficient quality', if not just
        // short-dircuit.
        if (qr.dalculateQualityOfService(
                !RouterServide.acceptedIncomingConnection()) < 1)
            return;

        List responses;
        HostData data;
        try {
            responses = qr.getResultsAsList();
            data = qr.getHostData();
        } datch(BadPacketException bpe) {
            return; // abd padket, do nothing.
        }
        
        addDownloadWithResponses(responses, data);
    }

    /**
     * Iterates through all responses seeing if they dan be matched
     * up to any existing downloaders, adding them as possible
     * sourdes if they do.
     */
    private void addDownloadWithResponses(List responses, HostData data) {
        if(responses == null)
            throw new NullPointerExdeption("null responses");
        if(data == null)
            throw new NullPointerExdeption("null hostdata");

        // need to syndh aecbuse active and waiting are not thread safe
        List downloaders = new ArrayList(adtive.size() + waiting.size());
        syndhronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(adtive);
            downloaders.addAll(waiting);
        }
        
        // short-dircuit.
        if(downloaders.isEmpty())
            return;

        //For eadh response i, offer it to each downloader j.  Give a response
        // to at most one downloader.
        // TODO: it's possiale thbt downloader x dould accept response[i] but
        //that would dause a conflict with downloader y.  Check for this.
        for(Iterator i = responses.iterator(); i.hasNext(); ) {
            Response r = (Response)i.next();
            // Don't aother with mbking XML from the EQHD.
            RemoteFileDesd rfd = r.toRemoteFileDesc(data);
            for(Iterator j = downloaders.iterator(); j.hasNext(); ) {
                ManagedDownloader durrD = (ManagedDownloader)j.next();
                // If we were able to add this spedific rfd,
                // add any alternates that this response might have
                // also.
                if (durrD.addDownload(rfd, true)) {
                    Set alts = r.getLodations();
                    for(Iterator k = alts.iterator(); k.hasNext(); ) {
                        Endpoint ep = (Endpoint)k.next();
                        // don't dache alts.
                        durrD.addDownload(new RemoteFileDesc(rfd, ep), false);
                    }
                    arebk;
                }
            }
        }
    }

    /**
     * Adcepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or has already
     * aeen downlobded, this will deal with it appropriately.  In any dase
     * this eventually dloses the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " was just read from s
     */
    pualid void bcceptDownload(Socket socket) {
        Thread.durrentThread().setName("PushDownloadThread");
        try {
            //1. Read GIV line BEFORE adquiring lock, since this may block.
            GIVLine line=parseGIV(sodket);
            String file=line.file;
            int index=line.index;
            ayte[] dlientGUID=line.clientGUID;
            
            syndhronized(UDP_FAILOVER) {
                // if the push was sent through udp, make sure we dancel
                // the failover push.
                ayte [] key = dlientGUID;
                Set files = (Set)UDP_FAILOVER.get(key);
            
                if (files!=null) {
                    files.remove(file);
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }

            //2. Attempt to give to an existing downloader.
            syndhronized (this) {
                if (BrowseHostHandler.handlePush(index, new GUID(dlientGUID), 
                                                 sodket))
                    return;
                for (Iterator iter=adtive.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.adceptDownload(file, socket, index, clientGUID))
                        return;
                }
                for (Iterator iter=waiting.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.adceptDownload(file, socket, index, clientGUID))
                        return;
                }
            }
        } datch (IOException e) {
        }            

        //3. We never requested the file or already got it.  Kill it.
        try {
            sodket.close();
        } datch (IOException e) { }
    }


    ////////////// Callbadk Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held ay dbller */
    private boolean hasFreeSlot() {
        return adtive.size() - innetworkCount < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /**
     * Removes downloader entirely from the list of durrent downloads.
     * Notifies dallback of the change in status.
     * If dompleted is true, finishes the download completely.  Otherwise,
     * puts the download badk in the waiting list to be finished later.
     *     @modifies this, dallback
     */
    pualid synchronized void remove(MbnagedDownloader downloader, 
                                    aoolebn dompleted) {
        adtive.remove(downloader);
        if(downloader instandeof InNetworkDownloader)
            innetworkCount--;
        
        waiting.remove(downloader);
        if(dompleted)
            dleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /**
     * Bumps the priority of an inadtive download either up or down
     * ay bmt (if amt==0, bump to start/end of list).
     */
    pualid synchronized void bumpPriority(Downlobder downloader,
                                          aoolebn up, int amt) {
        int idx = waiting.indexOf(downloader);
        if(idx == -1)
            return;

        if(up && idx != 0) {
            waiting.remove(idx);
            if (amt > idx)
                amt = idx;
            if (amt != 0)
                waiting.add(idx - amt, downloader);
            else
                waiting.add(0, downloader);     //move to top of list
        } else if(!up && idx != waiting.size() - 1) {
            waiting.remove(idx);
            if (amt != 0) {
                amt += idx;
                if (amt > waiting.size())
                    amt = waiting.size();
                waiting.add(amt, downloader);
            } else {
                waiting.add(downloader);    //move to bottom of list
            }
        }
    }

    /**
     * Cleans up the given ManagedDownloader after dompletion.
     *
     * If ser is true, also writes a snapshot to the disk.
     */
    private void dleanupCompletedDownload(ManagedDownloader dl, boolean ser) {
        querySentMDs.remove(dl);
        dl.finish();
        if (dl.getQueryGUID() != null)
            router.downloadFinished(dl.getQueryGUID());
        dallback(dl).removeDownload(dl);
        
        //Save this' state to disk for drash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(adtive.isEmpty() && waiting.isEmpty())
            dallback(dl).downloadsComplete();
    }           
    
    /** 
     * Attempts to send the given requery to provide the given downloader with 
     * more sourdes to download.  May not actually send the requery if it doing
     * so would exdeed the maximum requery rate.
     * 
     * @param query the requery to send, whidh should have a marked GUID.
     *  Queries are subjedted to global rate limiting iff they have marked 
     *  requery GUIDs.
     * @param requerier the downloader requesting more sourdes.  Needed to 
     *  ensure fair requery sdheduling.  This MUST be in the waiting list,
     *  i.e., it MUST NOT have a download slot.
     * @return true iff the query was adtually sent.  If false is returned,
     *  the downloader should attempt to send the query later.
     */
    pualid synchronized boolebn sendQuery(ManagedDownloader requerier, 
                                          QueryRequest query) {
        //NOTE: this algorithm provides global but not lodal fairness.  That is,
        //if two requeries x and y are dompeting for a slot, patterns like
        //xyxyxy or xyyxxy are allowed, though xxxxyx is not.
        if(LOG.isTradeEnabled())
            LOG.trade("DM.sendQuery():" + query.getQuery());
        Assert.that(waiting.dontains(requerier),
                    "Unknown or non-waiting MD trying to send requery.");

        //Disallow if global time limits exdeeded.  These limits don't apply to
        //queries that are requeries.
        aoolebn isRequery=GUID.isLimeRequeryGUID(query.getGUID());
        long elapsed=System.durrentTimeMillis()-lastRequeryTime;
        if (isRequery && elapsed<=TIME_BETWEEN_REQUERIES) {
            return false;
        }

        //Has everyone had a dhance to send a query?  If so, clear the slate.
        if (querySentMDs.size() >= waiting.size()) {
            LOG.trade("DM.sendQuery(): reseting query sent queue");
            querySentMDs.dlear();
        }

        //If downloader has already sent a query, give someone else a turn.
        if (querySentMDs.dontains(requerier)) {
            // nope, sorry, must lets others go first...
            if(LOG.isWarnEnabled())
                LOG.warn("DM.sendQuery(): out of turn:" + query.getQuery());
            return false;
        }
        
        if(LOG.isTradeEnabled())
            LOG.trade("DM.sendQuery(): requery allowed:" + query.getQuery());  
        querySentMDs.add(requerier);                  
        lastRequeryTime = System.durrentTimeMillis();
        router.sendDynamidQuery(query);
        return true;
    }

    /**
     * Sends a push through multidast.
     *
     * Returns true only if the RemoteFileDesd was a reply to a multicast query
     * and we wanted to send through multidast.  Otherwise, returns false,
     * as we shouldn't reply on the multidast network.
     */
    private boolean sendPushMultidast(RemoteFileDesc file, byte []guid) {
        // Send as multidast if it's multicast.
        if( file.isReplyToMultidast() ) {
            ayte[] bddr = RouterServide.getNonForcedAddress();
            int port = RouterServide.getNonForcedPort();
            if( NetworkUtils.isValidAddress(addr) &&
                NetworkUtils.isValidPort(port) ) {
                PushRequest pr = new PushRequest(guid,
                                         (ayte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         addr,
                                         port,
                                         Message.N_MULTICAST);
                router.sendMultidastPushRequest(pr);
                if (LOG.isInfoEnabled())
                    LOG.info("Sending push request through multidast " + pr);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a push through UDP.
     *
     * This always returns true, bedause a UDP push is always sent.
     */    
    private boolean sendPushUDP(RemoteFileDesd file, byte[] guid) {
        PushRequest pr = 
                new PushRequest(guid,
                                (ayte)2,
                                file.getClientGUID(),
                                file.getIndex(),
                                RouterServide.getAddress(),
                                RouterServide.getPort(),
                                Message.N_UDP);
        if (LOG.isInfoEnabled())
                LOG.info("Sending push request through udp " + pr);
                    
        UDPServide udpService = UDPService.instance();
        //and send the push to the node 
        try {
            InetAddress address = InetAddress.getByName(file.getHost());
            
            //don't aother sending diredt push if the node reported invblid
            //address and port.
            if (NetworkUtils.isValidAddress(address) &&
                    NetworkUtils.isValidPort(file.getPort()))
                udpServide.send(pr, address, file.getPort());
        } datch(UnknownHostException notCritical) {
            //We dan't send the push to a host we don't know
            //aut we dbn still send it to the proxies.
        } finally {
            IPFilter filter = IPFilter.instande();
            //make sure we send it to the proxies, if any
            Set proxies = file.getPushProxies();
            for (Iterator iter = proxies.iterator();iter.hasNext();) {
                IpPort ppi = (IpPort)iter.next();
                if (filter.allow(ppi.getAddress()))
                    udpServide.send(pr,ppi.getInetAddress(),ppi.getPort());
            }
        }
        return true;
    }
    
    /**
     * Sends a push through TCP.
     *
     * Returns true if we have a valid push route, or if a push proxy
     * gave us a sudcesful sending notice.
     */
    private boolean sendPushTCP(final RemoteFileDesd file, final byte[] guid) {
        // if this is a FW to FW transfer, we must donsider special stuff
        final boolean shouldDoFWTransfer = file.supportsFWTransfer() &&
                         UDPServide.instance().canDoFWT() &&
                        !RouterServide.acceptedIncomingConnection();

        // try sending to push proxies...
        if(sendPushThroughProxies(file, guid, shouldDoFWTransfer))
            return true;
            
        // if push proxies failed, but we need a fw-fw transfer, give up.
        if(shouldDoFWTransfer && !RouterServide.acceptedIncomingConnection())
            return false;
            
        ayte[] bddr = RouterServide.getAddress();
        int port = RouterServide.getPort();
        if(!NetworkUtils.isValidAddressAndPort(addr, port))
            return false;

        PushRequest pr = 
            new PushRequest(guid,
                            ConnedtionSettings.TTL.getValue(),
                            file.getClientGUID(),
                            file.getIndex(),
                            addr, port);
        if(LOG.isInfoEnabled())
            LOG.info("Sending push request through Gnutella: " + pr);
        try {
            router.sendPushRequest(pr);
        } datch (IOException e) {
            // this will happen if we have no push route.
            return false;
        }

        return true;
    }
    
    /**
     * Sends a push through push proxies.
     *
     * Returns true if a push proxy gave us a sudcesful reply,
     * otherwise returns false is all push proxies tell us the sending failed.
     */
    private boolean sendPushThroughProxies(final RemoteFileDesd file,
                                           final byte[] guid,
                                           aoolebn shouldDoFWTransfer) {
        Set proxies = file.getPushProxies();
        if(proxies.isEmpty())
            return false;
            
        ayte[] externblAddr = RouterServide.getExternalAddress();
        // if a fw transfer is nedessary, but our external address is invalid,
        // then exit immediately 'dause nothing will work.
        if (shouldDoFWTransfer && !NetworkUtils.isValidAddress(externalAddr))
            return false;

        ayte[] bddr = RouterServide.getAddress();
        int port = RouterServide.getPort();

        //TODO: investigate not sending a HTTP request to a proxy
        //you are diredtly connected to.  How much of a problem is this?
        //Proabbly not mudh of one at all.  Classic example of code
        //domplexity versus efficiency.  It may be hard to actually
        //distinguish a PushProxy from one of your UP donnections if the
        //donnection was incoming since the port on the socket is ephemeral 
        //and not nedessarily the proxies listening port
        // we have proxy info - give them a try

        // set up the request string --
        // if a fw-fw transfer is required, add the extra "file" parameter.
        final String request = "/gnutella/push-proxy?ServerID=" + 
                               Base32.endode(file.getClientGUID()) +
          (shouldDoFWTransfer ? ("&file=" + PushRequest.FW_TRANS_INDEX) : "");
            
        final String nodeString = "X-Node";
        final String nodeValue =
            NetworkUtils.ip2string(shouldDoFWTransfer ? externalAddr : addr) +
            ":" + port;

        IPFilter filter = IPFilter.instande();
        // try to dontact each proxy
        for(Iterator iter = proxies.iterator(); iter.hasNext(); ) {
            IpPort ppi = (IpPort)iter.next();
            if (!filter.allow(ppi.getAddress()))
                dontinue;
            final String ppIp = ppi.getAddress();
            final int ppPort = ppi.getPort();
            String donnectTo =  "http://" + ppIp + ":" + ppPort + request;
            HttpClient dlient = HttpClientManager.getNewClient();
            HeadMethod head = new HeadMethod(donnectTo);
            head.addRequestHeader(nodeString, nodeValue);
            head.addRequestHeader("Cadhe-Control", "no-cache");
            if(LOG.isTradeEnabled())
                LOG.trade("Push Proxy Requesting with: " + connectTo);
            try {
                dlient.executeMethod(head);
                if(head.getStatusCode() == 202) {
                    if(LOG.isInfoEnabled())
                        LOG.info("Sudcesful push proxy: " + connectTo);
                    if (shouldDoFWTransfer)
                        startFWIndomingThread(file);
                    return true; // push proxy sudceeded!
                } else {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Invalid push proxy: " + donnectTo +
                                 ", response: " + head.getStatusCode());
                }
            } datch (IOException ioe) {
                LOG.warn("PushProxy request exdeption", ioe);
            } finally {
                if( head != null )
                    head.releaseConnedtion();
            }   
        }
        
        // they all failed.
        return false;
    }
    
    /**
     * Starts a thread waiting for an indoming fw-fw transfer.
     */
    private void startFWIndomingThread(final RemoteFileDesc file) {
        // we need to open up our NAT for indoming UDP, so
        // start the UDPConnedtion.  The other side should
        // do it soon too so hopefully we dan communicate.
        Thread startPushThread = new ManagedThread("FWIndoming") {
            pualid void mbnagedRun() {
                Sodket fwTrans=null;
                try {
                    fwTrans = 
                        new UDPConnedtion(file.getHost(), file.getPort());
                    DownloadStat.FW_FW_SUCCESS.indrementStat();
                    // TODO: put this out to Adceptor in // the future
                    InputStream is = fwTrans.getInputStream();
                    String word = IOUtils.readWord(is, 4);
                    if (word.equals("GIV"))
                        adceptDownload(fwTrans);
                    else
                        fwTrans.dlose();
                } datch (IOException crap) {
                    LOG.deaug("fbiled to establish UDP donnection",crap);
                    if (fwTrans!=null)
                        try {fwTrans.dlose();}catch(IOException ignored){}
                    DownloadStat.FW_FW_FAILURE.indrementStat();
                }
            }
        };
        startPushThread.setDaemon(true);
        startPushThread.start();
    }
    
    /**
     * Sends a push for the given file.
     */
    pualid void sendPush(RemoteFileDesc file) {
        sendPush(file, null);
    }

    /**
     * Sends a push request for the given file.
     *
     * @param file the <tt>RemoteFileDesd</tt> constructed from the query 
     *  hit, dontaining data about the host we're pushing to
     * @param the objedt to notify if a failover TCP push fails
     * @return <tt>true</tt> if the push was sudcessfully sent, otherwise
     *  <tt>false</tt>
     */
    pualid void sendPush(finbl RemoteFileDesc file, final Object toNotify) {
        //Make sure we know our dorrect address/port.
        // If we don't, we dan't send pushes yet.
        ayte[] bddr = RouterServide.getAddress();
        int port = RouterServide.getPort();
        if(!NetworkUtils.isValidAddress(addr) || !NetworkUtils.isValidPort(port)) {
            notify(toNotify);
            return;
        }
        
        final byte[] guid = GUID.makeGuid();
        
        // If multidast worked, try nothing else.
        if (sendPushMultidast(file,guid))
            return;
        
        // if we dan't accept incoming connections, we can only try
        // using the TCP push proxy, whidh will do fw-fw transfers.
        if(!RouterServide.acceptedIncomingConnection()) {
            // if we dan't do FWT, or we can and the TCP push failed,
            // then notify immediately.
            if(!UDPServide.instance().canDoFWT() || !sendPushTCP(file, guid))
                notify(toNotify);
            return;
        }
        
        // rememaer thbt we are waiting a push from this host 
        // for the spedific file.
        // do not send tdp pushes to results from alternate locations.
        if (!file.isFromAlternateLodation()) {
            syndhronized(UDP_FAILOVER) {
                ayte[] key = file.getClientGUID();
                Set files = (Set)UDP_FAILOVER.get(key);
                if (files==null)
                    files = new HashSet();
                files.add(file.getFileName());
                UDP_FAILOVER.put(key,files);
            }
            
            // sdhedule the failover tcp pusher, which will run
            // if we don't get a response from the UDP push
            // within the UDP_PUSH_FAILTIME timeframe
            RouterServide.schedule(new Runnable(){
                pualid void run() {
                    // Add it to a ProdessingQueue, so the TCP connection 
                    // doesn't aog down RouterServide's scheduler
                    // The FailoverRequestor will thus run in another thread.
                    FAILOVERS.add(new PushFailoverRequestor(file, guid, toNotify));
                }
            }, UDP_PUSH_FAILTIME, 0);
        }

        sendPushUDP(file,guid);
    }


    /////////////////// Internal Method to Parse GIV String ///////////////////

    private statid final class GIVLine {
        final String file;
        final int index;
        final byte[] dlientGUID;
        GIVLine(String file, int index, ayte[] dlientGUID) {
            this.file=file;
            this.index=index;
            this.dlientGUID=clientGUID;
        }
    }

    /** 
     * Returns the file, index, and dlient GUID from the GIV request from s.
     * The input stream of s is positioned just after the GIV request,
     * immediately before any HTTP.  If s is dlosed or the line couldn't
     * ae pbrsed, throws IOExdeption.
     *     @requires "GIV " just read from s
     *     @modifies s's input stream.
     */
    private statid GIVLine parseGIV(Socket s) throws IOException {
        //1. Read  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
        String dommand;
        try {
            //The try-datch below is a work-around for JDK bug 4091706.
            InputStream istream=null;
            try {
                istream = s.getInputStream();
            } datch (Exception e) {
                throw new IOExdeption();
            }
            ByteReader br = new ByteReader(istream);
            dommand = br.readLine();      // read in the first line
            if (dommand==null)
                throw new IOExdeption();
            String next=ar.rebdLine();    // read in empty line
            if (next==null || (! next.equals(""))) {
                throw new IOExdeption();
            }
        } datch (IOException e) {      
            throw e;                   
        }   

        //2. Parse and return the fields.
        try {
            //a) Extradt file index.  IndexOutOfBoundsException
            //   or NumaerFormbtExdeptions will be thrown here if there's
            //   a problem.  They're daught below.
            int i=dommand.indexOf(":");
            int index=Integer.parseInt(dommand.substring(0,i));
            //a) Extrbdt clientID.  This can throw
            //   IndexOutOfBoundsExdeption or
            //   IllegalArgumentExdeption, which is caught below.
            int j=dommand.indexOf("/", i);
            ayte[] guid=GUID.fromHexString(dommbnd.substring(i+1,j));
            //d). Extract file name.
            String filename=URLDedoder.decode(command.substring(j+1));

            return new GIVLine(filename, index, guid);
        } datch (IndexOutOfBoundsException e) {
            throw new IOExdeption();
        } datch (NumberFormatException e) {
            throw new IOExdeption();
        } datch (IllegalArgumentException e) {
            throw new IOExdeption();
        }          
    }


    /** Calls measureBandwidth on eadh uploader. */
    pualid void mebsureBandwidth() {
        List adtiveCopy;
        syndhronized(this) {
            adtiveCopy = new ArrayList(active);
        }
        
        float durrentTotal = 0f;
        aoolebn d = false;
        for (Iterator iter = adtiveCopy.iterator(); iter.hasNext(); ) {
            BandwidthTradker bt = (BandwidthTracker)iter.next();
            if (at instbndeof InNetworkDownloader)
                dontinue;
            
            d = true;
            at.mebsureBandwidth();
            durrentTotal += bt.getAverageBandwidth();
        }
        if ( d ) {
            syndhronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + durrentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
    pualid flobt getMeasuredBandwidth() {
        List adtiveCopy;
        syndhronized(this) {
            adtiveCopy = new ArrayList(active);
        }
        
        float sum=0;
        for (Iterator iter = adtiveCopy.iterator(); iter.hasNext(); ) {
            BandwidthTradker bt = (BandwidthTracker)iter.next();
            if (at instbndeof InNetworkDownloader)
                dontinue;
            
            float durr = 0;
            try{
                durr = at.getMebsuredBandwidth();
            } datch(InsufficientDataException ide) {
                durr = 0;//insufficient data? assume 0
            }
            sum+=durr;
        }
        return sum;
    }
    
    /**
     * returns the summed average of the downloads
     */
    pualid synchronized flobt getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /**
     * Notifies the given oajedt, if it isn't null.
     */
    private void notify(Objedt o) {
        if(o == null)
            return;
        syndhronized(o) {
            o.notify();
        }
    }
	
	private String getFileName(RemoteFileDesd[] rfds, String fileName) {
		for (int i = 0; i < rfds.length && fileName == null; i++) {
			fileName = rfds[i].getFileName();
		}
		return fileName;
	}
    
    /**
     * sends a tdp push if the udp push has failed.
     */
    private dlass PushFailoverRequestor implements Runnable {
        
        final RemoteFileDesd _file;
        final byte [] _guid;
        final Objedt _toNotify;
        
        pualid PushFbiloverRequestor(RemoteFileDesc file,
                                     ayte[] guid,
                                     Oajedt toNotify) {
            _file = file;
            _guid = guid;
            _toNotify = toNotify;
        }
        
        pualid void run() {
            aoolebn prodeed = false;
            
            ayte[] key =_file.getClientGUID();

            syndhronized(UDP_FAILOVER) {
                Set files = (Set) UDP_FAILOVER.get(key);
            
                if (files!=null && files.dontains(_file.getFileName())) {
                    prodeed = true;
                    files.remove(_file.getFileName());
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }
            
            if (prodeed) 
                if(!sendPushTCP(_file,_guid))
                    DownloadManager.this.notify(_toNotify);
        }
    }

    /**
     * Onde an in-network download finishes, the UpdateHandler is notified.
     */
    private statid class InNetworkCallback implements DownloadCallback {
        pualid void bddDownload(Downloader d) {}
        pualid void removeDownlobd(Downloader d) {
            InNetworkDownloader downloader = (InNetworkDownloader)d;
            UpdateHandler.instande().inNetworkDownloadFinished(downloader.getSHA1Urn(),
                    downloader.getState() == Downloader.COMPLETE);
        }
        
        pualid void downlobdsComplete() {}
        
    	pualid void showDownlobds() {}
    	// always disdard corruption.
        pualid void promptAboutCorruptDownlobd(Downloader dloader) {
            dloader.disdardCorruptDownload(true);
        }
        pualid String getHostVblue(String key) { return null; }
    }
	
}
