package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.ErrorService;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class ConnectionTest extends LimeTestCase {
    
    private static int LISTEN_PORT = 9999;
    private ConnectionAcceptor ACCEPTOR;
    
    public ConnectionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionTest.class);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        ACCEPTOR = new ConnectionAcceptor();
        ACCEPTOR.start();
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
    }
    
    public void tearDown() throws Exception {
        ACCEPTOR.shutdown();
        
        Thread.sleep(1000);
    }
    
    public void testBlockingConnectFailing() throws Exception {
        Connection c = new Connection("127.0.0.1", LISTEN_PORT+1);
        try {
            c.initialize(new Properties(), new UltrapeerHandshakeResponder("127.0.0.1"), 1000);
            fail("shouldn't have initialized");
        } catch(IOException iox) {
            // timed out.
        }
    }
    
    public void testNonBlockingConnectFailing() throws Exception {
        Connection c = new Connection("127.0.0.1", LISTEN_PORT+1);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        c.initialize(new Properties(), new UltrapeerHandshakeResponder("127.0.0.1"), observer);
        observer.waitForResponse(3000);
        assertTrue(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertFalse(observer.isNoGOK());
    }
    
    public void testBlockingConnectSucceeds() throws Exception {
        ManagedConnection c = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        c.initialize();
    }
    
    public void testNonBlockingConnectSucceeds() throws Exception {
        ManagedConnection c = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        c.initialize(observer);
        observer.waitForResponse(3000);
        assertFalse(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isConnect());
    }
    
    public void testNonBlockingNoGOK() throws Exception {
        ACCEPTOR.getObserver().setNoGOK(true);
        Connection c = new Connection("127.0.0.1", LISTEN_PORT);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        c.initialize(new Properties(), new UltrapeerHandshakeResponder("127.0.0.1"), observer);
        observer.waitForResponse(10000);
        assertFalse(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertTrue(observer.isNoGOK());
        assertEquals(401, observer.getCode());
    }
    
    public void testNonBlockingBadHandshake() throws Exception {
        ACCEPTOR.getObserver().setBadHandshake(true);
        Connection c = new Connection("127.0.0.1", LISTEN_PORT);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        c.initialize(new Properties(), new UltrapeerHandshakeResponder("127.0.0.1"), observer);
        observer.waitForResponse(10000);
        assertFalse(observer.isShutdown());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertFalse(observer.isNoGOK());
    }
        
    private static class ConnectionAcceptor {
        private ServerSocket socket;
        private SimpleAcceptObserver observer;
        
        
        public void start() throws Exception {
            observer = new SimpleAcceptObserver();
            socket = new NIOServerSocket(LISTEN_PORT, observer);
        }
        
        public void shutdown() throws Exception {
            socket.close();
        }
        
        public SimpleAcceptObserver getObserver() {
            return observer;
        }
    }
    
    private static class SimpleAcceptObserver implements AcceptObserver {
        private boolean noGOK = false;
        private boolean badHandshake = false;
        
        public void handleIOException(IOException iox) {}

        public void handleAccept(final Socket socket) throws IOException {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        if(badHandshake) {
                            socket.close();
                            return;
                        }
                        socket.setSoTimeout(3000);
                        InputStream in = socket.getInputStream();
                        String word = IOUtils.readWord(in, 9);
                        if (!word.equals("GNUTELLA"))
                            throw new IOException("Bad word: " + word);

                        if(noGOK) {
                            socket.getOutputStream().write("GNUTELLA/0.6 401 Failed\r\n\r\n".getBytes());
                            socket.getOutputStream().flush();
                            return;
                        }
                        
                        final Connection con = new Connection(socket);
                        con.initialize(null, new UltrapeerHandshakeResponder("127.0.0.1"), 1000);
                    } catch (Exception e) {
                        ErrorService.error(e);
                    }
                }
            }, "conninit");
        }

        public void shutdown() {
        }

        public void setBadHandshake(boolean badHandshake) {
            this.badHandshake = badHandshake;
        }

        public void setNoGOK(boolean noGOK) {
            this.noGOK = noGOK;
        }
    }
}
