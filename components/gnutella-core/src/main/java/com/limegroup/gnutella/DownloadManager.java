package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.DualIterator;
import org.limewire.collection.MultiIterable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.service.MessageService;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandler;
import com.limegroup.gnutella.downloader.RequeryDownloader;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.version.DownloadInformation;
import com.limegroup.gnutella.version.UpdateHandler;


/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number 
 * of download slots given by the MAX_SIM_DOWNLOADS property.  It is
 * responsible for starting downloads and scheduling and queueing them as 
 * needed.  This class is thread safe.<p>
 *
 * As with other classes in this package, a DownloadManager instance may not be
 * used until initialize(..) is called.  The arguments to this are not passed
 * in to the constructor in case there are circular dependencies.<p>
 *
 * DownloadManager provides ways to serialize download state to disk.  Reads 
 * are initiated by RouterService, since we have to wait until the GUI is
 * initiated.  Writes are initiated by this, since we need to be notified of
 * completed downloads.  Downloads in the COULDNT_DOWNLOAD state are not 
 * serialized.  
 */
public class DownloadManager implements BandwidthTracker {
    
    private static final Log LOG = LogFactory.getLog(DownloadManager.class);
    
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds

    /** The callback for notifying the GUI of major changes. */
    private DownloadCallback callback;
    /** The callback for innetwork downloaders. */
    private DownloadCallback innetworkCallback;
    /** The message router to use for pushes. */
    private MessageRouter router;
    /** Used to check if the file exists. */
    private FileManager fileManager;
    /** The repository of incomplete files 
     *  INVARIANT: incompleteFileManager is same as those of all downloaders */
    private IncompleteFileManager incompleteFileManager
        =new IncompleteFileManager();

    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private final List <AbstractDownloader> active=new LinkedList<AbstractDownloader>();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private final List <AbstractDownloader> waiting=new LinkedList<AbstractDownloader>();
    
    private final MultiIterable<AbstractDownloader> activeAndWaiting = 
    	new MultiIterable<AbstractDownloader>(active,waiting); 
    
    /**
     * Whether or not the GUI has been init'd.
     */
    private volatile boolean guiInit = false;
    
    /** The number if IN-NETWORK active downloaders.  We don't count these when
     * determing how many downloaders are active.
     */
    private int innetworkCount = 0;

    /** 
     * The global minimum time between any two Gnutella requeries, in milliseconds.
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#TIME_BETWEEN_REQUERIES
     */
    public static final long TIME_BETWEEN_GNUTELLA_REQUERIES = 45 * 60 * 1000; 
    
    /** 
     * The last time that a Gnutella requery was sent.
     */
    private long lastGnutellaRequeryTime = 0L;

    /** This will hold the MDs that have sent requeries.
     *  When this size gets too big - meaning bigger than active.size(), then
     *  that means that all MDs have been serviced at least once, so you can
     *  clear it and start anew....
     */
    private List<AbstractDownloader> querySentMDs = new ArrayList<AbstractDownloader>();
    
    /**
     * The number of times we've been bandwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads.
     * This is only counted while downloads are active.
     */
    private float averageBandwidth = 0;
    
    /** The last measured bandwidth, as counted from measureBandwidth. */
    private volatile float lastMeasuredBandwidth;
    
    /**
     * The runnable that pumps inactive downloads to the correct state.
     */
    private Runnable _waitingPump;
    
    /**
     * The controller for push downloads.  This will handle sending
     * out pushes (using proxies, UDP, etc..) and handle incoming GIVs.
     * Only valid pushes will be sent back here.
     */
    private PushDownloadManager pushManager;

    //////////////////////// Creation and Saving /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @uses RouterService.getCallback for the UI callback 
     *       to notify of download changes
     *     @uses RouterService.getMessageRouter for the message 
     *       router to use for sending push requests
     *     @uses RouterService.getFileManager for the FileManager
     *       to check if files exist
     */
    public void initialize() {
        initialize(
                   RouterService.getCallback(),
                   RouterService.getMessageRouter(),
                   RouterService.getFileManager()
                  );
    }
    
    protected void initialize(DownloadCallback guiCallback, MessageRouter router,
                              FileManager fileManager) {
        this.callback = guiCallback;
        this.innetworkCallback = new InNetworkCallback();
        this.router = router;
        this.fileManager = fileManager;
        scheduleWaitingPump();
        pushManager = new PushDownloadManager(new PushedSocketHandler() {
            public void acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket) {
                handleIncomingPush(file, index, clientGUID, socket);
            }}, 
    		router,
    		RouterService.getHttpExecutor(),
    		RouterService.getScheduledExecutorService(),
    		RouterService.getAcceptor());
        pushManager.initialize(RouterService.getConnectionDispatcher());
    }

    /**
     * Performs the slow, low-priority initialization tasks: reading in
     * snapshots and scheduling snapshot checkpointing.
     */
    public void postGuiInit() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        // Try once with the real file, then with the backup file.
        if( !readSnapshot(real) ) {
            LOG.debug("Reading real downloads.dat failed");
            // if backup succeeded, copy into real.
            if( readSnapshot(backup) ) {
                LOG.debug("Reading backup downloads.bak succeeded.");
                copyBackupToReal();
            // only show the error if the files existed but couldn't be read.
            } else if(backup.exists() || real.exists()) {
                LOG.debug("Reading both downloads files failed.");
                MessageService.showError("DOWNLOAD_COULD_NOT_READ_SNAPSHOT");
            }   
        } else {
            LOG.debug("Reading downloads.dat worked!");
        }
        
        Runnable checkpointer=new Runnable() {
            public void run() {
                if (downloadsInProgress() > 0) { //optimization
                    // If the write failed, move the backup to the real.
                    if(!writeSnapshot())
                        copyBackupToReal();
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
    public boolean isGUIInitd() {
        return guiInit;
    }
    
    /**
     * Determines if an 'In Network' download exists in either active or waiting.
     */
    public synchronized boolean hasInNetworkDownload() {
        if(innetworkCount > 0)
            return true;
        for(Iterator i = waiting.iterator(); i.hasNext(); ) {
            if(i.next() instanceof InNetworkDownloader)
                return true;
        }
        return false;
    }
    
    /**
     * Kills all in-network downloaders that are not present in the list of URNs
     * @param urns a current set of urns that we are downloading in-network.
     */
    public synchronized void killDownloadersNotListed(Collection<? extends DownloadInformation> updates) {
        if (updates == null)
            return;
        
        Set<String> urns = new HashSet<String>(updates.size());
        for(DownloadInformation ui : updates)
            urns.add(ui.getUpdateURN().httpStringValue());
        
        for (Iterator<AbstractDownloader> iter = new DualIterator<AbstractDownloader>(waiting.iterator(),active.iterator());
        iter.hasNext();) {
            Downloader d = iter.next();
            if (d instanceof InNetworkDownloader  && 
                    !urns.contains(d.getSHA1Urn().httpStringValue())) 
                d.stop();
        }
        
        Set<String> hopeless = UpdateSettings.FAILED_UPDATES.getValue();
        hopeless.retainAll(urns);
        UpdateSettings.FAILED_UPDATES.setValue(hopeless);
    }
    
    public PushDownloadManager getPushManager() {
    	return pushManager;
    }

    /**
     * Delegates the incoming socket out to BrowseHostHandler & then attempts to assign it
     * to any ManagedDownloader.
     * 
     * Closes the socket if neither BrowseHostHandler nor any ManagedDownloaders wanted it.
     * 
     * @param file
     * @param index
     * @param clientGUID
     * @param socket
     */
    private synchronized void handleIncomingPush(String file, int index, byte [] clientGUID, Socket socket) {
    	 if (BrowseHostHandler.handlePush(index, new GUID(clientGUID), socket))
             return;
         for (AbstractDownloader md : activeAndWaiting) {
         	if (! (md instanceof ManagedDownloader))
         		continue; // pushes apply to gnutella downloads only
         	ManagedDownloader mmd = (ManagedDownloader)md;
         	if (mmd.acceptDownload(file, socket, index, clientGUID))
         		return;
         }
         
         // Will only get here if no matching push existed.
         IOUtils.close(socket);
    }
    
    
    /**
     * Schedules the runnable that pumps through waiting downloads.
     */
    public void scheduleWaitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            public void run() {
                pumpDownloads();
            }
        };
        RouterService.schedule(_waitingPump,
                               1000,
                               1000);
    }
    
    /**
     * Pumps through each waiting download, either removing it because it was
     * stopped, or adding it because there's an active slot and it requires
     * attention.
     */
    protected synchronized void pumpDownloads() {
        int index = 1;
        for(Iterator<AbstractDownloader> i = waiting.iterator(); i.hasNext(); ) {
            AbstractDownloader md = i.next();
            if(md.isAlive()) {
                continue;
            } else if(md.shouldBeRemoved()) {
                i.remove();
                cleanupCompletedDownload(md, false);
            } else if(hasFreeSlot() && (md.shouldBeRestarted())) {
                i.remove();
                if(md instanceof InNetworkDownloader)
                    innetworkCount++;
                active.add(md);
                md.startDownload();
            } else {
                if(md.isQueuable())
                    md.setInactivePriority(index++);
                md.handleInactivity();
            }
        }
    }
    
    /**
     * Copies the backup downloads.dat (downloads.bak) file to the
     * the real downloads.dat location.
     */
    private synchronized void copyBackupToReal() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();        
        real.delete();
        FileUtils.copy(backup, real);
    }
    
    /**
     * Determines if the given URN has an incomplete file.
     */
    public boolean isIncomplete(URN urn) {
        return incompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /**
     * Returns the IncompleteFileManager used by this DownloadManager
     * and all ManagedDownloaders.
     */
    public IncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }    
 
    public synchronized int downloadsInProgress() {
        return active.size() + waiting.size();
    }
    
    public synchronized int getNumIndividualDownloaders() {
        int ret = 0;
        for (Iterator<AbstractDownloader> iter=active.iterator(); iter.hasNext(); ) {  //active
        	Object next = iter.next();
        	if (! (next instanceof ManagedDownloader))
        		continue; // TODO: count torrents separately
            ManagedDownloader md=(ManagedDownloader)next;
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    public synchronized int getNumActiveDownloads() {
        return active.size() - innetworkCount;
    }
   
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }
    
    public synchronized Downloader getDownloaderForURN(URN sha1) {
    	for (AbstractDownloader md : activeAndWaiting) {
    		if (md.getSHA1Urn() != null && sha1.equals(md.getSHA1Urn()))
    			return md;
    	}
    	return null;
    }
    
    /**
     * Returns the active or waiting downloader that uses or will use 
     * <code>file</code> as incomplete file.
     * @param file the incomplete file candidate
     * @return <code>null</code> if no downloader for the file is found
     */
    public synchronized Downloader getDownloaderForIncompleteFile(File file) {
    	for (AbstractDownloader dl : activeAndWaiting) {
    		if (dl.conflictsWithIncompleteFile(file)) {
    			return dl;
    		}
    	}
    	return null;
    }

    public synchronized boolean isGuidForQueryDownloading(GUID guid) {
    	for (AbstractDownloader md : activeAndWaiting) {
    		GUID dGUID = md.getQueryGUID();
    		if ((dGUID != null) && (dGUID.equals(guid)))
    			return true;
    	}
        return false;
    }
    
    /**
     * Clears all downloads.
     */
    public void clearAllDownloads() {
        List<Downloader> buf;
        synchronized(this) {
            buf = new ArrayList<Downloader>(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
            active.clear();
            waiting.clear();
        }
        for(Downloader md : buf ) 
            md.stop();
    }   

    /** Writes a snapshot of all downloaders in this and all incomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  at any time for checkpointing purposes.  Returns true iff the file was
     *  successfully written. */
    boolean writeSnapshot() {
        List<AbstractDownloader> buf;
        synchronized(this) {
            buf = new ArrayList<AbstractDownloader>(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
        }
        
        File outFile = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backupFile = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        
        //must delete in order for renameTo to work.
        backupFile.delete();
        outFile.renameTo(backupFile);
        
        // Write list of active and waiting downloaders, then block list in
        //   IncompleteFileManager.
        ObjectOutputStream out = null;
        try {
            out=new ObjectOutputStream(
                    new BufferedOutputStream(
                        new FileOutputStream(outFile)));
            
            out.writeObject(buf);
            //Blocks can be written to incompleteFileManager from other threads
            //while this downloader is being serialized, so lock is needed.
            synchronized (incompleteFileManager) {
                out.writeObject(incompleteFileManager);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.close(out);
        }
    }

    /** Reads the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downloads will restart immediately if slots
     *  are available.  Returns false iff the file could not be read for any
     *  reason.  THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. 
     *  It is public for testing purposes only!  
     *  @param file the downloads.dat snapshot file */
    public synchronized boolean readSnapshot(File file) {
        //Read downloaders from disk.
        List<AbstractDownloader> buf=null;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(
                                    new BufferedInputStream(
                                        new FileInputStream(file)));
            //This does not try to maintain backwards compatibility with older
            //versions of LimeWire, which only wrote the list of downloaders.
            //Note that there is a minor race condition here; if the user has
            //started some downloads before this method is called, the new and
            //old downloads will use different IncompleteFileManager instances.
            //This doesn't really cause an errors, however.
            buf = GenericsUtils.scanForList(in.readObject(), AbstractDownloader.class, ScanMode.REMOVE);
            incompleteFileManager=(IncompleteFileManager)in.readObject();
        } catch(Throwable t) {
            LOG.error("Unable to read download file", t);
            return false;
        } finally {
            IOUtils.close(in);
        }
        
        // Pump the downloaders through a set, to remove duplicate values.
        // This is necessary in case LimeWire got into a state where a
        // downloader was written to disk twice.
        buf = new LinkedList<AbstractDownloader>(new LinkedHashSet<AbstractDownloader>(buf));

        //Initialize and start downloaders. This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (AbstractDownloader downloader : buf) {                
                // ignore RequeryDownloaders -- they're legacy
                if(downloader instanceof RequeryDownloader)
                    continue;
                
                waiting.add(downloader);                                 //1
                downloader.initialize(this, this.fileManager, callback(downloader));       //2
                callback(downloader).addDownload(downloader);                        //3
            }
            return true;
        } finally {
            // Remove entries that are too old or no longer existent and not actively 
            // downloaded.  
            if (incompleteFileManager.initialPurge(getActiveDownloadFiles(buf)))
                writeSnapshot();
        }
    }
     
    private static Collection<File> getActiveDownloadFiles(List<AbstractDownloader> downloaders) {
        List<File> ret = new ArrayList<File>(downloaders.size());
        for (Downloader d : downloaders) {
    	    File f = d.getFile();
    	    if (f != null) {
                try {
                    ret.add(FileUtils.getCanonicalFile(f));
                } catch (IOException iox) { 
                    ret.add(f.getAbsoluteFile());
                }
    	    }
        }
        
        return ret;
    }
    ////////////////////////// Main Public Interface ///////////////////////
           
    /** 
     * Tries to "smart download" any of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The DownloadCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.
     * 
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID the guid of the query that resulted in the RFDs being
     * downloaded.
     * @param saveDir can be null, then the default save directory is used
	 * @param fileName can be null, then the first filename of one of element of
	 * <code>files</code> is taken.
     * @throws SaveLocationException when there was an error setting the
     * location of the final download destination.
     *
     *     @modifies this, disk 
     */
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            List<? extends RemoteFileDesc> alts, GUID queryGUID, 
                                            boolean overwrite, File saveDir,
											String fileName) 
		throws SaveLocationException {

		String fName = getFileName(files, fileName);
        if (conflicts(files, new File(saveDir,fName))) {
			throw new SaveLocationException
			(SaveLocationException.FILE_ALREADY_DOWNLOADING,
					new File(fName != null ? fName : ""));
        }

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader =
            new ManagedDownloader(files, incompleteFileManager, queryGUID,
								  saveDir, fileName, overwrite);

        initializeDownload(downloader);
        
        //Now that the download is started, add the sources w/o caching
        downloader.addDownload(alts,false);
        
        return downloader;
    }   
    
    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURL</tt>, if specified.  If that fails, or if defaultURL does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filename</tt> is specified, it will be used as the name of the
     * complete file; otherwise it will be taken from any search results or
     * guessed from <tt>defaultURLs</tt>.
     *
     * @param urn the hash of the file (exact topic), or null if unknown
     * @param textQuery requery keywords (keyword topic), or null if unknown
     * @param filename the final file name, or <code>null</code> if unknown
     * @param saveLocation can be null, then the default save location is used
     * @param defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     *
     * @exception IllegalArgumentException all urn, textQuery, filename are
	 *  null 
     * @throws SaveLocationException 
     */
    public synchronized Downloader download(MagnetOptions magnet,
			boolean overwrite,
			File saveDir,
			String fileName)
	throws IllegalArgumentException, SaveLocationException {
		
		if (!magnet.isDownloadable()) 
            throw new IllegalArgumentException("magnet not downloadable");
        
        //remove entry from IFM if the incomplete file was deleted.
        incompleteFileManager.purge();
        
        if (fileName == null) {
        	fileName = magnet.getFileNameForSaving();
        }
        if (conflicts(magnet.getSHA1Urn(), 0, new File(saveDir,fileName))) {
			throw new SaveLocationException
			(SaveLocationException.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }

        //Note: If the filename exists, it would be nice to check that we are
        //not already downloading the file by calling conflicts with the
        //filename...the problem is we cannot do this effectively without the
        //size of the file (atleast, not without being risky in assuming that
        //two files with the same name are the same file). So for now we will
        //just leave it and download the same file twice.

        //Instantiate downloader, validating incompleteFile first.
        MagnetDownloader downloader = 
            new MagnetDownloader(incompleteFileManager, magnet, 
					overwrite, saveDir, fileName);
        initializeDownload(downloader);
        return downloader;
    }

    /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SaveLocationException 
     */ 
    public synchronized Downloader download(File incompleteFile)
            throws CantResumeException, SaveLocationException { 
     
		if (conflictsWithIncompleteFile(incompleteFile)) {
			throw new SaveLocationException
			(SaveLocationException.FILE_ALREADY_DOWNLOADING, incompleteFile);
		}
		
		if (IncompleteFileManager.isTorrentFolder(incompleteFile)) 
			return resumeTorrentDownload(incompleteFile);

        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downloadDir=SettingsManager.instance().getSaveDirectory();
        //        File completeFile=new File(
        //            downloadDir, 
        //            incompleteFileManager.getCompletedName(incompleteFile));
        //        if (completeFile.exists())
        //            throw new FileExistsException(filename);
        //    } catch (IllegalArgumentException e) {
        //        throw new CantResumeException(incompleteFile.getName());
        //    }
        //}

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Instantiate downloader, validating incompleteFile first.
        ResumeDownloader downloader=null;
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
            String name=IncompleteFileManager.getCompletedName(incompleteFile);
            long size= IncompleteFileManager.getCompletedSize(incompleteFile);
            downloader = new ResumeDownloader(incompleteFileManager,
                                              incompleteFile,
                                              name,
                                              size);
        } catch (IllegalArgumentException e) {
            throw new CantResumeException(incompleteFile.getName());
        } catch (IOException ioe) {
            throw new CantResumeException(incompleteFile.getName());
        }
        
        initializeDownload(downloader);
        return downloader;
    }
    
    private Downloader resumeTorrentDownload(File torrentFolder) 
    throws CantResumeException, SaveLocationException {
    	File infohash = null; 
    	for (File f : torrentFolder.listFiles()){
    		if (f.getName().startsWith(".dat")) {
    			infohash = f;
    			break;
    		}
    	}
    	
    	String name = IncompleteFileManager.getCompletedName(torrentFolder);
        if(infohash == null)
            throw new CantResumeException(name);
    	
    	BTMetaInfo info = null;
    	try {
    		Object infoObj = FileUtils.readObject(infohash.getAbsolutePath());
    		info = (BTMetaInfo)infoObj;
    	} catch (Throwable bad) {
    		throw new CantResumeException(name);
    	}
    	
    	Downloader ret = downloadTorrent(info, false);
    	if (ret.isResumable())
    		ret.resume();
    	return ret;
    }
    
    /**
     * Downloads an InNetwork update, using the info from the DownloadInformation.
     */
    public synchronized Downloader download(DownloadInformation info, long now) 
    throws SaveLocationException {
        File dir = FileManager.PREFERENCE_SHARE;
        dir.mkdirs();
        File f = new File(dir, info.getUpdateFileName());
        if(conflicts(info.getUpdateURN(), (int)info.getSize(), f))
			throw new SaveLocationException(SaveLocationException.FILE_ALREADY_DOWNLOADING, f);
        
        incompleteFileManager.purge();
        ManagedDownloader d = 
            new InNetworkDownloader(incompleteFileManager, info, dir, now);
        initializeDownload(d);
        return d;
    }
    
    public synchronized Downloader downloadTorrent(BTMetaInfo info, boolean overwrite) 
    throws SaveLocationException {
    	TorrentFileSystem system = info.getFileSystem();
    	checkActiveAndWaiting(info.getURN(), system);
    	if (!overwrite)
    		checkTargetLocation(system, overwrite);
    	else
    		RouterService.getTorrentManager().killTorrentForFile(system.getCompleteFile());
    	AbstractDownloader ret = new BTDownloader(info);
    	initializeDownload(ret);
    	return ret;
    }
    
    private void checkTargetLocation(TorrentFileSystem info, boolean overwrite) 
    throws SaveLocationException{
    	for (File f : info.getFilesAndFolders()) {
    		if (f.exists())
    			throw new SaveLocationException
    			(SaveLocationException.FILE_ALREADY_EXISTS, f);
    	}
    }
    
    private void checkActiveAndWaiting(URN urn, TorrentFileSystem system) 
    throws SaveLocationException {
    	for (AbstractDownloader current : activeAndWaiting) {
    		if (urn.equals(current.getSHA1Urn())) {
    			// this is the place to add new trackers eventually.
    			throw new SaveLocationException
    			(SaveLocationException.FILE_ALREADY_DOWNLOADING, system.getCompleteFile());
    		}
    		for (File f : system.getFilesAndFolders()) {
    			if (current.conflictsSaveFile(f)) {
    				throw new SaveLocationException
    				(SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO, f);
    			}
    		}
    	}
    }
    
    /**
     * Performs common tasks for initializing the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the callback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private void initializeDownload(AbstractDownloader md) {
        md.initialize(this, fileManager, callback(md));
		waiting.add(md);
        callback(md).addDownload(md);
        RouterService.schedule(new Runnable() {
        	public void run() {
        		writeSnapshot(); // Save state for crash recovery.
        	}
        },0,0);
    }
    
    /**
     * Returns the callback that should be used for the given md.
     */
    private DownloadCallback callback(Downloader md) {
        return (md instanceof InNetworkDownloader) ? innetworkCallback : callback;
    }
        
	/**
	 * Returns true if there already exists a download for the same file.
	 * <p>
	 * Same file means: same urn, or as fallback same filename + same filesize
	 * @param rfds
	 * @return
	 */
	private boolean conflicts(RemoteFileDesc[] rfds, File... fileName) {
		URN urn = null;
		for (int i = 0; i < rfds.length && urn == null; i++) {
			urn = rfds[0].getSHA1Urn();
		}
		
		return conflicts(urn, rfds[0].getSize(), fileName);
	}
	
	/**
	 * Returns <code>true</code> if there already is a download with the same urn. 
	 * @param urn may be <code>null</code>, then a check based on the fileName
	 * and the fileSize is performed
	 * @return
	 */
	public boolean conflicts(URN urn, long fileSize, File... fileName) {
		
		if (urn == null && fileSize == 0) {
			return false;
		}
		
		synchronized (this) {
			for (AbstractDownloader md : activeAndWaiting) {
				if (md.conflicts(urn, fileSize, fileName)) 
					return true;
			}
			return false;
		}
	}
	
	/**
	 * Returns <code>true</code> if there already is a download that is or
	 * will be saving to this file location.
	 * @param candidateFile the final file location.
	 * @return
	 */
	public synchronized boolean isSaveLocationTaken(File candidateFile) {
		for (AbstractDownloader md : activeAndWaiting) {
			if (md.conflictsSaveFile(candidateFile)) 
				return true;
		}
		return false;
	}

	private synchronized boolean conflictsWithIncompleteFile(File incompleteFile) {
		for (AbstractDownloader md : activeAndWaiting) {
			if (md.conflictsWithIncompleteFile(incompleteFile))
				return true;
		}
		return false;
	}
	
    /** 
     * Adds all responses (and alternates) in qr to any downloaders, if
     * appropriate.
     */
    public void handleQueryReply(QueryReply qr) {
        // first check if the qr is of 'sufficient quality', if not just
        // short-circuit.
        if (qr.calculateQualityOfService(
                !RouterService.acceptedIncomingConnection()) < 1)
            return;

        List<Response> responses;
        HostData data;
        try {
            responses = qr.getResultsAsList();
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            return; // bad packet, do nothing.
        }
        
        addDownloadWithResponses(responses, data);
    }

    /**
     * Iterates through all responses seeing if they can be matched
     * up to any existing downloaders, adding them as possible
     * sources if they do.
     */
    private void addDownloadWithResponses(List<? extends Response> responses, HostData data) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(data == null)
            throw new NullPointerException("null hostdata");

        // need to synch because active and waiting are not thread safe
        List<AbstractDownloader> downloaders = new ArrayList<AbstractDownloader>(active.size() + waiting.size());
        synchronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(active);
            downloaders.addAll(waiting);
        }
        
        // short-circuit.
        if(downloaders.isEmpty())
            return;

        //For each response i, offer it to each downloader j.  Give a response
        // to at most one downloader.
        // TODO: it's possible that downloader x could accept response[i] but
        //that would cause a conflict with downloader y.  Check for this.
        for(Response r : responses) {
            // Don't bother with making XML from the EQHD.
            RemoteFileDesc rfd = r.toRemoteFileDesc(data);
            for(Downloader current : downloaders) {
            	if ( !(current instanceof ManagedDownloader))
            		continue; // can't add sources to torrents yet
                ManagedDownloader currD = (ManagedDownloader) current;
                // If we were able to add this specific rfd,
                // add any alternates that this response might have
                // also.
                if (currD.addDownload(rfd, true)) {
                    for(IpPort ipp : r.getLocations()) {
                        // don't cache alts.
                        currD.addDownload(new RemoteFileDesc(rfd, ipp), false);
                    }
                    break;
                }
            }
        }
    }

    // //////////// Callback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by caller */
    private boolean hasFreeSlot() {
        return active.size() - innetworkCount < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     * If completed is true, finishes the download completely.  Otherwise,
     * puts the download back in the waiting list to be finished later.
     *     @modifies this, callback
     */
    public synchronized void remove(AbstractDownloader downloader, 
                                    boolean completed) {
        active.remove(downloader);
        if(downloader instanceof InNetworkDownloader)
            innetworkCount--;
        
        waiting.remove(downloader);
        if(completed)
            cleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /**
     * Bumps the priority of an inactive download either up or down
     * by amt (if amt==0, bump to start/end of list).
     */
    public synchronized void bumpPriority(Downloader downl,
                                          boolean up, int amt) {
    	AbstractDownloader downloader = (AbstractDownloader)downl;
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
     * Cleans up the given Downloader after completion.
     *
     * If ser is true, also writes a snapshot to the disk.
     */
    private void cleanupCompletedDownload(AbstractDownloader dl, boolean ser) {
        querySentMDs.remove(dl);
        dl.finish();
        if (dl.getQueryGUID() != null)
            router.downloadFinished(dl.getQueryGUID());
        callback(dl).removeDownload(dl);
        
        //Save this' state to disk for crash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback(dl).downloadsComplete();
    }           
    
    /** 
     * Attempts to send the given requery to provide the given downloader with 
     * more sources to download.  May not actually send the requery if it doing
     * so would exceed the maximum requery rate.
     * 
     * @param query the requery to send, which should have a marked GUID.
     *  Queries are subjected to global rate limiting iff they have marked 
     *  requery GUIDs.
     * @param requerier the downloader requesting more sources.  Needed to 
     *  ensure fair requery scheduling.  This MUST be in the waiting list,
     *  i.e., it MUST NOT have a download slot.
     * @return true iff the query was actually sent.  If false is returned,
     *  the downloader should attempt to send the query later.
     */
    public synchronized boolean sendQuery(ManagedDownloader requerier, 
                                          QueryRequest query) {
        //NOTE: this algorithm provides global but not local fairness.  That is,
        //if two requeries x and y are competing for a slot, patterns like
        //xyxyxy or xyyxxy are allowed, though xxxxyx is not.
        if(LOG.isTraceEnabled())
            LOG.trace("DM.sendQuery():" + query.getQuery());
        assert waiting.contains(requerier) : "Unknown or non-waiting MD trying to send requery.";

        //Disallow if global time limits exceeded.  These limits don't apply to
        //queries that are requeries.
        boolean isRequery=GUID.isLimeRequeryGUID(query.getGUID());
        long elapsed=System.currentTimeMillis()-lastGnutellaRequeryTime;
        if (isRequery && elapsed <= TIME_BETWEEN_GNUTELLA_REQUERIES) {
            return false;
        }

        //Has everyone had a chance to send a query?  If so, clear the slate.
        if (querySentMDs.size() >= waiting.size()) {
            LOG.trace("DM.sendQuery(): reseting query sent queue");
            querySentMDs.clear();
        }

        //If downloader has already sent a query, give someone else a turn.
        if (querySentMDs.contains(requerier)) {
            // nope, sorry, must lets others go first...
            if(LOG.isWarnEnabled())
                LOG.warn("DM.sendQuery(): out of turn:" + query.getQuery());
            return false;
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("DM.sendQuery(): requery allowed:" + query.getQuery());  
        querySentMDs.add(requerier);                  
        lastGnutellaRequeryTime = System.currentTimeMillis();
        router.sendDynamicQuery(query);
        return true;
    }

    /** Calls measureBandwidth on each uploader. */
    public void measureBandwidth() {
        List<AbstractDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<AbstractDownloader>(active);
        }
        
        float currentTotal = 0f;
        boolean c = false;
        for (BandwidthTracker bt : activeCopy) {
            if (bt instanceof InNetworkDownloader)
                continue;
            
            c = true;
            bt.measureBandwidth();
            currentTotal += bt.getAverageBandwidth();
        }
        
        if ( c ) {
            synchronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
    public float getMeasuredBandwidth() {
        List<AbstractDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<AbstractDownloader>(active);
        }
        
        float sum=0;
        for (BandwidthTracker bt : activeCopy) {
            if (bt instanceof InNetworkDownloader)
                continue;
            
            float curr = 0;
            try{
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;//insufficient data? assume 0
            }
            sum+=curr;
        }
                
        lastMeasuredBandwidth = sum;
        return sum;
    }
    
    /**
     * returns the summed average of the downloads
     */
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /**
     * Returns the measured bandwidth as calculated from the last
     * getMeasuredBandwidth() call.
     */
    public float getLastMeasuredBandwidth() {
        return lastMeasuredBandwidth;
    }
	
	private String getFileName(RemoteFileDesc[] rfds, String fileName) {
		for (int i = 0; i < rfds.length && fileName == null; i++) {
			fileName = rfds[i].getFileName();
		}
		return fileName;
	}
    

    /**
     * Once an in-network download finishes, the UpdateHandler is notified.
     */
    private static class InNetworkCallback implements DownloadCallback {
        public void addDownload(Downloader d) {}
        public void removeDownload(Downloader d) {
            InNetworkDownloader downloader = (InNetworkDownloader)d;
            UpdateHandler.instance().inNetworkDownloadFinished(downloader.getSHA1Urn(),
                    downloader.getState() == DownloadStatus.COMPLETE);
        }
        
        public void downloadsComplete() {}
        
    	public void showDownloads() {}
    	// always discard corruption.
        public void promptAboutCorruptDownload(Downloader dloader) {
            dloader.discardCorruptDownload(true);
        }
        public String getHostValue(String key) { return null; }
    }
	
}
