package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.limewire.core.settings.UpdateSettings;
import org.limewire.io.IOUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.http.httpclient.HttpClientInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.security.Certificate;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertifiedMessageSourceType;
import com.limegroup.gnutella.security.CertifiedMessageVerifier;
import com.limegroup.gnutella.security.DefaultDataProvider;
import com.limegroup.gnutella.security.CertifiedMessageVerifier.CertifiedMessage;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Used for managing signed messages published by LimeWire, and changing settings
 * as necessary.
 */
@Singleton
public class SimppManagerImpl implements SimppManager {
    
    private static final Log LOG = LogFactory.getLog(SimppManagerImpl.class);
    
    private static int MIN_VERSION = 3;
   
    private static final String FILENAME = "simpp.xml";    
    private static final Random RANDOM = new Random();
    private static final int IGNORE_ID = Certificate.IGNORE_ID;
   
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private volatile byte[] _lastBytes = new byte[0];
    private volatile int _lastId = MIN_VERSION;
    private volatile int newVersion = MIN_VERSION;
    
    /** If an HTTP failover update is in progress */
    private final HttpRequestControl httpRequestControl = new HttpRequestControl();

    private final List<SimppListener> listeners = new CopyOnWriteArrayList<SimppListener>();
    private final CopyOnWriteArrayList<SimppSettingsManager> simppSettingsManagers;    
    private final Clock clock;
    private final Provider<HttpExecutor> httpExecutor;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<HttpParams> defaultParams;
    private final DefaultDataProvider simppDataProvider;
    private final HttpClientInstanceUtils httpClientUtils;
    
    /**
     * If the key used by {@link SimppDataVerifier} is leaked, but not the master
     * key used by {@link CertificateVerifier}, the urls that would have to serve
     * the final simpp message are the same as below, except for v3 has to be
     * replaced with v2. 
     */
    private volatile List<String> maxedUpdateList = Arrays.asList("http://simpp1.limewire.com/v3/simpp.def",
            "http://simpp2.limewire.com/v3/simpp.def",
            "http://simpp3.limewire.com/v3/simpp.def",
            "http://simpp4.limewire.com/v3/simpp.def",
            "http://simpp5.limewire.com/v3/simpp.def",
            "http://simpp6.limewire.com/v3/simpp.def",
            "http://simpp7.limewire.com/v3/simpp.def",
            "http://simpp8.limewire.com/v3/simpp.def",
            "http://simpp9.limewire.com/v3/simpp.def",
            "http://simpp10.limewire.com/v3/simpp.def");
    private volatile int minMaxHttpRequestDelay = 1000 * 60;
    private volatile int maxMaxHttpRequestDelay = 1000 * 60 * 30;
    private volatile int silentPeriodForMaxHttpRequest = 1000 * 60 * 5;

    private final CertifiedMessageVerifier simppMessageVerifier;

    private final SimppDataVerifier simppDataVerifier;

    private final CertificateProvider certificateProvider;

    @Inject
    public SimppManagerImpl(Clock clock,
            Provider<HttpExecutor> httpExecutor,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            @Simpp DefaultDataProvider simppDataProvider, HttpClientInstanceUtils httpClientUtils,
            @Simpp CertifiedMessageVerifier simppMessageVerifier,
            SimppDataVerifier simppDataVerifier,
            @Simpp CertificateProvider certificateProvider) {
        this.clock = clock;
        this.simppMessageVerifier = simppMessageVerifier;
        this.simppDataVerifier = simppDataVerifier;
        this.certificateProvider = certificateProvider;
        this.simppSettingsManagers = new CopyOnWriteArrayList<SimppSettingsManager>();
        this.httpExecutor = httpExecutor;
        this.backgroundExecutor = backgroundExecutor;
        this.defaultParams = defaultParams;
        this.simppDataProvider = simppDataProvider;
        this.httpClientUtils = httpClientUtils;
    }
    
    List<String> getMaxUrls() {
        return maxedUpdateList;
    }
    
    void setMaxUrls(List<String> urls) {
        this.maxedUpdateList = urls;
    }
    
    int getMinHttpRequestUpdateDelayForMaxFailover() {
        return minMaxHttpRequestDelay;
    }
    
    int getMaxHttpRequestUpdateDelayForMaxFailover() {
        return maxMaxHttpRequestDelay;
    }
    
    void setMinHttpRequestUpdateDelayForMaxFailover(int min) {
        minMaxHttpRequestDelay = min;
    }
    
    void setMaxHttpRequestUpdateDelayForMaxFailover(int max) {
        maxMaxHttpRequestDelay = max;
    }
    
    int getSilentPeriodForMaxHttpRequest() {
        return silentPeriodForMaxHttpRequest;
    }
    
    void setSilentPeriodForMaxHttpRequest(int silentPeriodForMaxHttpRequest) {
        this.silentPeriodForMaxHttpRequest = silentPeriodForMaxHttpRequest;
    }
    
    
    public void initialize() {
        LOG.trace("Initializing SimppManager");
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(new File(CommonUtils
                        .getUserSettingsDir(), FILENAME)), CertifiedMessageSourceType.FROM_DISK, null);
                handleDataInternal(simppDataProvider.getDefaultData(), CertifiedMessageSourceType.FROM_DISK, null);
            }
        });
    }
        
    public int getVersion() {
        return _lastId;
    }

    @Override
    public int getKeyVersion() {
        return certificateProvider.get().getKeyVersion();
    }

    @Override
    public int getNewVersion() {
        return newVersion;
    }
    
    /**
     * @return the cached value of the simpp bytes. 
     */ 
    public byte[] getSimppBytes() {
        return _lastBytes;
    }

    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager) {
        simppSettingsManagers.add(simppSettingsManager);
    }

    public List<SimppSettingsManager> getSimppSettingsManagers() {
        return simppSettingsManagers;
    }
    
    public void addListener(SimppListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SimppListener listener) {
        listeners.remove(listener);
    }
    
    public void checkAndUpdate(final ReplyHandler handler, final byte[] data) {
        if(data != null) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, CertifiedMessageSourceType.FROM_NETWORK, handler);
                }
            });
        }
    }
    
    void handleDataInternal(byte[] data, CertifiedMessageSourceType updateType, ReplyHandler handler) {
        if (data == null) {
            LOG.warn("No data to handle.");
            return;
        }
        
        byte[] signedData = null;
        try {
            signedData = simppDataVerifier.extractSignedData(data);
        } catch (SignatureException se) {
            LOG.warn("Couldn't verify signature on data.");
            return;
        }
        
        
        SimppParser parser = null;
        try {
            parser = new SimppParser(signedData);
        } catch(IOException iox) {
            LOG.error("IOX parsing simpp data", iox);
            return;
        }
        
        CertifiedMessage certifiedMessage = parser.getCertifiedMessage();
        Certificate certificate = null;
        try { 
            certificate = simppMessageVerifier.verify(certifiedMessage, handler);
        } catch (SignatureException se) {
            LOG.error("message did not verify", se);
            return;
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Got data with version: " + parser.getNewVersion() + " from: " + updateType + ", current version is: " + newVersion);
        }
        
        switch(updateType) {
        case FROM_NETWORK:
            if(certifiedMessage.getKeyVersion() == IGNORE_ID) {
                if(getKeyVersion() != IGNORE_ID)
                    doHttpMaxFailover();
            } else if(certificate.getKeyVersion() > getKeyVersion() || (certificate.getKeyVersion() == getKeyVersion() && parser.getNewVersion() > newVersion)) {
                storeAndUpdate(data, parser, updateType, certificate);
            }
            break;
        case FROM_DISK:
            if(parser.getNewVersion() > newVersion) {
                storeAndUpdate(data, parser, updateType, certificate);
            }
            break;
        case FROM_HTTP:
            if(parser.getNewVersion() == IGNORE_ID && certifiedMessage.getKeyVersion() == IGNORE_ID) {
                storeAndUpdate(data, parser, updateType, certificate);
            }
            break;
        }
    }
    
    private void storeAndUpdate(byte[] data, SimppParser parser, CertifiedMessageSourceType updateType, Certificate certificate) {
        CertifiedMessage certifiedMessage = parser.getCertifiedMessage();
        if(LOG.isTraceEnabled())
            LOG.trace("Retrieved new data from: " + updateType + ", storing & updating");
        if(certifiedMessage.getKeyVersion() == IGNORE_ID && updateType == CertifiedMessageSourceType.FROM_NETWORK)
            throw new IllegalStateException("shouldn't be here!");
        
        if(updateType == CertifiedMessageSourceType.FROM_NETWORK && httpRequestControl.isRequestPending())
            return;
        
        if (certificate.getKeyVersion() == IGNORE_ID) {
            assert updateType != CertifiedMessageSourceType.FROM_NETWORK;
        }
        
        _lastId = parser.getVersion();
        newVersion = parser.getNewVersion();
        _lastBytes = data;
        certificateProvider.set(certificate);
        
        if(updateType != CertifiedMessageSourceType.FROM_DISK) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
        }
        
        for(SimppSettingsManager ssm : simppSettingsManagers)
            ssm.updateSimppSettings(parser.getPropsData());
        for (SimppListener listener : listeners)
            listener.simppUpdated();
    }
    
    private void doHttpMaxFailover() {
        long maxTimeAgo = clock.now() - silentPeriodForMaxHttpRequest; 
        if(!httpRequestControl.requestQueued() &&
                UpdateSettings.LAST_SIMPP_FAILOVER.getValue() < maxTimeAgo) {
            int rndDelay = RANDOM.nextInt(maxMaxHttpRequestDelay) + minMaxHttpRequestDelay;
            final String rndUri = maxedUpdateList.get(RANDOM.nextInt(maxedUpdateList.size()));
            LOG.debug("Scheduling http max failover in: " + rndDelay + ", to: " + rndUri);
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    String url = rndUri;
                    try {
                        launchHTTPUpdate(url);
                    } catch (URISyntaxException e) {
                        httpRequestControl.requestFinished();
                        httpRequestControl.cancelRequest();
                        LOG.warn("uri failure", e);
                    }
                }
            }, rndDelay, TimeUnit.MILLISECONDS);
        } else {
            LOG.debug("Ignoring http max failover.");
        }
    }
    
    /**
     * Launches an http update to the failover url.
     */
    private void launchHTTPUpdate(String url) throws URISyntaxException {
        if (!httpRequestControl.isRequestPending())
            return;
        LOG.debug("about to issue http request method");
        HttpGet get = new HttpGet(httpClientUtils.addClientInfoToUrl(url));
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        httpRequestControl.requestActive();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(get, params, new RequestHandler());
    }
    
    public byte[] getOldUpdateResponse() {
        return simppDataProvider.getOldDefaultData();
    }

    
    private class RequestHandler implements HttpClientListener {

        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            LOG.debug("http request method succeeded");
            
            // remember we made an attempt even if it didn't succeed
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            final byte[] inflated;
            try {
                if (response.getStatusLine().getStatusCode() < 200
                        || response.getStatusLine().getStatusCode() >= 300)
                    throw new IOException("bad code " + response.getStatusLine().getStatusCode());
    
                byte [] resp = null;
                if(response.getEntity() != null) {
                    resp = IOUtils.readFully(response.getEntity().getContent());
                }
                if (resp == null || resp.length == 0)
                    throw new IOException("bad body");
    
                // inflate the response and process.
                inflated = IOUtils.inflate(resp);
            } catch (IOException failed) {
                httpRequestControl.requestFinished();
                LOG.warn("couldn't fetch data ",failed);
                return false;
            } finally {
                httpExecutor.get().releaseResources(response);
            }
            
            // Handle the data in the background thread.
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    httpRequestControl.requestFinished();
                    
                    LOG.trace("Parsing new data...");
                    handleDataInternal(inflated, CertifiedMessageSourceType.FROM_HTTP, null);
                }
            });
            
            return false; // no more requests
        }
        
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.warn("http failover failed",exc);
            httpRequestControl.requestFinished();
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            
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
     * A simple control to let the flow of HTTP requests happen differently
     * depending on why it was requested.
     */
    private static class HttpRequestControl {
        
        private final AtomicBoolean requestQueued = new AtomicBoolean(false);
        private final AtomicBoolean requestActive = new AtomicBoolean(false);
//        private volatile RequestReason requestReason;
        
        /** Returns true if a request is queued or active. */
        boolean isRequestPending() {
            return requestActive.get() || requestQueued.get();
        }
        
        /** Sets a queued request and returns true if a request is pending or active. */
        boolean requestQueued() {
            boolean prior = requestQueued.getAndSet(true);
            return prior || requestActive.get();
        }
        
        /** Sets a request to be active. */
        void requestActive() {
            requestActive.set(true);
            requestQueued.set(false);
        }
        
        void cancelRequest() {
            requestQueued.set(false);
        }
        
        void requestFinished() {
            requestActive.set(false);
        }
    }

    /**
     * Old clients won't send us neither newVersion nor keyVersion, so their
     * values will be -1. If we get a capabilities update from a new client, we
     * will only look at newVersion and keyVersion and ignore the old version
     * field completely. This is the first if branch. In that case we request a
     * simpp, if its newVersion number is greater and the key version is the
     * same as the current keyVersion.
     * 
     * If an old client is sending us a capabilities update, we are in the
     * second if branch, where we check that the advertised version is higher
     * than the currently known one.
     * 
     * If none of the two cases above was the case, it could be that a newer key
     * version is advertised and we should download the simpp regardless of its
     * version or its newVersion. That's the last if branch.
     */
    @Override
    public boolean shouldRequestSimppMessage(int version, int newVersion, int keyVersion) {
        if (LOG.isDebugEnabled())
            LOG.debugf("version {0}, new version {1}, key version {2}", version, newVersion, keyVersion);
        if (newVersion != -1) {
            if (newVersion > getNewVersion() && keyVersion == getKeyVersion()) {
                return true;
            }
        } else if (version > getVersion()) {
            return true;
        }
        if (getKeyVersion() > MIN_VERSION && keyVersion > getKeyVersion()) {
            return true;
        }
        return false;
    }
}
