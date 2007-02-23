package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Buffer;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.collection.Interval;
import org.limewire.util.FileLocker;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.HTTPSession;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.LimitReachedUploadState;
import com.limegroup.gnutella.uploader.UploadSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/**
 * 
 */
public abstract class AbstractUploadManager implements FileLocker,
        ConnectionAcceptor, BandwidthTracker, UploadManager {

    private static final Log LOG = LogFactory
            .getLog(AbstractUploadManager.class);

    /** An enumeration of return values for queue checking. */
    private final int BYPASS_QUEUE = -1;

    private final int REJECTED = 0;

    private final int QUEUED = 1;

    private final int ACCEPTED = 2;

    private final int BANNED = 3;

    // private final int NOT_VALIDATED = 4;

    /**
     * This is a <tt>List</tt> of all of the current <tt>Uploader</tt>
     * instances (all of the uploads in progress).
     */
    private List<Uploader> _activeUploadList = new LinkedList<Uploader>();

    /** A manager for the available upload slots */
    private final UploadSlotManager slotManager;

    /** set to true when an upload has been succesfully completed. */
    private volatile boolean _hadSuccesfulUpload = false;

    /** Number of force-shared active uploads */
    private int _forcedUploads;

    /**
     * Number of active uploads that are not accounted in the slot manager but
     * whose bandwidth is counted. (i.e. Multicast)
     */
    private Set<Uploader> forceAllowedUploads = new CopyOnWriteArraySet<Uploader>();

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

    /** The average speed in kiloBITs/second of the last few uploads. */
    private Buffer /* of Integer */speeds = new Buffer(MAX_SPEED_SAMPLE_SIZE);

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

    public AbstractUploadManager(UploadSlotManager slotManager) {
        this.slotManager = slotManager;
        FileUtils.addFileLocker(this);
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
    private boolean shouldBypassQueue(HTTPUploader uploader) {
        return uploader.getState() != Uploader.CONNECTING
                || uploader.getMethod() == HTTPRequestMethod.HEAD
                || uploader.isForcedShare();
    }

    /**
     * Cleans up a finished uploader. This does the following: 1) Reports the
     * speed at which this upload occured. 2) Removes the uploader from the
     * active upload list 3) Closes the file streams that the uploader has left
     * open 4) Increments the completed uploads in the FileDesc 5) Removes the
     * uploader from the GUI. (4 & 5 are only done if 'shouldShowInGUI' is true)
     */
    private void cleanupFinishedUploader(Uploader uploader, long startTime) {
        if (LOG.isTraceEnabled())
            LOG.trace(uploader + " cleaning up finished.");

        int state = uploader.getState();
        int lastState = uploader.getLastTransferState();
//        assertAsFinished(state);

        long finishTime = System.currentTimeMillis();
        synchronized (this) {
            // Report how quickly we uploaded the data.
            if (startTime > 0) {
                reportUploadSpeed(finishTime - startTime, uploader
                        .getTotalAmountUploaded());
            }
            removeFromList(uploader);
            forceAllowedUploads.remove(uploader);
        }

//        uploader.closeFileStreams();

        switch (state) {
        case Uploader.COMPLETE:
            UploadStat.COMPLETED.incrementStat();
            if (lastState == Uploader.UPLOADING
                    || lastState == Uploader.THEX_REQUEST)
                UploadStat.COMPLETED_FILE.incrementStat();
            break;
        case Uploader.INTERRUPTED:
            UploadStat.INTERRUPTED.incrementStat();
            break;
        }

        if (uploader.getUploadType() != null
                && !uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null
                    && state == Uploader.COMPLETE
                    && (lastState == Uploader.UPLOADING || lastState == Uploader.THEX_REQUEST)) {
                fd.incrementCompletedUploads();
                RouterService.getCallback()
                        .handleSharedFileUpdate(fd.getFile());
            }
        }

        RouterService.getCallback().removeUpload(uploader);
    }

    // public void setUploaderState(Uploader uploader) {
    // // This is the normal case ...
    // FileManager fm = RouterService.getFileManager();
    // FileDesc fd = null;
    // int index = uploader.getIndex();
    // // First verify the file index
    // synchronized(fm) {
    // if(fm.isValidIndex(index)) {
    // fd = fm.get(index);
    // }
    // }
    //
    // // If the index was invalid or the file was unshared, FNF.
    // if(fd == null) {
    // if(LOG.isDebugEnabled())
    // LOG.debug(uploader + " fd is null");
    // uploader.setState(Uploader.FILE_NOT_FOUND);
    // return;
    // }
    // // If the name they want isn't the name we have, FNF.
    // if(!uploader.getFileName().equals(fd.getFileName())) {
    // if(LOG.isDebugEnabled())
    // LOG.debug(uploader + " wrong file name");
    // uploader.setState(Uploader.FILE_NOT_FOUND);
    // return;
    // }
    //            
    // try {
    // uploader.setFileDesc(fd);
    // } catch(IOException ioe) {
    // if(LOG.isDebugEnabled())
    // LOG.debug(uploader + " could not create file stream "+ioe);
    // uploader.setState(Uploader.FILE_NOT_FOUND);
    // return;
    // }
    //
    // assertAsConnecting( uploader.getState() );
    // }
    // }

    /**
     * Sets the uploader's state based off values read in the headers.
     */
    private void setUploaderStateOffHeaders(HTTPUploader uploader) {
        FileDesc fd = uploader.getFileDesc();

        // If it's still trying to connect, do more checks ...
        if (uploader.getState() == Uploader.CONNECTING) {
            // If it's the wrong URN, File Not Found it.
            URN urn = uploader.getRequestedURN();
            if (fd != null && urn != null && !fd.containsUrn(urn)) {
                if (LOG.isDebugEnabled())
                    LOG.debug(uploader + " wrong content urn");
                uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }

            // handling THEX Requests
            if (uploader.isTHEXRequest()) {
                if (uploader.getFileDesc().getHashTree() != null)
                    uploader.setState(Uploader.THEX_REQUEST);
                else
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                return;
            }

            // Special handling for incomplete files...
            if (fd instanceof IncompleteFileDesc) {
                // Check to see if we're allowing PFSP.
                if (!UploadSettings.ALLOW_PARTIAL_SHARING.getValue()) {
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    return;
                }

                // cannot service THEXRequests for partial files
                if (uploader.isTHEXRequest()) {
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    return;
                }

                // If we are allowing, see if we have the range.
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                int upStart = uploader.getUploadBegin();
                // uploader.getUploadEnd() is exclusive!
                int upEnd = uploader.getUploadEnd() - 1;
                // If the request contained a 'Range:' header, then we can
                // shrink the request to what we have available.
                if (uploader.containedRangeRequest()) {
                    Interval request = ifd.getAvailableSubRange(upStart, upEnd);
                    if (request == null) {
                        uploader.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                    uploader
                            .setUploadBeginAndEnd(request.low, request.high + 1);
                } else {
                    if (!ifd.isRangeSatisfiable(upStart, upEnd)) {
                        uploader.setState(Uploader.UNAVAILABLE_RANGE);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Adds an accepted HTTPUploader to the internal list of active downloads.
     */
    protected synchronized void addAcceptedUploader(HTTPUploader uploader) {
        if (uploader.isForcedShare())
            _forcedUploads++;
        _activeUploadList.add(uploader);
    }

    /**
     * Adds this upload to the GUI and increments the attempted uploads. Does
     * nothing if 'shouldShowInGUI' is false.
     */
    private void addToGUI(HTTPUploader uploader) {

        // We want to increment attempted only for uploads that may
        // have a chance of failing.
        UploadStat.ATTEMPTED.incrementStat();

        // We are going to notify the gui about the new upload, and let
        // it decide what to do with it - will act depending on it's
        // state
        RouterService.getCallback().addUpload(uploader);

        if (!uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null) {
                fd.incrementAttemptedUploads();
                RouterService.getCallback()
                        .handleSharedFileUpdate(fd.getFile());
            }
        }
    }

    protected void updateStatistics(Uploader uploader) throws IOException {
        switch (uploader.getState()) {
        case Uploader.UNAVAILABLE_RANGE:
            UploadStat.UNAVAILABLE_RANGE.incrementStat();
            break;
        case Uploader.FILE_NOT_FOUND:
            UploadStat.FILE_NOT_FOUND.incrementStat();
            break;
        case Uploader.FREELOADER:
            UploadStat.FREELOADER.incrementStat();
            break;
        case Uploader.LIMIT_REACHED:
            UploadStat.LIMIT_REACHED.incrementStat();
            break;
        case Uploader.QUEUED:
            UploadStat.QUEUED.incrementStat();
            break;
        case Uploader.BANNED_GREEDY:
            UploadStat.BANNED.incrementStat();
            break;
        case Uploader.CONNECTING:
            // uploader.setState(Uploader.UPLOADING);
            UploadStat.UPLOADING.incrementStat();
            break;
        case Uploader.THEX_REQUEST:
            UploadStat.THEX.incrementStat();
            break;
        case Uploader.COMPLETE:
        case Uploader.INTERRUPTED:
            Assert.that(false, "invalid state in doSingleUpload");
            break;
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
        return _activeUploadList.size() - _forcedUploads;
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
        return _hadSuccesfulUpload;
    }

    public synchronized boolean isConnectedTo(InetAddress addr) {
        if (slotManager.getNumUsersForHost(addr.getHostAddress()) > 0)
            return true;

        for (Uploader uploader : _activeUploadList) {
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
        for (Uploader uploader : _activeUploadList) {
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
     * Always accepts Browse Host requests, though. Notifies callback of this.
     * 
     * @return ACCEPTED if the download may proceed, QUEUED if this is in the
     *         upload queue, REJECTED if this is flat-out disallowed (and hence
     *         not queued) and BANNED if the downloader is hammering us, and
     *         BYPASS_QUEUE if this is a File-View request that isn't hammering
     *         us. If REJECTED, <tt>uploader</tt>'s state will be set to
     *         LIMIT_REACHED. If BANNED, the <tt>Uploader</tt>'s state will
     *         be set to BANNED_GREEDY.
     * @exception IOException the request came sooner than allowed by upload
     *            queueing rules. (Throwing IOException forces the connection to
     *            be closed by the calling code.)
     */
    private synchronized int checkAndQueue(UploadSession session)
            throws IOException {
        RequestCache rqc = (RequestCache) REQUESTS.get(session.getHost());
        if (rqc == null)
            rqc = new RequestCache();
        // make sure we don't forget this RequestCache too soon!
        REQUESTS.put(session.getHost(), rqc);
        rqc.countRequest();
        if (rqc.isHammering()) {
            if (LOG.isWarnEnabled())
                LOG.warn(session.getUploader() + " banned.");
            return BANNED;
        }

        FileDesc fd = session.getUploader().getFileDesc();
        if (!fd.isVerified()) // spawn a validation
            RouterService.getFileManager().validate(fd);

        URN sha1 = fd.getSHA1Urn();

        if (rqc.isDupe(sha1))
            return REJECTED;

        // check the host limit unless this is a poll
        if (slotManager.positionInQueue(session) == -1
                && hostLimitReached(session.getHost())) {
            if (LOG.isDebugEnabled())
                LOG.debug("host limit reached for " + session.getHost());
            UploadStat.LIMIT_REACHED_GREEDY.incrementStat();
            return REJECTED;
        }

        int queued = slotManager.pollForSlot(session, session.getUploader()
                .supportsQueueing(), session.getUploader().isPriorityShare());

        if (LOG.isDebugEnabled())
            LOG.debug("queued at " + queued);

        if (queued == -1) // not accepted nor queued.
            return REJECTED;

        if (queued > 0 && session.poll()) {
            slotManager.cancelRequest(session);
            throw new IOException("came back too soon");
        }
        if (queued > 0)
            return QUEUED;
        else {
            rqc.startedUpload(sha1);
            return ACCEPTED;
        }
    }

    public int getPositionInQueue(HTTPSession session) {
        // return _queuedUploads.indexOf(session);
        return slotManager.positionInQueue(session);
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
    private synchronized void removeFromList(Uploader uploader) {
        // if the uploader is not in the active list, we should not
        // try remove the urn from the map of unique uploaded files for that
        // host.

        if (_activeUploadList.remove(uploader)) {
            if (((HTTPUploader) uploader).isForcedShare())
                _forcedUploads--;

            // at this point it is safe to allow other uploads from the same
            // host
            RequestCache rcq = (RequestCache) REQUESTS.get(uploader.getHost());

            // check for nulls so that unit tests pass
            if (rcq != null && uploader != null
                    && uploader.getFileDesc() != null)
                rcq.uploadDone(uploader.getFileDesc().getSHA1Urn());
        }

        // Enable auto shutdown
        if (_activeUploadList.size() == 0)
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

    // //////////////// Bandwith Allocation and Measurement///////////////

    /**
     * calculates the appropriate burst size for the allocating bandwith on the
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
        speeds.add(new Integer(bandwidth));

        // Update maximum speed if possible. This should be atomic. TODO: can
        // the compiler replace the temporary variable max with highestSpeed?
        if (speeds.size() >= MIN_SPEED_SAMPLE_SIZE) {
            int max = 0;
            for (int i = 0; i < speeds.size(); i++)
                max = Math.max(max, ((Integer) speeds.get(i)).intValue());
            this.highestSpeed = max;
        }
    }

    /**
     * @return the bandwidth for uploads in bytes per second
     */
    public float getUploadSpeed() {
        // if the user chose not to limit his uploads
        // by setting the upload speed to unlimited
        // set the upload speed to 3.4E38 bytes per second.
        // This is de facto not limiting the uploads
        int uSpeed = UploadSettings.UPLOAD_SPEED.getValue();
        float ret = (uSpeed == 100) ? Float.MAX_VALUE :
        // if the uploads are limited, take messageUpstream
                // for ultrapeers into account, - don't allow lower
                // speeds than 1kb/s so uploads won't stall completely
                // if the user accidently sets his connection speed
                // lower than his message upstream
                Math.max(
                        // connection speed is in kbits per second
                        ConnectionSettings.CONNECTION_SPEED.getValue() / 8.f
                                // upload speed is in percent
                                * uSpeed
                                / 100.f
                                // reduced upload speed if we are an ultrapeer
                                - RouterService.getConnectionManager()
                                        .getMeasuredUpstreamBandwidth() * 1.f,
                        1.f)
                // we need bytes per second
                * 1024;
        return ret;
    }

    public void measureBandwidth() {
        slotManager.measureBandwidth();
        for (Uploader forced : forceAllowedUploads) {
            forced.measureBandwidth();
        }
    }

    public float getMeasuredBandwidth() {
        float bw = 0;
        try {
            bw += slotManager.getMeasuredBandwidth();
        } catch (InsufficientDataException notEnough) {
        }
        
        for (Uploader forced : forceAllowedUploads) {
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
     * This class keeps track of client requests.
     */
    private static class RequestCache {
        // we don't allow more than 1 request per 5 seconds
        private static final double MAX_REQUESTS = 5 * 1000;

        // time we expect the downloader to wait before sending
        // another request after our initial LIMIT_REACHED reply
        // must be greater than or equal to what we send in our RetryAfter
        // header, otherwise we'll incorrectly mark guys as greedy.
        static long WAIT_TIME = LimitReachedUploadState.RETRY_AFTER_TIME * 1000;

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
         * tells the cache that an upload to the host has started.
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
         * checks whether the given URN is a duplicate request
         */
        boolean isDupe(URN sha1) {
            return ACTIVE_UPLOADS.contains(sha1);
        }

        /**
         * informs the request cache that the given URN is no longer actively
         * uploaded.
         */
        void uploadDone(URN sha1) {
            ACTIVE_UPLOADS.remove(sha1);
        }
    }
}
