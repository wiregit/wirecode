package com.limegroup.gnutella.uploader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.limewire.io.ConnectableImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class FileRequestHandlerTest extends LimeTestCase {

    private FileDesc fd = new FileDescStub("filename");

    private URN urn1;

    private MockHTTPUploadSessionManager sessionManager;

    private FileManagerStub fileManager;

    private FileRequestHandler fileRequestHandler;

    private Injector injector;

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
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
                bind(HTTPUploadSessionManager.class).toInstance(sessionManager);
                bind(FileManager.class).to(FileManagerStub.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
            }
        });

        ConnectionManagerStub connectionManager = (ConnectionManagerStub) injector
                .getInstance(ConnectionManager.class);
        connectionManager.setPushProxies(Collections.singleton(new ConnectableImpl("127.0.0.1",
                9999, false)));

        fileManager = (FileManagerStub) injector.getInstance(FileManager.class);
        fileManager.setUrns(urns);
        fileManager.setDescs(descs);
        fileRequestHandler = injector.getInstance(FileRequestHandler.class);
    }

    public void testHandleAccept() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        uploader.setFileDesc(fd);

        NetworkManagerStub networkManager = (NetworkManagerStub) injector
                .getInstance(NetworkManager.class);
        networkManager.setCanDoFWT(true);

        fileRequestHandler.handleAccept(new BasicHttpContext(null), request, response,
                uploader, fd);
        Header header = response.getFirstHeader(HTTPHeaderName.FWTPORT.httpStringValue());
        assertNotNull("expected header: " + HTTPHeaderName.FWTPORT.httpStringValue(), header);
        assertEquals(networkManager.getStableUDPPort() + "", header.getValue());
    }

    public void testFeatureHeaderInterceptor() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session);
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", "/get/0/abc1.txt");
        request.addHeader("X-Features", "fwalt/0.1,browse/1.0,chat/0.1");
        request.addHeader("X-Node", "127.0.0.1:1234");
        request.addHeader("X-Downloaded", "123456");
        request.addHeader("Range", "bytes 1-2");
        request.addHeader("X-Gnutella-Content-URN", urn1.httpStringValue());
        request.addHeader("X-Queue", "1.0");

        fileRequestHandler.handle(request, response, new BasicHttpContext(null));
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
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", "/get/0/abc1.txt");
        request.addHeader("Chat", "128.0.0.1:5678");
        fileRequestHandler.handle(request, response, new BasicHttpContext(null));
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(5678, uploader.getGnutellaPort());
        assertEquals("128.0.0.1", uploader.getHost());
        assertTrue(uploader.isChatEnabled());
        assertTrue(uploader.isBrowseHostEnabled());
    }
    
    /**
     * Tests if browse host is enabled on the uploader if the downloader sent
     * its push endpoint information along in the X-FWT-Node header.  
     */
    public void testIsFWTBrowseHostEnabled() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session);
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", "/get/0/abc1.txt");
        
        String pushEndpoint = "FF9EEA9E8B2E1D737828EFD1B7DAC500;129.168.0.1:5555";
        
        request.addHeader(HTTPHeaderName.FW_NODE_INFO.create(pushEndpoint));
        fileRequestHandler.handle(request, response, new BasicHttpContext(null));
        
        assertTrue(uploader.isBrowseHostEnabled());
        assertEquals(pushEndpoint, uploader.getPushEndpoint().httpStringValue());
    }

}
