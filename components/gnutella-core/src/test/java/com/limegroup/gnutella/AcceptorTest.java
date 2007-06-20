package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.nio.ssl.TLSNIOSocket;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class AcceptorTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(AcceptorTest.class);
    
    private static Acceptor acceptThread;

    public AcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AcceptorTest.class);
    }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    private static void setSettings() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }

    public static void globalSetUp() throws Exception {
        setSettings();
        new RouterService(new ActivityCallbackStub());
        RouterService.getConnectionManager().initialize();
        
        // start it up!
        acceptThread = new Acceptor();
        acceptThread.start();
        
        // Give thread time to find and open it's sockets.   This race condition
        // is tolerated in order to speed up LimeWire startup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}                
    }
    
    public void setUp() throws Exception {
        setSettings();
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptThread.setListeningPort(0);
    }
    
    public void tearDown() throws Exception {
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptThread.setListeningPort(0);
    }
    
    
        
    /**
     * This test checks to ensure that Acceptor.setListeningPort
     * cannot use a port if the UDP part is already bound.
     */
    public void testCannotUseBoundUDPPort() {        
        int portToTry = 2000;
        DatagramSocket udp = null;
        while (true) {
            // get a free port for UDP traffic.
            try {
                udp = new DatagramSocket(portToTry);
                break;
            }
            catch (IOException e) {
                portToTry++;
                continue;
            }
        }

        try {
            acceptThread.setListeningPort(portToTry);
            assertTrue("had no trouble binding UDP port!", false);
        }
        catch (IOException expected) {
            udp.close();
        }
    }
        
    /**
     * This test checks to ensure that Acceptor.setListeningPort
     * cannot use port if the TCP part is already bound.
     */
    public void testCannotUseBoundTCPPort() {
        int portToTry = 2000;
        ServerSocket tcp = null;
        while (true) {
            // get a free port for UDP traffic.
            try {
                tcp = new ServerSocket(portToTry);
                break;
            }
            catch (IOException e) {
                portToTry++;
                continue;
            }
        }

        try {
            acceptThread.setListeningPort(portToTry);
            fail("had no trouble binding TCP port!");
        }
        catch (IOException expected) {
            try {
                tcp.close();
            }
            catch (Exception ignored) {}
        }
    }

    /**
     * This test checks to make sure that Acceptor.setListeningPort
     * correctly binds the UDP & TCP port.
     */
     public void testAcceptorBindsUDPandTCP() {
        int portToTry = 2000;
        while (true) {
            try {
                acceptThread.setListeningPort(portToTry);
                break;
            }
            catch (IOException occupied) {
                portToTry++;
                continue;
            }
        }
        
        try {
            DatagramSocket udp = new DatagramSocket(portToTry);
            udp.close();
            fail("had no trouble binding UDP to occupied port!");
        }
        catch (IOException good) {}
        
        try {
            ServerSocket tcp = new ServerSocket(portToTry);
            tcp.close();
            fail("had no trouble binding TCP to occupied port!");
        }
        catch (IOException good) {}
    }
     
     public void testIncomingTLSHomemadeTLS() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         RouterService.getConnectionDispatcher().addConnectionAcceptor(acceptor, false, true, "BLOCKING");
         
         Socket tls = new TLSNIOSocket("localhost", port);
         tls.getOutputStream().write("BLOCKING MORE DATA".getBytes());
         tls.getOutputStream().flush();
         assertTrue(acceptor.waitForAccept());
         tls.close();
     }
     
     public void testIncomingTLSBuiltIn() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         RouterService.getConnectionDispatcher().addConnectionAcceptor(acceptor, false, true, "BLOCKING");
         
         SSLContext context = SSLUtils.getTLSContext();
         SSLSocket tls = (SSLSocket)context.getSocketFactory().createSocket();
         tls.setUseClientMode(true);
         tls.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" } );
         tls.connect(new InetSocketAddress("localhost", port));
         tls.getOutputStream().write("BLOCKING MORE DATA".getBytes());
         tls.getOutputStream().flush();
         assertTrue(acceptor.waitForAccept());
         tls.close();
     }
     
     private int bindAcceptor() throws Exception {
         for(int p = 2000; p < Integer.MAX_VALUE; p++) {
             try {
                 acceptThread.setListeningPort(p);
                 return p;
             } catch(IOException ignored) {}
         }
         throw new IOException("unable to bind acceptor");
     }

     private static class MyConnectionAcceptor implements ConnectionAcceptor {
         private final String word;
         private CountDownLatch latch = new CountDownLatch(1);
         
         MyConnectionAcceptor(String word) {
             this.word = word;
         }
         
         public void acceptConnection(String word, Socket s) {
            LOG.debug("Got connection for word: " + word + ", socket: " + s);
            assertEquals(this.word, word);
            if("BLOCKING".equals(word)) {
                try {
                    LOG.debug("Getting IS");
                    InputStream in = s.getInputStream();
                    byte[] b = new byte[1000];
                    LOG.debug("Reading");
                    int read = in.read(b);
                    LOG.debug("read");
                    assertEquals("MORE DATA", new String(b, 0, read));
                    latch.countDown();
                    s.close();
                } catch(IOException iox) {
                    throw new RuntimeException(iox);
                }
            }
         }
         
         boolean waitForAccept() throws Exception {
             return latch.await(5, TimeUnit.SECONDS);
         }
         
     }
     
}
