package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * This class extends Connection and is invkoed when the Connection Manager has
 * reached it's threshold and cannot handle any more connections without
 * failing.
 *
 * In this situation, the Connecmtion manager opens a reject connection with
 * the requesting host and grabs a list of best hosts from the host catcher.
 * Creates messages as if they were pongs from the hosts and sends them along
 * and closes the connection
 *
 *The connection that requested the connection thus gets fake pongs from us and
 *populates it's host catcher with the very best connections on the network at this time
 *
 */
class RejectConnection extends Connection {
    private class PortMonitorThread extends Thread {
        public PortMonitorThread() {
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                initialize();
                loopForPingRequest();
            } catch(IOException e) {
                // finally does all the cleanup we need.
            } finally {
                shutdown(); // whether we have an IO Exception or
                            // a successful set of PONGs, drop the connection
            }
        }
    }

    private HostCatcher _hostCatcher;

    /**
     * Constructs a temporary connection that waits for a ping and then replies
     * with pongs for the given HostCatcher's ten best hosts.
     *
     * The constructor kicks off a thread that does the initialize() call,
     * so allow you need to do is construct the RejectConnection, and it's
     * off and running.
     */
    RejectConnection(Socket socket, HostCatcher hostCatcher) {
        super(socket);
        _hostCatcher = hostCatcher;
        new PortMonitorThread(); // The constructor does the start call
    }

    private void loopForPingRequest()
            throws IOException {
        while (true) { // continue till we get a PingRequest
            Message m=null;
            try {
                // get the timeout from SettingsManager
                m=receive(SettingsManager.instance().getTimeout());
                if (m==null)
                    continue;
            }// end of try for BadPacketEception from socket
            catch (BadPacketException e) {
                continue;
            }
            if((m instanceof PingRequest) && (m.getHops()==0)) {
                // this is the only kind of message we will deal with
                // in Reject Connection
                // If any other kind of message comes in we drop
                Iterator iter = _hostCatcher.getBestHosts(10);
                 // we are going to send rejected host the top ten
                 // connections
                while(iter.hasNext()) {
                    Endpoint bestEndPoint =(Endpoint)iter.next();
                    // make a pong with this host info
                    PingReply pr = new PingReply(m.getGUID(),(byte)1,
                        bestEndPoint.getPort(),
                        bestEndPoint.getHostBytes(), 0, 0);
                    // the ttl is 1; and for now the number of files
                    // and kbytes is set to 0 until chris stores more
                    // state in the hostcatcher
                    send(pr);
                }
                // we have sent 10 hosts, so this thread's work is done.
            }// end of (if m is PingRequest)
        } // End of while(true)
    }
}
