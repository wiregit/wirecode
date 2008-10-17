package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentEncoderChannel;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HttpCoreUtils;
import org.limewire.http.entity.AbstractProducingNHttpEntity;
import org.limewire.nio.channel.NoInterestWritableByteChannel;

import com.google.inject.Provider;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.connection.BasicQueue;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

/**
 * Responds to Gnutella browse requests by returning a list of all shared files.
 * Only supports the application/x-gnutella-packets mime-type, browsing through
 * HTML is not supported.
 */
public class BrowseRequestHandler extends SimpleNHttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(BrowseRequestHandler.class);
    
    private final HTTPUploadSessionManager sessionManager;
    private final Provider<ResponseFactory> responseFactory;
    private final OutgoingQueryReplyFactory outgoingQueryReplyFactory;
    /**
     * This is set to true as default while old clients still don't send 
     * the request header correctly. Will be set to false in the future.
     */
    private boolean requestorCanDoFWT = true;

    private final HttpRequestFileListProvider browseRequestFileListProvider;

    BrowseRequestHandler(HTTPUploadSessionManager sessionManager,
            Provider<ResponseFactory> responseFactory,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            HttpRequestFileListProvider browseRequestFileListProvider) {
        this.sessionManager = sessionManager;
        this.responseFactory = responseFactory;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
        this.browseRequestFileListProvider = browseRequestFileListProvider;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        
        HTTPUploader uploader = sessionManager.getOrCreateUploader(request,
                context, UploadType.BROWSE_HOST, "Browse-File");
        uploader.setState(UploadStatus.BROWSE_HOST);
        
        if (request.getHeaders(HTTPHeaderName.FW_NODE_INFO.name()).length > 0) {
            requestorCanDoFWT = true;
        }
        
        try {
            FileList fileList = browseRequestFileListProvider.getFileList(request, context);
            if (!HttpCoreUtils.hasHeaderListValue(request, "Accept", Constants.QUERYREPLY_MIME_TYPE)) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Browse request is missing Accept header");
                
                response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
            } else {
                response.setEntity(new BrowseResponseEntity(uploader, fileList));
                response.setStatusCode(HttpStatus.SC_OK);
            }
        } catch (com.limegroup.gnutella.uploader.HttpException he) {
            response.setStatusCode(he.getErrorCode());
            response.setReasonPhrase(he.getMessage());
        }
        
        sessionManager.sendResponse(uploader, response);
    }

    public class BrowseResponseEntity extends AbstractProducingNHttpEntity {

        private static final int RESPONSES_PER_REPLY = 10;
        
        private static final int MAX_PENDING_REPLIES = 5;

        private final HTTPUploader uploader;

        private Iterator<FileDesc> iterator;
        
        private MessageWriter sender;
        
        private volatile int pendingMessageCount = 0;

        private GUID sessionGUID = new GUID();

        public BrowseResponseEntity(HTTPUploader uploader, FileList files) {
            this.uploader = uploader;
            // getting all file descs creates a copy, so we can thread-safely iterate
            // not ideal memorywise though
            iterator = files.getAllFileDescs().iterator();

            // XXX LW can't handle chunked responses: CORE-199
            //setChunked(true);
            
            setContentType(Constants.QUERYREPLY_MIME_TYPE);
        }

        @Override
        public long getContentLength() {
            return -1;
        }
        
        @Override
        public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
            SentMessageHandler sentMessageHandler = new SentMessageHandler() {
                public void processSentMessage(Message m) {
                    uploader.addAmountUploaded(m.getTotalLength());
                    pendingMessageCount--;
                }                
            };
            
            sender = new MessageWriter(new ConnectionStats(), new BasicQueue(), sentMessageHandler);
            sender.setWriteChannel(new NoInterestWritableByteChannel(new ContentEncoderChannel(
                    contentEncoder)));
        }
        
        @Override
        public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
            addMessages();
            
            boolean more = sender.handleWrite();
            assert more || pendingMessageCount == 0;
            
            activateTimeout();
            return more || iterator.hasNext();
        }
        
        /**
         * Adds a query reply with {@link #RESPONSES_PER_REPLY} responses to the
         * message queue.
         */
        private void addMessages() {
            if (pendingMessageCount >= MAX_PENDING_REPLIES) {
                return;
            }
            
            List<Response> responses = new ArrayList<Response>(RESPONSES_PER_REPLY); 
            for (int i = 0; iterator.hasNext() && i < RESPONSES_PER_REPLY; i++) {
                responses.add(responseFactory.get().createResponse(iterator.next()));
            }
            
            Iterable<QueryReply> it = outgoingQueryReplyFactory.createReplies(responses.toArray(new Response[0]),
                    10, null, sessionGUID.bytes(), (byte)1, false, requestorCanDoFWT);
            
            for (QueryReply queryReply : it) {
                sender.send(queryReply);
                pendingMessageCount++;
            }
        }

        public void finish() {
            deactivateTimeout();
            sender = null;
        }

        @Override
        public void timeout() {
            if (LOG.isDebugEnabled())
                LOG.debug("Browse request timed out");

            uploader.stop();
        }
        
    }

}
