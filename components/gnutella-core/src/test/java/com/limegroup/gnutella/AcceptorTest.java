package com.limegroup.gnutella;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import junit.framework.Test;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class AcceptorTest extends com.limegroup.gnutella.util.LimeTestCase {
    
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

    public static void globalSetUp() throws Exception {
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
            catch (Exception e) {
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
            catch (Exception e) {
                portToTry++;
                continue;
            }
        }

        try {
            acceptThread.setListeningPort(portToTry);
            assertTrue("had no trouble binding TCP port!", false);
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
            assertTrue("had no trouble binding UDP to occupied port!",
                       false);
        }
        catch (Exception good) {}
        
        try {
            ServerSocket tcp = new ServerSocket(portToTry);
            tcp.close();
            assertTrue("had no trouble binding TCP to occupied port!",
                       false);
        }
        catch (Exception good) {}
    }

}
