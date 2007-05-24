package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.Function;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocUtils;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
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
    
    public void testWritesAltsWhenEmpty() throws Exception {
        StubUploadState state = new StubUploadState();
        state.getStubUploader().setAlts();
        checkAltsOutput("", state);
    }
    
    public void testWritesAltsNoTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        state.getStubUploader().setAlts("1.2.3.4:5", "2.3.4.6", "7.3.2.1", "2.1.5.3:6201", "1.2.65.2");
        checkAltsOutput("1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", state);
    }
    
    public void testWritesAltsWithTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        state.getStubUploader().setAlts("1.2.3.4:5", "T2.3.4.6", "T7.3.2.1", "2.1.5.3:6201", "T1.2.65.2");
        String expected = "tls=68,1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2";
        checkAltsOutput(expected, state);
        
        // Just make sure that AltLocUtils can parse this.
        AltLocUtils.parseAlternateLocations(HugeTestUtils.SHA1, expected, true, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, "1.2.3.4", 5,    false); break;
                case 1: checkDirect(argument, "2.3.4.6", 6346, true); break;
                case 2: checkDirect(argument, "7.3.2.1", 6346, true); break;
                case 3: checkDirect(argument, "2.1.5.3", 6201, false); break;
                case 4: checkDirect(argument, "1.2.65.2",6346, true); break;
                default: throw new IllegalArgumentException("bad loc: " + argument + ", i: " + i);
                }
                return null;
            }
        });
    }
    
    private void checkDirect(AlternateLocation alt, String host, int port, boolean tls) {
        assertInstanceof(DirectAltLoc.class, alt);
        DirectAltLoc d = (DirectAltLoc)alt;
        assertEquals(host, d.getHost().getAddress());
        assertEquals(port, d.getHost().getPort());
        if(tls) {
            assertInstanceof(Connectable.class, d.getHost());
            assertTrue(((Connectable)d.getHost()).isTLSCapable());
        } else {
            if(d.getHost() instanceof Connectable)
                assertFalse(((Connectable)d.getHost()).isTLSCapable());
        }
    }
    
    public void testWritePushProxiesWhenEmpty() throws Exception {
        StubUploadState state = new StubUploadState();
        checkProxiesOutput("", state);
    }
    
    public void testWritePushProxiesNoTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        checkProxiesOutput("1.2.3.4:5,2.3.4.5:6", state);
    }
    
    public void testWritePushProxiesSomeTLS() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        checkProxiesOutput("pptls=A,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7", state);
    }
    
    public void testWritePushProxiesLimitsAt4() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        checkProxiesOutput("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    public void testWritePushProxiesLimitsAt4NoTLSIfLater() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        checkProxiesOutput("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    public void testWritePushProxiesLimitsAt4TLSRight() throws Exception {
        StubUploadState state = new StubUploadState();
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        checkProxiesOutput("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", state);
    }
    
    private void checkAltsOutput(String expected, StubUploadState state) throws Exception {
        String output = "X-Gnutella-Content-URN: " + HugeTestUtils.SHA1.httpStringValue() + "\r\n";
        
        if(expected.length() > 0)
            output += "X-Alt: " + expected + "\r\n";
        
        StringWriter writer = new StringWriter();
        state.writeAlts(writer);
        assertEquals(output, writer.toString());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        state.writeAlts(out);
        assertEquals(output.getBytes(), out.toByteArray());
    }
    
    
    private void checkProxiesOutput(String expected, StubUploadState state) throws Exception {
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
        private StubUploader uploader;
        
        public StubUploadState() {
            super(new StubUploader());
            uploader = (StubUploader)UPLOADER;
        }
        
        StubUploader getStubUploader() {
            return uploader;
        }

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
    
    private static class StubUploader extends HTTPUploader {
        private Collection<DirectAltLoc> alts;
        
        @Override
        Collection<DirectAltLoc> getNextSetOfAltsToSend() {
            return alts;
        }

        @Override
        public FileDesc getFileDesc() {
            return new StubFileDesc();
        }
        
        void setAlts(String... strings) throws Exception {
            alts = new ArrayList<DirectAltLoc>(strings.length);
            for(String string : strings) {
                boolean tls = false;
                if(string.startsWith("T")) {
                    tls = true;
                    string = string.substring(1);
                }
                alts.add((DirectAltLoc)AlternateLocation.create(string, HugeTestUtils.SHA1, tls));
            }
        }
    }
    
    private static class StubFileDesc extends FileDesc {
        @Override
        public URN getSHA1Urn() {
            return HugeTestUtils.SHA1;
        }
    }

}
