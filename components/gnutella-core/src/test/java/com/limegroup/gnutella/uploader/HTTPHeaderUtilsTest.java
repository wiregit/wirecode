package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
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

public class HTTPHeaderUtilsTest extends LimeTestCase {
    
    private StubConnectionManager stub;
    private ConnectionManager oldCM;


    public HTTPHeaderUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPHeaderUtilsTest.class);
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
        MockHTTPUploader uploader = new MockHTTPUploader();
        MockAltLocTracker tracker = (MockAltLocTracker)uploader.getAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        StubFileDesc fd = new StubFileDesc();
        
        tracker.setNextSetOfAltsToSend(new ArrayList<DirectAltLoc>());
        HTTPHeaderUtils.addAltLocationsHeader(response, uploader, fd);
        assertNull(response.getLastHeader("X-Alt"));
    }
    
    public void testWritesAltsNoTLS() throws Exception {
        MockHTTPUploader uploader = new MockHTTPUploader();
        MockAltLocTracker tracker = (MockAltLocTracker)uploader.getAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        StubFileDesc fd = new StubFileDesc();
        
        tracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "2.3.4.6", "7.3.2.1", "2.1.5.3:6201", "1.2.65.2"));
        HTTPHeaderUtils.addAltLocationsHeader(response, uploader, fd);
        assertEquals("1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", response.getLastHeader("X-Alt").getValue());
    }
    
    public void testWritesAltsWithTLS() throws Exception {
        MockHTTPUploader uploader = new MockHTTPUploader();
        MockAltLocTracker tracker = (MockAltLocTracker)uploader.getAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        StubFileDesc fd = new StubFileDesc();
        
        tracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "T2.3.4.6", "T7.3.2.1", "2.1.5.3:6201", "T1.2.65.2"));
        HTTPHeaderUtils.addAltLocationsHeader(response, uploader, fd);
        String expected = "tls=68,1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2";
        assertEquals(expected, response.getLastHeader("X-Alt").getValue());
        
        // Just make sure that AltLocUtils can parse this.
        final AtomicInteger index = new AtomicInteger(0);
        AltLocUtils.parseAlternateLocations(HugeTestUtils.SHA1, expected, true, new Function<AlternateLocation, Void>() {
            public Void apply(AlternateLocation argument) {
                switch(index.getAndIncrement()) {
                case 0: checkDirect(argument, "1.2.3.4", 5,    false); break;
                case 1: checkDirect(argument, "2.3.4.6", 6346, true); break;
                case 2: checkDirect(argument, "7.3.2.1", 6346, true); break;
                case 3: checkDirect(argument, "2.1.5.3", 6201, false); break;
                case 4: checkDirect(argument, "1.2.65.2",6346, true); break;
                default: throw new IllegalArgumentException("bad loc: " + argument + ", i: " + (index.get()-1));
                }
                return null;
            }
        });
        assertEquals(5, index.get());
    }
    
    public void testWritePushProxiesWhenEmpty() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        HTTPHeaderUtils.addProxyHeader(response);
        assertNull(response.getLastHeader("X-Push-Proxy"));
    }
    
    public void testWritePushProxiesNoTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        HTTPHeaderUtils.addProxyHeader(response);
        assertEquals("1.2.3.4:5,2.3.4.5:6", response.getLastHeader("X-Push-Proxy").getValue());
    }
    
    public void testWritePushProxiesSomeTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        HTTPHeaderUtils.addProxyHeader(response);
        assertEquals("pptls=A,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7", response.getLastHeader("X-Push-Proxy").getValue());
    }
    
    public void testWritePushProxiesLimitsAt4() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        HTTPHeaderUtils.addProxyHeader(response);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", response.getLastHeader("X-Push-Proxy").getValue());
    }
    
    public void testWritePushProxiesLimitsAt4NoTLSIfLater() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        HTTPHeaderUtils.addProxyHeader(response);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", response.getLastHeader("X-Push-Proxy").getValue());
    }
    
    public void testWritePushProxiesLimitsAt4TLSRight() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        stub.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        stub.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        stub.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        stub.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        stub.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        HTTPHeaderUtils.addProxyHeader(response);
        assertEquals("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", response.getLastHeader("X-Push-Proxy").getValue());
    }
    
    private Collection<DirectAltLoc> altsFor(String... locs) throws Exception {
        List<DirectAltLoc> alts = new ArrayList<DirectAltLoc>(locs.length);
        for(String loc : locs) {
            boolean tls = false;
            if(loc.startsWith("T")) {
                tls = true;
                loc = loc.substring(1);
            }
            alts.add((DirectAltLoc)AlternateLocation.create(loc, HugeTestUtils.SHA1, tls));
        }
        return alts;
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
    
    
    
    /** A fake ConnectionManager with custom proxies. */
    private static class StubConnectionManager extends ConnectionManager {
        private Set<Connectable> proxies;
        @Override
        public Set<? extends Connectable> getPushProxies() {
            return proxies;
        }        
    }
    
    private static class StubFileDesc extends FileDesc {
        @Override
        public URN getSHA1Urn() {
            return HugeTestUtils.SHA1;
        }
    }


}
