package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.http.BasicHeaderProcessor;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.connection.BasicQueue;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.http.AltLocHeaderInterceptor;
import com.limegroup.gnutella.http.FeatureHeaderInterceptor;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.UploadStat;
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
        HTTPUploader uploader;
        FileRequest fileRequest;

        // parse request
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
            if (fileRequest == null) {
                uploader = sessionManager.getOrCreateUploader(context,
                        UploadType.MALFORMED_REQUEST, "Malformed Request");
                uploader.setState(Uploader.MALFORMED_REQUEST);
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            }
        }

        // process request
        if (fileRequest != null) {
            FileDesc fd = getFileDesc(fileRequest);
            if (fd != null) {
                findFileAndProcessHeaders(request, response, context,
                        fileRequest, fd);
            } else {
                uploader = sessionManager.getOrCreateUploader(context,
                        UploadType.MALFORMED_REQUEST, "Malformed Request");
                uploader.setState(Uploader.MALFORMED_REQUEST);
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
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
            uploader.setState(Uploader.MALFORMED_REQUEST);
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        } catch (FreeloaderUploadingException e) {
            handleFreeLoader(request, response, context, uploader);
        }

        if (!validateHeaders(uploader)) {
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

    private void handleFileUpload(HttpContext context, HttpRequest request,
            HttpResponse response, HTTPUploader uploader, FileDesc fd) {
        response.setEntity(new FileResponseEntity(uploader));
        sessionManager.enqueue(context, request, response);
    }

    private void handleTHEXRequest(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader, FileDesc fd) throws HttpException, IOException {
        new THEXRequestHandler(uploader, fd).handle(request, response, context);
    }

    private void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException, IOException {
        uploader.setState(Uploader.FREELOADER);
        new FreeLoaderRequestHandler().handle(request, response, context);
    }

    private void handleInvalidRange(HttpResponse response,
            HTTPUploader uploader, FileDesc fd) {
        uploader.getAltLocTracker().addAltLocHeaders(response);
        addRangeHeader(response, uploader, fd);
        addProxyHeader(response);

        if (fd instanceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
            if (!ifd.isActivelyDownloading()) {
                response.addHeader(HTTPHeaderName.RETRY_AFTER
                        .create(INACTIVE_RETRY_AFTER));
            }
        }

        uploader.setState(Uploader.UNAVAILABLE_RANGE);
        response.setStatusCode(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private void addRangeHeader(HttpResponse response, HTTPUploader uploader,
            FileDesc fd) {
        if (fd instanceof IncompleteFileDesc) {
            URN sha1 = uploader.getFileDesc().getSHA1Urn();
            if (sha1 != null) {
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                response.addHeader(HTTPHeaderName.AVAILABLE_RANGES.create(ifd));
            }
        }
    }

    /**
     * Writes out the X-Push-Proxies header as specified by section 4.2 of the
     * Push Proxy proposal, v. 0.7
     */
    private void addProxyHeader(HttpResponse response) {
        if (RouterService.acceptedIncomingConnection())
            return;

        Set<IpPort> proxies = RouterService.getConnectionManager()
                .getPushProxies();

        StringBuilder buf = new StringBuilder();
        int proxiesWritten = 0;
        for (Iterator<IpPort> iter = proxies.iterator(); iter.hasNext()
                && proxiesWritten < 4;) {
            IpPort current = iter.next();
            buf.append(current.getAddress()).append(":").append(
                    current.getPort()).append(",");

            proxiesWritten++;
        }

        if (proxiesWritten > 0)
            buf.deleteCharAt(buf.length() - 1);
        else
            return;

        response.addHeader(HTTPHeaderName.PROXIES.create(buf.toString()));
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
     */
    private FileRequest parseURNGet(final String requestLine)
            throws IOException {
        URN urn = URN.createSHA1UrnFromHttpRequest(requestLine);

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
            int f = requestLine.indexOf(" HTTP/", d);
            try {
                fileName = URLDecoder.decode(requestLine.substring((d + 1), f));
            } catch (IllegalArgumentException e) {
                fileName = requestLine.substring((d + 1), f);
            }
            UploadStat.TRADITIONAL_GET.incrementStat();

            return new FileRequest(index, fileName);
        } catch (NumberFormatException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
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

    private boolean validateHeaders(HTTPUploader uploader) {
        FileDesc fd = uploader.getFileDesc();
        // If it's the wrong URN, File Not Found it.
        URN urn = uploader.getRequestedURN();
        if (fd != null && urn != null && !fd.containsUrn(urn)) {
            if (LOG.isDebugEnabled())
                LOG.debug(uploader + " wrong content urn");
            return false;
        }

        // handling THEX Requests
        if (uploader.isTHEXRequest()) {
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
            if (uploader.isTHEXRequest()) {
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

        String serviceID;

    }

    private class FileResponseEntity extends AbstractHttpNIOEntity {

        private HTTPUploader uploader;

        private QueryRequest query;

        private Iterator<Response> iterable;

        MessageWriter sender;

        public FileResponseEntity(HTTPUploader uploader) {
            this.uploader = uploader;

            SentMessageHandler sentMessageHandler = new SentMessageHandler() {
                public void processSentMessage(Message m) {
                    // TODO update progress
                }
            };

            sender = new MessageWriter(new ConnectionStats(), new BasicQueue(),
                    sentMessageHandler);
            sender.setWriteChannel(this);

            // XXX LW can't actually handle chunked responses
            setChunked(true);

            query = QueryRequest.createBrowseHostQuery();
            iterable = RouterService.getFileManager().getIndexingIterator(
                    query.desiresXMLResponses()
                            || query.desiresOutOfBandReplies());
        }

        @Override
        public boolean handleWrite() throws IOException {
            addMessages();

            boolean more = sender.handleWrite();
            return more || iterable.hasNext();
        }

        private void addMessages() {
            List<Response> responses = new ArrayList<Response>(10);
            for (int i = 0; iterable.hasNext() && i < 10; i++) {
                responses.add(iterable.next());
            }

            Iterable<QueryReply> it = RouterService.getMessageRouter()
                    .responsesToQueryReplies(
                            responses.toArray(new Response[0]), query);

            for (QueryReply queryReply : it) {
                sender.send(queryReply);
            }
        }

    }

}
