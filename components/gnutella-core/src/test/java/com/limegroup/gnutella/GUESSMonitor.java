package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.guess.*;
import java.io.*;
import java.util.*;
import java.net.*;

/** Starts a BackEnd (which should have a .props file configured to be an
 *  Ultrapeer) and any time it gets a pong from a GUESS Ultrapeer, it sends
 *  it a query and take statistics on how many acks it recieves.
 *  It seems like you need to start this guy from a directory which has a com
 *  underneath it.
 *  If you run main, this will stop when you enter anything and press RETURN.
 */
public class GUESSMonitor {

    public final static String INSTRUCTIONS = 
        "? - Help; verbose - switch verbose on/off; connect - start the " +
        "backend; disconnect - stop the backend";

    private Backend _backend;
    private MyMessageRouter _messageRouter;

    public GUESSMonitor() {
        // make my own MessageRouter....            
        ActivityCallback stub = new ActivityCallbackStub();
        FileManager staticFM = RouterService.getFileManager();
        _messageRouter = new MyMessageRouter(stub, staticFM);
        _backend = Backend.createLongLivedBackend(stub, _messageRouter);
    }

    public void shutdown() {
        _messageRouter.shutdown();
        _messageRouter.join();
        _backend.shutdown("GUESSMonitor exiting!");
    }

    public void connect() {
        _backend.getRouterService().connect();
    }

    public void disconnect() {
        _backend.getRouterService().disconnect();
    }

    public static void main(String argv[]) throws Exception {
        System.out.println("Type 'quit' to Exit....");
        GUESSMonitor guessMon = new GUESSMonitor();
        //  open up standard input
        String input = "";
        BufferedReader br = 
        new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Ready - Type '?' for Help.");
        while (!input.equals("quit")) {
            System.out.print("% ");
            try {
                input = br.readLine(); // just wait for input...
                if (input.equals("?")) 
                    System.out.println(INSTRUCTIONS);
                else if (input.equals("verbose"))
                    guessMon.switchDebug();
                else if (input.equals("connect"))
                    guessMon.connect();
                else if (input.equals("disconnect"))
                    guessMon.disconnect();
            } 
            catch (IOException ioe) {
            }
        }
        guessMon.shutdown();
    }

    
    private class MyMessageRouter extends MetaEnabledMessageRouter {

        private List _guessPongs = new Vector();

        private boolean _shouldRun = true;
        public void shutdown() {
            _shouldRun = false;
            _pongLoop.interrupt();
        }

        private Thread _pongLoop = null;
        public void join() {
            try {
                _pongLoop.join();
            }
            catch (Exception ignored) {}
        }

        public MyMessageRouter(ActivityCallback ac, FileManager fm) {
            super(ac, fm);
            _pongLoop = new Thread() {
                    public void run() {
                        guessPongLoop();
                    }
                };
            _pongLoop.start();
        }
        
        protected void handlePingReply(PingReply reply,
                                       ReplyHandler handler) {
            super.handlePingReply(reply, handler);
            try {
                if (!Endpoint.isPrivateAddress(reply.getIPBytes()) &&
                    notMe(InetAddress.getByName(reply.getIP()), 
                          reply.getPort()) &&
                    reply.supportsUnicast()) {
                    synchronized (_guessPongs) {
                        _guessPongs.add(reply);
                        _guessPongs.notify();
                    }
                }
            }
            catch (BadPacketException ignored) {}
            catch (UnknownHostException ignored) {}
        }
                
        private void guessPongLoop() {
            debug("guessPongLoop(): starting.");
            while (_shouldRun) {
                synchronized (_guessPongs) {
                    while (_shouldRun && _guessPongs.size() == 0) {
                        try {
                            _guessPongs.wait();
                        }
                        catch (InterruptedException ignored) {}
                    }
                }
                if (_shouldRun && (_guessPongs.size() > 0)) {
                    PingReply currPong = (PingReply) _guessPongs.remove(0);
                    debug("guessPongLoop(): consuming Pong = " + currPong);
                    Object[] retObjs = 
                        GUESSStatistics.getAckStatistics(currPong.getIP(),
                                                         currPong.getPort());
                    float numSent = ((Float)retObjs[1]).floatValue();
                    float numGot = ((Float)retObjs[0]).floatValue();
                    float averageTime = ((Float)retObjs[2]).floatValue();

                    debug("Sent Queries to " + currPong.getIP() + ":" +
                          currPong.getPort() + " . " + "Success Rate = " +
                          ((numGot/numSent)*100) + " at an average of " +
                          averageTime + " ms per Query.");
                    
                }
            }
            debug("guessPongLoop(): returning.");
        }

    }


    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * false if it does (NOT not me == me).
     */
    private boolean notMe(InetAddress address, int port) {
        boolean retVal = true;

        if ((port == RouterService.getPort()) &&
				 Arrays.equals(address.getAddress(), 
							   RouterService.getAddress())) {			
			retVal = false;
		}

        return retVal;
    }


    private boolean debugOn = false;
    private void debug(String out) {
        if (debugOn) {
            System.out.println(out);
        }
    }
    public void switchDebug() {
        debugOn = !debugOn;
    }
}
