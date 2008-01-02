package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesDelegate;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandler;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.version.DownloadInformation;


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
public interface DownloadManager extends BandwidthTracker, SaveLocationManager, LWSIntegrationServicesDelegate, PushedSocketHandler {
    
    public void register(PushedSocketHandlerRegistry registry);

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
    public void initialize();

    /**
     * Performs the slow, low-priority initialization tasks: reading in
     * snapshots and scheduling snapshot checkpointing.
     * 
     * @param lwsManager 
     */
    public void postGuiInit();

    /**
     * Is the GUI init'd?
     */
    public boolean isGUIInitd();

    /**
     * Determines if an 'In Network' download exists in either active or waiting.
     */
    public boolean hasInNetworkDownload();

    /**
     * Determines if any store download exists in either active or waiting
     * state. 
     */
    public boolean hasStoreDownload();

    /**
     * Kills all in-network downloaders that are not present in the list of URNs
     * @param urns a current set of urns that we are downloading in-network.
     */
    public void killDownloadersNotListed(Collection<? extends DownloadInformation> updates);

    public PushDownloadManager getPushManager();

    public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket);

    /**
     * Schedules the runnable that pumps through waiting downloads.
     */
    public void scheduleWaitingPump();

    /**
     * Determines if the given URN has an incomplete file.
     */
    public boolean isIncomplete(URN urn);

    /**
     * Returns whether or not we are actively downloading this file.
     */
    public boolean isActivelyDownloading(URN urn);

    /**
     * Returns the IncompleteFileManager used by this DownloadManager
     * and all ManagedDownloaders.
     */
    public IncompleteFileManager getIncompleteFileManager();

    public int downloadsInProgress();

    public int getNumIndividualDownloaders();

    /**
     * Inner network traffic and downloads from the LWS don't count towards overall
     * download activity.
     */
    public int getNumActiveDownloads();

    public int getNumWaitingDownloads();

    public Downloader getDownloaderForURN(URN sha1);

    public Downloader getDownloaderForURNString(String urn);

    /**
     * Returns the active or waiting downloader that uses or will use 
     * <code>file</code> as incomplete file.
     * @param file the incomplete file candidate
     * @return <code>null</code> if no downloader for the file is found
     */
    public Downloader getDownloaderForIncompleteFile(File file);

    public boolean isGuidForQueryDownloading(GUID guid);

    /**
     * Clears all downloads.
     */
    public void clearAllDownloads();

    /** Reads the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downloads will restart immediately if slots
     *  are available.  Returns false iff the file could not be read for any
     *  reason.  THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. 
     *  It is public for testing purposes only!  
     *  @param file the downloads.dat snapshot file */
    public boolean readAndInitializeSnapshot(File file);

    /* public for testing only right now. */
    public List<AbstractDownloader> readSnapshot(File file) throws IOException;

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
    public Downloader download(RemoteFileDesc[] files, List<? extends RemoteFileDesc> alts,
            GUID queryGUID, boolean overwrite, File saveDir, String fileName)
            throws SaveLocationException;

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
    public Downloader download(MagnetOptions magnet, boolean overwrite, File saveDir,
            String fileName) throws IllegalArgumentException, SaveLocationException;

    /**
     * Creates a new LimeWire Store (LWS) download. Store downloads are handled in a similar fashion as
     * MAGNET links except there are no alternative locations.  <tt>filename</tt> should always
     * be specified since we have complete control over META-DATA for these downloads, it will 
     * be used as the name of the complete file. Unlike all other downloads performed here, 
     * saveDir is a unique directory specified in the options menu under Store Downloads
     * 
     * @param store - Descriptor describing the download from the store including URN
     * @param overwrite - true if same file names should be overwritten
     * @param saveDir - directory to save the completed file into
     * @param fileName - name of the completed file
     * @return
     * @throws IllegalArgumentException
     * @throws SaveLocationException
     */
    public Downloader downloadFromStore(RemoteFileDesc rfd, boolean overwrite, File saveDir,
            String fileName) throws IllegalArgumentException, SaveLocationException;

    /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SaveLocationException 
     */
    public Downloader download(File incompleteFile) throws CantResumeException,
            SaveLocationException;

    /**
     * Downloads an InNetwork update, using the info from the DownloadInformation.
     */
    public Downloader download(DownloadInformation info, long now) throws SaveLocationException;

    public Downloader downloadTorrent(BTMetaInfo info, boolean overwrite)
            throws SaveLocationException;

    /**
     * Returns <code>true</code> if there already is a download with the same urn. 
     * @param urn may be <code>null</code>, then a check based on the fileName
     * and the fileSize is performed
     * @return
     */
    public boolean conflicts(URN urn, long fileSize, File... fileName);

    /**
     * Returns <code>true</code> if there already is a download that is or
     * will be saving to this file location.
     * @param candidateFile the final file location.
     * @return
     */
    public boolean isSaveLocationTaken(File candidateFile);

    /** 
     * Adds all responses (and alternates) in qr to any downloaders, if
     * appropriate.
     */
    public void handleQueryReply(QueryReply qr);

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     * If completed is true, finishes the download completely.  Otherwise,
     * puts the download back in the waiting list to be finished later.
     *     @modifies this, callback
     */
    public void remove(AbstractDownloader downloader, boolean completed);

    /**
     * Bumps the priority of an inactive download either up or down
     * by amt (if amt==0, bump to start/end of list).
     */
    public void bumpPriority(Downloader downl, boolean up, int amt);

    /** 
     * Attempts to send the given requery to provide the given downloader with 
     * more sources to download.  May not actually send the requery if it doing
     * so would exceed the maximum requery rate.
     * @param query the requery to send, which should have a marked GUID.
     *  Queries are subjected to global rate limiting iff they have marked 
     *  requery GUIDs.
     */
    public void sendQuery(QueryRequest query);

    /** Calls measureBandwidth on each uploader. */
    public void measureBandwidth();

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
    public float getMeasuredBandwidth();

    /**
     * returns the summed average of the downloads
     */
    public float getAverageBandwidth();

    /**
     * Returns the measured bandwidth as calculated from the last
     * getMeasuredBandwidth() call.
     */
    public float getLastMeasuredBandwidth();

    public Iterable<AbstractDownloader> getAllDownloaders();
    
    /** Writes a snapshot of all downloaders in this and all incomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  at any time for checkpointing purposes.  Returns true iff the file was
     *  successfully written. */
    public boolean writeSnapshot();

}
