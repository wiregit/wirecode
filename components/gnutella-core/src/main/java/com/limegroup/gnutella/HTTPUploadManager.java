package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.collection.Buffer;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.http.BasicHeaderProcessor;
import org.limewire.http.HttpResponseListener;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileLocker;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.BrowseRequestHandler;
import com.limegroup.gnutella.uploader.FileRequestHandler;
import com.limegroup.gnutella.uploader.FileResponseEntity;
import com.limegroup.gnutella.uploader.FreeLoaderRequestHandler;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.LimitReachedRequestHandler;
import com.limegroup.gnutella.uploader.PushProxyRequestHandler;
import com.limegroup.gnutella.uploader.UploadSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadType;

public class HTTPUploadManager implements FileLocker, BandwidthTracker,
        UploadManager, HTTPUploadSessionManager {

    private final static String SESSION_KEY = "org.limewire.session";

    private static final Log LOG = LogFactory.getLog(HTTPUploadManager.class);

    /**
     * This is a <tt>List</tt> of all of the current <tt>Uploader</tt>
     * instances (all of the uploads in progress).
     */
    private List<HTTPUploader> activeUploadList = new LinkedList<HTTPUploader>();

    /** A manager for the available upload slots */
    private final UploadSlotManager slotManager;

    /** set to true when an upload has been succesfully completed. */
    private volatile boolean hadSuccesfulUpload = false;

    /** Number of force-shared active uploads */
    private int forcedUploads;

    /**
     * Number of active uploads that are not accounted in the slot manager but
     * whose bandwidth is counted. (i.e. Multicast)
     */
    private Set<HTTPUploader> forceAllowedUploads = new CopyOnWriteArraySet<HTTPUploader>();

    /**
     * LOCKING: obtain this' monitor before modifying any of the data structures
     */

    /**
     * The number of uploads considered when calculating capacity, if possible.
     * BearShare uses 10. Settings it too low causes you to be fooled be a
     * streak of slow downloaders. Setting it too high causes you to be fooled
     * by a number of quick downloads before your slots become filled.
     */
    private static final int MAX_SPEED_SAMPLE_SIZE = 5;

    /**
     * The min number of uploads considered to give out your speed. Same
     * criteria needed as for MAX_SPEED_SAMPLE_SIZE.
     */
    private static final int MIN_SPEED_SAMPLE_SIZE = 5;

    /** The minimum number of bytes transferred by an uploadeder to count. */
    private static final int MIN_SAMPLE_BYTES = 200000; // 200KB

    public static final int TRANSFER_SOCKET_TIMEOUT = 2 * 60 * 1000;

    /** The average speed in kiloBITs/second of the last few uploads. */
    private Buffer<Integer> speeds = new Buffer<Integer>(MAX_SPEED_SAMPLE_SIZE);

    /**
     * The highestSpeed of the last few downloads, or -1 if not enough downloads
     * have been down for an accurate sample. INVARIANT: highestSpeed>=0 ==>
     * highestSpeed==max({i | i in speeds}) INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE
     * <==> highestSpeed==-1
     */
    private volatile int highestSpeed = -1;

    /**
     * The number of measureBandwidth's we've had
     */
    private int numMeasures = 0;

    /**
     * The current average bandwidth.
     * 
     * This is only counted while uploads are active.
     */
    private float averageBandwidth = 0f;

    /** The last value that getMeasuredBandwidth created. */
    private volatile float lastMeasuredBandwidth;

    /**
     * Remembers uploaders to disadvantage uploaders that hammer us for download
     * slots. Stores up to 250 entries Maps IP String to RequestCache
     */
    private final Map<String, RequestCache> REQUESTS = new FixedsizeForgetfulHashMap<String, RequestCache>(
            250);

    private HTTPAcceptor acceptor;

    private HttpRequestHandler freeLoaderRequestHandler = new FreeLoaderRequestHandler();

    private ResponseListener responseListener = new ResponseListener();

    public HTTPUploadManager(HTTPAcceptor acceptor,
            UploadSlotManager slotManager) {
        if (acceptor == null) {
            throw new IllegalArgumentException("acceptor may not be null");
        }
        if (slotManager == null) {
            throw new IllegalArgumentException("slotManager may not be null");
        }
        
        this.acceptor = acceptor;
        this.slotManager = slotManager;

        FileUtils.addFileLocker(this);

        acceptor.addResponseListener(responseListener);
        
        inititalizeHandlers();
    }

    private void inititalizeHandlers() {
        // browse
        acceptor.registerHandler("/", new BrowseRequestHandler(this));

        // update
        acceptor.registerHandler("/update.xml", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.UPDATE_FILE.incrementStat();
                HTTPUploader uploader = getOrCreateUploader(request, context,
                        UploadType.UPDATE_FILE, "Update-File Request");

                BasicHeaderProcessor processor = new BasicHeaderProcessor();
                processor.addInterceptor(new UserAgentHeaderInterceptor(
                        uploader));
                processor.process(request, context);
                if (UserAgentHeaderInterceptor.isFreeloader(uploader
                        .getUserAgent())) {
                    handleFreeLoader(request, response, context, uploader);
                } else {
                    File file = new File(CommonUtils.getUserSettingsDir(),
                            "update.xml");
                    uploader.setFile(file);
                    uploader.setState(Uploader.UPDATE_FILE);

                    // TODO set mime-type to Constants.QUERYREPLY_MIME_TYPE?
                    response.setEntity(new FileResponseEntity(uploader, file));
                }

                sendResponse(uploader, response);
            }
        });

        // push-proxy requests
        HttpRequestHandler pushProxyHandler = new PushProxyRequestHandler();
        acceptor.registerHandler("/gnutella/push-proxy", pushProxyHandler);
        acceptor.registerHandler("/gnet/push-proxy", pushProxyHandler);

        // uploads
        FileRequestHandler fileRequestHandler = new FileRequestHandler(this);
        acceptor.registerHandler("/get*", fileRequestHandler);
        acceptor.registerHandler("/uri-res/*", fileRequestHandler);

        // unsupported requests
        HttpRequestHandler notFoundHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.FILE_NOT_FOUND.incrementStat();

                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        };
        acceptor.registerHandler("/browser-control", notFoundHandler);
        acceptor.registerHandler("/gnutella/file-view*", notFoundHandler);
        acceptor.registerHandler("/gnutella/res/*", notFoundHandler);
    }

    public void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException,
            IOException {
        UploadStat.FREELOADER.incrementStat();

        uploader.setState(Uploader.FREELOADER);
        freeLoaderRequestHandler.handle(request, response, context);
    }

    /**
     * Push uploads from firewalled clients.
     */
    public void acceptUpload(Socket socket, boolean lan) {
        if (lan) {
            acceptor.acceptLocalConnection(socket);
        } else {
            acceptor.acceptConnection(null, socket);
        }
    }

    /**
     * Determines whether or not this Uploader should bypass queueing, (meaning
     * that it will always work immediately, and will not use up slots for other
     * uploaders).
     * 
     * All requests that are not the 'connecting' state should bypass the queue,
     * because they have already been queued once.
     * 
     * Don't let FILE_VIEW requests bypass the queue, we want to make sure those
     * guys don't hammer.
     */
    private boolean shouldBypassQueue(HttpRequest request, HTTPUploader uploader) {
        return uploader.getState() != HTTPUploader.CONNECTING
                || "HEAD".equals(request.getRequestLine().getMethod())
                || uploader.isForcedShare();
    }

    /**
     * Cleans up a finished uploader. This does the following: 1) Reports the
     * speed at which this upload occured. 2) Removes the uploader from the
     * active upload list 3) Closes the file streams that the uploader has left
     * open 4) Increments the completed uploads in the FileDesc 5) Removes the
     * uploader from the GUI. (4 & 5 are only done if 'shouldShowInGUI' is true)
     */
    public void cleanupFinishedUploader(HTTPUploader uploader) {
        if (LOG.isTraceEnabled())
            LOG.trace("Cleaning uploader " + uploader);

        int state = uploader.getState();
        int lastState = uploader.getLastTransferState();
        // assertAsFinished(state);

        long finishTime = System.currentTimeMillis();
        synchronized (this) {
            // Report how quickly we uploaded the data.
            if (uploader.getStartTime() > 0) {
                reportUploadSpeed(finishTime - uploader.getStartTime(),
                        uploader.getTotalAmountUploaded());
            }
            removeFromList(uploader);
            forceAllowedUploads.remove(uploader);
        }

        switch (state) {
        case HTTPUploader.COMPLETE:
            UploadStat.COMPLETED.incrementStat();
            if (lastState == HTTPUploader.UPLOADING
                    || lastState == HTTPUploader.THEX_REQUEST)
                UploadStat.COMPLETED_FILE.incrementStat();
            break;
        case HTTPUploader.INTERRUPTED:
            UploadStat.INTERRUPTED.incrementStat();
            break;
        }

        if (uploader.getUploadType() != null
                && !uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null
                    && state == HTTPUploader.COMPLETE
                    && (lastState == HTTPUploader.UPLOADING || lastState == HTTPUploader.THEX_REQUEST)) {
                fd.incrementCompletedUploads();
                RouterService.getCallback()
                        .handleSharedFileUpdate(fd.getFile());
            }
        }

        RouterService.getCallback().removeUpload(uploader);
    }

    /**
     * Adds an accepted HTTPUploader to the internal list of active downloads.
     */
    public synchronized void addAcceptedUploader(HTTPUploader uploader) {
        if (uploader.isForcedShare()) {
            forceAllowedUploads.add(uploader);
            forcedUploads++;
        }
        activeUploadList.add(uploader);
        uploader.setStartTime(System.currentTimeMillis());
    }

    /**
     * Adds this upload to the GUI and increments the attempted uploads. Does
     * nothing if 'shouldShowInGUI' is false.
     */
    public void sendResponse(HTTPUploader uploader, HttpResponse response) {
        uploader.setLastResponse(response);

        if (uploader.isVisible()) {
            return;
        }

        // We want to increment attempted only for uploads that may
        // have a chance of failing.
        UploadStat.ATTEMPTED.incrementStat();

        // We are going to notify the gui about the new upload, and let
        // it decide what to do with it - will act depending on it's
        // state
        RouterService.getCallback().addUpload(uploader);
        uploader.setVisible(true);

        if (!uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null) {
                fd.incrementAttemptedUploads();
                RouterService.getCallback()
                        .handleSharedFileUpdate(fd.getFile());
            }
        }
    }

    /**
     * Returns whether or not an upload request can be serviced immediately. In
     * particular, if there are more available upload slots than queued uploads
     * this will return true.
     */
    public synchronized boolean isServiceable() {
        return slotManager.hasHTTPSlot(uploadsInProgress()
                + getNumQueuedUploads());
    }

    /**
     * @return true if an incoming query (not actual upload request) may be
     *         serviceable.
     */
    public synchronized boolean mayBeServiceable() {
        if (RouterService.getFileManager().hasApplicationSharedFiles())
            return slotManager.hasHTTPSlotForMeta(uploadsInProgress()
                    + getNumQueuedUploads());
        return isServiceable();
    }

    public synchronized int uploadsInProgress() {
        return activeUploadList.size() - forcedUploads;
    }

    public synchronized int getNumQueuedUploads() {
        return slotManager.getNumQueued();
    }

    /**
     * Returns true if this has ever successfully uploaded a file during this
     * session.
     * <p>
     * 
     * This method was added to adopt more of the BearShare QHD standard.
     */
    public boolean hadSuccesfulUpload() {
        return hadSuccesfulUpload;
    }

    public synchronized boolean isConnectedTo(InetAddress addr) {
        if (slotManager.getNumUsersForHost(addr.getHostAddress()) > 0)
            return true;

        for (HTTPUploader uploader : activeUploadList) {
            InetAddress host = uploader.getConnectedHost();
            if (host != null && host.equals(addr))
                return true;
        }
        return false;
    }

    public boolean releaseLock(File file) {
        FileDesc fd = RouterService.getFileManager().getFileDescForFile(file);
        if (fd != null)
            return killUploadsForFileDesc(fd);
        else
            return false;
    }

    /**
     * Kills all uploads that are uploading the given FileDesc.
     */
    public synchronized boolean killUploadsForFileDesc(FileDesc fd) {
        boolean ret = false;
        // This causes the uploader to generate an exception,
        // and ultimately remove itself from the list.
        for (HTTPUploader uploader : activeUploadList) {
            FileDesc upFD = uploader.getFileDesc();
            if (upFD != null && upFD.equals(fd)) {
                ret = true;
                uploader.stop();
            }
        }
        return ret;
    }

    /**
     * Checks whether the given upload may proceed based on number of slots,
     * position in upload queue, etc. Updates the upload queue as necessary.
     * 
     * @return ACCEPTED if the download may proceed, QUEUED if this is in the
     *         upload queue, REJECTED if this is flat-out disallowed (and hence
     *         not queued) and BANNED if the downloader is hammering us, and
     *         BYPASS_QUEUE if this is a File-View request that isn't hammering
     *         us. If REJECTED, <tt>uploader</tt>'s state will be set to
     *         LIMIT_REACHED. If BANNED, the <tt>Uploader</tt>'s state will
     *         be set to BANNED_GREEDY.
     */
    private synchronized QueueStatus checkAndQueue(UploadSession session) {
        RequestCache rqc = REQUESTS.get(session.getHost());
        if (rqc == null)
            rqc = new RequestCache();
        // make sure we don't forget this RequestCache too soon!
        REQUESTS.put(session.getHost(), rqc);
        rqc.countRequest();
        if (rqc.isHammering()) {
            if (LOG.isWarnEnabled())
                LOG.warn(session.getUploader() + " banned.");
            return QueueStatus.BANNED;
        }

        FileDesc fd = session.getUploader().getFileDesc();
        if (!fd.isVerified()) // spawn a validation
            RouterService.getFileManager().validate(fd);

        URN sha1 = fd.getSHA1Urn();

        if (rqc.isDupe(sha1))
            return QueueStatus.REJECTED;

        // check the host limit unless this is a poll
        if (slotManager.positionInQueue(session) == -1
                && hostLimitReached(session.getHost())) {
            if (LOG.isDebugEnabled())
                LOG.debug("host limit reached for " + session.getHost());
            UploadStat.LIMIT_REACHED_GREEDY.incrementStat();
            return QueueStatus.REJECTED;
        }

        int queued = slotManager.pollForSlot(session, session.getUploader()
                .supportsQueueing(), session.getUploader().isPriorityShare());

        if (LOG.isDebugEnabled())
            LOG.debug("queued at " + queued);

        if (queued == -1) // not accepted nor queued.
            return QueueStatus.REJECTED;

        if (queued > 0 && session.poll()) {
            slotManager.cancelRequest(session);
            // TODO we used to just drop the connection
            return QueueStatus.BANNED;
        }
        if (queued > 0) {
            return QueueStatus.QUEUED;
        } else {
            rqc.startedUpload(sha1);
            return QueueStatus.ACCEPTED;
        }
    }

    /**
     * Decrements the number of active uploads for the host specified in the
     * <tt>host</tt> argument, removing that host from the <tt>Map</tt> if
     * this was the only upload allocated to that host.
     * <p>
     * 
     * This method also removes the <tt>Uploader</tt> from the <tt>List</tt>
     * of active uploads.
     */
    private synchronized void removeFromList(HTTPUploader uploader) {
        // if the uploader is not in the active list, we should not
        // try remove the urn from the map of unique uploaded files for that
        // host.

        if (activeUploadList.remove(uploader)) {
            if (uploader.isForcedShare())
                forcedUploads--;

            // at this point it is safe to allow other uploads from the same
            // host
            RequestCache rcq = REQUESTS.get(uploader.getHost());

            // check for nulls so that unit tests pass
            if (rcq != null && uploader != null
                    && uploader.getFileDesc() != null)
                rcq.uploadDone(uploader.getFileDesc().getSHA1Urn());
        }

        // Enable auto shutdown
        if (activeUploadList.size() == 0)
            RouterService.getCallback().uploadsComplete();
    }

    /**
     * @return false if the number of uploads from the host is strictly LESS
     *         than the MAX, although we want to allow exactly MAX uploads from
     *         the same host. This is because this method is called BEFORE we
     *         add/allow the. upload.
     */
    private synchronized boolean hostLimitReached(String host) {
        return slotManager.getNumUsersForHost(host) >= UploadSettings.UPLOADS_PER_PERSON
                .getValue();
    }

    // //////////////// Bandwidth Allocation and Measurement///////////////

    /**
     * Calculates the appropriate burst size for the allocating bandwidth on the
     * upload.
     * 
     * @return burstSize. if it is the special case, in which we want to upload
     *         as quickly as possible.
     */
    public int calculateBandwidth() {
        // public int calculateBurstSize() {
        float totalBandwith = getTotalBandwith();
        float burstSize = totalBandwith / uploadsInProgress();
        return (int) burstSize;
    }

    /**
     * @return the total bandwith available for uploads
     */
    private float getTotalBandwith() {

        // To calculate the total bandwith available for
        // uploads, there are two properties. The first
        // is what the user *thinks* their connection
        // speed is. Note, that they may have set this
        // wrong, but we have no way to tell.
        float connectionSpeed = ConnectionSettings.CONNECTION_SPEED.getValue() / 8.0f;
        // the second number is the speed that they have
        // allocated to uploads. This is really a percentage
        // that the user is willing to allocate.
        float speed = UploadSettings.UPLOAD_SPEED.getValue();
        // the total bandwith available then, is the percentage
        // allocated of the total bandwith.
        float totalBandwith = connectionSpeed * speed / 100.0f;
        return totalBandwith;
    }

    /**
     * Returns the estimated upload speed in <b>KILOBITS/s</b> [sic] of the
     * next transfer, assuming the client (i.e., downloader) has infinite
     * bandwidth. Returns -1 if not enough data is available for an accurate
     * estimate.
     */
    public int measuredUploadSpeed() {
        // Note that no lock is needed.
        return highestSpeed;
    }

    /**
     * Notes that some uploader has uploaded the given number of BYTES in the
     * given number of milliseconds. If bytes is too small, the data may be
     * ignored.
     * 
     * @requires this' lock held
     * @modifies this.speed, this.speeds
     */
    private void reportUploadSpeed(long milliseconds, long bytes) {
        // This is critical for ignoring 404's messages, etc.
        if (bytes < MIN_SAMPLE_BYTES)
            return;

        // Calculate the bandwidth in kiloBITS/s. We just assume that 1 kilobyte
        // is 1000 (not 1024) bytes for simplicity.
        int bandwidth = 8 * (int) ((float) bytes / (float) milliseconds);
        synchronized (speeds) {
            speeds.add(bandwidth);

            // Update maximum speed if possible
            if (speeds.size() >= MIN_SPEED_SAMPLE_SIZE) {
                int max = 0;
                for (int i = 0; i < speeds.size(); i++)
                    max = Math.max(max, speeds.get(i));
                this.highestSpeed = max;
            }
        }
    }

    public void measureBandwidth() {
        slotManager.measureBandwidth();
        for (HTTPUploader forced : forceAllowedUploads) {
            forced.measureBandwidth();
        }
    }

    public float getMeasuredBandwidth() {
        float bw = 0;
        try {
            bw += slotManager.getMeasuredBandwidth();
        } catch (InsufficientDataException notEnough) {
        }

        for (HTTPUploader forced : forceAllowedUploads) {
            try {
                bw += forced.getMeasuredBandwidth();
            } catch (InsufficientDataException e) {
            }
        }

        synchronized (this) {
            averageBandwidth = ((averageBandwidth * numMeasures) + bw)
                    / ++numMeasures;
        }
        lastMeasuredBandwidth = bw;
        return bw;
    }

    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }

    public float getLastMeasuredBandwidth() {
        return lastMeasuredBandwidth;
    }

    /**
     * @return whether there are any active internet (non-multicast) transfers
     *         going at speed greater than 0.
     */
    public boolean hasActiveInternetTransfers() {
        slotManager.measureBandwidth();
        try {
            return slotManager.getMeasuredBandwidth() > 0;
        } catch (InsufficientDataException ide) {
        }
        return false;
    }

    /**
     * Keeps track of client requests.
     */
    private static class RequestCache {
        // we don't allow more than 1 request per 5 seconds
        private static final double MAX_REQUESTS = 5 * 1000;

        // time we expect the downloader to wait before sending
        // another request after our initial LIMIT_REACHED reply
        // must be greater than or equal to what we send in our RetryAfter
        // header, otherwise we'll incorrectly mark guys as greedy.
        static long WAIT_TIME = LimitReachedRequestHandler.RETRY_AFTER_TIME * 1000;

        // time to wait before checking for hammering: 30 seconds.
        // if the averge number of requests per time frame exceeds MAX_REQUESTS
        // after FIRST_CHECK_TIME, the downloader will be banned.
        static long FIRST_CHECK_TIME = 30 * 1000;

        /**
         * The set of sha1 requests we've seen in the past WAIT_TIME.
         */
        private final Set<URN> ACTIVE_UPLOADS;

        /**
         * The number of requests we've seen from this host so far.
         */
        private double _numRequests;

        /**
         * The time of the last request.
         */
        private long _lastRequest;

        /**
         * The time of the first request.
         */
        private long _firstRequest;

        /**
         * Constructs a new RequestCache.
         */
        RequestCache() {
            ACTIVE_UPLOADS = new HashSet<URN>();
            _numRequests = 0;
            _lastRequest = _firstRequest = System.currentTimeMillis();
        }

        /**
         * Tells the cache that an upload to the host has started.
         * 
         * @param sha1 the urn of the file being uploaded.
         */
        void startedUpload(URN sha1) {
            ACTIVE_UPLOADS.add(sha1);
        }

        /**
         * Determines whether or not the host is hammering.
         */
        boolean isHammering() {
            if (_lastRequest - _firstRequest <= FIRST_CHECK_TIME) {
                return false;
            } else {
                return ((_lastRequest - _firstRequest) / _numRequests) < MAX_REQUESTS;
            }
        }

        /**
         * Adds a new request.
         */
        void countRequest() {
            _numRequests++;
            _lastRequest = System.currentTimeMillis();
        }

        /**
         * Checks whether the given URN is a duplicate request
         */
        boolean isDupe(URN sha1) {
            return ACTIVE_UPLOADS.contains(sha1);
        }

        /**
         * Informs the request cache that the given URN is no longer actively
         * uploaded.
         */
        void uploadDone(URN sha1) {
            ACTIVE_UPLOADS.remove(sha1);
        }
    }

    public UploadSlotManager getSlotManager() {
        return slotManager;
    }

    public UploadSession getOrCreateSession(HttpContext context) {
        UploadSession session = (UploadSession) context
                .getAttribute(SESSION_KEY);
        if (session == null) {
            HttpInetConnection conn = (HttpInetConnection) context
                    .getAttribute(HttpExecutionContext.HTTP_CONNECTION);
            InetAddress host;
            if (conn != null) {
                host = conn.getRemoteAddress();
            } else {
                // XXX this is a bad work around to make request processing
                // testable without having an underlying connection
                try {
                    host = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
            session = new UploadSession(getSlotManager(), host,
                    HttpContextParams.getIOSession(context));
            context.setAttribute(SESSION_KEY, session);
        }
        return session;
    }

    /**
     * Returns the session stored in <code>context</code>.
     *
     * @return null, if no session exists
     */
    public UploadSession getSession(HttpContext context) {
        UploadSession session = (UploadSession) context
                .getAttribute(SESSION_KEY);
        return session;
    }

    public HTTPUploader getOrCreateUploader(HttpRequest request,
            HttpContext context, UploadType type, String filename) {
        UploadSession session = getOrCreateSession(context);
        HTTPUploader uploader = session.getUploader();
        if (uploader != null) {
            if (!uploader.getFileName().equals(filename)
                    || !uploader.getMethod().equals(
                            request.getRequestLine().getMethod())) {
                // start new file
                slotManager.requestDone(session);

                // Because queuing is per-socket (and not per file),
                // we do not want to reset the queue status if they're
                // requesting a new file.
                if (session.isQueued()) {
                    // However, we DO want to make sure that the old file
                    // is interpreted as interrupted. Otherwise,
                    // the GUI would show two lines with the the same slot
                    // until the newer line finished, at which point
                    // the first one would display as a -1 queue position.
                    uploader.setState(Uploader.INTERRUPTED);
                } else {
                    slotManager.requestDone(session);
                }

                cleanupFinishedUploader(uploader);

                uploader = new HTTPUploader(filename, session);
            } else {
                // reuse existing uploader object
                uploader.reinitialize();
            }
        } else {
            // first request for this session
            uploader = new HTTPUploader(filename, session);
        }

        String method = request.getRequestLine().getMethod();
        uploader.setMethod(method);
        uploader.setUploadType("HEAD".equals(method) ? UploadType.HEAD_REQUEST
                : type);
        session.setUploader(uploader);
        return uploader;
    }

    public HTTPUploader getUploader(HttpContext context) {
        UploadSession session = getSession(context);
        HTTPUploader uploader = session.getUploader();
        assert uploader != null;
        return uploader;
    }

    public QueueStatus enqueue(HttpContext context, HttpRequest request) {
        UploadSession session = getSession(context);
        assert !session.isAccepted();

        if (shouldBypassQueue(request, session.getUploader())) {
            session.setQueueStatus(QueueStatus.BYPASS);
        } else if (HttpContextParams.isLocal(context)) {
            session.setQueueStatus(QueueStatus.ACCEPTED);
        } else {
            session.setQueueStatus(checkAndQueue(session));
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Queued upload: " + session);

        return session.getQueueStatus();
    }

    /** Removes active uploaders for testing. */
    protected void cleanup() {
        for (HTTPUploader uploader : activeUploadList
                .toArray(new HTTPUploader[0])) {
            slotManager.cancelRequest(uploader.getSession());
            removeFromList(uploader);
        }
    }

    /**
     * Manages the {@link UploadSession} associated with a connection.
     */
    private class ResponseListener implements HttpResponseListener {

        public void connectionOpen(NHttpConnection conn) {
        }

        public void connectionClosed(NHttpConnection conn) {
            UploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Closing session for " + session.getHost());

                    boolean stillInQueue = slotManager.positionInQueue(session) > -1;
                    slotManager.cancelRequest(session);
                    if (stillInQueue) {
                        assert uploader.getState() == Uploader.QUEUED;
                        uploader.setState(Uploader.INTERRUPTED);
                    } else if (uploader.getState() != Uploader.COMPLETE) {
                        // the complete state may have been set by
                        // responseSent() already
                        uploader.setState(Uploader.COMPLETE);
                    }
                    cleanupFinishedUploader(uploader);
                    session.setUploader(null);
                }
            }
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            UploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null && uploader.getLastResponse() == response) {
                    uploader.setLastResponse(null);
                    uploader.setState(Uploader.COMPLETE);
                }
                
                if (session.getQueueStatus() == QueueStatus.QUEUED) {
                    session.getIOSession().setSocketTimeout(
                            UploadSession.MAX_POLL_TIME);
                }
            }
        }

    }

}
