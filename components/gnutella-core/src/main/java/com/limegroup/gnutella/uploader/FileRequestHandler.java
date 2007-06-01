package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.BasicHeaderProcessor;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.AltLocHeaderInterceptor;
import com.limegroup.gnutella.http.FeatureHeaderInterceptor;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.uploader.FileRequestParser.FileRequest;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager.QueueStatus;

/**
 * Handles upload requests for files and THEX trees.
 *   
 * @see FileResponseEntity
 * @see THEXResponseEntity
 */
public class FileRequestHandler implements HttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(FileRequestHandler.class);

    /**
     * Constant for the amount of time to wait before retrying if we are not
     * actively downloading this file. (1 hour)
     * <p>
     * The value is meant to be used only as a suggestion to when newer ranges
     * may be available if we do not have any ranges that the downloader may
     * want.
     */
    private static final String INACTIVE_RETRY_AFTER = "" + (60 * 60);

    private HTTPUploadSessionManager sessionManager;

    private FileManager fileManager = RouterService.getFileManager();
    
    public FileRequestHandler(HTTPUploadSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }
    
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling upload request: "
                    + request.getRequestLine().getUri());

        FileRequest fileRequest = null;
        HTTPUploader uploader = null;

        // parse request
        try {
            String uri = request.getRequestLine().getUri();
            if (FileRequestParser.isURNGet(uri)) {
                fileRequest = FileRequestParser.parseURNGet(fileManager, uri);
                if (fileRequest == null) {
                    uploader = sessionManager.getOrCreateUploader(request,
                            context, UploadType.INVALID_URN,
                            "Invalid URN query");
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                }
            } else {
                fileRequest = FileRequestParser.parseTraditionalGet(uri);
                assert fileRequest != null;
            }
        } catch (IOException e) {
            uploader = sessionManager.getOrCreateUploader(request, context,
                    UploadType.MALFORMED_REQUEST, "Malformed Request");
            handleMalformedRequest(response, uploader);
        }

        // process request
        if (fileRequest != null) {
            FileDesc fd = getFileDesc(fileRequest);
            if (fd != null) {
                uploader = findFileAndProcessHeaders(request, response,
                        context, fileRequest, fd);
            } else {
                uploader = sessionManager.getOrCreateUploader(request, context,
                        UploadType.SHARED_FILE, fileRequest.filename);
                uploader.setState(Uploader.FILE_NOT_FOUND);
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        }

        assert uploader != null;
        updateStatistics(uploader);
        
        sessionManager.sendResponse(uploader, response);
    }

    /**
     * Looks up file in {@link FileManager} and processes request headers.
     */
    private HTTPUploader findFileAndProcessHeaders(HttpRequest request,
            HttpResponse response, HttpContext context,
            FileRequest fileRequest, FileDesc fd) throws IOException,
            HttpException {
        // create uploader
        UploadType type = (FileManager.isForcedShare(fd)) ? UploadType.FORCED_SHARE
                : UploadType.SHARED_FILE;
        HTTPUploader uploader = sessionManager.getOrCreateUploader(request,
                context, type, fd.getFileName());
        uploader.setFileDesc(fd);

        if (fileRequest.isThexRequest()) {
            // XXX reset range to size of THEX tree
            int outputLength = fd.getHashTree().getOutputLength();
            // XXX the setFileSize() is confusing when uploading THEX trees
            uploader.setFileSize(outputLength);
            uploader.setUploadBegin(0);
            uploader.setUploadEnd(outputLength);
        }

        // process headers
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
        processor.addInterceptor(new FeatureHeaderInterceptor(uploader));
        processor.addInterceptor(new AltLocHeaderInterceptor(uploader));
        if (!uploader.getFileName().toUpperCase().startsWith("LIMEWIRE")) {
            processor.addInterceptor(new UserAgentHeaderInterceptor(uploader));
        }
        try {
            processor.process(request, context);
        } catch (ProblemReadingHeaderException e) {
            handleMalformedRequest(response, uploader);
            return uploader;
        }

        if (UserAgentHeaderInterceptor.isFreeloader(uploader.getUserAgent())) {
            sessionManager.handleFreeLoader(request, response, context,
                    uploader);
            return uploader;
        }

        if (!validateHeaders(uploader, fileRequest.isThexRequest())) {
            uploader.setState(Uploader.FILE_NOT_FOUND);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return uploader;
        }

        if (!uploader.validateRange()) {
            handleInvalidRange(response, uploader, fd);
            return uploader;
        }

        // start upload
        if (fileRequest.isThexRequest()) {
            handleTHEXRequest(request, response, context, uploader, fd);
        } else {
            handleFileUpload(context, request, response, uploader, fd);
        }

        return uploader;
    }

    private void handleMalformedRequest(HttpResponse response,
            HTTPUploader uploader) {
        UploadStat.MALFORMED_REQUEST.incrementStat();

        uploader.setState(Uploader.MALFORMED_REQUEST);
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        response.setReasonPhrase("Malformed Request");
    }

    /**
     * Enqueues <code>request</code> and handles <code>uploader</code>
     * in respect to the returned queue status.
     */
    private void handleFileUpload(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd)
            throws IOException, HttpException {
        if (!uploader.getSession().isAccepted()) {
            QueueStatus queued = sessionManager.enqueue(context, request);
            switch (queued) {
            case REJECTED:
                new LimitReachedRequestHandler(uploader).handle(request,
                        response, context);
                break;
            case BANNED:
                uploader.setState(Uploader.BANNED_GREEDY);
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                response.setReasonPhrase("Banned");
                break;
            case QUEUED:
                handleQueued(context, request, response, uploader, fd);
                break;
            case ACCEPTED:
                sessionManager.addAcceptedUploader(uploader, context);
                break;
            case BYPASS: // ignore
            }
        }

        if (uploader.getSession().canUpload()) {
            handleAccept(context, request, response, uploader, fd);
        }
    }

    /**
     * Processes an accepted file upload by adding headers and setting the entity.
     */
    protected void handleAccept(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd)
            throws IOException, HttpException {

        response
                .addHeader(HTTPHeaderName.DATE.create(HTTPUtils.getDateValue()));
        response.addHeader(HTTPHeaderName.CONTENT_DISPOSITION
                .create("attachment; filename=\""
                        + HTTPUtils.encode(uploader.getFileName(), "US-ASCII")
                        + "\""));

        if (uploader.containedRangeRequest()) {
            // uploadEnd is an EXCLUSIVE index internally, but HTTP uses an
            // INCLUSIVE index.
            String value = "bytes " + uploader.getUploadBegin() + "-"
                    + (uploader.getUploadEnd() - 1) + "/"
                    + uploader.getFileSize();
            response.addHeader(HTTPHeaderName.CONTENT_RANGE.create(value));
        }

        HTTPHeaderUtils.addAltLocationsHeader(response, uploader, fd);
        HTTPHeaderUtils.addRangeHeader(response, uploader, fd);
        HTTPHeaderUtils.addProxyHeader(response);

        if (fd != null) {
            URN urn = fd.getSHA1Urn();
            if (uploader.isFirstReply()) {
                // write the creation time if this is the first reply.
                // if this is just a continuation, we don't need to send
                // this information again.
                // it's possible t do that because we don't use the same
                // uploader for different files
                CreationTimeCache cache = CreationTimeCache.instance();
                if (cache.getCreationTime(urn) != null) {
                    response.addHeader(HTTPHeaderName.CREATION_TIME
                            .create(cache.getCreationTime(urn).toString()));
                }
            }
        }

        // write x-features header once because the downloader is
        // supposed to cache that information anyway
        if (uploader.isFirstReply()) {
            HTTPHeaderUtils.addFeatures(response);
        }

        // write X-Thex-URI header with root hash if we have already
        // calculated the tigertree
        if (fd.getHashTree() != null) {
            response
                    .addHeader(HTTPHeaderName.THEX_URI.create(fd.getHashTree()));
        }

        response.setEntity(new FileResponseEntity(uploader, fd.getFile()));
        uploader.setState(Uploader.UPLOADING);

        if (uploader.isPartial()) {
            response.setStatusCode(HttpStatus.SC_PARTIAL_CONTENT);
        } else {
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * Processes an accepted THEX tree upload by adding headers and setting the
     * entity.
     */
    private void handleTHEXRequest(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader, FileDesc fd)
            throws HttpException, IOException {
        // reset the poll interval to allow subsequent requests right away
        uploader.getSession().updatePollTime(QueueStatus.BYPASS);
        
        // do not count THEX transfers towards the 
        uploader.setIgnoreTotalAmountUploaded(true);
        
        HashTree tree = fd.getHashTree();
        assert tree != null;

        // see CORE-174
        // response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd.getSHA1Urn()));

        uploader.setState(Uploader.THEX_REQUEST);
        response.setEntity(new THEXResponseEntity(uploader, tree, uploader.getFileSize()));
        response.setStatusCode(HttpStatus.SC_OK);
    }

    /**
     * Processes a request for an invalid range.
     */
    private void handleInvalidRange(HttpResponse response,
            HTTPUploader uploader, FileDesc fd) {
        uploader.getAltLocTracker().addAltLocHeaders(response);
        HTTPHeaderUtils.addRangeHeader(response, uploader, fd);
        HTTPHeaderUtils.addProxyHeader(response);

        if (fd instanceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
            if (!ifd.isActivelyDownloading()) {
                response.addHeader(HTTPHeaderName.RETRY_AFTER
                        .create(INACTIVE_RETRY_AFTER));
            }
        }

        uploader.setState(Uploader.UNAVAILABLE_RANGE);
        response.setStatusCode(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setReasonPhrase("Requested Range Unavailable");
    }

    /**
     * Processes a queued file upload by adding headers.
     */
    private void handleQueued(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd)
            throws IOException, HttpException {
        // if not queued, this should never be the state
        int position = uploader.getSession().positionInQueue();
        Assert.that(position != -1);

        String value = "position=" + (position + 1) + ", pollMin="
                + (HTTPUploadSession.MIN_POLL_TIME / 1000) + /* mS to S */
                ", pollMax=" + (HTTPUploadSession.MAX_POLL_TIME / 1000) /* mS to S */;
        response.addHeader(HTTPHeaderName.QUEUE.create(value));

        HTTPHeaderUtils.addAltLocationsHeader(response, uploader, fd);
        HTTPHeaderUtils.addRangeHeader(response, uploader, fd);

        if (uploader.isFirstReply()) {
            HTTPHeaderUtils.addFeatures(response);
        }

        // write X-Thex-URI header with root hash if we have already
        // calculated the tigertree
        if (fd.getHashTree() != null) {
            response
                    .addHeader(HTTPHeaderName.THEX_URI.create(fd.getHashTree()));
        }

        response.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);

        uploader.setState(Uploader.QUEUED);
        response.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    /**
     * Returns the description for the file requested by <code>request</code>.
     * 
     * @return null, if <code>request</code> does not map to a file
     */
    private FileDesc getFileDesc(FileRequest request) {
        FileDesc fd = null;

        int index = request.index;

        // first verify the file index
        synchronized (fileManager) {
            if (fileManager.isValidIndex(index)) {
                fd = fileManager.get(index);
            }
        }

        if (fd == null) {
            // if (LOG.isDebugEnabled())
            // LOG.debug(uploader + " fd is null");
            return null;
        }

        if (!request.filename.equals(fd.getFileName())) {
            if (LOG.isDebugEnabled())
                LOG.debug("Wrong file name in request: " + request);
            return null;
        }

        return fd;
    }

    private boolean validateHeaders(HTTPUploader uploader, boolean thexRequest) {
        FileDesc fd = uploader.getFileDesc();

        // If it's the wrong URN, File Not Found it.
        URN urn = uploader.getRequestedURN();
        if (fd != null && urn != null && !fd.containsUrn(urn)) {
            if (LOG.isDebugEnabled())
                LOG.debug(uploader + " wrong content urn");
            return false;
        }

        // handling THEX Requests
        if (thexRequest && uploader.getFileDesc().getHashTree() == null) {
            return false;
        }

        // Special handling for incomplete files...
        if (fd instanceof IncompleteFileDesc) {
            // Check to see if we're allowing PFSP.
            if (!UploadSettings.ALLOW_PARTIAL_SHARING.getValue()) {
                return false;
            }

            // cannot service THEXRequests for partial files
            if (thexRequest) {
                return false;
            }
        }

        return true;
    }

    protected void updateStatistics(HTTPUploader uploader) {
        switch (uploader.getState()) {
        case HTTPUploader.UNAVAILABLE_RANGE:
            UploadStat.UNAVAILABLE_RANGE.incrementStat();
            break;
        case HTTPUploader.FILE_NOT_FOUND:
            UploadStat.FILE_NOT_FOUND.incrementStat();
            break;
        case HTTPUploader.LIMIT_REACHED:
            UploadStat.LIMIT_REACHED.incrementStat();
            break;
        case HTTPUploader.QUEUED:
            UploadStat.QUEUED.incrementStat();
            break;
        case HTTPUploader.BANNED_GREEDY:
            UploadStat.BANNED.incrementStat();
            break;
        case HTTPUploader.CONNECTING:
            UploadStat.UPLOADING.incrementStat();
            break;
        case HTTPUploader.THEX_REQUEST:
            UploadStat.THEX.incrementStat();
            break;
        case HTTPUploader.COMPLETE:
        case HTTPUploader.INTERRUPTED:
            Assert.that(false,
                    "Invalid state in FileRequestHandler.updateStatistics()");
            break;
        }
    }

}
