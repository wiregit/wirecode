package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
import org.limewire.io.LocalSocketAddressService;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.nio.ssl.TLSNIOSocket;
import org.limewire.service.ErrorService;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class AcceptorTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(AcceptorTest.class);
    
    private Acceptor acceptor;
    private ConnectionDispatcher connectionDispatcher;

    private SocketsManager socketsManager;

    public AcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AcceptorTest.class);
    }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    @Override
    public void setUp() throws Exception {
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProviderStub().setTLSCapable(true));
        
        Injector injector = LimeTestUtils.createInjector();
        
        connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        socketsManager = injector.getInstance(SocketsManager.class);
        
        acceptor = injector.getInstance(Acceptor.class);
        acceptor.start();
        
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptor.setListeningPort(0);
    }
    
    @Override
    public void tearDown() throws Exception {
        //shut off the various services,
        //if an exception is thrown, something bad happened.
        acceptor.setListeningPort(0);
        ConnectionSettings.LOCAL_IS_PRIVATE.revertToDefault();
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
            acceptor.setListeningPort(portToTry);
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
            acceptor.setListeningPort(portToTry);
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
                acceptor.setListeningPort(portToTry);
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
     
     public void testAcceptedIncoming() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12);
                 os = sock.getOutputStream();
                 os.write("\n\n".getBytes());
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } catch (Throwable t) {
                 ErrorService.error(t);
             } finally {
                 if(sock != null)
                     try { sock.close(); } catch(IOException ignored) {}
                 if(os != null)
                     try { os.close(); } catch(IOException ignored) {}
             }
         }        

         Thread.sleep(250);
         // CONNECT-BACK is hardcoded on
         assertFalse(acceptor.acceptedIncoming());
     }
     
     public void testAcceptedConnectBack() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12);
                 os = sock.getOutputStream();
                 os.write("CONNECT ".getBytes());
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } catch (Throwable t) {
                 ErrorService.error(t);
             } finally {
                 if(sock != null)
                     try { sock.close(); } catch(IOException ignored) {}
                 if(os != null)
                     try { os.close(); } catch(IOException ignored) {}
             }
         }        

         Thread.sleep(250);
         // test on acceptor since network manager is stubbed
         assertTrue(acceptor.acceptedIncoming());
     }

     public void testTLSAcceptedIncoming() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12, ConnectType.TLS);
                 os = sock.getOutputStream();
                 os.write("\n\n".getBytes());
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } catch (Throwable t) {
                 ErrorService.error(t);
             } finally {
                 if(sock != null)
                     try { sock.close(); } catch(IOException ignored) {}
                 if(os != null)
                     try { os.close(); } catch(IOException ignored) {}
             }
         }        

         Thread.sleep(250);
         // CONNECT-BACK is hardcoded on
         assertFalse(acceptor.acceptedIncoming());
     }
     
     public void testTLSAcceptedConnectBack() throws Exception {
         ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
         int port = bindAcceptor();
         assertFalse(acceptor.acceptedIncoming());
         // open up incoming to the test node
         {
             Socket sock = null;
             OutputStream os = null;
             try {
                 sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                      port), 12, ConnectType.TLS);
                 os = sock.getOutputStream();
                 os.write("CONNECT ".getBytes());
                 os.flush();
             } catch (IOException ignored) {
             } catch (SecurityException ignored) {
             } catch (Throwable t) {
                 ErrorService.error(t);
             } finally {
                 if(sock != null)
                     try { sock.close(); } catch(IOException ignored) {}
                 if(os != null)
                     try { os.close(); } catch(IOException ignored) {}
             }
         }        

         Thread.sleep(250);
         // test on acceptor since network manager is stubbed
         assertTrue(acceptor.acceptedIncoming());
     }
     
     public void testIncomingTLSHomemadeTLS() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         
         connectionDispatcher.addConnectionAcceptor(acceptor, true, "BLOCKING");
         
         Socket tls = new TLSNIOSocket("localhost", port);
         tls.getOutputStream().write("BLOCKING MORE DATA".getBytes());
         tls.getOutputStream().flush();
         assertTrue(acceptor.waitForAccept());
         tls.close();
     }
     
     public void testIncomingTLSBuiltIn() throws Exception {
         int port = bindAcceptor();
         
         MyConnectionAcceptor acceptor = new MyConnectionAcceptor("BLOCKING");
         
         connectionDispatcher.addConnectionAcceptor(acceptor, true, "BLOCKING");
         
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
     
     public void testAcceptorPortForcing() throws Exception {
         int localPort = acceptor.getPort(false);
         ConnectionSettings.FORCED_PORT.setValue(1000);
         ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
         ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(InetAddress.getLocalHost().getHostAddress());
         assertEquals(1000, acceptor.getPort(true));
         assertNotEquals(1000,localPort); 
     }
     
     private int bindAcceptor() throws Exception {
         for(int p = 2000; p < Integer.MAX_VALUE; p++) {
             try {
                 acceptor.setListeningPort(p);
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
         
         public boolean isBlocking() {
            return true;
        }
         
     }
     
}
