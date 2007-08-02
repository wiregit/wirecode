package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpExecutionContext;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.HackMessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

public class PushProxyRequestHandlerTest extends BaseTestCase {

    private PushProxyRequestHandler requestHandler;

    private HttpExecutionContext context;

    private HTTPUploader uploader;

    private StubMessageRouter messageRouter;

    public PushProxyRequestHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushProxyRequestHandlerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        messageRouter = new StubMessageRouter();
        MockHTTPUploadSessionManager sessionManager = new MockHTTPUploadSessionManager();
        uploader = new HTTPUploader("filename", new HTTPUploadSession(null,
                null, null)) {
            @Override
            public void setState(UploadStatus state) {
                // avoid assertion errors
            }
        };
        sessionManager.uploader = uploader;
        requestHandler = new PushProxyRequestHandler(sessionManager,
                messageRouter);
        context = new HttpExecutionContext(null);
    }

    public void testHandleMalformedRequest() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        // no parameters
        HttpRequest request = new BasicHttpRequest("GET", "/push/proxy");
        requestHandler.handle(request, response, context);
        assertNull(messageRouter.push);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());

        // no node header
        request = new BasicHttpRequest("GET",
                "/push/proxy/?ServerID=6MTB46XQ3PTC3QVZRDW6PAFQAA");
        requestHandler.handle(request, response, context);
        assertNull(messageRouter.push);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
                .getStatusCode());

    }

    public void testHandleValidRequest() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        BasicHttpRequest request = new BasicHttpRequest("GET",
                "/push/proxy/?ServerID=6MTB46XQ3PTC3QVZRDW6PAFQAA");
        request.addHeader("X-Node", "localhost:4567");
        requestHandler.handle(request, response, context);
        assertNotNull(messageRouter.push);
        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine()
                .getStatusCode());
    }

    private class StubMessageRouter extends HackMessageRouter {
        
        private PushRequest push;

        @Override
        protected List<QueryReply> createQueryReply(byte[] guid, byte ttl,
                long speed, Response[] res, byte[] clientGUID, boolean busy,
                boolean uploaded, boolean measuredSpeed, boolean isFromMcast,
                boolean shouldMarkForFWTransfer, SecurityToken securityToken) {
            return null;
        }

        @Override
        protected void respondToPingRequest(PingRequest request,
                ReplyHandler handler) {
        }

        @Override
        protected boolean respondToQueryRequest(QueryRequest queryRequest,
                byte[] clientGUID, ReplyHandler handler) {
            return false;
        }

        @Override
        protected void respondToUDPPingRequest(PingRequest request,
                InetSocketAddress addr, ReplyHandler handler) {
        }

        @Override
        public void sendPushRequest(PushRequest push) throws IOException {
            this.push = push;
        }

    }
}
