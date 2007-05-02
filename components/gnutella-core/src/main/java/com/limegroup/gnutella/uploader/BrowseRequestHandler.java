package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.http.HttpCoreUtils;
import org.limewire.nio.observer.WriteObserver;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.connection.BasicQueue;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.statistics.UploadStat;

/**
 * Responds to Gnutella browse requests. Only supports the
 * application/x-gnutella-packets mime-type, browsing through HTML is not
 * supported.
 */
public class BrowseRequestHandler implements HttpRequestHandler {

    private HTTPUploadSessionManager sessionManager;

    public BrowseRequestHandler(HTTPUploadSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        UploadStat.BROWSE_HOST.incrementStat();
        
        HTTPUploader uploader = sessionManager.getOrCreateUploader(request,
                context, UploadType.BROWSE_HOST, "Browse-File");
        uploader.setState(Uploader.BROWSE_HOST);
        
        if (!HttpCoreUtils.hasHeader(request, "Accept",
                Constants.QUERYREPLY_MIME_TYPE)) {
            response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
        } else {
            response.setEntity(new BrowseResponseEntity(uploader));
            response.setStatusCode(HttpStatus.SC_OK);
        }
        
        sessionManager.sendResponse(uploader, response);
    }

    public class BrowseResponseEntity extends AbstractHttpNIOEntity {

        private static final int RESPONSES_PER_REPLY = 10;

        private HTTPUploader uploader;

        private QueryRequest query;

        private Iterator<Response> iterable;

        MessageWriter sender;
        
        public BrowseResponseEntity(HTTPUploader uploader) {
            this.uploader = uploader;

            // XXX LW can't actually handle chunked responses
            //setChunked(true);
            setContentType(Constants.QUERYREPLY_MIME_TYPE);
        }

        @Override
        public long getContentLength() {
            return -1;
        }
        
        @Override
        public void initialize() throws IOException {
            SentMessageHandler sentMessageHandler = new SentMessageHandler() {
                public void processSentMessage(Message m) {
                    uploader.addAmountUploaded(m.getTotalLength());
                }                
            };
            
            sender = new MessageWriter(new ConnectionStats(), new BasicQueue(), sentMessageHandler);
            sender.setWriteChannel(this);
            
            query = QueryRequest.createBrowseHostQuery();
            iterable = RouterService.getFileManager().getIndexingIterator(query.desiresXMLResponses() || 
                    query.desiresOutOfBandReplies());
        }
        
        @Override
        public void interestWrite(WriteObserver observer, boolean status) {
            // make sure all events are delegated through this 
            super.interestWrite(this, status);
        }
        
        @Override
        public boolean handleWrite() throws IOException {
            addMessages();
            
            boolean more = sender.handleWrite();
            return more || iterable.hasNext();
        }
        
        /**
         * Adds a query reply with {@link #RESPONSES_PER_REPLY} responses to the
         * message queue.
         */
        private void addMessages() {
            List<Response> responses = new ArrayList<Response>(10); 
            for (int i = 0; iterable.hasNext() && i < RESPONSES_PER_REPLY; i++) {
                responses.add(iterable.next());
            }
            
            Iterable<QueryReply> it = RouterService.getMessageRouter()
                    .responsesToQueryReplies(responses.toArray(new Response[0]), query);

            for (QueryReply queryReply : it) {
                sender.send(queryReply);
            }
        }

        @Override
        public void finished() {
            sender = null;
        }

    }

}
