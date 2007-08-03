package com.limegroup.gnutella.uploader;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpExecutionContext;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class FileRequestHandlerTest extends LimeTestCase {

    private FileDesc fd = new FileDescStub("filename");

    private ConnectionManager proxyManager = new ConnectionManagerStub() {
        @Override
        public Set<Connectable> getPushProxies() {
            Connectable ip;
            try {
                ip = new ConnectableImpl("127.0.0.1", 9999, false);
                return Collections.singleton(ip);
            } catch (UnknownHostException e) {
            }
            return null;
        }
    };

    private URN urn1;

    private MockHTTPUploadSessionManager sessionManager;

    private FileManagerStub fileManager;

    private FileRequestHandler requestHandler;

    public FileRequestHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileRequestHandlerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Map<URN, FileDesc> urns = new HashMap<URN, FileDesc>();
        Vector<FileDesc> descs = new Vector<FileDesc>();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");

        FileDesc fd1 = new FileDescStub("abc1.txt", urn1, 0);
        urns.put(urn1, fd1);
        descs.add(fd1);

        sessionManager = new MockHTTPUploadSessionManager();
        fileManager = new FileManagerStub(urns, descs);        
        requestHandler = new FileRequestHandler(sessionManager, fileManager, ProviderHacks.getHTTPHeaderUtils());        
    }
    
    public void testHandleAccept() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        uploader.setFileDesc(fd);

        assertFalse(ProviderHacks.getUdpService().canDoFWT());

        boolean acceptsSolicited = ProviderHacks.getUdpService().canReceiveSolicited();
        boolean lastFWTState = ConnectionSettings.LAST_FWT_STATE.getValue();
        ConnectionManager manager = ProviderHacks.getConnectionManager();

        PrivilegedAccessor.setValue(RouterService.class, "manager",
                proxyManager);
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),
                "_acceptedSolicitedIncoming", true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);

        try {
            assertFalse(RouterService.isConnected());
            assertTrue(ProviderHacks.getUdpService().canDoFWT());

            requestHandler.handleAccept(new HttpExecutionContext(null),
                    request, response, uploader, fd);
            Header header = response.getFirstHeader(HTTPHeaderName.FWTPORT
                    .httpStringValue());
            assertNotNull("expected header: "
                    + HTTPHeaderName.FWTPORT.httpStringValue(), header);
            assertEquals(header.getValue(), ProviderHacks.getUdpService()
                    .getStableUDPPort()
                    + "");
        } finally {
            PrivilegedAccessor
                    .setValue(RouterService.class, "manager", manager);
            PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),
                    "_acceptedSolicitedIncoming", acceptsSolicited);
            ConnectionSettings.LAST_FWT_STATE.setValue(lastFWTState);
        }

    }

    public void testFeatureHeaderInterceptor() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session); 
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", "/get/0/abc1.txt");
        request.addHeader("X-Features", "fwalt/0.1,browse/1.0,chat/0.1");
        request.addHeader("X-Node", "127.0.0.1:1234");
        request.addHeader("X-Downloaded", "123456");
        request.addHeader("Range", "bytes 1-2");
        request.addHeader("X-Gnutella-Content-URN", urn1.httpStringValue());
        request.addHeader("X-Queue", "1.0");
        
        requestHandler.handle(request, response, new HttpExecutionContext(null));
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(1234, uploader.getGnutellaPort());
        assertEquals("127.0.0.1", uploader.getHost());
        assertTrue(uploader.isChatEnabled());
        assertTrue(uploader.isBrowseHostEnabled());
        assertEquals(123456, uploader.getTotalAmountUploaded());
        assertEquals(1, uploader.getUploadBegin());
        assertEquals(3, uploader.getUploadEnd());
        assertEquals(true, uploader.containedRangeRequest());
        assertEquals(urn1, uploader.getRequestedURN());
        assertTrue(uploader.supportsQueueing());
    }
        
    public void testFeatureHeaderInterceptorChat() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session); 
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", "/get/0/abc1.txt");
        request.addHeader("Chat", "128.0.0.1:5678");       
        requestHandler.handle(request, response, new HttpExecutionContext(null));
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(5678, uploader.getGnutellaPort());
        assertEquals("128.0.0.1", uploader.getHost());
        assertTrue(uploader.isChatEnabled());
        assertTrue(uploader.isBrowseHostEnabled());
    }

}
