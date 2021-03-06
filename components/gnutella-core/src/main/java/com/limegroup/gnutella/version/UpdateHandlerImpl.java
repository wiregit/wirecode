package com.limegroup.gnutella.version;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.activation.api.ActivationManager;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;
import org.limewire.util.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryStatusEvent;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.security.Certificate;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertifiedMessageSourceType;
import com.limegroup.gnutella.security.CertifiedMessageVerifier;
import com.limegroup.gnutella.security.DefaultSignedMessageDataProvider;
import com.limegroup.gnutella.security.CertifiedMessageVerifier.CertifiedMessage;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Manager for version updates.
 * <p>
 * Handles queueing new data for parsing and keeping track of which current
 * version is stored in memory & on disk.
 */
@EagerSingleton
public class UpdateHandlerImpl implements UpdateHandler, EventListener<LibraryStatusEvent>, Service {
    
    private static final Log LOG = LogFactory.getLog(UpdateHandlerImpl.class);
    
    private static final long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    
    /** If we haven't had new updates in this long, schedule HTTP failover */
    private static final long ONE_MONTH = 10L * THREE_DAYS;
    
    /**
     * The filename on disk where data is stored.
     */
    private static final String FILENAME = "version.xml";
    
    // Package access for testing
    protected static final int IGNORE_ID = Certificate.IGNORE_ID;
    
    /**
     * init the random generator on class load time
     */
    private static final Random RANDOM = new Random();
    
    /**
     * means to override the current time for tests
     */
    private final Clock clock;
    
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
    private volatile int newVersion;
    
    /**
     * The bytes to send on the wire.
     */
     /* TODO: Don't store in memory.
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
    private final HttpRequestControl httpRequestControl = new HttpRequestControl();
    
    private final ScheduledExecutorService backgroundExecutor;
    private final ConnectionServices connectionServices;
    private final Provider<HttpExecutor> httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<DownloadManager> downloadManager;
    private final Library library;
    private final FileView gnutellaFileView;
    private final UpdateCollectionFactory updateCollectionFactory;
    private final UpdateMessageVerifier updateMessageVerifier;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final EventListenerList<UpdateEvent> listeners;
    private final ActivationManager activationManager;
    
    /**
     * If the key used by {@link UpdateMessageVerifier} is leaked, but not the master
     * key used by {@link CertificateVerifier}, the urls that would have to serve
     * the final update message are the same as below, except for v3 has to be
     * replaced with v2.
     * <p>
     * If the master key used by {@link CertificateVerifier} is leaked, the urls
     * below will have to serve. 
     */
    private volatile String timeoutUpdateLocation = "http://update0.limewire.com/v3/update.def";
    private volatile List<String> maxedUpdateList = Arrays.asList("http://update1.limewire.com/v3/update.def",
            "http://update2.limewire.com/v3/update.def",
            "http://update3.limewire.com/v3/update.def",
            "http://update4.limewire.com/v3/update.def",
            "http://update5.limewire.com/v3/update.def",
            "http://update6.limewire.com/v3/update.def",
            "http://update7.limewire.com/v3/update.def",
            "http://update8.limewire.com/v3/update.def",
            "http://update9.limewire.com/v3/update.def",
            "http://update10.limewire.com/v3/update.def");
    private volatile int minMaxHttpRequestDelay = 1000 * 60;
    private volatile int maxMaxHttpRequestDelay = 1000 * 60 * 30;
    private volatile int silentPeriodForMaxHttpRequest = 1000 * 60 * 5;
    
    private volatile UpdateCollection updateCollection;

    private final HttpClientInstanceUtils httpClientInstanceUtils;

    private final CertificateProvider certificateProvider;

    private final CertifiedMessageVerifier certifiedMessageVerifier;

    private final DefaultSignedMessageDataProvider updateDataProvider;
    
    @Inject
    UpdateHandlerImpl(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ConnectionServices connectionServices,
            Provider<HttpExecutor> httpExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<ConnectionManager> connectionManager,
            Provider<DownloadManager> downloadManager,
            UpdateCollectionFactory updateCollectionFactory,
            Clock clock,
            UpdateMessageVerifier updateMessageVerifier, 
            RemoteFileDescFactory remoteFileDescFactory,
            @GnutellaFiles FileView gnutellaFileView,
            Library library, ActivationManager activationManager,
            HttpClientInstanceUtils httpClientInstanceUtils,
            @Update CertificateProvider certificateProvider,
            @Update CertifiedMessageVerifier certifiedMessageVerifier,
            @Update DefaultSignedMessageDataProvider updateDataProvider) {
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.connectionManager = connectionManager;
        this.downloadManager = downloadManager;
        this.library = library;
        this.updateCollectionFactory = updateCollectionFactory;
        this.clock = clock;
        this.updateMessageVerifier = updateMessageVerifier;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.gnutellaFileView = gnutellaFileView;
        this.activationManager = activationManager;
        this.httpClientInstanceUtils = httpClientInstanceUtils;
        this.certificateProvider = certificateProvider;
        this.certifiedMessageVerifier = certifiedMessageVerifier;
        this.updateDataProvider = updateDataProvider;
        
        this.listeners = new EventListenerList<UpdateEvent>();
    }
    
    @Inject
    void register(ListenerSupport<LibraryStatusEvent> listener) {
        listener.addListener(this);
    }
        
    String getTimeoutUrl() {
        return timeoutUpdateLocation;
    }
    
    List<String> getMaxUrls() {
        return maxedUpdateList;
    }
    
    void setMaxUrls(List<String> urls) {
        this.maxedUpdateList = urls;
    }
    
    void setSilentPeriodForMaxHttpRequest(int silentPeriodForMaxHttpRequest) {
        this.silentPeriodForMaxHttpRequest = silentPeriodForMaxHttpRequest;
    }

    /**
     * Initializes data as read from disk.
     */
    public void start() {
        LOG.trace("Initializing UpdateHandler");
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(getStoredFile()), CertifiedMessageSourceType.FROM_DISK, null);
                handleDataInternal(updateDataProvider.getDefaultSignedMessageData(), CertifiedMessageSourceType.FROM_DISK, null);
            }
        });
        
        // Try to update ourselves (re-use hosts for downloading, etc..)
        // at a specified interval.
        backgroundExecutor.schedule(new Poller(), UpdateSettings.UPDATE_RETRY_DELAY.getValue(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sparks off an attempt to down load any pending updates.
     */
    private void tryToDownloadUpdates() {
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                UpdateInformation updateInfo = _updateInfo;
                
                if (updateInfo != null && 
                		updateInfo.getUpdateURN() != null &&
                		isMyUpdateDownloaded(updateInfo)) {
                    fireUpdate(updateInfo);
                }
                downloadUpdates(_updatesToDownload, null);
            }
        });
    }
    
    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int newVersion) {
        if(newVersion == this.newVersion) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    addSourceIfIdMatches(rh, newVersion);
                }
            });
        } else if(LOG.isDebugEnabled())
            LOG.debug("Another version from rh: " + rh + ", them: " + newVersion + ", me: " + this.newVersion);
    }
    
    /**
     * Notification that a new message has arrived.
     * <p>
     * (The actual processing is passed of to be run in a different thread.
     *  All notifications are processed in the same thread, sequentially.)
     */
    public void handleNewData(final byte[] data, final ReplyHandler handler) {
        LOG.debug("handling new network data");
        if(data != null) {
            backgroundExecutor.execute(new NetworkDataRunnable(data, handler));
        }
    }
    
    /**
     * Package private and explicit so test code can check for it
     */
    class NetworkDataRunnable implements Runnable {
        
        private final byte[] data;
        private final ReplyHandler handler;

        public NetworkDataRunnable(byte[] data, ReplyHandler handler) {
            this.data = data;
            this.handler = handler;
        }

        @Override
        public void run() {
            LOG.trace("Parsing new data...");
            handleDataInternal(data, CertifiedMessageSourceType.FROM_NETWORK, handler);
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
     * Handles processing a newly arrived message. Package access for testing.
     * <p>
     * (Processes the data immediately.)
     */
    protected void handleDataInternal(byte[] data, CertifiedMessageSourceType updateType, ReplyHandler handler) {
        if (data == null) {
            LOG.warn("No data to handle.");
            return;
        }

        String xml = updateMessageVerifier.getVerifiedData(data);
        if (xml == null) {
            LOG.warn("Couldn't verify signature on data.");
            return;
        }
        
        UpdateCollection uc = null;
        try {
            uc = updateCollectionFactory.createUpdateCollection(xml);
        } catch (InvalidDataException e) {
            LOG.debug("invalid update data", e);
            return;
        }
        
        CertifiedMessage certifiedMessage = uc.getCertifiedMessage();
        Certificate certificate = null;
        try {
            certificate = certifiedMessageVerifier.verify(certifiedMessage, handler);
        } catch (SignatureException se) {
            LOG.error("message did not verify", se);
            return;
        }
                
        if (LOG.isDebugEnabled())
            LOG.debug("Got a collection with id: " + uc.getNewVersion() + ", from " + updateType + ".  Current id is: " + this.newVersion);


        int networkKeyVersion = certificate.getKeyVersion();
        int localKeyVersion = getKeyVersion();
        int networkNewVersion = uc.getNewVersion();
        switch (updateType) {
        case FROM_NETWORK:
            if (localKeyVersion == IGNORE_ID) {
                break;
            }
            // if key version is higher than the local one
            if (networkKeyVersion > localKeyVersion) {
                if (networkKeyVersion == IGNORE_ID) {
                    doHttpMaxFailover(uc);
                } else {
                    storeAndUpdate(data, uc, updateType, certificate);
                }
            } else if(networkKeyVersion == localKeyVersion && networkNewVersion > newVersion) {
                // if key versions are the same, but new version is higher than the local one
                storeAndUpdate(data, uc, updateType, certificate);            
            } else { // update is not accepted, check for stale
                checkForStaleUpdateAndMaybeDoHttpFailover();
                addSourceIfIdMatches(handler, networkNewVersion);
            }
            break;
        case FROM_DISK:
            // on first load:
            // a) always check for stale
            // b) update if we didn't get an update before this ran.
            checkForStaleUpdateAndMaybeDoHttpFailover();
            // if key version is higher, or
            // if key versions are the same, but new version is higher
            if ((networkKeyVersion > localKeyVersion) || 
                    (networkKeyVersion == localKeyVersion && networkNewVersion > newVersion)){                
                storeAndUpdate(data, uc, updateType, certificate);
            }
            break;
        case FROM_HTTP:
            // on HTTP response:
            // a) update if >= stored.
            // (note this is >=, different than >, which is from network)
            if (networkKeyVersion > localKeyVersion || (networkKeyVersion == localKeyVersion && networkNewVersion >= newVersion)){
                storeAndUpdate(data, uc, updateType, certificate);
            }
            break;
        }
    }
    
    /**
     * Stores the given data to disk & posts an update to neighboring
     * connections. Starts the download of any updates
     */
    private void storeAndUpdate(byte[] data, UpdateCollection uc, CertifiedMessageSourceType updateType, Certificate certificate) {
        if(LOG.isTraceEnabled())
            LOG.trace("Retrieved new data from: " + updateType + ", storing & updating.");
        if(uc.getId() == IGNORE_ID && updateType == CertifiedMessageSourceType.FROM_NETWORK)
            throw new IllegalStateException("shouldn't be here!");
        
        // If an http max request is pending, don't even bother with this stuff.
        // We want to get it straight from the source...
        if (updateType == CertifiedMessageSourceType.FROM_NETWORK && httpRequestControl.isRequestPending()
                && httpRequestControl.getRequestReason() == HttpRequestControl.RequestReason.MAX)
            return;
        
        _lastId = uc.getId();
        newVersion = uc.getNewVersion();
        updateCollection = uc;
        certificateProvider.set(certificate);
        
        _lastTimestamp = uc.getTimestamp();
        UpdateSettings.LAST_UPDATE_TIMESTAMP.setValue(_lastTimestamp);
        
        long delay = UpdateSettings.UPDATE_DOWNLOAD_DELAY.getValue();
        long random = Math.abs(RANDOM.nextLong() % delay);
        _nextDownloadTime = _lastTimestamp + random;
        
        _lastBytes = data;
        
        if(updateType != CertifiedMessageSourceType.FROM_DISK) {
            // cancel any http and pretend we just updated.
            if(httpRequestControl.getRequestReason() == HttpRequestControl.RequestReason.TIMEOUT)
                httpRequestControl.cancelRequest();
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
        int style = Math.min(UpdateStyle.STYLE_MAJOR,
                             UpdateSettings.UPDATE_STYLE.getValue());
        
        UpdateData updateInfo = uc.getUpdateDataFor(limeV, 
                    ApplicationSettings.getLanguage(),
                    activationManager.isProActive(),
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
            
            backgroundExecutor.schedule(new NotificationFailover(newVersion),
                    delay(clock.now(), uc.getTimestamp()),
                    TimeUnit.MILLISECONDS);
        } else if (isMyUpdateDownloaded(updateInfo)) {
            LOG.debug("there is an update for me, but I happen to have it on disk");
            fireUpdate(updateInfo);
        } else
            LOG.debug("we have an update, it needs a download.  Rely on callbacks");
    }

    /**
     * Begins an HTTP failover.
     */
    private void checkForStaleUpdateAndMaybeDoHttpFailover() {
        LOG.debug("checking for timeout http failover");
        long monthAgo = clock.now() - ONE_MONTH;
        if (UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue() < monthAgo && // more than a month ago
                UpdateSettings.LAST_HTTP_FAILOVER.getValue() < monthAgo &&  // and last failover too
                !httpRequestControl.requestQueued(HttpRequestControl.RequestReason.TIMEOUT)) { // and we're not already doing a failover
            
            long when = (connectionServices.isConnected() ? 1 : 5 ) * 60 * 1000;
            if (LOG.isDebugEnabled())
                LOG.debug("scheduling http failover in "+when);
            
            backgroundExecutor.schedule(new StaleHttpUpdateRunnable(), when, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Package private and explicit so test code can check for it
     */
    class StaleHttpUpdateRunnable implements Runnable {

        @Override
        public void run() {
            try {
                launchHTTPUpdate(timeoutUpdateLocation);
            } catch (URISyntaxException e) {
                httpRequestControl.requestFinished();
                httpRequestControl.cancelRequest();
                LOG.warn(e.toString(), e);
            }
        }
    }
    
    private void doHttpMaxFailover(UpdateCollection updateCollection) {
        long maxTimeAgo = clock.now() - silentPeriodForMaxHttpRequest; 
        if(!httpRequestControl.requestQueued(HttpRequestControl.RequestReason.MAX) &&
                UpdateSettings.LAST_HTTP_FAILOVER.getValue() < maxTimeAgo) {
            LOG.debug("Scheduling http max failover...");
            backgroundExecutor.schedule(new HttpMaxFailOverRunnable(), RANDOM.nextInt(maxMaxHttpRequestDelay) + minMaxHttpRequestDelay, TimeUnit.MILLISECONDS);
        } else {
            LOG.debug("Ignoring http max failover.");
        }
    }
    
    /**
     * Package private and explicit so test code can check for it.
     */
    class HttpMaxFailOverRunnable implements Runnable {

        @Override
        public void run() {
            String url = maxedUpdateList.get(RANDOM.nextInt(maxedUpdateList.size()));
            try {
                launchHTTPUpdate(url);
            } catch (URISyntaxException e) {
                httpRequestControl.requestFinished();
                httpRequestControl.cancelRequest();
                LOG.warn(e.toString(), e);
            }
        }
        
    }

    /**
     * Launches an HTTP update to the failover url.
     */
    private void launchHTTPUpdate(String url) throws URISyntaxException {
        if (!httpRequestControl.isRequestPending())
            return;
        LOG.debug("about to issue http request method");
        HttpGet get = new HttpGet(httpClientInstanceUtils.addClientInfoToUrl(url));
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        httpRequestControl.requestActive();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(get, params, new RequestHandler());
    }
    
    /**
     * Replaces tokens in the update command with info about the specific system,
     * i.e. <PATH> -> C:\Documents And Settings.... 
     */
    private static void prepareUpdateCommand(UpdateData info) {
        if (info == null || info.getUpdateCommand() == null)
            return;
        
        File path = LibraryUtils.PREFERENCE_SHARE.getAbsoluteFile();
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
        if(version == this.newVersion)
            downloadUpdates(_updatesToDownload, rh);
        else if (LOG.isDebugEnabled())
            LOG.debug("Another version? Me: " + version + ", here: " + this.newVersion);
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
            
            if(downloadManager.get().isSavedDownloadsLoaded() && library.isLoadFinished()) {
                
                //TODO: remove the cast
                ManagedDownloader md = (ManagedDownloader)downloadManager.get().getDownloaderForURN(next.getUpdateURN());
                
                // Skip to the next one since we already have a complete file.
                if(hasCompleteFile(next.getUpdateURN())) {
                    if(md != null) {
                        md.stop();
                    }
                    continue;
                }
                
                // If we don't have an existing download ...
                // and there's no existing InNetwork downloads & 
                // no existing Store downloads & 
                // we're allowed to start a new one.
                if(md == null && !downloadManager.get().hasInNetworkDownload() && canStartDownload()) {
                    LOG.debug("Starting a new InNetwork Download");
                    try {
                        md = (ManagedDownloader)downloadManager.get().download(next, clock.now());
                    } catch(DownloadException e) {
                        LOG.error("Unable to construct download", e);
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
     * Kills all in-network downloaders whose URNs are not listed in the list of updates.
     * Deletes any files in the folder that are not listed in the update message.
     */
    private void killObsoleteUpdates(List<? extends DownloadInformation> toDownload) {
        if (!downloadManager.get().isSavedDownloadsLoaded() || !library.isLoadFinished())
            return;

        if (_killingObsoleteNecessary) {
            _killingObsoleteNecessary = false;
            downloadManager.get().killDownloadersNotListed(toDownload);
            
            Set<URN> urns = new HashSet<URN>(toDownload.size());
            for(DownloadInformation data : toDownload)
                urns.add(data.getUpdateURN());
            
            List<FileDesc> shared = gnutellaFileView.getFilesInDirectory(LibraryUtils.PREFERENCE_SHARE);
            for (FileDesc fd : shared) {
                if (fd.getSHA1Urn() != null && !urns.contains(fd.getSHA1Urn())) {
                    library.remove(fd.getFile());
                    fd.getFile().delete();
                }
            }
        }
    }
    
    /**
     * Adds all current connections that have the right update ID as a source for this download.
     */
    private void addCurrentDownloadSources(ManagedDownloader md, DownloadInformation info) {
        for(RoutedConnection mc : connectionManager.get().getConnections()) {
            if(mc.getConnectionCapabilities().getRemoteHostNewUpdateVersion() == newVersion) {
                LOG.debug("Adding source: " + mc);
                md.addDownload(rfd(mc, info), false);
            } else
                LOG.debug("Not adding source because bad id: " + mc.getConnectionCapabilities().getRemoteHostNewUpdateVersion() + ", us: " + newVersion);
        }
    }
    
    /**
     * Constructs an RFD out of the given information & connection.
     */
    private RemoteFileDesc rfd(ReplyHandler rh, DownloadInformation info) {
        Set<URN> urns = new UrnSet(info.getUpdateURN());
        return remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl(rh.getInetSocketAddress(), rh instanceof Connectable ? ((Connectable)rh).isTLSCapable() : false), Integer.MAX_VALUE,
                info.getUpdateFileName(), info.getSize(), rh.getClientGUID(), 0, 2, false, null, urns, false,
                "LIME", -1);
                        
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
        if (id != newVersion)
            return;
        
        UpdateInformation update = _updateInfo;
        assert(update != null);
        fireUpdate(update);
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
                     "\nNow    : " + now + " (" + new Date(now) + ")" + 
                     "\nStamp  : " + timestamp + " (" + new Date(timestamp) + ")" +  
                     "\nDelay  : " + delay + " (" + CommonUtils.seconds2time(delay/1000) + ")" + 
                     "\nRandom : " + random + " (" + CommonUtils.seconds2time(random/1000) + ")" +  
                     "\nThen   : " + then + " (" + new Date(then) + ")" + 
                     "\nDiff   : " + (then-now) + " (" + CommonUtils.seconds2time((then-now)/1000) + ")"); 
        }

        return Math.max(0,then - now);
    }
    
    /**
     * Notifies this that an update with the given URN has finished downloading.
     * <p>
     * If this was our update, we notify the GUI.  It's OK if the user restarts
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
                        backgroundExecutor.schedule(new NotificationFailover(newVersion),delay,TimeUnit.MILLISECONDS);
                    } else {
                        fireUpdate(updateInfo);
                        connectionManager.get().sendUpdatedCapabilities();
                    }
                }
            }
        };
        
        backgroundExecutor.execute(r);
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
        
        if (downloader.getDownloadAttempts() < UpdateSettings.UPDATE_MIN_ATTEMPTS.getValue())
            return false;
        
        return true;
    }

    /**
     * @return true if the update for our specific machine is downloaded or
     * there was nothing to download
     */
    private boolean isMyUpdateDownloaded(UpdateInformation myInfo) {
        if (!library.isLoadFinished())
            return false;
        
        URN myUrn = myInfo.getUpdateURN();
        if (myUrn == null)
            return true;
        
        return hasCompleteFile(myUrn);
    }
    
    private boolean hasCompleteFile(URN urn) {
        List<FileDesc> fds = library.getFileDescsMatching(urn);
        for(FileDesc fd : fds) {
            if(!(fd instanceof IncompleteFileDesc)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Simple accessor for the stored file.
     */
    private File getStoredFile() {
        return new File(CommonUtils.getUserSettingsDir(), FILENAME);
    }
    
    /**
     * A functor that repeatedly tries to download updates at a variable
     * interval. 
     */
    private class Poller implements Runnable {
        public void run() {
            downloadUpdates(_updatesToDownload, null);
            killHopelessUpdates(_updatesToDownload);
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

    private class RequestHandler implements HttpClientListener {

        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            LOG.debug("http request method succeeded");
            
            // remember we made an attempt even if it didn't succeed
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            final byte[] inflated;
            try {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    throw new IOException("bad code "+response.getStatusLine().getStatusCode());
    
                byte [] resp = null;
                if(response.getEntity() != null) {
                    resp = IOUtils.readFully(response.getEntity().getContent());
                }
                if (resp == null || resp.length == 0)
                    throw new IOException("bad body");
    
                // inflate the response and process.
                inflated = updateMessageVerifier.inflateNetworkData(resp);
            } catch (IOException failed) {
                httpRequestControl.requestFinished();
                LOG.warn("couldn't fetch data ",failed);
                return false;
            } finally {
                httpExecutor.get().releaseResources(response);
            }
            
            // Handle the data in the background thread.
            backgroundExecutor.execute(new RequestHandlerDataRunnable(inflated));
            
            return false; // no more requests
        }
        
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.warn("http failover failed",exc);
            httpRequestControl.requestFinished();
            UpdateSettings.LAST_HTTP_FAILOVER.setValue(clock.now());
            
            httpExecutor.get().releaseResources(response);
            // nothing we can do.
            return false;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }
    }
    
    /**
     * Package private and explicit so test code can check for it.
     */
    class RequestHandlerDataRunnable implements Runnable {

        private final byte[] data;

        public RequestHandlerDataRunnable(byte[] data) {
            this.data = data;
        }
        
        @Override
        public void run() {
            httpRequestControl.requestFinished();
            LOG.trace("Parsing new data...");
            handleDataInternal(data, CertifiedMessageSourceType.FROM_HTTP, null);
        }
        
    }
    
    /**
     * A simple control to let the flow of HTTP requests happen differently
     * depending on why it was requested.
     */
    private static class HttpRequestControl {
        private static enum RequestReason { TIMEOUT, MAX };
        
        private final AtomicBoolean requestQueued = new AtomicBoolean(false);
        private final AtomicBoolean requestActive = new AtomicBoolean(false);
        private volatile RequestReason requestReason;
        
        /** Returns true if a request is queued or active. */
        boolean isRequestPending() {
            return requestActive.get() || requestQueued.get();
        }
        
        /** Sets a queued request and returns true if a request is pending or active. */
        boolean requestQueued(RequestReason reason) {
            boolean prior = requestQueued.getAndSet(true);
            if(!prior || reason == RequestReason.MAX) // upgrade reason
                requestReason = reason;
            return prior || requestActive.get();
        }
        
        /** Sets a request to be active. */
        void requestActive() {
            requestActive.set(true);
            requestQueued.set(false);
        }
        
        /** Returns the reason the last request was queueud. */
        RequestReason getRequestReason() {
            return requestReason;
        }
        
        void cancelRequest() {
            requestQueued.set(false);
        }
        
        void requestFinished() {
            requestActive.set(false);
        }
    }
        
    public String getServiceName() {
        return I18nMarker.marktr("Update Checks");
    }
    
    public void initialize() {
    }
    
    public void stop() {
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    /**
     * Listens for events from FileManager.
     */
    @Override
    public void handleEvent(LibraryStatusEvent evt) {
        if(evt.getType() == LibraryStatusEvent.Type.LOAD_COMPLETE) {
            tryToDownloadUpdates();
        }
    }

    @Override
    public UpdateCollection getUpdateCollection() {
        return updateCollection;
    }
    
    private void fireUpdate(UpdateInformation update) {
        listeners.broadcast(new UpdateEvent(update, UpdateEvent.Type.UPDATE));
    }
    
    @Override
    public void addListener(EventListener<UpdateEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<UpdateEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    public int getKeyVersion() {
        return certificateProvider.get().getKeyVersion();
    }

    @Override
    public byte[] getOldUpdateResponse() {
        return updateDataProvider.getDisabledKeysSignedMessageData();
    }

    @Override
    public int getNewVersion() {
        return newVersion;
    }

    /**
     * Old clients won't send us neither newVersion nor keyVersion, so their
     * values will be -1. If we get a capabilities update from a new client, we
     * will only look at newVersion and keyVersion and ignore the old version
     * field completely. This is the first if branch. In that case we request an
     * update message, if its newVersion number is greater and the key version is the
     * same as the current keyVersion.
     * <p>
     * If an old client is sending us a capabilities update, we are in the
     * second if branch, where we check that the advertised version is higher
     * than the currently known one.
     * <p>
     * If none of the two cases above was the case, it could be that a newer key
     * version is advertised and we should download the update regardless of its
     * version or its newVersion. That's the last if branch.
     */
    @Override
    public boolean shouldRequestUpdateMessage(int version, int newVersion, int keyVersion) {
        if (LOG.isDebugEnabled())
            LOG.debugf("version {0}, new version {1}, key version {2}", version, newVersion, keyVersion);
        if (newVersion != -1) {
            if (newVersion > getNewVersion() && keyVersion == getKeyVersion()) {
                return true;
            }
        } else if (version > getLatestId()) {
            return true;
        }
        if (getKeyVersion() > 3 && keyVersion > getKeyVersion()) {
            return true;
        }
        return false;
    }

}
