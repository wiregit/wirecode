package com.limegroup.gnutella;

import junit.framework.*;
import java.io.IOException;
import java.net.*;

public class AcceptorTest extends TestCase {

    public AcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AcceptorTest.class);
    }

    public void testPortsOccupied() {
        // strategy: open sockets on various ports and try to set the listening
        // port via Acceptor.

        // start it up!
        Acceptor acceptThread = new Acceptor();
        acceptThread.start();

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


        // hopefully this'll stop the thread...
        try {
            acceptThread.setListeningPort(0);
        }
        catch (IOException whatever) {}
        acceptThread = null;    }


    public void testPortsAreOccupied() {

        // start it up!
        Acceptor acceptThread = new Acceptor();
        acceptThread.start();

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


        // hopefully this'll stop the thread...
        try {
            acceptThread.setListeningPort(0);
        }
        catch (IOException whatever) {}
        acceptThread = null;
    }

}
