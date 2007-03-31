package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.http.BasicHeaderProcessor;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.AltLocHeaderInterceptor;
import com.limegroup.gnutella.http.FeatureHeaderInterceptor;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager.QueueStatus;
import com.limegroup.gnutella.util.URLDecoder;

public class FileRequestHandler implements HttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(FileRequestHandler.class);

    /**
     * Constant for the amount of time to wait before retrying if we are not
     * actively downloading this file. (1 hour)
     * 
     * The value is meant to be used only as a suggestion to when newer ranges
     * may be available if we do not have any ranges that the downloader may
     * want.
     */
    private static final String INACTIVE_RETRY_AFTER = "" + (60 * 60);

    private HTTPUploadSessionManager sessionManager;

    public FileRequestHandler(HTTPUploadSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        FileRequest fileRequest = null;
        HTTPUploader uploader;
        
        // parse request
        try {
            String uri = request.getRequestLine().getUri();
            if (isURNGet(uri)) {
                fileRequest = parseURNGet(uri);
                if (fileRequest == null) {
                    uploader = sessionManager.getOrCreateUploader(context,
                            UploadType.INVALID_URN, "Invalid URN query");
                    uploader.setState(Uploader.FILE_NOT_FOUND);
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                }
            } else {
                fileRequest = parseTraditionalGet(uri);
                assert fileRequest != null;
            }
        } catch (IOException e) {
            handleMalformedRequest(response, sessionManager.getOrCreateUploader(context,
                    UploadType.MALFORMED_REQUEST, "Malformed Request"));
        }
        
        // process request
        if (fileRequest != null) {
            FileDesc fd = getFileDesc(fileRequest);
            if (fd != null) {
                findFileAndProcessHeaders(request, response, context,
                        fileRequest, fd);
            } else {
                handleMalformedRequest(response, sessionManager.getOrCreateUploader(context,
                        UploadType.MALFORMED_REQUEST, "Malformed Request"));
            }
        }
    }

    private void findFileAndProcessHeaders(HttpRequest request,
            HttpResponse response, HttpContext context,
            FileRequest fileRequest, FileDesc fd) throws IOException,
            HttpException {
        // create uploader
        UploadType type = (FileManager.isForcedShare(fd)) ? UploadType.FORCED_SHARE
                : UploadType.SHARED_FILE;
        HTTPUploader uploader = sessionManager.getOrCreateUploader(context,
                type, fd.getFileName());
        uploader.setFileDesc(fd);
        uploader.setIndex(fileRequest.index);
        uploader.setState(Uploader.CONNECTING);

        boolean thexRequest = HTTPConstants.NAME_TO_THEX.equals(fileRequest.serviceID); 
        if (thexRequest) {
            // XXX reset range to size of THEX tree
            uploader.setUploadBegin(0);
            uploader.setUploadEnd(fd.getHashTree().getOutputLength());
        }
        
        // process headers
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
        processor.addInterceptor(new FeatureHeaderInterceptor(uploader));
        processor.addInterceptor(new AltLocHeaderInterceptor(uploader));
        if (!uploader.getFileName().startsWith("LIMEWIRE")) {
            processor.addInterceptor(new UserAgentHeaderInterceptor(uploader));
        }
        try {
            processor.process(request, context);
        } catch (ProblemReadingHeaderException e) {
            handleMalformedRequest(response, uploader);
            return;
        } catch (FreeloaderUploadingException e) {
            handleFreeLoader(request, response, context, uploader);
        }

        if (!validateHeaders(uploader, thexRequest)) {
            uploader.setState(Uploader.FILE_NOT_FOUND);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return;
        }

        if (!uploader.validateRange()) {
            handleInvalidRange(response, uploader, fd);
            return;
        }

        // start upload
        if (uploader.getState() == Uploader.THEX_REQUEST) {
            handleTHEXRequest(request, response, context, uploader, fd);
        } else {
            handleFileUpload(context, request, response, uploader, fd);
        }
    }

    private void handleMalformedRequest(HttpResponse response, HTTPUploader uploader) {
        uploader.setState(Uploader.MALFORMED_REQUEST);
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        response.setReasonPhrase("Malformed Request");
    }

    private void handleFileUpload(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd)
            throws IOException, HttpException {
        if (!uploader.isAccepted()) {
            QueueStatus queued = sessionManager.enqueue(context, request,
                    response);
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
                System.out.println("queued");
                handleQueued(context, request, response, uploader, fd);
                break;
            case ACCEPTED:
            case BYPASS: // TODO reset session poll state?
                sessionManager.addAcceptedUploader(uploader);
                uploader.setAccepted(true);
                break;
            }

        }

        if (uploader.isAccepted()) {
            handleAccept(context, request, response, uploader, fd);
        }
    }

    private void handleAccept(HttpContext context, HttpRequest request,
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

        response.setEntity(new FileResponseEntity(uploader, fd));
        uploader.setState(Uploader.CONNECTING);

        if (uploader.getUploadEnd() - uploader.getUploadBegin() < uploader
                .getFileSize()) {
            response.setStatusCode(HttpStatus.SC_PARTIAL_CONTENT);
        } else {
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private void handleTHEXRequest(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader, FileDesc fd)
            throws HttpException, IOException {
        new THEXRequestHandler(uploader, fd).handle(request, response, context);
    }

    private void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException,
            IOException {
        uploader.setState(Uploader.FREELOADER);
        new FreeLoaderRequestHandler().handle(request, response, context);
    }

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

    private void handleQueued(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd)
            throws IOException, HttpException {
        // if not queued, this should never be the state
        int position = uploader.getSession().positionInQueue();
        Assert.that(position != -1);

        String value = "position=" + (position + 1) + ", pollMin="
                + (HTTPSession.MIN_POLL_TIME / 1000) + /* mS to S */
                ", pollMax=" + (HTTPSession.MAX_POLL_TIME / 1000) /* mS to S */;
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
     * Returns whether or not the get request for the specified line is a URN
     * request.
     * 
     * @param requestLine the <tt>String</tt> to parse to check whether it's
     *        following the URN request syntax as specified in HUGE v. 0.93
     * @return <tt>true</tt> if the request is a valid URN request,
     *         <tt>false</tt> otherwise
     */
    private boolean isURNGet(final String requestLine) {
        // int slash1Index = requestLine.indexOf("/");
        // int slash2Index = requestLine.indexOf("/", slash1Index+1);
        // if((slash1Index==-1) || (slash2Index==-1)) {
        // return false;
        // }
        // String idString = requestLine.substring(slash1Index+1, slash2Index);
        // return idString.equalsIgnoreCase("uri-res");
        return requestLine.startsWith("/uri-res/");
    }

    /**
     * Parses the get line for a URN request, throwing an exception if there are
     * any errors in parsing.
     * 
     * If we do not have the URN, we request a HttpRequestLine whose index is
     * BAD_URN_QUERY_INDEX. It is up to HTTPUploader to properly read the index
     * and set the state to FILE_NOT_FOUND.
     * 
     * @param requestLine the <tt>String</tt> instance containing the get
     *        request
     * @return a new <tt>RequestLine</tt> instance containing all of the data
     *         for the get request
     * @throws IOException
     */
    private FileRequest parseURNGet(final String requestLine)
            throws IOException {
        URN urn = URN.createSHA1UrnFromHttpRequest(requestLine + " HTTP/1.1");

        // Parse the service identifier, whether N2R, N2X or something
        // we cannot satisfy. URI scheme names are not case-sensitive.
        String serviceId;
        String requestUpper = requestLine.toUpperCase(Locale.US);
        if (requestUpper.indexOf(HTTPConstants.NAME_TO_THEX) > 0) {
            serviceId = HTTPConstants.NAME_TO_THEX;
        } else if (requestUpper.indexOf(HTTPConstants.NAME_TO_RESOURCE) > 0) {
            serviceId = HTTPConstants.NAME_TO_RESOURCE;
        } else {
            if (LOG.isWarnEnabled())
                LOG.warn("Invalid URN query: " + requestLine);
            return null;
        }

        FileDesc desc = RouterService.getFileManager().getFileDescForUrn(urn);
        if (desc == null) {
            UploadStat.UNKNOWN_URN_GET.incrementStat();
            return null;
        }

        UploadStat.URN_GET.incrementStat();
        return new FileRequest(desc.getIndex(), desc.getFileName(), serviceId);
    }

    /**
     * Performs the parsing for a traditional HTTP Gnutella get request,
     * returning a new <tt>RequestLine</tt> instance with the data for the
     * request.
     * 
     * @param requestLine the HTTP get request string
     * @return a new <tt>FileRequest</tt> instance for the request or
     *         <code>null</code> if the request is malformed
     */
    private FileRequest parseTraditionalGet(final String requestLine)
            throws IOException {
        try {
            int index = -1;

            // file information part: /get/0/sample.txt
            String fileName = null;

            int g = requestLine.indexOf("/get/");

            // find the next "/" after the "/get/", the number in between is the
            // index
            int d = requestLine.indexOf("/", (g + 5));

            // get the index
            String str_index = requestLine.substring((g + 5), d);
            index = java.lang.Integer.parseInt(str_index);
            // get the filename, which should be right after
            // the "/", and before the next " ".
            try {
                fileName = URLDecoder.decode(requestLine.substring(d + 1));
            } catch (IllegalArgumentException e) {
                fileName = requestLine.substring(d + 1);
            }
            UploadStat.TRADITIONAL_GET.incrementStat();

            return new FileRequest(index, fileName);
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
    }

    private FileDesc getFileDesc(FileRequest request) {
        FileManager fm = RouterService.getFileManager();
        FileDesc fd = null;

        int index = request.index;

        // first verify the file index
        synchronized (fm) {
            if (fm.isValidIndex(index)) {
                fd = fm.get(index);
            }
        }

        if (fd == null) {
            // if (LOG.isDebugEnabled())
            // LOG.debug(uploader + " fd is null");
            return null;
        }

        if (!request.filename.equals(fd.getFileName())) {
            // if (LOG.isDebugEnabled())
            // LOG.debug(uploader + " wrong file name");
            return null;
        }

        // try {
        // uploader.setFileDesc(fd);
        // } catch (IOException ioe) {
        // if (LOG.isDebugEnabled())
        // LOG.debug(uploader + " could not create file stream " + ioe);
        // uploader.setState(Uploader.FILE_NOT_FOUND);
        // return;
        // }

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
        if (thexRequest) {
            if (uploader.getFileDesc().getHashTree() != null) {
                uploader.setState(Uploader.THEX_REQUEST);
            } else {
                return false;
            }
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

    private class FileRequest {

        public FileRequest(int index, String filename, String serviceID) {
            this.index = index;
            this.filename = filename;
            this.serviceID = serviceID;
        }

        public FileRequest(int index, String filename) {
            this(index, filename, null);
        }

        String filename;

        int index;

        // TODO only used to determine if this is a THEX request, make this an enum?
        String serviceID;

    }

    private class FileResponseEntity extends AbstractHttpNIOEntity {

        private HTTPUploader uploader;

        private FileDesc fd;

        private ByteBuffer buffer;

        private long length;

        private long begin;

        public FileResponseEntity(HTTPUploader uploader, FileDesc fd) {
            this.uploader = uploader;
            this.fd = fd;

            setContentType(Constants.FILE_MIME_TYPE);
            
            begin = uploader.getUploadBegin();
            long end = uploader.getUploadEnd();
            length = end - begin;
        }

        @Override
        public long getContentLength() {
            return length;
        }

        @Override
        public boolean handleWrite() throws IOException {
            if (buffer == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = fd.createInputStream();
                int i;
                while ((i = in.read()) != -1) {
                    out.write(i);
                }

                buffer = ByteBuffer.wrap(out.toByteArray(),(int) begin, (int) length);
            }
            
            write(buffer);
            return buffer.hasRemaining();
        }

    }

}
