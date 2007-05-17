package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.StrictIpPortSet;

public class UploadStateTest extends LimeTestCase {
    
    private StubConnectionManager stub;
    private ConnectionManager oldCM;
    
    public UploadStateTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UploadStateTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        stub = new StubConnectionManager();
        stub.proxies = new StrictIpPortSet<Connectable>();
        oldCM = RouterService.getConnectionManager();
        PrivilegedAccessor.setValue(RouterService.class, "manager", stub);  
    }
    
    public void tearDown() throws Exception {
        PrivilegedAccessor.setValue(RouterService.class, "manager", oldCM);
    }
    
    public void testWritePushProxiesWhenEmpty() throws Exception {
        StubUploadState state = new StubUploadState();
        checkOutput("", state);
    }
    
    public void testWritePushProxiesNoTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        checkOutput("1.2.3.4:5,2.3.4.5:6", state);
    }
    
    public void testWritePushProxiesSomeTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        checkOutput("pptls=A,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7", state);
    }
    
    public void testWritePushProxiesLimitsAt4() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        checkOutput("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    public void testWritePushProxiesLimitsAt4NoTLSIfLater() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        checkOutput("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    public void testWritePushProxiesLimitsAt4TLSRight() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        checkOutput("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    private void checkOutput(String expected, StubUploadState state) throws Exception {
        if(expected.length() > 0)
            expected = "X-Push-Proxy: " + expected + "\r\n";
        
        StringWriter writer = new StringWriter();
        state.writeProxies(writer);
        assertEquals(expected, writer.toString());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        state.writeProxies(out);
        assertEquals(expected.getBytes(), out.toByteArray());
    }
    
    /** Upgrade access to writeProxies methods. */
    private static class StubUploadState extends UploadState {

        @Override
        public void writeProxies(OutputStream os) throws IOException {
            super.writeProxies(os);
        }

        @Override
        public void writeProxies(Writer os) throws IOException {
            super.writeProxies(os);
        }

        public boolean getCloseConnection() {
            return false;
        }

        public void writeMessageBody(OutputStream os) throws IOException {
        }

        public void writeMessageHeaders(OutputStream os) throws IOException {
        }
    }
    
    /** A fake ConnectionManager with custom proxies. */
    private static class StubConnectionManager extends ConnectionManager {
        private Set<Connectable> proxies;

        @Override
        public Set<? extends Connectable> getPushProxies() {
            return proxies;
        }
        
    }

}
