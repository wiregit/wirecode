package com.limegroup.gnutella.version;



import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.Connectable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.security.SignatureVerifier;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;
import org.limewire.util.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Manager for version updates.
 *
 * Handles queueing new data for parsing and keeping track of which current
 * version is stored in memory & on disk.
 */
@Singleton
public class UpdateHandler implements HttpClientListener {
    
    private static final Log LOG = LogFactory.getLog(UpdateHandler.class);
    
    private static final long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    
    /** If we haven't had new updates in this long, schedule http failover */
    private static final long ONE_MONTH = 10L * THREE_DAYS;
    
    /** URL to get the new version message from */
    private static final String HTTP_FAILOVER = "http://update.limewire.com/version.def";
    
    /**
     * The filename on disk where data is stored.
     */
    private static final String FILENAME = "version.xml";
    
    /**
     * The public key.
     */
    private static final String KEY = "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJC" +
            "SUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2" +
            "NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6" +
            "FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7" +
            "AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25" +
            "NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4" +
            "N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMANJHPNL2" +
            "K3FJIH54PPBPLMCHVEAVTDQQSU3GKB3N2WG7RDC4WSWCM3HACQJ3MNHJ32STPGSZJCTYZRPCHJORQR4HN2" +
            "J4KXHJ6JYYLTIBM64EKRTDBVLTWFJDEIC5SYR24CTHM3H3" +
            "NTBHY4AB26LFPFYMOSK3O4BACF2I4GCRGUPNJS6XGTSNU33APRHI2BJ7ZDJTTU5C4EI6DY";
    
    /**
     * init the random generator on class load time
     */
    private static final Random RANDOM = new Random();
    
    /**
     * means to override the current time for tests
     */
    private static Clock clock = new Clock();
    
    /**
     * The queue that handles all incoming data.
     */
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("UpdateHandler");
    
    /**
     * The most recent update info for this machine.
     */
    private volatile UpdateInformation _updateInfo;
    
    /**
     * A collection of DownloadInformation's that we need to retrieve an update for.
     */
    private volatile List<DownloadInformation> _updatesToDownload;
    
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
    
    /** If an HTTP failover update is in progress */
    private final AtomicBoolean httpUpdate = new AtomicBoolean(false);
    
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<ActivityCallback> activityCallback;
    private final ConnectionServices connectionServices;
    private final Provider<HttpExecutor> httpExecutor;
    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<FileManager> fileManager;
    
    @Inject
    public UpdateHandler(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            Provider<HttpExecutor> httpExecutor,
            Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
            CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<ConnectionManager> connectionManager,
            Provider<DownloadManager> downloadManager,
            Provider<FileManager> fileManager) {
        this.backgroundExecutor = backgroundExecutor;
        this.activityCallback = activityCallback;
        this.connectionServices = connectionServices;
        this.httpExecutor = httpExecutor;
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.connectionManager = connectionManager;
        this.downloadManager = downloadManager;
        this.fileManager = fileManager;
        
        initialize(); // DPINJ: move to an initializer
    }

    /**
     * Initializes data as read from disk.
     */
    private void initialize() {
        LOG.trace("Initializing UpdateHandler");
        QUEUE.execute(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(getStoredFile()), true, null);
            }
        });
        
        // Try to update ourselves (re-use hosts for downloading, etc..)
        // at a specified interval.
        backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                QUEUE.execute(new Poller());
            }
        }, UpdateSettings.UPDATE_RETRY_DELAY.getValue(),  0, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sparks off an attempt to download any pending updates.
     */
    public void tryToDownloadUpdates() {
        QUEUE.execute(new Runnable() {
            public void run() {
                UpdateInformation updateInfo = _updateInfo;
                
                if (updateInfo != null && 
                		updateInfo.getUpdateURN() != null &&
                		isMyUpdateDownloaded(updateInfo))
                    activityCallback.get().updateAvailable(updateInfo);
                
                downloadUpdates(_updatesToDownload, null);
            }
        });
    }
    
    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int version) {
        if(version == _lastId) {
            QUEUE.execute(new Runnable() {
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
    public void handleNewData(final byte[] data, final ReplyHandler handler) {
        if(data != null) {
            QUEUE.execute(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, false, handler);
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
    private void handleDataInternal(byte[] data, boolean fromDisk, ReplyHandler handler) {
        if(data != null) {
            String xml = SignatureVerifier.getVerifiedData(data, KEY, "DSA", "SHA1");
            if(xml != null) {
                if(!fromDisk && handler != null)
                    networkUpdateSanityChecker.get().handleValidResponse(handler, RequestType.VERSION);
                UpdateCollection uc = UpdateCollection.create(xml);
                if (fromDisk || uc.getId() <= _lastId)
                    doHttpFailover(uc);
                if(uc.getId() > _lastId)
                    storeAndUpdate(data, uc, fromDisk);
            } else {
                if(!fromDisk && handler != null)
                    networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.VERSION);
                LOG.warn("Couldn't verify signature on data.");
            }
        } else {
            if(!fromDisk && handler != null)
                networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.VERSION);
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
        UpdateSettings.LAST_UPDATE_TIMESTAMP.setValue(_lastTimestamp);
        
        long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
        long random = Math.abs(RANDOM.nextLong() % delay);
        _nextDownloadTime = _lastTimestamp + random;
        
        _lastBytes = data;
        
        if(!fromDisk) {
            // cancel any http and pretend we just updated.
            httpUpdate.set(false);
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            capabilitiesVMFactory.updateCapabilities();
            connectionManager.get().sendUpdatedCapabilities();
        }

        Version limeV;
        try {
            limeV = new Version(LimeWireUtils.getLimeWireVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid LimeWire version", vfe);
            return;
        }

        Version javaV = null;        
        try {
            javaV = new Version(VersionUtils.getJavaVersion());
        } catch(VersionFormatException vfe) {
            LOG.warn("Invalid java version", vfe);
        }
        
        // don't allow someone to set the style to be above major.
        int style = Math.min(UpdateInformation.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        UpdateData updateInfo = uc.getUpdateDataFor(limeV, 
                    ApplicationSettings.getLanguage(),
                    LimeWireUtils.isPro(),
                    style,
                    javaV);

        List<DownloadInformation> updatesToDownload = uc.getUpdatesWithDownloadInformation();
        _killingObsoleteNecessary = true;
        
        // if we have an update for our machine, prepare the command line
        // and move our update to the front of the list of updates
        if (updateInfo != null && updateInfo.getUpdateURN() != null) {
            prepareUpdateCommand(updateInfo);
            updatesToDownload = new LinkedList<DownloadInformation>(updatesToDownload);
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
            
            backgroundExecutor.scheduleWithFixedDelay(new NotificationFailover(_lastId),
                    delay(clock.now(), uc.getTimestamp()),
                    0, TimeUnit.MILLISECONDS);
        } else if (isMyUpdateDownloaded(updateInfo)) {
            LOG.debug("there is an update for me, but I happen to have it on disk");
            activityCallback.get().updateAvailable(updateInfo);
        } else
            LOG.debug("we have an update, it needs a download.  Rely on callbacks");
    }

    /**
     * begins an http failover.
     */
    private void doHttpFailover(UpdateCollection uc) {
        LOG.debug("checking for http failover");
        long monthAgo = clock.now() - ONE_MONTH;
        if (UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue() < monthAgo && // more than a month ago
                UpdateSettings.LAST_HTTP_FAILOVER.getValue() < monthAgo &&  // and last failover too
                !httpUpdate.getAndSet(true)) { // and we're not already doing a failover
            
            long when = (connectionServices.isConnected() ? 1 : 5 ) * 60 * 1000;
            if (LOG.isDebugEnabled())
                LOG.debug("scheduling http failover in "+when);
            
            backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    launchHTTPUpdate();
                }
            }, when, 0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Launches an http update to the failover url.
     */
    private void launchHTTPUpdate() {
        if (!httpUpdate.get())
            return;
        LOG.debug("about to issue http request method");
        HttpMethod get = new GetMethod(LimeWireUtils.addLWInfoToUrl(HTTP_FAILOVER));
        get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        get.setFollowRedirects(true);
        httpExecutor.get().execute(get,this, 10000);
    }

    public boolean requestComplete(HttpMethod method) {
        httpUpdate.set(false);
        LOG.debug("http request method succeeded");
        
        // remember we made an attempt even if it didn't succeed
        UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
        byte [] inflated = null;
        try {
            if (method.getStatusCode() < 200 || method.getStatusCode() >= 300) 
                throw new IOException("bad code "+method.getStatusCode());

            byte [] resp = method.getResponseBody();
            if (resp == null || resp.length == 0)
                throw new IOException("bad body");

            // inflate the response and process.
            inflated = IOUtils.inflate(resp);
        } catch (IOException failed) {
            LOG.warn("couldn't fetch data ",failed);
            return false;
        } finally {
            httpExecutor.get().releaseResources(method);
        }
        
        handleNewData(inflated, null);
        // no more requests
        return false;
    }
    
    public boolean requestFailed(HttpMethod m, IOException exc) {
        LOG.warn("http failover failed",exc);
        httpUpdate.set(false);
        httpExecutor.get().releaseResources(m);
        // nothing we can do.
        return false;
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
    private void downloadUpdates(List<? extends DownloadInformation> toDownload, ReplyHandler source) {
        if (toDownload == null)
            toDownload = Collections.emptyList();
        
        killObsoleteUpdates(toDownload);
        
        for(DownloadInformation next : toDownload) {
            if (isHopeless(next))
                continue; 
            
            if(downloadManager.get().isGUIInitd() && fileManager.get().isLoadFinished()) {
                
                FileDesc shared = fileManager.get().getFileDescForUrn(next.getUpdateURN());
                //TODO: remove the cast
                ManagedDownloader md = (ManagedDownloader)downloadManager.get().getDownloaderForURN(next.getUpdateURN());
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
                if(md == null && !downloadManager.get().hasInNetworkDownload() && canStartDownload()) {
                    LOG.debug("Starting a new InNetwork Download");
                    try {
                        md = (ManagedDownloader)downloadManager.get().download(next, clock.now());
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
    private void killObsoleteUpdates(List<? extends DownloadInformation> toDownload) {
    	if (!downloadManager.get().isGUIInitd() || !fileManager.get().isLoadFinished())
    		return;
    	
        if (_killingObsoleteNecessary) {
            _killingObsoleteNecessary = false;
            downloadManager.get().killDownloadersNotListed(toDownload);
            
            Set<URN> urns = new HashSet<URN>(toDownload.size());
            for(DownloadInformation data : toDownload)
                urns.add(data.getUpdateURN());
            
            FileDesc [] shared = fileManager.get().getSharedFileDescriptors(FileManager.PREFERENCE_SHARE);
            for (int i = 0; i < shared.length; i++) {
            	if (shared[i].getSHA1Urn() != null &&
            			!urns.contains(shared[i].getSHA1Urn())) {
            	    fileManager.get().removeFileIfShared(shared[i].getFile());
            		shared[i].getFile().delete();
            	}
			}
        }
    }
    
    /**
     * Adds all current connections that have the right update ID as a source for this download.
     */
    private void addCurrentDownloadSources(ManagedDownloader md, DownloadInformation info) {
        for(ManagedConnection mc : connectionManager.get().getConnections()) {
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
        Set<URN> urns = new UrnSet(info.getUpdateURN());
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
                                  IpPort.EMPTY_SET,             // push proxies
                                  0,                            // creation time
                                  0,                            //  firewalled transfer
                               rh instanceof Connectable ? 
                                  ((Connectable)rh).isTLSCapable() : false );  // tls capability
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
        assert(update != null);
        
        activityCallback.get().updateAvailable(update);
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
                        backgroundExecutor.scheduleWithFixedDelay(new NotificationFailover(_lastId),delay,0, TimeUnit.MILLISECONDS);
                    } else
                        activityCallback.get().updateAvailable(updateInfo);
                }
            }
        };
        
        QUEUE.execute(r);
    }
    
    /**
     * @return whether we killed any hopeless update downloads
     */
    private void killHopelessUpdates(List<? extends DownloadInformation> updates) {
        if (updates == null)
            return;
        
        if (!downloadManager.get().hasInNetworkDownload())
            return;
        
        long now = clock.now();
        for(DownloadInformation info : updates) {
            Downloader downloader = downloadManager.get().getDownloaderForURN(info.getUpdateURN());
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
    private boolean isHopeless(InNetworkDownloader downloader, long now) {
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
    private boolean isMyUpdateDownloaded(UpdateInformation myInfo) {
        if (!fileManager.get().isLoadFinished())
            return false;
        
        URN myUrn = myInfo.getUpdateURN();
        if (myUrn == null)
            return true;
        
        FileDesc desc = fileManager.get().getFileDescForUrn(myUrn);
        
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
     * a functor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private class Poller implements Runnable {
        public void run() {
            downloadUpdates(_updatesToDownload, null);
            killHopelessUpdates(_updatesToDownload);
            backgroundExecutor.scheduleWithFixedDelay( new Runnable() {
                public void run() {
                    QUEUE.execute(new Poller());
                }
            },UpdateSettings.UPDATE_RETRY_DELAY.getValue(),0, TimeUnit.MILLISECONDS);
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
