package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.limewire.concurrent.Providers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.HackConnectionManager;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.StrictIpPortSet;

public class HTTPHeaderUtilsTest extends BaseTestCase {
    
    private StubConnectionManager connectionManager;
    private AltLocManager altLocManager;
    private HTTPHeaderUtils httpHeaderUtils;

    public HTTPHeaderUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPHeaderUtilsTest.class);
    }

    @Override
    public void setUp() throws Exception {
        connectionManager = new StubConnectionManager();
        connectionManager.proxies = new StrictIpPortSet<Connectable>();
        altLocManager = new AltLocManager();
        NetworkManager networkManager = new NetworkManagerStub();
        httpHeaderUtils = new HTTPHeaderUtils(new FeaturesWriter(networkManager), networkManager, Providers.of((ConnectionManager) connectionManager));
    }

    @Override
    public void tearDown() throws Exception {
    }
        
    public void testWritesAltsWhenEmpty() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");

        altLocTracker.setNextSetOfAltsToSend(new ArrayList<DirectAltLoc>());
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        assertNull(response.getLastHeader("X-Alt"));
    }
    
    public void testWritesAltsNoTLS() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");

        altLocTracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "2.3.4.6", "7.3.2.1", "2.1.5.3:6201", "1.2.65.2"));
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        Header header = response.getLastHeader("X-Alt");
        assertNotNull("Missing X-Alt header", header);
        assertEquals("1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", header.getValue());
    }
    
    public void testWritesAltsWithTLS() throws Exception {
        StubAltLocTracker altLocTracker = new StubAltLocTracker();
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        
        altLocTracker.setNextSetOfAltsToSend(altsFor("1.2.3.4:5", "T2.3.4.6", "T7.3.2.1", "2.1.5.3:6201", "T1.2.65.2"));
        httpHeaderUtils.addAltLocationsHeader(response, altLocTracker, altLocManager);
        Header header = response.getLastHeader("X-Alt");
        assertNotNull("Missing X-Alt header", header);
        assertEquals("tls=68,1.2.3.4:5,2.3.4.6,7.3.2.1,2.1.5.3:6201,1.2.65.2", header.getValue());
    }
    
    public void testWritePushProxiesWhenEmpty() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        httpHeaderUtils.addProxyHeader(response);
        assertNull(response.getLastHeader("X-Push-Proxy"));
    }
    
    public void testWritePushProxiesNoTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6", header.getValue());
    }
    
    public void testWritePushProxiesSomeTLS() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("pptls=A,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, false));
        httpHeaderUtils.addProxyHeader(response);

        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4NoTLSIfLater() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, false));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, false));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, false));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, false));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        httpHeaderUtils.addProxyHeader(response);

        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    public void testWritePushProxiesLimitsAt4TLSRight() throws Exception {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        connectionManager.proxies.add(new ConnectableImpl("1.2.3.4", 5, true));
        connectionManager.proxies.add(new ConnectableImpl("2.3.4.5", 6, true));
        connectionManager.proxies.add(new ConnectableImpl("3.4.5.6", 7, true));
        connectionManager.proxies.add(new ConnectableImpl("4.5.6.7", 8, true));
        connectionManager.proxies.add(new ConnectableImpl("5.6.7.8", 9, true));
        httpHeaderUtils.addProxyHeader(response);
        
        Header header = response.getLastHeader("X-Push-Proxy");
        assertNotNull("Missing X-Push-Proxy header", header);
        assertEquals("pptls=F,1.2.3.4:5,2.3.4.5:6,3.4.5.6:7,4.5.6.7:8", header.getValue());
    }
    
    private Collection<DirectAltLoc> altsFor(String... locs) throws Exception {
        List<DirectAltLoc> alts = new ArrayList<DirectAltLoc>(locs.length);
        for(String loc : locs) {
            boolean tls = false;
            if(loc.startsWith("T")) {
                tls = true;
                loc = loc.substring(1);
            }
            alts.add((DirectAltLoc)ProviderHacks.getAlternateLocationFactory().create(loc, HugeTestUtils.SHA1, tls));
        }
        return alts;
    }
        
    /** A fake ConnectionManager with custom proxies. */
    private static class StubConnectionManager extends HackConnectionManager {

        private Set<Connectable> proxies;
        
        @Override
        public Set<? extends Connectable> getPushProxies() {
            return proxies;
        }
        
    }
    
}
