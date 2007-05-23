package com.limegroup.gnutella.uploader;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpExecutionContext;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;

public class FileRequestHandlerTest extends BaseTestCase {

    private FileDesc fd = new FileDescStub("filename");

    private ConnectionManager proxyManager = new ConnectionManagerStub() {
        @Override
        public Set<IpPort> getPushProxies() {
            IpPort ip;
            try {
                ip = new IpPortImpl("127.0.0.1:9999");
                return Collections.singleton(ip);
            } catch (UnknownHostException e) {
            }
            return null;
        }
    };

    public FileRequestHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileRequestHandlerTest.class);
    }

    public void testHandleAccept() throws Exception {
        FileRequestHandler requestHandler = new FileRequestHandler(
                new MockHTTPUploadSessionManager());
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        uploader.setFileDesc(fd);

        assertFalse(UDPService.instance().canDoFWT());

        boolean acceptsSolicited = UDPService.instance().canReceiveSolicited();
        boolean lastFWTState = ConnectionSettings.LAST_FWT_STATE.getValue();
        ConnectionManager manager = RouterService.getConnectionManager();

        PrivilegedAccessor.setValue(RouterService.class, "manager",
                proxyManager);
        PrivilegedAccessor.setValue(UDPService.instance(),
                "_acceptedSolicitedIncoming", true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);

        try {
            assertFalse(RouterService.isConnected());
            assertTrue(UDPService.instance().canDoFWT());

            requestHandler.handleAccept(new HttpExecutionContext(null),
                    request, response, uploader, fd);
            Header header = response.getFirstHeader(HTTPHeaderName.FWTPORT
                    .httpStringValue());
            assertNotNull("expected header: "
                    + HTTPHeaderName.FWTPORT.httpStringValue(), header);
            assertEquals(header.getValue(), UDPService.instance()
                    .getStableUDPPort()
                    + "");
        } finally {
            PrivilegedAccessor
                    .setValue(RouterService.class, "manager", manager);
            PrivilegedAccessor.setValue(UDPService.instance(),
                    "_acceptedSolicitedIncoming", acceptsSolicited);
            ConnectionSettings.LAST_FWT_STATE.setValue(lastFWTState);
        }

    }

}
