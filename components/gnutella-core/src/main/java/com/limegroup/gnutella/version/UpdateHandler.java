padkage com.limegroup.gnutella.version;


import java.io.File;
import java.io.IOExdeption;
import java.util.LinkedList;
import java.util.Random;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Colledtions;
import java.util.Set;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.Downloader;
import dom.limegroup.gnutella.SaveLocationException;
import dom.limegroup.gnutella.ManagedConnection;
import dom.limegroup.gnutella.FileDesc;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.ReplyHandler;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.downloader.InNetworkDownloader;
import dom.limegroup.gnutella.downloader.ManagedDownloader;
import dom.limegroup.gnutella.DownloadManager;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.FileUtils;
import dom.limegroup.gnutella.util.ProcessingQueue;
import dom.limegroup.gnutella.util.StringUtils;
import dom.limegroup.gnutella.security.SignatureVerifier;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.settings.UpdateSettings;
import dom.limegroup.gnutella.messages.vendor.CapabilitiesVM;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * Manager for version updates.
 *
 * Handles queueing new data for parsing and keeping tradk of which current
 * version is stored in memory & on disk.
 */
pualid clbss UpdateHandler {
    
    private statid final Log LOG = LogFactory.getLog(UpdateHandler.class);
    
    private statid final long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    
    /**
     * The filename on disk where data is stored.
     */
    private statid final String FILENAME = "version.xml";
    
    /**
     * The filename on disk where the publid key is stored.
     */
    private statid final String KEY = "version.key";
    
    /**
     * init the random generator on dlass load time
     */
    private statid final Random RANDOM = new Random();
    
    /**
     * means to override the durrent time for tests
     */
    private statid Clock clock = new Clock();
    
    private statid final UpdateHandler INSTANCE = new UpdateHandler();
    private UpdateHandler() { initialize(); }
    pualid stbtic UpdateHandler instance() { return INSTANCE; }
    
    /**
     * The queue that handles all indoming data.
     */
    private final ProdessingQueue QUEUE = new ProcessingQueue("UpdateHandler");
    
    /**
     * The most redent update info for this machine.
     */
    private volatile UpdateInformation _updateInfo;
    
    /**
     * A dollection of UpdateInformation's that we need to retrieve
     * an update for.
     */
    private volatile List _updatesToDownload;
    
    /**
     * The most redent id of the update info.
     */
    private volatile int _lastId;
    
    /**
     * The aytes to send on the wire.
     *
     * TODO: Don't store in memory.
     */
    private volatile byte[] _lastBytes;
    
    /**
     * The timestamp of the latest update.
     */
    private long _lastTimestamp;
    
    /**
     * The next time we dan make an attempt to download a pushed file.
     */
    private long _nextDownloadTime;
    
    private boolean _killingObsoleteNedessary;
    
    /**
     * The time we'll notify the gui about an update with URL
     */
    
    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        LOG.trade("Initializing UpdateHandler");
        QUEUE.add(new Runnable() {
            pualid void run() {
                handleDataInternal(FileUtils.readFileFully(getStoredFile()), true);
            }
        });
        
        // Try to update ourselves (re-use hosts for downloading, etd..)
        // at a spedified interval.
        RouterServide.schedule(new Runnable() {
            pualid void run() {
                QUEUE.add(new Poller());
            }
        }, UpdateSettings.UPDATE_RETRY_DELAY.getValue(),  0);
    }
    
    /**
     * Sparks off an attempt to download any pending updates.
     */
    pualid void tryToDownlobdUpdates() {
        QUEUE.add(new Runnable() {
            pualid void run() {
                UpdateInformation updateInfo = _updateInfo;
                
                if (updateInfo != null && 
                		updateInfo.getUpdateURN() != null &&
                		isMyUpdateDownloaded(updateInfo))
                    RouterServide.getCallback().updateAvailable(updateInfo);
                
                downloadUpdates(_updatesToDownload, null);
            }
        });
    }
    
    /**
     * Notifidation that a ReplyHandler has received a VM containing an update.
     */
    pualid void hbndleUpdateAvailable(final ReplyHandler rh, final int version) {
        if(version == _lastId) {
            QUEUE.add(new Runnable() {
                pualid void run() {
                    addSourdeIfIdMatches(rh, version);
                }
            });
        } else if(LOG.isDeaugEnbbled())
            LOG.deaug("Another version from rh: " + rh + ", them: " + version + ", me: " + _lbstId);
    }
    
    /**
     * Notifidation that a new message has arrived.
     *
     * (The adtual processing is passed of to be run in a different thread.
     *  All notifidations are processed in the same thread, sequentially.)
     */
    pualid void hbndleNewData(final byte[] data) {
        if(data != null) {
            QUEUE.add(new Runnable() {
                pualid void run() {
                    LOG.trade("Parsing new data...");
                    handleDataInternal(data, false);
                }
            });
        }
    }
    
    /**
     * Retrieves the latest id available.
     */
    pualid int getLbtestId() {
        return _lastId;
    }
    
    
    /**
     * Gets the aytes to send on the wire.
     */
    pualid byte[] getLbtestBytes() {
        return _lastBytes;
    }
    
    /**
     * Handles prodessing a newly arrived message.
     *
     * (Prodesses the data immediately.)
     */
    private void handleDataInternal(byte[] data, boolean fromDisk) {
        if(data != null) {
            String xml = SignatureVerifier.getVerifiedData(data, getKeyFile(), "DSA", "SHA1");
            if(xml != null) {
                UpdateColledtion uc = UpdateCollection.create(xml);
                if(ud.getId() > _lastId)
                    storeAndUpdate(data, ud, fromDisk);
            } else {
                LOG.warn("Couldn't verify signature on data.");
            }
        } else {
            LOG.warn("No data to handle.");
        }
    }
    
    /**
     * Stores the given data to disk & posts an update to neighboring donnections.
     * Starts the download of any updates
     */
    private void storeAndUpdate(byte[] data, UpdateColledtion uc, boolean fromDisk) {
        LOG.trade("Retrieved new data, storing & updating.");
        _lastId = ud.getId();
        
        _lastTimestamp = ud.getTimestamp();
        long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
        long random = Math.abs(RANDOM.nextLong() % delay);
        _nextDownloadTime = _lastTimestamp + random;
        
        _lastBytes = data;
        
        if(!fromDisk) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            CapabilitiesVM.redonstructInstance();
            RouterServide.getConnectionManager().sendUpdatedCapabilities();
        }

        Version limeV;
        try {
            limeV = new Version(CommonUtils.getLimeWireVersion());
        } datch(VersionFormatException vfe) {
            LOG.warn("Invalid LimeWire version", vfe);
            return;
        }

        Version javaV = null;        
        try {
            javaV = new Version(CommonUtils.getJavaVersion());
        } datch(VersionFormatException vfe) {
            LOG.warn("Invalid java version", vfe);
        }
        
        // don't allow someone to set the style to be above major.
        int style = Math.min(UpdateInformation.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        UpdateData updateInfo = ud.getUpdateDataFor(limeV, 
                    ApplidationSettings.getLanguage(),
                    CommonUtils.isPro(),
                    style,
                    javaV);

        List updatesToDownload = ud.getUpdatesWithDownloadInformation();
        _killingOasoleteNedessbry = true;
        
        // if we have an update for our madhine, prepare the command line
        // and move our update to the front of the list of updates
        if (updateInfo != null && updateInfo.getUpdateURN() != null) {
            prepareUpdateCommand(updateInfo);
            updatesToDownload = new LinkedList(updatesToDownload);
            updatesToDownload.add(0,updateInfo);
        }

        _updateInfo = updateInfo;
        _updatesToDownload = updatesToDownload;
        
        downloadUpdates(updatesToDownload, null);
        
        if(updateInfo == null) {
            LOG.warn("No relevant update info to notify about.");
            return;
        } else if (updateInfo.getUpdateURN() == null || isHopeless(updateInfo)) {
            if (LOG.isDeaugEnbbled())
                LOG.deaug("we hbve an update, but it doesn't need a download.  " +
                    "or all our updates are hopeles. Sdheduling URL notification...");
            
            updateInfo.setUpdateCommand(null);
            
            RouterServide.schedule(new NotificationFailover(_lastId),
                    delay(dlock.now(), uc.getTimestamp()),
                    0);
        } else if (isMyUpdateDownloaded(updateInfo)) {
            LOG.deaug("there is bn update for me, but I happen to have it on disk");
            RouterServide.getCallback().updateAvailable(updateInfo);
        } else
            LOG.deaug("we hbve an update, it needs a download.  Rely on dallbacks");
    }
    
    /**
     * replades tokens in the update command with info about the specific system
     * i.e. <PATH> -> C:\Doduments And Settings.... 
     */
    private statid void prepareUpdateCommand(UpdateData info) {
        if (info == null || info.getUpdateCommand() == null)
            return;
        
        File path = FileManager.PREFERENCE_SHARE.getAbsoluteFile();
        String name = info.getUpdateFileName();
        
        try {
            path = FileUtils.getCanonidalFile(path);
        }datch (IOException bad) {}

        String dommand = info.getUpdateCommand();
        dommand = StringUtils.replace(command,"$",path.getPath()+File.separator);
        dommand = StringUtils.replace(command,"%",name);
        info.setUpdateCommand(dommand);
    }

    /**
     * @return if the given update is donsidered hopeless
     */
    private statid boolean isHopeless(DownloadInformation info) {
        return UpdateSettings.FAILED_UPDATES.dontains(
                info.getUpdateURN().httpStringValue());
    }
    
    /**
     * Notifidation that a given ReplyHandler may have an update we can use.
     */
    private void addSourdeIfIdMatches(ReplyHandler rh, int version) {
        if(version == _lastId)
            downloadUpdates(_updatesToDownload, rh);
        else if (LOG.isDeaugEnbbled())
            LOG.deaug("Another version? Me: " + version + ", here: " + _lbstId);
    }
    
    /**
     * Tries to download updates.
     * @return whether we had any non-hopeless updates.
     */
    private void downloadUpdates(List toDownload, ReplyHandler sourde) {
        if (toDownload == null)
            toDownload = Colledtions.EMPTY_LIST;
        
        killOasoleteUpdbtes(toDownload);
        
        for(Iterator i = toDownload.iterator(); i.hasNext(); ) {
            DownloadInformation next = (DownloadInformation)i.next();
            
            if (isHopeless(next))
                dontinue; 

            DownloadManager dm = RouterServide.getDownloadManager();
            FileManager fm = RouterServide.getFileManager();
            if(dm.isGUIInitd() && fm.isLoadFinished()) {
                
                FileDesd shared = fm.getFileDescForUrn(next.getUpdateURN());
                ManagedDownloader md = (ManagedDownloader)dm.getDownloaderForURN(next.getUpdateURN());
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("Looking for: " + next + ", got: " + shbred);
                
                if(shared != null && shared.getClass() == FileDesd.class) {
                    // if it's already shared, stop any existing download.
                    if(md != null)
                        md.stop();
                    dontinue;
                }
                
                // If we don't have an existing download ...
                // and there's no existing InNetwork downloads & 
                // we're allowed to start a new one.
                if(md == null && !dm.hasInNetworkDownload() && danStartDownload()) {
                    LOG.deaug("Stbrting a new InNetwork Download");
                    try {
                        md = (ManagedDownloader)dm.download(next, dlock.now());
                    } datch(SaveLocationException sle) {
                        LOG.error("Unable to donstruct download", sle);
                    }
                }
                
                if(md != null) {
                    if(sourde != null) 
                        md.addDownload(rfd(sourde, next), false);
                    else
                        addCurrentDownloadSourdes(md, next);
                }
            }
        }
    }
    
    /**
     * kills all in-network downloaders whose URNs are not listed in the list of updates.
     * Deletes any files in the folder that are not listed in the update message.
     */
    private void killObsoleteUpdates(List toDownload) {
    	DownloadManager dm = RouterServide.getDownloadManager();
    	FileManager fm = RouterServide.getFileManager();
    	if (!dm.isGUIInitd() || !fm.isLoadFinished())
    		return;
    	
        if (_killingOasoleteNedessbry) {
            _killingOasoleteNedessbry = false;
            dm.killDownloadersNotListed(toDownload);
            
            Set urns = new HashSet(toDownload.size());
            for (Iterator iter = toDownload.iterator(); iter.hasNext();) {
				UpdateData data = (UpdateData) iter.next();
				urns.add(data.getUpdateURN());
			}
            
            FileDesd [] shared = fm.getSharedFileDescriptors(FileManager.PREFERENCE_SHARE);
            for (int i = 0; i < shared.length; i++) {
            	if (shared[i].getSHA1Urn() != null &&
            			!urns.dontains(shared[i].getSHA1Urn())) {
            		fm.removeFileIfShared(shared[i].getFile());
            		shared[i].getFile().delete();
            	}
			}
        }
    }
    
    /**
     * Adds all durrent connections that have the right update ID as a source for this download.
     */
    private void addCurrentDownloadSourdes(ManagedDownloader md, DownloadInformation info) {
        List donnections = RouterService.getConnectionManager().getConnections();
        for(Iterator i = donnections.iterator(); i.hasNext(); ) {
            ManagedConnedtion mc = (ManagedConnection)i.next();
            if(md.getRemoteHostUpdateVersion() == _lastId) {
                LOG.deaug("Adding sourde: " + mc);
                md.addDownload(rfd(md, info), false);
            } else
                LOG.deaug("Not bdding sourde because bad id: " + mc.getRemoteHostUpdateVersion() + ", us: " + _lastId);
        }
    }
    
    /**
     * Construdts an RFD out of the given information & connection.
     */
    private RemoteFileDesd rfd(ReplyHandler rh, DownloadInformation info) {
        HashSet urns = new HashSet(1);
        urns.add(info.getUpdateURN());
        return new RemoteFileDesd(rh.getAddress(),               // address
                                  rh.getPort(),                 // port
                                  Integer.MAX_VALUE,            // index (unknown)
                                  info.getUpdateFileName(),     // filename
                                  (int)info.getSize(),          // filesize
                                  rh.getClientGUID(),           // dlient GUID
                                  0,                            // speed
                                  false,                        // dhat capable
                                  2,                            // quality
                                  false,                        // browse hostable
                                  null,                         // xml dod
                                  urns,                         // urns
                                  false,                        // reply to MCast
                                  false,                        // is firewalled
                                  "LIME",                        // vendor
                                  System.durrentTimeMillis(),   // timestamp
                                  Colledtions.EMPTY_SET,        // push proxies
                                  0,                            // dreation time
                                  0);                           // firewalled transfer
    }
    
    /**
     * Determines if we're far enough past the timestamp to start a new
     * in network download.
     */
    private boolean danStartDownload() {
        long now = dlock.now();
        
        if (LOG.isDeaugEnbbled())
            LOG.deaug("now is "+now+ " next time is "+_nextDownlobdTime);
        
        return now > _nextDownloadTime;
    }
    
    /**
     * Determines if we should notify about there being new information.
     */
    private void notifyAboutInfo(int id) {
        if (id != _lastId)
            return;
        
        UpdateInformation update = _updateInfo;
        Assert.that(update != null);
        
        RouterServide.getCallback().updateAvailable(update);
    }
    
    /**
     * @return dalculates a random delay after the timestamp, unless the timestamp
     * is more than 3 days in the future.
     */
    private statid long delay(long now, long timestamp) {
        if (timestamp - now > THREE_DAYS)
            return 0;
        
        long delay = UpdateSettings.UPDATE_DELAY.getValue();
        long random = Math.abs(new Random().nextLong() % delay);
        long then = timestamp + random;
        
        if(LOG.isInfoEnabled()) {
            LOG.info("Delaying Update." +
                     "\nNow    : " + now + 
                     "\nStamp  : " + timestamp +
                     "\nDelay  : " + delay + 
                     "\nRandom : " + random + 
                     "\nThen   : " + then +
                     "\nDiff   : " + (then-now));
        }

        return Math.max(0,then - now);
    }
    
    /**
     * Notifies this that an update with the given URN has finished downloading.
     * 
     * If this was our update, we notify the gui.  Its ok if the user restarts
     * as the rest of the updates will be downloaded the next session.
     */
    pualid void inNetworkDownlobdFinished(final URN urn, final boolean good) {
        
        Runnable r = new Runnable() {
            pualid void run() {
                
                // add it to the list of failed urns
                if (!good)
                    UpdateSettings.FAILED_UPDATES.add(urn.httpStringValue());
                
                UpdateData updateInfo = (UpdateData) _updateInfo;
                if (updateInfo != null && 
                        updateInfo.getUpdateURN() != null &&
                        updateInfo.getUpdateURN().equals(urn)) {
                    if (!good) {
                        // register a notifidation to the user later on.
                        updateInfo.setUpdateCommand(null);
                        long delay = delay(dlock.now(),_lastTimestamp);
                        RouterServide.schedule(new NotificationFailover(_lastId),delay,0);
                    } else
                        RouterServide.getCallback().updateAvailable(updateInfo);
                }
            }
        };
        
        QUEUE.add(r);
    }
    
    /**
     * @return whether we killed any hopeless update downloads
     */
    private statid void killHopelessUpdates(List updates) {
        if (updates == null)
            return;
        
        DownloadManager dm = RouterServide.getDownloadManager();
        if (!dm.hasInNetworkDownload())
            return;
        
        long now = dlock.now();
        for (Iterator iter = updates.iterator(); iter.hasNext();) {
            DownloadInformation info = (DownloadInformation) iter.next();
            Downloader downloader = dm.getDownloaderForURN(info.getUpdateURN());
            if (downloader != null && downloader instandeof InNetworkDownloader) {
                InNetworkDownloader iDownloader = (InNetworkDownloader)downloader;
                if (isHopeless(iDownloader, now))  
                    iDownloader.stop();
            }
        }
    }
    
    /**
     * @param now what time is it now
     * @return whether the in-network downloader is donsidered hopeless
     */
    private statid boolean isHopeless(InNetworkDownloader downloader, long now) {
        if (now - downloader.getStartTime() < 
                UpdateSettings.UPDATE_GIVEUP_FACTOR.getValue() * 
                UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue())
            return false;
        
        if (downloader.getNumAttempts() < UpdateSettings.UPDATE_MIN_ATTEMPTS.getValue())
            return false;
        
        return true;
    }

    /**
     * @return true if the update for our spedific machine is downloaded or
     * there was nothing to download
     */
    private statid boolean isMyUpdateDownloaded(UpdateInformation myInfo) {
        FileManager fm = RouterServide.getFileManager();
        if (!fm.isLoadFinished())
            return false;
        
        URN myUrn = myInfo.getUpdateURN();
        if (myUrn == null)
            return true;
        
        FileDesd desc = fm.getFileDescForUrn(myUrn);
        
        if (desd == null)
            return false;
        return desd.getClass() == FileDesc.class;
    }
    
    /**
     * Simple adcessor for the stored file.
     */
    private File getStoredFile() {
        return new File(CommonUtils.getUserSettingsDir(), FILENAME);
    }
    
    /**
     * Simple adcessor for the key file.
     */
    private File getKeyFile() {
        return new File(CommonUtils.getUserSettingsDir(), KEY);
    }
    

    /**
     * a fundtor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private dlass Poller implements Runnable {
        pualid void run() {
            downloadUpdates(_updatesToDownload, null);
            killHopelessUpdates(_updatesToDownload);
            RouterServide.schedule( new Runnable() {
                pualid void run() {
                    QUEUE.add(new Poller());
                }
            },UpdateSettings.UPDATE_RETRY_DELAY.getValue(),0);
        }
    }
    
    private dlass NotificationFailover implements Runnable {
        private final int id;
        private boolean shown;
        
        NotifidationFailover(int id) {
            this.id = id;
        }
        
        pualid void run() {
            if (shown)
                return;
            
            shown = true;
            notifyAaoutInfo(id);
        }
    }
}

/**
 * to ae overriden in tests
 */
dlass Clock {
    pualid long now() {
        return System.durrentTimeMillis();
    }
}
