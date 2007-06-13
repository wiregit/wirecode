package org.limewire.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestCase;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class HttpIOReactorTest extends TestCase {

    public void testHttpIOReactor() throws Exception {
        try {
            new HttpIOReactor(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testExecute() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        try {
            reactor.execute(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testGetHttpParams() {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        assertSame(params, reactor.getHttpParams());
    }

    public void testShutdown() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        reactor.shutdown();
    }

    public void testConnect() {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        SocketAddress remoteAddress = new InetSocketAddress(9999);
        try {
            reactor.connect(remoteAddress, null, null, null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    public void testAcceptConnection() {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        SocketAddress remoteAddress = new InetSocketAddress(9999);
        try {
            reactor.connect(remoteAddress, null, null, null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

}
