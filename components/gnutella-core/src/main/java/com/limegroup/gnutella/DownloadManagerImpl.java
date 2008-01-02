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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.DualIterator;
import org.limewire.collection.MultiIterable;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.service.MessageService;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PurchasedStoreDownloaderFactory;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.downloader.StoreDownloader;
import com.limegroup.gnutella.downloader.AbstractDownloader.DownloaderType;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.version.DownloadInformation;

@Singleton
public class DownloadManagerImpl implements DownloadManager {
private static final Log LOG = LogFactory.getLog(DownloadManagerImpl.class);
    
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds

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
     * The number of active store downloads. These are counted when determining
     * how many downloaders are active
     */
    private int storeDownloadCount = 0;
    
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
    
    private final NetworkManager networkManager;
    private final DownloadCallback innetworkCallback;
    private final BTDownloaderFactory btDownloaderFactory;
    private final Provider<DownloadCallback> downloadCallback;
    private final Provider<MessageRouter> messageRouter;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final GnutellaDownloaderFactory gnutellaDownloaderFactory;
    private final PurchasedStoreDownloaderFactory purchasedDownloaderFactory;
    
    @Inject
    public DownloadManagerImpl(NetworkManager networkManager,
            @Named("inNetwork") DownloadCallback innetworkCallback,
            BTDownloaderFactory btDownloaderFactory,
            Provider<DownloadCallback> downloadCallback,
            Provider<MessageRouter> messageRouter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<TorrentManager> torrentManager,
            Provider<PushDownloadManager> pushDownloadManager,
            GnutellaDownloaderFactory gnutellaDownloaderFactory,
            PurchasedStoreDownloaderFactory purchasedDownloaderFactory) {
        this.networkManager = networkManager;
        this.innetworkCallback = innetworkCallback;
        this.btDownloaderFactory = btDownloaderFactory;
        this.downloadCallback = downloadCallback;
        this.messageRouter = messageRouter;
        this.backgroundExecutor = backgroundExecutor;
        this.torrentManager = torrentManager;
        this.pushDownloadManager = pushDownloadManager;
        this.gnutellaDownloaderFactory = gnutellaDownloaderFactory;
        this.purchasedDownloaderFactory = purchasedDownloaderFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#register(com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry)
     */
    @Inject
    public void register(PushedSocketHandlerRegistry registry) {
        registry.register(this);
    }

    //////////////////////// Creation and Saving /////////////////////////

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#initialize()
     */
    public void initialize() {
        scheduleWaitingPump();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#postGuiInit()
     */
    public void postGuiInit() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE.getValue();
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.getValue();
        // Try once with the real file, then with the backup file.
        if( !readAndInitializeSnapshot(real) ) {
            LOG.debug("Reading real downloads.dat failed");
            // if backup succeeded, copy into real.
            if( readAndInitializeSnapshot(backup) ) {
                LOG.debug("Reading backup downloads.bak succeeded.");
                copyBackupToReal();
            // only show the error if the files existed but couldn't be read.
            } else if(backup.exists() || real.exists()) {
                LOG.debug("Reading both downloads files failed.");
                MessageService.showError(I18nMarker.marktr("Sorry, but LimeWire was unable to restart your old downloads."));
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
        backgroundExecutor.scheduleWithFixedDelay(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME, TimeUnit.MILLISECONDS);                
                               
        guiInit = true;
    }      
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isGUIInitd()
     */
    public boolean isGUIInitd() {
        return guiInit;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#hasInNetworkDownload()
     */
    public synchronized boolean hasInNetworkDownload() {
        if(innetworkCount > 0)
            return true;
        for(Iterator<AbstractDownloader> i = waiting.iterator(); i.hasNext(); ) {
            if(i.next().getDownloadType() == DownloaderType.INNETWORK)
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#hasStoreDownload()
     */
    public synchronized boolean hasStoreDownload() {
        if(storeDownloadCount > 0)
            return true;
        for(Iterator<AbstractDownloader> i = waiting.iterator(); i.hasNext(); ) {
            if( i.next().getDownloadType() == DownloaderType.STORE)
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#killDownloadersNotListed(java.util.Collection)
     */
    public synchronized void killDownloadersNotListed(Collection<? extends DownloadInformation> updates) {
        if (updates == null)
            return;
        
        Set<String> urns = new HashSet<String>(updates.size());
        for(DownloadInformation ui : updates)
            urns.add(ui.getUpdateURN().httpStringValue());
        
        for (Iterator<AbstractDownloader> iter = new DualIterator<AbstractDownloader>(waiting.iterator(),active.iterator());
        iter.hasNext();) {
            AbstractDownloader d = iter.next();
            if (d.getDownloadType() == DownloaderType.INNETWORK  && 
                    !urns.contains(d.getSHA1Urn().httpStringValue())) 
                d.stop();
        }
        
        Set<String> hopeless = UpdateSettings.FAILED_UPDATES.getValue();
        hopeless.retainAll(urns);
        UpdateSettings.FAILED_UPDATES.setValue(hopeless);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getPushManager()
     */
    public PushDownloadManager getPushManager() {
        return pushDownloadManager.get();
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
    private synchronized boolean handleIncomingPush(String file, int index, byte [] clientGUID, Socket socket) {
         boolean handled = false;
         for (AbstractDownloader md : activeAndWaiting) {
            if (! (md instanceof ManagedDownloader))
                continue; // pushes apply to gnutella downloads only
            ManagedDownloader mmd = (ManagedDownloader)md;
            if (mmd.acceptDownload(file, socket, index, clientGUID))
                handled = true;
         }                 
         return handled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#acceptPushedSocket(java.lang.String, int, byte[], java.net.Socket)
     */
    public boolean acceptPushedSocket(String file, int index,
            byte[] clientGUID, Socket socket) {
        return handleIncomingPush(file, index, clientGUID, socket);
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#scheduleWaitingPump()
     */
    public void scheduleWaitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            public void run() {
                pumpDownloads();
            }
        };
        backgroundExecutor.scheduleWithFixedDelay(_waitingPump,
                               1000,
                               1000, TimeUnit.MILLISECONDS);
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
            }
            // handle downloads from LWS seperately, only allow 1 at a time
            else if( storeDownloadCount == 0 && md.getDownloadType() == DownloaderType.STORE ) {
                    i.remove();
                    storeDownloadCount++;
                    active.add(md);
                    md.startDownload();
            } else if(hasFreeSlot() && (md.shouldBeRestarted()) && (md.getDownloadType() != DownloaderType.STORE)) {
                i.remove();
                if(md.getDownloadType() == DownloaderType.INNETWORK)
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isIncomplete(com.limegroup.gnutella.URN)
     */
    public boolean isIncomplete(URN urn) {
        return incompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isActivelyDownloading(com.limegroup.gnutella.URN)
     */
    public boolean isActivelyDownloading(URN urn) {
        Downloader md = getDownloaderForURN(urn);
        
        if(md == null)
            return false;
            
        switch(md.getState()) {
        case QUEUED:
        case BUSY:
        case ABORTED:
        case GAVE_UP:
        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case REMOTE_QUEUED:
        case WAITING_FOR_USER:
            return false;
        default:
            return true;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getIncompleteFileManager()
     */
    public IncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }    
 
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#downloadsInProgress()
     */
    public synchronized int downloadsInProgress() {
        return active.size() + waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNumIndividualDownloaders()
     */
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNumActiveDownloads()
     */
    public synchronized int getNumActiveDownloads() {
        return active.size() - innetworkCount - storeDownloadCount;
    }
   
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getNumWaitingDownloads()
     */
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForURN(com.limegroup.gnutella.URN)
     */
    public synchronized Downloader getDownloaderForURN(URN sha1) {
        for (AbstractDownloader md : activeAndWaiting) {
            if (md.getSHA1Urn() != null && sha1.equals(md.getSHA1Urn()))
                return md;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForURNString(java.lang.String)
     */
    public synchronized Downloader getDownloaderForURNString(String urn) {
        for (AbstractDownloader md : activeAndWaiting) {
            if (md.getSHA1Urn() != null && urn.equals(md.getSHA1Urn().toString()))
                return md;
        }
        return null;
    }    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getDownloaderForIncompleteFile(java.io.File)
     */
    public synchronized Downloader getDownloaderForIncompleteFile(File file) {
        for (AbstractDownloader dl : activeAndWaiting) {
            if (dl.conflictsWithIncompleteFile(file)) {
                return dl;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isGuidForQueryDownloading(com.limegroup.gnutella.GUID)
     */
    public synchronized boolean isGuidForQueryDownloading(GUID guid) {
        for (AbstractDownloader md : activeAndWaiting) {
            GUID dGUID = md.getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#clearAllDownloads()
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
    
    


    public boolean writeSnapshot() {
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

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#readAndInitializeSnapshot(java.io.File)
     */
    public synchronized boolean readAndInitializeSnapshot(File file) {
        List<AbstractDownloader> buf;
        try {
            buf = readSnapshot(file);
        } catch(IOException iox) {
            LOG.warn("Couldn't read snapshot", iox);
            return false;
        }

        //Initialize and start downloaders. This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (AbstractDownloader downloader : buf) {
                
                waiting.add(downloader);
                downloader.initialize();
                callback(downloader).addDownload(downloader);
            }
            return true;
        } finally {
            // Remove entries that are too old or no longer existent and not actively 
            // downloaded.  
            if (incompleteFileManager.initialPurge(getActiveDownloadFiles(buf)))
                writeSnapshot();
        }
    }
    
    /* public for testing only right now. */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#readSnapshot(java.io.File)
     */
    public List<AbstractDownloader> readSnapshot(File file) throws IOException {        
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
            throw (IOException)new IOException().initCause(t);
        } finally {
            IOUtils.close(in);
        }
        
        // Pump the downloaders through a set, to remove duplicate values.
        // This is necessary in case LimeWire got into a state where a
        // downloader was written to disk twice.
        return new LinkedList<AbstractDownloader>(new LinkedHashSet<AbstractDownloader>(buf));
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
           
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.RemoteFileDesc[], java.util.List, com.limegroup.gnutella.GUID, boolean, java.io.File, java.lang.String)
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
            gnutellaDownloaderFactory.createManagedDownloader(files, 
                queryGUID, saveDir, fileName, overwrite);

        initializeDownload(downloader);
        
        //Now that the download is started, add the sources w/o caching
        downloader.addDownload(alts,false);
        
        return downloader;
    }   
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.browser.MagnetOptions, boolean, java.io.File, java.lang.String)
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
            gnutellaDownloaderFactory.createMagnetDownloader( magnet,
                overwrite, saveDir, fileName);
        initializeDownload(downloader);
        return downloader;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#downloadFromStore(com.limegroup.gnutella.RemoteFileDesc, boolean, java.io.File, java.lang.String)
     */
    public synchronized Downloader downloadFromStore( RemoteFileDesc rfd,
            boolean overwrite,
            File saveDir,
            String fileName)
    throws IllegalArgumentException, SaveLocationException {
        
        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();
        
        if (conflicts(rfd.getSHA1Urn(), 0, new File(saveDir,fileName))) {
            throw new SaveLocationException
            (SaveLocationException.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }
      
        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        StoreDownloader downloader =
            purchasedDownloaderFactory.createStoreDownloader(rfd, incompleteFileManager, 
                                  saveDir, fileName, overwrite);

        initializeDownload(downloader);
        
        return downloader;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(java.io.File)
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
            downloader = gnutellaDownloaderFactory.createResumeDownloader(
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#download(com.limegroup.gnutella.version.DownloadInformation, long)
     */
    public synchronized Downloader download(DownloadInformation info, long now) 
    throws SaveLocationException {
        File dir = SharingUtils.PREFERENCE_SHARE;
        dir.mkdirs();
        File f = new File(dir, info.getUpdateFileName());
        if(conflicts(info.getUpdateURN(), (int)info.getSize(), f))
            throw new SaveLocationException(SaveLocationException.FILE_ALREADY_DOWNLOADING, f);
        
        incompleteFileManager.purge();
        ManagedDownloader d = gnutellaDownloaderFactory.createInNetworkDownloader(
                info, dir, now);
        initializeDownload(d);
        return d;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#downloadTorrent(com.limegroup.bittorrent.BTMetaInfo, boolean)
     */
    public synchronized Downloader downloadTorrent(BTMetaInfo info, boolean overwrite) 
    throws SaveLocationException {
        TorrentFileSystem system = info.getFileSystem();
        checkActiveAndWaiting(info.getURN(), system);
        if (!overwrite)
            checkTargetLocation(system, overwrite);
        else
            torrentManager.get().killTorrentForFile(system.getCompleteFile());
        AbstractDownloader ret = btDownloaderFactory.createBTDownloader(info);
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
        md.initialize();
        waiting.add(md);
        callback(md).addDownload(md);
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                writeSnapshot(); // Save state for crash recovery.
            }
        });
    }
    
    /**
     * Returns the callback that should be used for the given md.
     */
    private DownloadCallback callback(Downloader md) {
        return (md instanceof InNetworkDownloader) ? innetworkCallback : downloadCallback.get();
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#conflicts(com.limegroup.gnutella.URN, long, java.io.File)
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#isSaveLocationTaken(java.io.File)
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#handleQueryReply(com.limegroup.gnutella.messages.QueryReply)
     */
    public void handleQueryReply(QueryReply qr) {
        // first check if the qr is of 'sufficient quality', if not just
        // short-circuit.
        if (qr.calculateQualityOfService(
                !networkManager.acceptedIncomingConnection(), networkManager) < 1)
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
        return active.size() - innetworkCount - storeDownloadCount
            < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#remove(com.limegroup.gnutella.downloader.AbstractDownloader, boolean)
     */
    public synchronized void remove(AbstractDownloader downloader, 
                                    boolean completed) {
        boolean isRemoved = active.remove(downloader);
        if(downloader.getDownloadType() == DownloaderType.INNETWORK)
            innetworkCount--;
        // make sure an active download was removed prior to decrementing this index
        if(downloader.getDownloadType() == DownloaderType.STORE && isRemoved)
            storeDownloadCount--;
        
        waiting.remove(downloader);
        if(completed)
            cleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#bumpPriority(com.limegroup.gnutella.Downloader, boolean, int)
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
        dl.finish();
        if (dl.getQueryGUID() != null)
            messageRouter.get().downloadFinished(dl.getQueryGUID());
        callback(dl).removeDownload(dl);
        
        //Save this' state to disk for crash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback(dl).downloadsComplete();
    }           
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#sendQuery(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.messages.QueryRequest)
     */
    public void sendQuery(QueryRequest query) {
        messageRouter.get().sendDynamicQuery(query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#measureBandwidth()
     */
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

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getMeasuredBandwidth()
     */
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getAverageBandwidth()
     */
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getLastMeasuredBandwidth()
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
    
    // ---------------------------------------------------------------
    // Implementation of LWSIntegrationServicesDelegate

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadMI#getAllDownloaders()
     */
    public final Iterable<AbstractDownloader> getAllDownloaders() {
        return activeAndWaiting;
    }    

}
