package com.limegroup.gnutella.uploader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.UploadManagerStub;

public class NormalUploadStateTest extends BaseTestCase {

    private UploadManager uploadManager = new UploadManagerStub();
    
    private StalledUploadWatchdog watchDog = new StalledUploadWatchdog();
    
    private FileDesc fileDesc = new FileDescStub("filename");
    
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
    
    public NormalUploadStateTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NormalUploadStateTest.class);
    }
    
    public void testWritesXNodeWhenFWFWTransfersAreSupported() throws Exception {
        NormalUploadState state = new NormalUploadState(getHTTPUploader(), watchDog);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        state.writeMessageHeaders(os);
        assertFalse(UDPService.instance().canDoFWT());
        assertNotContainsHeader(HTTPHeaderName.FWTPORT, os.toByteArray());
        
        boolean acceptsSolicited = UDPService.instance().canReceiveSolicited();
        boolean lastFWTState = ConnectionSettings.LAST_FWT_STATE.getValue();
        ConnectionManager manager = RouterService.getConnectionManager();
        
        PrivilegedAccessor.setValue(RouterService.class, "manager", proxyManager);
        PrivilegedAccessor.setValue(UDPService.instance(), "_acceptedSolicitedIncoming", true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);

        try {
            assertFalse(RouterService.isConnected());
            assertTrue(UDPService.instance().canDoFWT());
            
            os = new ByteArrayOutputStream();
            state = new NormalUploadState(getHTTPUploader(), watchDog);
            state.writeMessageHeaders(os);
            assertContainsHeader(HTTPHeaderName.FWTPORT, os.toByteArray());
            assertEqualsValue(HTTPHeaderName.FWTPORT, UDPService.instance().getStableUDPPort() + "",  os.toByteArray());
        }
        finally {
            PrivilegedAccessor.setValue(RouterService.class, "manager", manager);
            PrivilegedAccessor.setValue(UDPService.instance(), "_acceptedSolicitedIncoming", acceptsSolicited);
            ConnectionSettings.LAST_FWT_STATE.setValue(lastFWTState);
        }
    }
    
    public void assertNotContainsHeader(HTTPHeaderName header, byte[] in) throws IOException {
        assertNull(getHeaderLine(header, in));
    }
    
    public void assertContainsHeader(HTTPHeaderName header, byte[] in) throws IOException {
        assertNotNull("Header " + header + " not found", getHeaderLine(header, in));
    }
    
    public void assertEqualsValue(HTTPHeaderName header, String value, byte[] in) throws IOException {
        String line = getHeaderLine(header, in);
        assertNotNull("Header " + header + " not found", line);
        String actual = line.substring(line.indexOf(':') + 1).trim();
        assertEquals(value, actual);
    }
    
    private String getHeaderLine(HTTPHeaderName header, byte[] in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(in)));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(header.httpStringValue())) {
                return line;
            }
        }
        return null;
    }
    
    private HTTPUploader getHTTPUploader() throws IOException {
        Map<String, Object> params = Collections.emptyMap();
        return getHTTPUploader(params);
    }

    private HTTPUploader getHTTPUploader(Map<String, Object> params) throws IOException {
        HTTPSession session = new HTTPSession(new Socket(), uploadManager);
        HTTPUploader uploader = new HTTPUploader(HTTPRequestMethod.GET, "filename", session, 5, params, watchDog);
        uploader.setFileDesc(fileDesc);
        return uploader;
    }
    
}
