package com.limegroup.gnutella;

import com.sun.java.util.collections.Iterator;
import java.net.Socket;
import java.io.IOException;

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
 * The connection that requested the connection thus gets fake pongs from us and
 * populates it's host catcher with the very best connections on the network at
 * this time.
 *
 */
class RejectConnection extends Connection {
    private HostCatcher _hostCatcher;

    /**
     * Constructs a temporary connection that waits for a ping and then replies
     * with pongs for the given HostCatcher's ten best hosts.
     *
     * The constructor does all the work -- initializing, waiting for the ping,
     * replying, so you better be ready to have your thread block for a while
     * if you construct a RejectConnection.
     */
    RejectConnection(Socket socket, HostCatcher hostCatcher) {
        super(socket);
        _hostCatcher = hostCatcher;
        try {
            initialize();
            loopForPingRequest();
        } catch(IOException e) {
            // finally does all the cleanup we need.
        } finally {
            close(); // whether we have an IO Exception or
                     // a successful set of PONGs, drop the connection
        }
    }

    private void loopForPingRequest()
            throws IOException {
        //The first message we get from the remote host should be its initial
        //ping.  However, some clients may start forwarding packets on the
        //connection before they send the ping.  Hence the following loop.  The
        //limit of 10 iterations guarantees that this method will not run for
        //more than TIMEOUT*10=80 seconds.  Thankfully this happens rarely.
        for (int i=0; i<10; i++) {
            Message m=null;
            try {
                // get the timeout from SettingsManager
                m=receive(SettingsManager.instance().getTimeout());
                if (m==null)
                    return; //Timeout has occured and we havent received the ping,
                            //so just return
            }// end of try for BadPacketEception from socket
            catch (BadPacketException e) {
                return; //Its a bad packet, just return
            }
            if((m instanceof PingRequest) && (m.getHops()==0)) {
                // this is the only kind of message we will deal with
                // in Reject Connection
                // If any other kind of message comes in we drop
                Iterator iter = _hostCatcher.getBestHosts(10);
                 // we are going to send rejected host the top ten
                 // connections
                for(int count=0; (count < 10) && iter.hasNext(); count++) {
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
                flush();
                return;
            }// end of (if m is PingRequest)
        } // End of while(true)
    }
}
