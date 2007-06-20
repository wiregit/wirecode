package org.limewire.net;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.util.BaseTestCase;

public class SocketAcceptorTest extends BaseTestCase {
    
    private int LISTEN_PORT = 9999;
    private SocketAcceptor acceptor;
    
    public SocketAcceptorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SocketAcceptorTest.class);
    }
    
    public void setUp() throws Exception {
    }
    
    public void tearDown() throws Exception {
        if (acceptor != null) {
            acceptor.setPort(0);
        }
    }
    
    public void testSetPort() throws Exception {
        acceptor = new SocketAcceptor();
        acceptor.setPort(LISTEN_PORT);
        write(LISTEN_PORT, "Hello");
        acceptor.setPort(LISTEN_PORT + 1);
        try {
            write(LISTEN_PORT, "Hello");
            fail("Expected connect exception");
        } catch (ConnectException expected) {            
        }
    }

    private void write(final int port, final String text) throws Exception {
        final Exception[] error = new Exception[1];
        Thread t = new ManagedThread() {
            @Override
            public void run() {
                Socket s;
                try {
                    s = new Socket();
                    s.connect(new InetSocketAddress("localhost", port), 200); 
                    s.getOutputStream().write(text.getBytes());
                } catch (Exception e) {
                    error[0] = e;
                }
            }
        };
        t.start();
        t.join();
        
        if (error[0] != null) {
            throw error[0];
        }
    }
    
}
