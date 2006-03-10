package com.limegroup.gnutella.version;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.security.SignatureVerifier;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Manager for version updates.
 *
 * Handles queueing new data for parsing and keeping track of which current
 * version is stored in memory & on disk.
 */
public class UpdateHandler {
    
    private static final Log LOG = LogFactory.getLog(UpdateHandler.class);
    
    private static final long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    
    /**
     * The filename on disk where data is stored.
     */
    private static final String FILENAME = "version.xml";
    
    /**
     * The filename on disk where the public key is stored.
     */
    private static final String KEY = "version.key";
    
    /**
     * init the random generator on class load time
     */
    private static final Random RANDOM = new Random();
    
    /**
     * means to override the current time for tests
     */
    private static Clock clock = new Clock();
    
    private static final UpdateHandler INSTANCE = new UpdateHandler();
    private UpdateHandler() { initialize(); }
    public static UpdateHandler instance() { return INSTANCE; }
    
    /**
     * The queue that handles all incoming data.
     */
    private final ProcessingQueue QUEUE = new ProcessingQueue("UpdateHandler");
    
    /**
     * The most recent update info for this machine.
     */
    private volatile UpdateInformation _updateInfo;
    
    /**
     * A collection of UpdateInformation's that we need to retrieve
     * an update for.
     */
    private volatile List _updatesToDownload;
    
    /**
     * The most recent id of the update info.
     */
    private volatile int _lastId;
    
    /**
     * The bytes to send on the wire.
     *
     * TODO: Don't store in memory.
     */
    private volatile byte[] _lastBytes;
    
    /**
     * The timestamp of the latest update.
     */
    private long _lastTimestamp;
    
    /**
     * The next time we can make an attempt to download a pushed file.
     */
    private long _nextDownloadTime;
    
    private boolean _killingObsoleteNecessary;
    
    /**
     * The time we'll notify the gui about an update with URL
     */
    
    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        LOG.trace("Initializing UpdateHandler");
        QUEUE.add(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(getStoredFile()), true);
            }
        });
        
        // Try to update ourselves (re-use hosts for downloading, etc..)
        // at a specified interval.
        RouterService.schedule(new Runnable() {
            public void run() {
                QUEUE.add(new Poller());
            }
        }, UpdateSettings.UPDATE_RETRY_DELAY.getValue(),  0);
    }
    
    /**
     * Sparks off an attempt to download any pending updates.
     */
    public void tryToDownloadUpdates() {
        QUEUE.add(new Runnable() {
            public void run() {
                UpdateInformation updateInfo = _updateInfo;
                
                if (updateInfo != null && 
                		updateInfo.getUpdateURN() != null &&
                		isMyUpdateDownloaded(updateInfo))
                    RouterService.getCallback().updateAvailable(updateInfo);
                
                downloadUpdates(_updatesToDownload, null);
            }
        });
    }
    
    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int version) {
        if(version == _lastId) {
            QUEUE.add(new Runnable() {
                public void run() {
                    addSourceIfIdMatches(rh, version);
                }
            });
        } else if(LOG.isDebugEnabled())
            LOG.debug("Another version from rh: " + rh + ", them: " + version + ", me: " + _lastId);
    }
    
    /**
     * Notification that a new message has arrived.
     *
     * (The actual processing is passed of to be run in a different thread.
     *  All notifications are processed in the same thread, sequentially.)
     */
    public void handleNewData(final byte[] data) {
        if(data != null) {
            QUEUE.add(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, false);
                }
            });
        }
    }
    
    /**
     * Retrieves the latest id available.
     */
    public int getLatestId() {
        return _lastId;
    }
    
    
    /**
     * Gets the bytes to send on the wire.
     */
    public byte[] getLatestBytes() {
        return _lastBytes;
    }
    
    /**
     * Handles processing a newly arrived message.
     *
     * (Processes the data immediately.)
     */
    private void handleDataInternal(byte[] data, boolean fromDisk) {
        if(data != null) {
            String xml = SignatureVerifier.getVerifiedData(data, getKeyFile(), "DSA", "SHA1");
            if(xml != null) {
                UpdateCollection uc = UpdateCollection.create(xml);
                if(uc.getId() > _lastId)
                    storeAndUpdate(data, uc, fromDisk);
            } else {
                LOG.warn("Couldn't verify signature on data.");
            }
        } else {
            LOG.warn("No data to handle.");
        }
    }
    
    /**
     * Stores the given data to disk & posts an update to neighboring connections.
     * Starts the download of any updates
     */
    private void storeAndUpdate(byte[] data, UpdateCollection uc, boolean fromDisk) {
        LOG.trace("Retrieved new data, storing & updating.");
        _lastId = uc.getId();
        
        _lastTimestamp = uc.getTimestamp();
        long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
        long random = Math.abs(RANDOM.nextLong() % delay);
        _nextDownloadTime = _lastTimestamp + random;
        
        _lastBytes = data;
        
        if(!fromDisk) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            CapabilitiesVM.reconstructInstance();
            RouterService.getConnectionManager().sendUpdatedCapabilities();
        }

        Version limeV;
        try {
            limeV = new Version(CommonUtils.getLimeWireVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid LimeWire version", vfe);
            return;
        }

        Version javaV = null;        
        try {
            javaV = new Version(CommonUtils.getJavaVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid java version", vfe);
        }
        
        // don't allow someone to set the style to be above major.
        int style = Math.min(UpdateInformation.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        UpdateData updateInfo = uc.getUpdateDataFor(limeV, 
                    ApplicationSettings.getLanguage(),
                    CommonUtils.isPro(),
                    style,
                    javaV);

        List updatesToDownload = uc.getUpdatesWithDownloadInformation();
        _killingObsoleteNecessary = true;
        
        // if we have an update for our machine, prepare the command line
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
            if (LOG.isDebugEnabled())
                LOG.debug("we have an update, but it doesn't need a download.  " +
                    "or all our updates are hopeles. Scheduling URL notification...");
            
            updateInfo.setUpdateCommand(null);
            
            RouterService.schedule(new NotificationFailover(_lastId),
                    delay(clock.now(), uc.getTimestamp()),
                    0);
        } else if (isMyUpdateDownloaded(updateInfo)) {
            LOG.debug("there is an update for me, but I happen to have it on disk");
            RouterService.getCallback().updateAvailable(updateInfo);
        } else
            LOG.debug("we have an update, it needs a download.  Rely on callbacks");
    }
    
    /**
     * replaces tokens in the update command with info about the specific system
     * i.e. <PATH> -> C:\Documents And Settings.... 
     */
    private static void prepareUpdateCommand(UpdateData info) {
        if (info == null || info.getUpdateCommand() == null)
            return;
        
        File path = FileManager.PREFERENCE_SHARE.getAbsoluteFile();
        String name = info.getUpdateFileName();
        
        try {
            path = FileUtils.getCanonicalFile(path);
        }catch (IOException bad) {}

        String command = info.getUpdateCommand();
        command = StringUtils.replace(command,"$",path.getPath()+File.separator);
        command = StringUtils.replace(command,"%",name);
        info.setUpdateCommand(command);
    }

    /**
     * @return if the given update is considered hopeless
     */
    private static boolean isHopeless(DownloadInformation info) {
        return UpdateSettings.FAILED_UPDATES.contains(
                info.getUpdateURN().httpStringValue());
    }
    
    /**
     * Notification that a given ReplyHandler may have an update we can use.
     */
    private void addSourceIfIdMatches(ReplyHandler rh, int version) {
        if(version == _lastId)
            downloadUpdates(_updatesToDownload, rh);
        else if (LOG.isDebugEnabled())
            LOG.debug("Another version? Me: " + version + ", here: " + _lastId);
    }
    
    /**
     * Tries to download updates.
     * @return whether we had any non-hopeless updates.
     */
    private void downloadUpdates(List toDownload, ReplyHandler source) {
        if (toDownload == null)
            toDownload = Collections.EMPTY_LIST;
        
        killObsoleteUpdates(toDownload);
        
        for(Iterator i = toDownload.iterator(); i.hasNext(); ) {
            DownloadInformation next = (DownloadInformation)i.next();
            
            if (isHopeless(next))
                continue; 

            DownloadManager dm = RouterService.getDownloadManager();
            FileManager fm = RouterService.getFileManager();
            if(dm.isGUIInitd() && fm.isLoadFinished()) {
                
                FileDesc shared = fm.getFileDescForUrn(next.getUpdateURN());
                ManagedDownloader md = (ManagedDownloader)dm.getDownloaderForURN(next.getUpdateURN());
                if(LOG.isDebugEnabled())
                    LOG.debug("Looking for: " + next + ", got: " + shared);
                
                if(shared != null && shared.getClass() == FileDesc.class) {
                    // if it's already shared, stop any existing download.
                    if(md != null)
                        md.stop();
                    continue;
                }
                
                // If we don't have an existing download ...
                // and there's no existing InNetwork downloads & 
                // we're allowed to start a new one.
                if(md == null && !dm.hasInNetworkDownload() && canStartDownload()) {
                    LOG.debug("Starting a new InNetwork Download");
                    try {
                        md = (ManagedDownloader)dm.download(next, clock.now());
                    } catch(SaveLocationException sle) {
                        LOG.error("Unable to construct download", sle);
                    }
                }
                
                if(md != null) {
                    if(source != null) 
                        md.addDownload(rfd(source, next), false);
                    else
                        addCurrentDownloadSources(md, next);
                }
            }
        }
    }
    
    /**
     * kills all in-network downloaders whose URNs are not listed in the list of updates.
     * Deletes any files in the folder that are not listed in the update message.
     */
    private void killObsoleteUpdates(List toDownload) {
    	DownloadManager dm = RouterService.getDownloadManager();
    	FileManager fm = RouterService.getFileManager();
    	if (!dm.isGUIInitd() || !fm.isLoadFinished())
    		return;
    	
        if (_killingObsoleteNecessary) {
            _killingObsoleteNecessary = false;
            dm.killDownloadersNotListed(toDownload);
            
            Set urns = new HashSet(toDownload.size());
            for (Iterator iter = toDownload.iterator(); iter.hasNext();) {
				UpdateData data = (UpdateData) iter.next();
				urns.add(data.getUpdateURN());
			}
            
            FileDesc [] shared = fm.getSharedFileDescriptors(FileManager.PREFERENCE_SHARE);
            for (int i = 0; i < shared.length; i++) {
            	if (shared[i].getSHA1Urn() != null &&
            			!urns.contains(shared[i].getSHA1Urn())) {
            		fm.removeFileIfShared(shared[i].getFile());
            		shared[i].getFile().delete();
            	}
			}
        }
    }
    
    /**
     * Adds all current connections that have the right update ID as a source for this download.
     */
    private void addCurrentDownloadSources(ManagedDownloader md, DownloadInformation info) {
        List connections = RouterService.getConnectionManager().getConnections();
        for(Iterator i = connections.iterator(); i.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)i.next();
            if(mc.getRemoteHostUpdateVersion() == _lastId) {
                LOG.debug("Adding source: " + mc);
                md.addDownload(rfd(mc, info), false);
            } else
                LOG.debug("Not adding source because bad id: " + mc.getRemoteHostUpdateVersion() + ", us: " + _lastId);
        }
    }
    
    /**
     * Constructs an RFD out of the given information & connection.
     */
    private RemoteFileDesc rfd(ReplyHandler rh, DownloadInformation info) {
        HashSet urns = new HashSet(1);
        urns.add(info.getUpdateURN());
        return new RemoteFileDesc(rh.getAddress(),               // address
                                  rh.getPort(),                 // port
                                  Integer.MAX_VALUE,            // index (unknown)
                                  info.getUpdateFileName(),     // filename
                                  (int)info.getSize(),          // filesize
                                  rh.getClientGUID(),           // client GUID
                                  0,                            // speed
                                  false,                        // chat capable
                                  2,                            // quality
                                  false,                        // browse hostable
                                  null,                         // xml doc
                                  urns,                         // urns
                                  false,                        // reply to MCast
                                  false,                        // is firewalled
                                  "LIME",                        // vendor
                                  System.currentTimeMillis(),   // timestamp
                                  Collections.EMPTY_SET,        // push proxies
                                  0,                            // creation time
                                  0);                           // firewalled transfer
    }
    
    /**
     * Determines if we're far enough past the timestamp to start a new
     * in network download.
     */
    private boolean canStartDownload() {
        long now = clock.now();
        
        if (LOG.isDebugEnabled())
            LOG.debug("now is "+now+ " next time is "+_nextDownloadTime);
        
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
        
        RouterService.getCallback().updateAvailable(update);
    }
    
    /**
     * @return calculates a random delay after the timestamp, unless the timestamp
     * is more than 3 days in the future.
     */
    private static long delay(long now, long timestamp) {
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
    public void inNetworkDownloadFinished(final URN urn, final boolean good) {
        
        Runnable r = new Runnable() {
            public void run() {
                
                // add it to the list of failed urns
                if (!good)
                    UpdateSettings.FAILED_UPDATES.add(urn.httpStringValue());
                
                UpdateData updateInfo = (UpdateData) _updateInfo;
                if (updateInfo != null && 
                        updateInfo.getUpdateURN() != null &&
                        updateInfo.getUpdateURN().equals(urn)) {
                    if (!good) {
                        // register a notification to the user later on.
                        updateInfo.setUpdateCommand(null);
                        long delay = delay(clock.now(),_lastTimestamp);
                        RouterService.schedule(new NotificationFailover(_lastId),delay,0);
                    } else
                        RouterService.getCallback().updateAvailable(updateInfo);
                }
            }
        };
        
        QUEUE.add(r);
    }
    
    /**
     * @return whether we killed any hopeless update downloads
     */
    private static void killHopelessUpdates(List updates) {
        if (updates == null)
            return;
        
        DownloadManager dm = RouterService.getDownloadManager();
        if (!dm.hasInNetworkDownload())
            return;
        
        long now = clock.now();
        for (Iterator iter = updates.iterator(); iter.hasNext();) {
            DownloadInformation info = (DownloadInformation) iter.next();
            Downloader downloader = dm.getDownloaderForURN(info.getUpdateURN());
            if (downloader != null && downloader instanceof InNetworkDownloader) {
                InNetworkDownloader iDownloader = (InNetworkDownloader)downloader;
                if (isHopeless(iDownloader, now))  
                    iDownloader.stop();
            }
        }
    }
    
    /**
     * @param now what time is it now
     * @return whether the in-network downloader is considered hopeless
     */
    private static boolean isHopeless(InNetworkDownloader downloader, long now) {
        if (now - downloader.getStartTime() < 
                UpdateSettings.UPDATE_GIVEUP_FACTOR.getValue() * 
                UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue())
            return false;
        
        if (downloader.getNumAttempts() < UpdateSettings.UPDATE_MIN_ATTEMPTS.getValue())
            return false;
        
        return true;
    }

    /**
     * @return true if the update for our specific machine is downloaded or
     * there was nothing to download
     */
    private static boolean isMyUpdateDownloaded(UpdateInformation myInfo) {
        FileManager fm = RouterService.getFileManager();
        if (!fm.isLoadFinished())
            return false;
        
        URN myUrn = myInfo.getUpdateURN();
        if (myUrn == null)
            return true;
        
        FileDesc desc = fm.getFileDescForUrn(myUrn);
        
        if (desc == null)
            return false;
        return desc.getClass() == FileDesc.class;
    }
    
    /**
     * Simple accessor for the stored file.
     */
    private File getStoredFile() {
        return new File(CommonUtils.getUserSettingsDir(), FILENAME);
    }
    
    /**
     * Simple accessor for the key file.
     */
    private File getKeyFile() {
        return new File(CommonUtils.getUserSettingsDir(), KEY);
    }
    

    /**
     * a functor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private class Poller implements Runnable {
        public void run() {
            downloadUpdates(_updatesToDownload, null);
            killHopelessUpdates(_updatesToDownload);
            RouterService.schedule( new Runnable() {
                public void run() {
                    QUEUE.add(new Poller());
                }
            },UpdateSettings.UPDATE_RETRY_DELAY.getValue(),0);
        }
    }
    
    private class NotificationFailover implements Runnable {
        private final int id;
        private boolean shown;
        
        NotificationFailover(int id) {
            this.id = id;
        }
        
        public void run() {
            if (shown)
                return;
            
            shown = true;
            notifyAboutInfo(id);
        }
    }
}

/**
 * to be overriden in tests
 */
class Clock {
    public long now() {
        return System.currentTimeMillis();
    }
}
