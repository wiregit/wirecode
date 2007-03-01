package com.limegroup.gnutella.uploader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.HttpCoreUtils;
import org.limewire.http.HttpNIOEntity;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.statistics.UploadStat;

public class BrowseRequestHandler implements HttpRequestHandler {

    private HTTPUploadSessionManager sessionManager;

    public BrowseRequestHandler(HTTPUploadSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        UploadStat.BROWSE_HOST.incrementStat();
        HTTPUploader uploader = sessionManager.getOrCreateUploader(context,
                UploadType.BROWSE_HOST, "Browse-File");
        if (!HttpCoreUtils.hasHeader(request, "Accept",
                Constants.QUERYREPLY_MIME_TYPE)) {
            response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
        } else {
            response.setEntity(new BrowseResponseEntity(uploader));
        }
    }

    public class BrowseResponseEntity extends HttpNIOEntity {

        private final ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        private HTTPUploader uploader;

        public BrowseResponseEntity(HTTPUploader uploader) {
            this.uploader = uploader;

            // create a new indexing query
            QueryRequest indexingQuery = QueryRequest.createBrowseHostQuery();

            // get responses from file manager
            Response[] responses = RouterService.getFileManager().query(
                    indexingQuery);
            if (responses == null) // we aren't sharing any files....
                responses = new Response[0];

            // convert to QueryReplies
            Iterable<QueryReply> iterable = RouterService.getMessageRouter()
                    .responsesToQueryReplies(responses, indexingQuery);

            try {
                for (QueryReply queryReply : iterable)
                    queryReply.write(BAOS);
            } catch (IOException e) {
                // if there is an error, do nothing..
            }

            // UPLOADER.setAmountUploaded(BAOS.size());
            setContentType(Constants.QUERYREPLY_MIME_TYPE);
        }

        public InputStream getContent() throws IOException,
                IllegalStateException {
            return new ByteArrayInputStream(BAOS.toByteArray());
        }

        public long getContentLength() {
            return -1;
        }

        public boolean isRepeatable() {
            return false;
        }

        public boolean isStreaming() {
            return true;
        }

        public void writeTo(OutputStream outstream) throws IOException {
            outstream.write(BAOS.toByteArray());
        }

    }

}
