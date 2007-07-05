package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.apache.http.nio.NHttpConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.collection.Buffer;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.util.FileLocker;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.BrowseRequestHandler;
import com.limegroup.gnutella.uploader.FileRequestHandler;
import com.limegroup.gnutella.uploader.FreeLoaderRequestHandler;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.PushProxyRequestHandler;
import com.limegroup.gnutella.uploader.UpdateFileRequestHandler;
import com.limegroup.gnutella.uploader.HTTPUploadSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadType;

/**
 * Manages {@link HTTPUploader} objects that are created by
 * {@link HttpRequestHandler}s through the {@link HTTPUploadSessionManager}
 * interface. Since HTTP 1.1 allows multiple requests for a single connection an
 * {@link HTTPUploadSession} is created for each connection. It keeps track of
 * queuing (which is per connection) and bandwidth and has a reference to the
 * {@link HTTPUploader} that represents the current request.
 * <p>
 * The state of <code>HTTPUploader</code> follows this pattern:
 * 
 * <pre>
 *                             |-&gt;---- THEX_REQUEST -------&gt;--|
 *                             |-&gt;---- UNAVAILABLE_RANGE --&gt;--|
 *                             |-&gt;---- PUSH_PROXY ---------&gt;--|
 *                            /--&gt;---- FILE NOT FOUND -----&gt;--|
 *                           /---&gt;---- MALFORMED REQUEST --&gt;--|
 *                          /----&gt;---- BROWSE HOST --------&gt;--|
 *                         /-----&gt;---- UPDATE FILE --------&gt;--|
 *                        /------&gt;---- QUEUED -------------&gt;--|
 *                       /-------&gt;---- LIMIT REACHED ------&gt;--|
 *                      /--------&gt;---- UPLOADING ----------&gt;--|
 * --&gt;--CONNECTING--&gt;--/                                      |
 *        |                                                  \|/
 *        |                                                   |
 *       /|\                                                  |---&gt;INTERRUPTED
 *        |--------&lt;---COMPLETE-&lt;------&lt;-------&lt;-------&lt;------/      (done)
 *                        |
 *                        |
 *                      (done)
 * </pre>
 * 
 * COMPLETE uploaders may be using HTTP/1.1, in which case the HTTPUploader
 * recycles back to CONNECTING upon receiving the next GET/HEAD request and
 * repeats.
 * <p>
 * INTERRUPTED HTTPUploaders are never reused. However, it is possible that the
 * socket may be reused. This case is only possible when a requester is queued
 * for one file and sends a subsequent request for another file. The first
 * <code>HTTPUploader</code> is set as interrupted and a second one is created
 * for the new file, using the same connection as the first one.
 * <p>
 * To initialize the upload manager {@link #start(HTTPAcceptor)} needs to be
 * invoked which registers handlers with an {@link HTTPAcceptor}.
 * 
 * @see com.limegroup.gnutella.uploader.HTTPUploader
 * @see com.limegroup.gnutella.HTTPAcceptor
 */
public class HTTPUploadManager implements FileLocker, BandwidthTracker,
        UploadManager, HTTPUploadSessionManager {

    /** The key used to store the {@link HTTPUploadSession} object. */
    private final static String SESSION_KEY = "org.limewire.session";

    private static final Log LOG = LogFactory.getLog(HTTPUploadManager.class);

    /**
     * This is a <code>List</code> of all of the current <code>Uploader</code>
     * instances (all of the uploads in progress that are not queued).
     */
    private List<HTTPUploader> activeUploadList = new LinkedList<HTTPUploader>();

    /** A manager for the available upload slots */
    private final UploadSlotManager slotManager;

    /** Set to true when an upload has been successfully completed. */
    private volatile boolean hadSuccesfulUpload = false;

    /** Number of force-shared active uploads. */
    private int forcedUploads;

    final private HttpRequestHandler freeLoaderRequestHandler = new FreeLoaderRequestHandler();

    private final ResponseListener responseListener = new ResponseListener();

    /**
     * Number of active uploads that are not accounted in the slot manager but
     * whose bandwidth is counted. (i.e. Multicast)
     */
    private final Set<HTTPUploader> localUploads = new CopyOnWriteArraySet<HTTPUploader>();

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

    private volatile ActivityCallback activityCallback;

    private volatile FileManager fileManager;

    private volatile boolean started;
    
    public HTTPUploadManager(UploadSlotManager slotManager) {
        if (slotManager == null) {
            throw new IllegalArgumentException("slotManager may not be null");
        }

        this.slotManager = slotManager;
    }

    /**
     * Registers the upload manager at <code>acceptor</code>.
     * 
     * @throws IllegalStateException if uploadmanager was already started
     * @see #stop(HTTPAcceptor)
     */
    public void start(HTTPAcceptor acceptor, FileManager fileManager, ActivityCallback activityCallback) {
        if (acceptor == null) {
            throw new IllegalArgumentException("acceptor may not be null");
        }
        if (activityCallback == null) {
            throw new IllegalArgumentException("activityCallback may not be null");
        }        
        if (fileManager == null) {
            throw new IllegalArgumentException("fileManager may not be null");
        }        
        if (started) {
            throw new IllegalStateException();
        }
        
        this.fileManager = fileManager;
        this.activityCallback = activityCallback;
        
        FileUtils.addFileLocker(this);

        acceptor.addAcceptorListener(responseListener);

        // browse
        acceptor.registerHandler("/", new BrowseRequestHandler(this));

        // update
        acceptor.registerHandler("/update.xml", new UpdateFileRequestHandler(this));

        // push-proxy requests
        HttpRequestHandler pushProxyHandler = new PushProxyRequestHandler(this);
        acceptor.registerHandler("/gnutella/push-proxy", pushProxyHandler);
        acceptor.registerHandler("/gnet/push-proxy", pushProxyHandler);

        // uploads
        FileRequestHandler fileRequestHandler = new FileRequestHandler(this, fileManager);
        acceptor.registerHandler("/get*", fileRequestHandler);
        acceptor.registerHandler("/uri-res/*", fileRequestHandler);
        
        started = true;
    }

    /**
     * Unregisters the upload manager at <code>acceptor</code>.
     * 
     * @see #start(HTTPAcceptor)
     */
    public void stop(HTTPAcceptor acceptor) {
        if (acceptor == null) {
            throw new IllegalArgumentException("acceptor may not be null");
        }
        if (!started) {
            throw new IllegalStateException();
        }

        acceptor.unregisterHandler("/");
        acceptor.unregisterHandler("/update.xml");
        acceptor.unregisterHandler("/gnutella/push-proxy");
        acceptor.unregisterHandler("/gnet/push-proxy");
        acceptor.unregisterHandler("/get*");
        acceptor.unregisterHandler("/uri-res/*");
        
        acceptor.removeAcceptorListener(responseListener);
        
        FileUtils.removeFileLocker(this);
        
        started = false;
        
        fileManager = null;
        activityCallback = null;
    }
    
    public void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException,
            IOException {
        assert started;
        
        UploadStat.FREELOADER.incrementStat();

        uploader.setState(UploadStatus.FREELOADER);
        freeLoaderRequestHandler.handle(request, response, context);
    }

    /**
     * Determines whether or not this uploader should bypass queuing, (meaning
     * that it will always work immediately, and will not use up slots for other
     * uploaders).
     */
    private boolean shouldBypassQueue(HttpRequest request, HTTPUploader uploader) {
        assert started;
        
        assert uploader.getState() == UploadStatus.CONNECTING;
        return "HEAD".equals(request.getRequestLine().getMethod())
            || uploader.isForcedShare();
    }

    /**
     * Cleans up a finished uploader. This does the following:
     * <ol>
     * <li>Reports the speed at which this upload occured.
     * <li>Removes the uploader from the active upload list
     * <li>Increments the completed uploads in the FileDesc
     * <li>Removes the uploader from the GUI
     * </ol>
     */
    public void cleanupFinishedUploader(HTTPUploader uploader) {
        assert started;
        
        if (LOG.isTraceEnabled())
            LOG.trace("Cleaning uploader " + uploader);

        UploadStatus state = uploader.getState();
        UploadStatus lastState = uploader.getLastTransferState();
        // assertAsFinished(state);

        long finishTime = System.currentTimeMillis();
        synchronized (this) {
            // Report how quickly we uploaded the data.
            if (uploader.getStartTime() > 0) {
                reportUploadSpeed(finishTime - uploader.getStartTime(),
                        uploader.getTotalAmountUploaded());
            }
            removeFromList(uploader);
            localUploads.remove(uploader);
        }

        switch (state) {
        case COMPLETE:
            UploadStat.COMPLETED.incrementStat();
            if (lastState == UploadStatus.UPLOADING
                    || lastState == UploadStatus.THEX_REQUEST)
                UploadStat.COMPLETED_FILE.incrementStat();
            break;
        case INTERRUPTED:
            UploadStat.INTERRUPTED.incrementStat();
            break;
        }

        if (uploader.getUploadType() != null
                && !uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null
                    && state == UploadStatus.COMPLETE
                    && (lastState == UploadStatus.UPLOADING || lastState == UploadStatus.THEX_REQUEST)) {
                fd.incrementCompletedUploads();
                activityCallback .handleSharedFileUpdate(fd.getFile());
            }
        }

        activityCallback.removeUpload(uploader);
    }

    public synchronized void addAcceptedUploader(HTTPUploader uploader, HttpContext context) {
        assert started;
        
        if (uploader.isForcedShare()) 
            forcedUploads++;
        else if (HttpContextParams.isLocal(context))
            localUploads.add(uploader);
        activeUploadList.add(uploader);
        uploader.setStartTime(System.currentTimeMillis());
    }

    public void sendResponse(HTTPUploader uploader, HttpResponse response) {
        assert started;
        
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
        activityCallback.addUpload(uploader);
        uploader.setVisible(true);

        if (!uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null) {
                fd.incrementAttemptedUploads();
                activityCallback.handleSharedFileUpdate(fd.getFile());
            }
        }
    }

    public synchronized boolean isServiceable() {
        assert started;
        
        return slotManager.hasHTTPSlot(uploadsInProgress()
                + getNumQueuedUploads());
    }

    public synchronized boolean mayBeServiceable() {
        assert started;
        
        if (fileManager.hasApplicationSharedFiles())
            return slotManager.hasHTTPSlotForMeta(uploadsInProgress()
                    + getNumQueuedUploads());
        return isServiceable();
    }

    public synchronized int uploadsInProgress() {
        assert started;
        
        return activeUploadList.size() - forcedUploads;
    }

    public synchronized int getNumQueuedUploads() {
        assert started;
        
        return slotManager.getNumQueued();
    }

    public boolean hadSuccesfulUpload() {
        return hadSuccesfulUpload;
    }

    public synchronized boolean isConnectedTo(InetAddress addr) {
        assert started;
        
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
        assert started;
        
        FileDesc fd = fileManager.getFileDescForFile(file);
        if (fd != null)
            return killUploadsForFileDesc(fd);
        else
            return false;
    }

    public synchronized boolean killUploadsForFileDesc(FileDesc fd) {
        assert started;
        
        boolean ret = false;
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
     *         us. If REJECTED, <code>uploader</code>'s state will be set to
     *         LIMIT_REACHED. If BANNED, the <code>Uploader</code>'s state
     *         will be set to BANNED_GREEDY.
     */
    private synchronized QueueStatus checkAndQueue(HTTPUploadSession session) {
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
            fileManager.validate(fd);

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
            rqc.startedTransfer(sha1);
            return QueueStatus.ACCEPTED;
        }
    }

    /**
     * Decrements the number of active uploads for the host specified in the
     * <code>host</code> argument, removing that host from the
     * <code>Map</code> if this was the only upload allocated to that host.
     * <p>
     * This method also removes <code>uploader</code> from the
     * <code>List</code> of active uploads.
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
                rcq.transferDone(uploader.getFileDesc().getSHA1Urn());
        }

        // Enable auto shutdown
        if (activeUploadList.size() == 0)
            activityCallback.uploadsComplete();
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
        assert started;
        
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

    public int measuredUploadSpeed() {
        assert started;
        
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
        assert started;
        
        slotManager.measureBandwidth();
        for (HTTPUploader active : localUploads) {
            active.measureBandwidth();
        }
    }

    public float getMeasuredBandwidth() {
        assert started;
        
        float bw = 0;
        try {
            bw += slotManager.getMeasuredBandwidth();
        } catch (InsufficientDataException notEnough) {
        }

        for (HTTPUploader forced : localUploads) {
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
        assert started;
        
        return averageBandwidth;
    }

    public float getLastMeasuredBandwidth() {
        assert started;
        
        return lastMeasuredBandwidth;
    }

    public UploadSlotManager getSlotManager() {
        assert started;
        
        return slotManager;
    }

    public HTTPUploadSession getOrCreateSession(HttpContext context) {
        assert started;
        
        HTTPUploadSession session = (HTTPUploadSession) context
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
            session = new HTTPUploadSession(getSlotManager(), host,
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
    public HTTPUploadSession getSession(HttpContext context) {
        assert started;
        
        HTTPUploadSession session = (HTTPUploadSession) context
                .getAttribute(SESSION_KEY);
        return session;
    }

    public HTTPUploader getOrCreateUploader(HttpRequest request,
            HttpContext context, UploadType type, String filename) {
        assert started;
        
        HTTPUploadSession session = getOrCreateSession(context);
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
                    uploader.setState(UploadStatus.INTERRUPTED);
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
        assert started;
        
        HTTPUploadSession session = getSession(context);
        HTTPUploader uploader = session.getUploader();
        assert uploader != null;
        return uploader;
    }

    public QueueStatus enqueue(HttpContext context, HttpRequest request) {
        assert started;
        
        HTTPUploadSession session = getSession(context);
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

    /** For testing: removes all uploaders and clears the request cache. */
    public void cleanup() {
        assert started;
        
        for (HTTPUploader uploader : activeUploadList
                .toArray(new HTTPUploader[0])) {
            uploader.stop();
            slotManager.cancelRequest(uploader.getSession());
            removeFromList(uploader);
        }
        slotManager.cleanup();
        REQUESTS.clear();
    }

    /**
     * Manages the {@link HTTPUploadSession} associated with a connection.
     */
    private class ResponseListener implements HTTPAcceptorListener {

        public void connectionOpen(NHttpConnection conn) {
        }

        public void connectionClosed(NHttpConnection conn) {
            assert started;
            
            HTTPUploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Closing session for " + session.getHost());

                    boolean stillInQueue = slotManager.positionInQueue(session) > -1;
                    slotManager.cancelRequest(session);
                    if (stillInQueue) {
                        // If it was queued, also set the state to INTERRUPTED
                        // (changing from COMPLETE)
                        uploader.setState(UploadStatus.INTERRUPTED);
                    } else if (uploader.getState() != UploadStatus.COMPLETE) {
                        // the complete state may have been set by
                        // responseSent() already
                        uploader.setState(UploadStatus.COMPLETE);
                    }
                    uploader.setLastResponse(null);
                    cleanupFinishedUploader(uploader);
                    session.setUploader(null);
                }
            }
        }

        public void requestReceived(NHttpConnection conn, HttpRequest request) {
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            assert started;
            
            HTTPUploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null && uploader.getLastResponse() == response) {
                    uploader.setLastResponse(null);
                    uploader.setState(UploadStatus.COMPLETE);
                }

                if (session.getQueueStatus() == QueueStatus.QUEUED) {
                    session.getIOSession().setSocketTimeout(
                            HTTPUploadSession.MAX_POLL_TIME);
                }
            }
        }

    }

}
