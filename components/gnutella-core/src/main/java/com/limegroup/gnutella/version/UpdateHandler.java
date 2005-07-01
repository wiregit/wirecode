package com.limegroup.gnutella.version;


import java.io.File;
import java.util.Random;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Collections;

import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
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
     * LOCKING: this
     */
    private long _lastTimestamp;
    
    /**
     * The next time we can make an attempt to download a pushed file.
     * LOCKING: this 
     */
    private long _nextDownloadTime;
    
    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        LOG.trace("Initializing UpdateHandler");
        handleDataInternal(FileUtils.readFileFully(getStoredFile()), true);
        // Try to update ourselves (re-use hosts for downloading, etc..)
        // at a reasonable interval.
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
        QUEUE.add(new Updater());
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
     * This will only return information if it was read from disk on startup
     * and did not have a delay.
     */
    public UpdateInformation getLatestUpdateInfo() {
        return _updateInfo;
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
     */
    private void storeAndUpdate(byte[] data, UpdateCollection uc, boolean fromDisk) {
        LOG.trace("Retrieved new data, storing & updating.");
        _lastId = uc.getId();
        
        synchronized(this) {
            _lastTimestamp = uc.getTimestamp();
            _nextDownloadTime = 0;
        }
        
        _lastBytes = data;
        
        _updatesToDownload = uc.getUpdatesWithDownloadInformation();
        downloadUpdates(_updatesToDownload, null);
        
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
        
        UpdateInformation info = uc.getUpdateDataFor(limeV, 
                                                     ApplicationSettings.getLanguage(),
                                                     CommonUtils.isPro(),
                                                     style,
                                                     javaV);

        notifyAboutInfo(uc, info, fromDisk);
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
     */
    private void downloadUpdates(List toDownload, ReplyHandler source) {
        if(toDownload != null) {
            for(Iterator i = toDownload.iterator(); i.hasNext(); ) {
                DownloadInformation next = (DownloadInformation)i.next();
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
                            md = (ManagedDownloader)dm.download(next);
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
                                  "ALT",                        // vendor
                                  System.currentTimeMillis(),   // timestamp
                                  Collections.EMPTY_SET,        // push proxies
                                  0,                            // creation time
                                  0);                           // firewalled transfer
    }
    
    /**
     * Determines if we're far enough past the timestamp to start a new
     * in network download.
     */
    private synchronized boolean canStartDownload() {
        long now = System.currentTimeMillis();
        if (_nextDownloadTime == 0) {
            long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
            long random = Math.abs(RANDOM.nextLong() % delay);
            _nextDownloadTime = _lastTimestamp + random;
        }
        return now < _nextDownloadTime;
    }
    
    /**
     * Determines if we should notify about there being new information.
     */
    private void notifyAboutInfo(UpdateCollection uc, final UpdateInformation update, boolean fromDisk) {
        if(update == null) {
            LOG.warn("No relevant update info to notify about.");
            return;
        }
        
        long now = System.currentTimeMillis();
        long delay = UpdateSettings.UPDATE_DELAY.getValue();
        long random = Math.abs(new Random().nextLong() % delay);
        long timestamp = uc.getTimestamp();
        long then = timestamp + random;
        long threeDays = 1000 * 60 * 60 * 24 * 3;
        final int id = uc.getId();
        // If now is before the time we can show it, 
        // or the time on the computer is hopelessly off, 
        // that being three days before the timestamp, where the timestamp
        // is supposed to be the current time of publishing, then delay. 
        if(now < then && !(fromDisk && now+threeDays < timestamp)) {
            if(LOG.isInfoEnabled())
                LOG.info("Delaying Update." +
                         "\nNow    : " + now + 
                         "\nStamp  : " + timestamp +
                         "\nDelay  : " + delay + 
                         "\nRandom : " + random + 
                         "\nThen   : " + then +
                         "\nDiff   : " + (then-now));

            RouterService.schedule(new Runnable() {
                public void run() {
                    // only run if the ids weren't updated while we waited.
                    if(id == _lastId)
                        RouterService.getCallback().updateAvailable(update);
                }
            }, then - now, 0);
        } else {
            // If this was from the disk, store it for the GUI to pick up later.
            if(fromDisk)
                _updateInfo = update;
            // Otherwise, it came while we were running -- send it off to the GUI.
            else
                RouterService.getCallback().updateAvailable(update);
        }
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
     * a functor that tries to download updates once.
     */
    private class Updater implements Runnable {
        public void run() {
            downloadUpdates(_updatesToDownload, null);
        }
    }

    /**
     * a functor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private class Poller extends Updater {
        public void run() {
            super.run();
            RouterService.schedule( new Runnable() {
                public void run() {
                    QUEUE.add(new Poller());
                }
            },UpdateSettings.UPDATE_RETRY_DELAY.getValue(),0);
        }
    }
    
}