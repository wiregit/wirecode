package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
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
        "backend; disconnect - stop the backend; stats - show stats";

    private Backend _backend;
    private MyMessageRouter _messageRouter;

    public GUESSMonitor() {
        // make my own MessageRouter....            
        ActivityCallback stub = new ActivityCallbackStub();
        FileManager staticFM = RouterService.getFileManager();
        _messageRouter = new MyMessageRouter(stub, staticFM);
        _backend = Backend.createLongLivedBackend(stub, _messageRouter);
        _backend.start();
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
                else if (input.equals("stats"))
                    guessMon.showOverallStats();
                else if (input.equals(""))
                    ;
                else
                    System.out.println("Unknown Command, type '?' for Help.");
            } 
            catch (IOException ioe) {
            }
        }
        guessMon.shutdown();
    }

    
    private class MyMessageRouter extends MetaEnabledMessageRouter {

        private List _guessPongs = new Vector();
        private Set  _uniqueHosts = Collections.synchronizedSet(new HashSet());
        private Set  _badHosts = Collections.synchronizedSet(new HashSet());

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
                    if (_badHosts.contains(currPong.getIP()))
                        continue;
                    debug("guessPongLoop(): consuming Pong = " + currPong);
                    Object[] retObjs = 
                        GUESSStatistics.getAckStatistics(currPong.getIP(),
                                                         currPong.getPort());
                    float numSent = ((Float)retObjs[1]).floatValue();
                    float numGot = ((Float)retObjs[0]).floatValue();
                    float averageTime = ((Float)retObjs[2]).floatValue();
                    float successRate = (numGot/numSent)*100;

                    tallyStats(numGot, numSent, averageTime, successRate);
                    // also keep track of unique tests done....
                    if (!_uniqueHosts.contains(currPong.getIP())) {
                        _uniqueHosts.add(currPong.getIP());
                        if (numGot == 0)
                            _badHosts.add(currPong.getIP());
                        else
                            goodGUESSers++;
                    }

                    debug("Sent Queries to " + currPong.getIP() + ":" +
                          currPong.getPort() + " . " + "Success Rate = " +
                          successRate + " at an average of " +
                          averageTime + " ms per Query.");

                }
            }
            debug("guessPongLoop(): returning.");
        }

    }

    /* Numbers for maintaining stats...
     */
    private float numTestsDone = 0;
    private float numSuccessTests = 0;
    private float overallSuccessRate = 0;
    private float overallLatency = 0;
    private float goodGUESSers = 0;

    private synchronized void tallyStats(float numGot, float numSent, 
                                    float averageTime, float successRate) {
        if (numGot > 0) {
            numSuccessTests++;
            overallSuccessRate = 
            ((overallSuccessRate*numTestsDone) + successRate) /
            (numTestsDone+1);
            overallLatency = 
            ((overallLatency*numTestsDone) + averageTime) /
            (numTestsDone+1);
        }
        numTestsDone++;
    }


    public synchronized void showOverallStats() {
        System.out.println("---- STATS -----");
        System.out.println("Current Time: " + 
                           Calendar.getInstance().getTime());
        System.out.println("Number of Tests : " + numTestsDone);
        if (numSuccessTests > 0) {
            float uniqueHostsSize = _messageRouter._uniqueHosts.size();
            System.out.println("Overall Throughput Rate : " + 
                               overallSuccessRate +
                               "%");
            System.out.println("Overall Latency : " + overallLatency +
                               "ms");
            System.out.println("Number of Unique GUESS-enabled IPs : " +
                               uniqueHostsSize);
            System.out.println("Percentage of GUESS IPs that work : " +
                               (goodGUESSers/uniqueHostsSize)*100 +
                               "%");
        }
        System.out.println("----------------");
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
