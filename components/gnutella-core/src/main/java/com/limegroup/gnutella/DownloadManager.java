package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.CommonUtils;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.util.NetworkUtils;
import com.bitzi.util.Base32;


/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number 
 * of download slots given by the MAX_SIM_DOWNLOADS property.  It is
 * responsible for starting downloads and scheduling and queing them as 
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
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds

    /** The callback for notifying the GUI of major changes. */
    private ActivityCallback callback;
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
    private List /* of ManagedDownloader */ active=new LinkedList();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private List /* of ManagedDownloader */ waiting=new LinkedList();


    /** The global minimum time between any two requeries, in milliseconds.
     *  @see com.limegroup.gnutella.downloader.ManagedDownloader#TIME_BETWEEN_REQUERIES*/
    public static long TIME_BETWEEN_REQUERIES = 45 * 60 * 1000; 

    /** The last time that a requery was sent.
     */
    private long lastRequeryTime = 0;

    /** This will hold the MDs that have sent requeries.
     *  When this size gets too big - meaning bigger than active.size(), then
     *  that means that all MDs have been serviced at least once, so you can
     *  clear it and start anew....
     */
    private List querySentMDs = new ArrayList();
    
    /**
     * The number of times we've been bandwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads
     */
    private float averageBandwidth = 0;

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
        this.callback = RouterService.getCallback();
        this.router = RouterService.getMessageRouter();
        this.fileManager = RouterService.getFileManager();
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
            // if backup succeeded, copy into real.
            if( readSnapshot(backup) )
                copyBackupToReal();
        }
        
        Runnable checkpointer=new Runnable() {
            public void run() {
                try {
                    if (downloadsInProgress() > 0) { //optimization
                        // If the write failed, move the backup to the real.
                        if(!writeSnapshot())
                            copyBackupToReal();
                    }
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };
        RouterService.schedule(checkpointer, 
							   SNAPSHOT_CHECKPOINT_TIME, 
							   SNAPSHOT_CHECKPOINT_TIME);
    }
    
    /**
     * Copies the backup downloads.dat (downloads.bak) file to the
     * the real downloads.dat location.
     */
    private synchronized void copyBackupToReal() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();        
        real.delete();
        CommonUtils.copy(backup, real);
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
        for (Iterator iter=active.iterator(); iter.hasNext(); ) {  //active
            ManagedDownloader md=(ManagedDownloader)iter.next();
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    public synchronized int getNumActiveDownloads() {
        return active.size();
    }
   
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }

    /** Writes a snapshot of all downloaders in this and all incomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  at any time for checkpointing purposes.  Returns true iff the file was
     *  successfully written. */
    synchronized boolean writeSnapshot() {
        List buf=new ArrayList();
        buf.addAll(active);
        buf.addAll(waiting);
        
        File outFile = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        //must delete in order for renameTo to work.
        SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue().delete();
        outFile.renameTo(
            SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue());
        
        // Write list of active and waiting downloaders, then block list in
        //   IncompleteFileManager.
        try {
            ObjectOutputStream out=new ObjectOutputStream(
                new FileOutputStream(
                    SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue()));
            out.writeObject(buf);
            //Blocks can be written to incompleteFileManager from other threads
            //while this downloader is being serialized, so lock is needed.
            synchronized (incompleteFileManager) {
                out.writeObject(incompleteFileManager);
            }
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
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
        List buf=null;
        try {
            ObjectInputStream in=new ObjectInputStream(new FileInputStream(file));
            //This does not try to maintain backwards compatibility with older
            //versions of LimeWire, which only wrote the list of downloaders.
            //Note that there is a minor race condition here; if the user has
            //started some downloads before this method is called, the new and
            //old downloads will use different IncompleteFileManager instances.
            //This doesn't really cause an errors, however.
            buf=(List)in.readObject();
            incompleteFileManager=(IncompleteFileManager)in.readObject();
        } catch (IOException e) {
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
        
        //Remove entries that are too old or no longer existent.  This is done
        //before starting downloads in the rare case that a downloader uses one
        //of these incomplete files.  Then commit changes to disk.  (This last
        //step isn't really needed.)
        if (incompleteFileManager.purge(true))
            writeSnapshot();

        //Initialize and start downloaders.  Must catch ClassCastException since
        //the data could be corrupt.  This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (Iterator iter=buf.iterator(); iter.hasNext(); ) {
                ManagedDownloader downloader=(ManagedDownloader)iter.next();
                waiting.add(downloader);                                 //1
                downloader.initialize(this, this.fileManager, callback, true);//2
                callback.addDownload(downloader);                        //3
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
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
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.
     *
     *     @modifies this, disk 
     */
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            List alts, 
                                            boolean overwrite) 
            throws FileExistsException, AlreadyDownloadingException, 
				   java.io.FileNotFoundException {
        //Check if file would conflict with any other downloads in progress.
        //TODO3: if only a few of many files conflicts, we could just ignore
        //them.
        String conflict=conflicts(files, null);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);


        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        if (! overwrite) {
            File downloadDir = SharingSettings.getSaveDirectory();
            String filename=files[0].getFileName();
            File completeFile = new File(downloadDir, filename);  
            if ( completeFile.exists() ) 
                throw new FileExistsException(filename);            
        }

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge(false);

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader =
			new ManagedDownloader(files, incompleteFileManager);

        startDownload(downloader, false);
        
        //Now that the download is started, add the alts without caching.
        for(Iterator iter = alts.iterator(); iter.hasNext(); ) {
            RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
            downloader.addDownload(rfd, false);
        }
        
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
     * @param filename the final file name, or null if unknown
     * @param defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     *
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception IllegalArgumentException both urn and textQuery are null 
     */
    public synchronized Downloader download(URN urn, String textQuery, 
            String filename, String [] defaultURL, boolean overwrite) 
            throws IllegalArgumentException, AlreadyDownloadingException,
                                                       FileExistsException {
        if (textQuery==null && urn==null)
            throw new IllegalArgumentException("Need something for requeries");
        
        //if we have a valid filename to check against, and we are not supposed
        //to overwrite, thrown an exception if the file already exists
        if(!overwrite && (filename!=null || !filename.equals(""))) {
            File downloadDir = SharingSettings.getSaveDirectory();
            File completeFile = new File(downloadDir,filename);
            if(completeFile.exists()) 
                throw new FileExistsException(filename);
        }
        
        //remove entry from IFM if the incomplete file was deleted.
        incompleteFileManager.purge(false);
        
        if(filename!=null && !filename.equals("")) {
            if(conflicts(urn)) 
                throw new AlreadyDownloadingException(filename);
        }
        
        //Instantiate downloader, validating incompleteFile first.
        MagnetDownloader downloader = new MagnetDownloader(this,
                                              fileManager,
                                              incompleteFileManager,
                                              callback,
                                              urn,
                                              textQuery,
                                              filename,
                                              defaultURL);
        startDownload(downloader, false);
        return downloader;
    }

    /**
     * Starts a resume download for the given incomplete file.
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     */ 
    public synchronized Downloader download(File incompleteFile)
            throws AlreadyDownloadingException, CantResumeException { 
        //Check for conflicts.  TODO: refactor to make less like conflicts().
        for (Iterator iter=active.iterator(); iter.hasNext(); ) {  //active
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md.conflicts(incompleteFile))                   
                throw new AlreadyDownloadingException(md.getFileName());
        }
        for (Iterator iter=waiting.iterator(); iter.hasNext(); ) { //queued
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md.conflicts(incompleteFile))                   
                throw new AlreadyDownloadingException(md.getFileName());
        }

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
        incompleteFileManager.purge(false);

        //Instantiate downloader, validating incompleteFile first.
        ResumeDownloader downloader=null;
        try {
            String name=IncompleteFileManager.getCompletedName(incompleteFile);
            int size=ByteOrder.long2int(
                IncompleteFileManager.getCompletedSize(incompleteFile));
            downloader = new ResumeDownloader(incompleteFileManager,
                                              incompleteFile,
                                              name,
                                              size);
        } catch (IllegalArgumentException e) {
            throw new CantResumeException(incompleteFile.getName());
        }
        
        startDownload(downloader, false);
        return downloader;
    }


    /**
     * Starts a "requery download", aka, a "wishlist download".  A "requery
     * download" should be started when the user has not received any results
     * for her query, and wants LimeWire to spawn a specialized Downloader that
     * requeries the network until a 'appropriate' file is found.
     * 
     * @param query The original query string.
     * @param richQuery The original richQuery string.
     * @param guid The guid associated with this query request.
     * @param type The mediatype associated with this search.  
     */
    public synchronized Downloader download(String query,
                                            String richQuery,
                                            byte[] guid,
                                            MediaType type) 
    throws AlreadyDownloadingException {
        AutoDownloadDetails add = new AutoDownloadDetails(query,
                                                          richQuery,
                                                          guid,
                                                          type);
        if (requeryConflicts(add))
            throw new AlreadyDownloadingException(query);

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge(false);

        RequeryDownloader downloader=
            new RequeryDownloader(incompleteFileManager,add);

        startDownload(downloader, false);
        return downloader;        
    }
    
    /**
     * Performs common tasks for starting the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the callback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private void startDownload(ManagedDownloader md, boolean deserialized) {
        md.initialize(this, fileManager, callback, deserialized);
        waiting.add(md);
        callback.addDownload(md);
        writeSnapshot(); // Save state for crash recovery.
    }


    /**
     * Returns the name of any of the files in 'files' conflict with any of the
     * downloads in this except for dloader, which may be null.  Returns null if
     * there are no conflicts.  This is used before starting and resuming
     * downloads.  
     */
    public synchronized String conflicts(RemoteFileDesc[] files,
                                         ManagedDownloader dloader) {
        for (int i=0; i<files.length; i++) {
            //Active downloads...
            for (Iterator iter=active.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))                   
                    return files[i].getFileName();
            }
            //Queued downloads...
            for (Iterator iter=waiting.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))
                    return files[i].getFileName();
            }
        }
        return null;
    }


    private synchronized boolean conflicts(URN urn) {
        Iterator iter = active.iterator();
        while(iter.hasNext()) {
            ManagedDownloader md = (ManagedDownloader)iter.next();
            if(md.conflicts(urn))
                return true;
        }
        iter = waiting.iterator();
        while(iter.hasNext()) {
            ManagedDownloader md = (ManagedDownloader)iter.next();
            if(md.conflicts(urn))
                return true;
        }
        return false;
    }


    /** Returns true if there is a RequeryDownloader of sufficient similarity
     *  in existence.
     */
    private synchronized boolean requeryConflicts(AutoDownloadDetails add) {
        boolean retVal = false;
        //Active downloads...
        for (Iterator iter=active.iterator(); iter.hasNext() && !retVal; ) {
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md instanceof RequeryDownloader) {
                RequeryDownloader rd = (RequeryDownloader) md;
                retVal = rd.conflicts(add);
            }
        }
        //Queued downloads...
        for (Iterator iter=waiting.iterator(); iter.hasNext() && !retVal; ) {
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md instanceof RequeryDownloader) {
                RequeryDownloader rd = (RequeryDownloader) md;
                retVal = rd.conflicts(add);
            }
        }
        return retVal;
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

        List responses;
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
    private void addDownloadWithResponses(List responses, HostData data) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(data == null)
            throw new NullPointerException("null hostdata");

        // need to synch because active and waiting are not thread safe
        List downloaders = new ArrayList(active.size() + waiting.size());
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
        for(Iterator i = responses.iterator(); i.hasNext(); ) {
            Response r = (Response)i.next();
            // Don't bother with making XML from the EQHD.
            RemoteFileDesc rfd = r.toRemoteFileDesc(data);
            for(Iterator j = downloaders.iterator(); j.hasNext(); ) {
                ManagedDownloader currD = (ManagedDownloader)j.next();
                // If we were able to add this specific rfd,
                // add any alternates that this response might have
                // also.
                if (currD.addDownload(rfd, true)) {
                    Set alts = r.getLocations();
                    for(Iterator k = alts.iterator(); k.hasNext(); ) {
                        Endpoint ep = (Endpoint)k.next();
                        // don't cache alts.
                        currD.addDownload(new RemoteFileDesc(rfd, ep), false);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Accepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or has already
     * been downloaded, this will deal with it appropriately.  In any case
     * this eventually closes the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " was just read from s
     */
    public void acceptDownload(Socket socket) {
        Thread.currentThread().setName("PushDownloadThread");
        try {
            //1. Read GIV line BEFORE acquiring lock, since this may block.
            GIVLine line=parseGIV(socket);
            String file=line.file;
            int index=line.index;
            byte[] clientGUID=line.clientGUID;

            //2. Attempt to give to an existing downloader.
            synchronized (this) {
                if (BrowseHostHandler.handlePush(index, new GUID(clientGUID), 
                                                 socket))
                    return;
                for (Iterator iter=active.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
                for (Iterator iter=waiting.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
            }
        } catch (IOException e) {
        }            

        //3. We never requested the file or already got it.  Kill it.
        try {
            socket.close();
        } catch (IOException e) { }
    }


    ////////////// Callback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by caller */
    private boolean hasFreeSlot() {
        return active.size() < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /** 
     * Blocks until a download slot has been assigned to downloader.  Throws
     * InterruptedException if the current thread is interrupted while waiting.
     * If InterruptedException is thrown, this is not modified.
     *     @requires downloader queued
     *     @modifies this 
     */
    public synchronized void waitForSlot(ManagedDownloader downloader) 
            throws InterruptedException {
        while (! hasFreeSlot()) 
            wait();
        waiting.remove(downloader);
        active.add(downloader);
    }

    /**
     * Relinquishes downloader's slot.  This is idempotent and non-blocking.
     *     @modifies this
     */
    public synchronized void yieldSlot(ManagedDownloader downloader) {
        Assert.that(downloader!=null, "Null downloader");
        Assert.that(active!=null, "Null active");
        Assert.that(waiting!=null, "Null waiting");
        active.remove(downloader);
        waiting.add(downloader);
        notify();
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     *     @modifies this, callback
     */
    public synchronized void remove(ManagedDownloader downloader,
                                    boolean success) {
        //As a minor optimization, only waiting.remove(..) or notify(..)
        //is needed.  But we do both just to be safe.
        active.remove(downloader);
        waiting.remove(downloader);
        querySentMDs.remove(downloader);
        downloader.finish();
        notify();
        callback.removeDownload(downloader);
        //Save this' state to disk for crash recovery.  Note that a downloader
        //in the GAVE_UP state is not serialized here even if still displayed in
        //the GUI.  Maybe this callback model needs a little tweaking.
        writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback.downloadsComplete();
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
        debug("DM.sendQuery():" + query.getQuery());
        Assert.that(waiting.contains(requerier),
                    "Unknown or non-waiting MD trying to send requery.");

        //Disallow if global time limits exceeded.  These limits don't apply to
        //queries that are requeries.
        boolean isRequery=GUID.isLimeRequeryGUID(query.getGUID());
        long elapsed=System.currentTimeMillis()-lastRequeryTime;
        if (isRequery && elapsed<=TIME_BETWEEN_REQUERIES) {
            return false;
        }

        //Has everyone had a chance to send a query?  If so, clear the slate.
        if (querySentMDs.size() >= waiting.size()) {
            debug("DM.sendQuery(): reseting query sent queue");
            querySentMDs.clear();
        }

        //If downloader has already sent a query, give someone else a turn.
        if (querySentMDs.contains(requerier)) {
            // nope, sorry, must lets others go first...
            debug("DM.sendQuery(): out of turn:" + query.getQuery());
            return false;
        }
        
        debug("DM.sendQuery(): requery allowed:" + query.getQuery());  
        querySentMDs.add(requerier);                  
        lastRequeryTime = System.currentTimeMillis();
		router.sendDynamicQuery(query);
        return true;
    }


    /**
     * Sends a push request for the given file.  Returns false iff no push could
     * be sent, i.e., because no routing entry exists. That generally means you
     * shouldn't send any more pushes for this file.
     *
     * @param file the <tt>RemoteFileDesc</tt> constructed from the query 
     *  hit, containing data about the host we're pushing to
     * @return <tt>true</tt> if the push was successfully sent, otherwise
     *  <tt>false</tt>
     */
    public boolean sendPush(RemoteFileDesc file) {
        debug("DM.sendPush(): entered.");
        
        // Send as multicast if it's multicast.
        if( file.isReplyToMulticast() ) {
            byte[] addr = RouterService.getNonForcedAddress();
            int port = RouterService.getNonForcedPort();
            if( NetworkUtils.isValidAddress(addr) &&
                NetworkUtils.isValidPort(port) ) {
                PushRequest pr = new PushRequest(GUID.makeGuid(),
                                         (byte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         addr,
                                         port);
                router.sendMulticastPushRequest(pr);
                return true;
            }
        }
        
        // Make sure we know our correct address/port.
        // If we don't, we can't send pushes yet.
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        if( !NetworkUtils.isValidAddress(addr) || 
            !NetworkUtils.isValidPort(port) )
            return false;
        
        // If it wasn't multicast, try sending to the proxies if it had them.
        Set proxies = file.getPushProxies();
        if (!proxies.isEmpty()) {
            //TODO: investigate not sending a HTTP request to a proxy
            //you are directly connected to.  How much of a problem is this?
            //Probably not much of one at all.  Classic example of code
            //complexity versus efficiency.  It may be hard to actually
            //distinguish a PushProxy from one of your UP connections if the
            //connection was incoming since the port on the socket is ephemeral 
            //and not necessarily the proxies listening port
            // we have proxy info - give them a try
            debug("DM.sendPush(): proxy info exists.");
            boolean requestSuccessful = false;

            // set up request
            final String requestString = "/gnutella/push-proxy?ServerID=" + 
                Base32.encode(file.getClientGUID());
            final String nodeString = "X-Node:";
            final String nodeValue = NetworkUtils.ip2string(addr) + ":" + port;

            // try to contact each proxy
            Iterator iter = proxies.iterator();
            while(iter.hasNext() && !requestSuccessful) {
                PushProxyInterface ppi = (PushProxyInterface)iter.next();
                try {
                    String ppIp = ppi.getPushProxyAddress().getHostName();
                    int ppPort = ppi.getPushProxyPort();
                    URL url = new URL("http",ppIp, ppPort, requestString);
                    HttpURLConnection connection = 
                    (HttpURLConnection) url.openConnection();
                    connection.setUseCaches(false);
                    connection.setRequestProperty(nodeString, nodeValue);
                    requestSuccessful = (connection.getResponseCode() == 202);
                    connection.disconnect();
                }
                catch (MalformedURLException url) {
                    ErrorService.error(url);
                }
                catch (IOException ioe) {
                }
            }

            if (requestSuccessful)
                return requestSuccessful;
            // else just send a PushRequest as normal
        }
        
        PushRequest pr = 
            new PushRequest(GUID.makeGuid(),
                            ConnectionSettings.TTL.getValue(),
                            file.getClientGUID(),
                            file.getIndex(),
                            addr,
                            port);

        try {
            router.sendPushRequest(pr);
        } catch (IOException e) {
            return false;
        }

        return true;
    }


    /////////////////// Internal Method to Parse GIV String ///////////////////

    private static final class GIVLine {
        final String file;
        final int index;
        final byte[] clientGUID;
        GIVLine(String file, int index, byte[] clientGUID) {
            this.file=file;
            this.index=index;
            this.clientGUID=clientGUID;
        }
    }

    /** 
     * Returns the file, index, and client GUID from the GIV request from s.
     * The input stream of s is positioned just after the GIV request,
     * immediately before any HTTP.  If s is closed or the line couldn't
     * be parsed, throws IOException.
     *     @requires "GIV " just read from s
     *     @modifies s's input stream.
     */
    private static GIVLine parseGIV(Socket s) throws IOException {
        //1. Read  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
        String command;
        try {
            //The try-catch below is a work-around for JDK bug 4091706.
            InputStream istream=null;
            try {
                istream = s.getInputStream();
            } catch (Exception e) {
                throw new IOException();
            }
            ByteReader br = new ByteReader(istream);
            command = br.readLine();      // read in the first line
            if (command==null)
                throw new IOException();
            String next=br.readLine();    // read in empty line
            if (next==null || (! next.equals(""))) {
                throw new IOException();
            }
        } catch (IOException e) {        
            throw e;                   
        }   

        //2. Parse and return the fields.
        try {            
            //a) Extract file index.  IndexOutOfBoundsException
            //   or NumberFormatExceptions will be thrown here if there's
            //   a problem.  They're caught below.
            int i=command.indexOf(":");
            int index=Integer.parseInt(command.substring(0,i));
            //b) Extract clientID.  This can throw
            //   IndexOutOfBoundsException or
            //   IllegalArgumentException, which is caught below.
            int j=command.indexOf("/", i);
            byte[] guid=GUID.fromHexString(command.substring(i+1,j));
            //c). Extract file name.
            String filename=URLDecoder.decode(command.substring(j+1));

            return new GIVLine(filename, index, guid);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IllegalArgumentException e) {
            throw new IOException();
        }          
    }


    /** Calls measureBandwidth on each uploader. */
    public synchronized void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        for (Iterator iter = active.iterator(); iter.hasNext(); ) {
            c = true;
			BandwidthTracker bt = (BandwidthTracker)iter.next();
			bt.measureBandwidth();
			currentTotal += bt.getAverageBandwidth();
		}
		if ( c )
		    averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
		                    / ++numMeasures;
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
	public synchronized float getMeasuredBandwidth() {
        float sum=0;
        for (Iterator iter = active.iterator(); iter.hasNext(); ) {
			BandwidthTracker bt = (BandwidthTracker)iter.next();
            float curr = 0;
            try{
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;//insufficient data? assume 0
            }
			sum+=curr;
		}
        return sum;
	}
	
	/**
	 * returns the summed average of the downloads
	 */
	public synchronized float getAverageBandwidth() {
        return averageBandwidth;
	}		

    public synchronized String dumpDownloaders() {
        String ret = "\nactive downloaders: ";
        Iterator iter = active.iterator();
        while(iter.hasNext()) {
            ret += ((ManagedDownloader)iter.next()) + "|";
        }
        iter = waiting.iterator();
        ret += "\nwaiting downloaders: ";
        while(iter.hasNext()) {
            ret += ((ManagedDownloader)iter.next()) + "|";
        }
        return ret;
    }


    private final boolean debugOn = false;
    private final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }


    /*
    public static void main(String argv[]) {
        DownloadManager dm = new DownloadManager();
        dm.extractQueryStringUNITTEST();
    }
    */

}
