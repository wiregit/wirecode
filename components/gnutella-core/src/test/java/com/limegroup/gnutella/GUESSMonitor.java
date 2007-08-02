package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.limewire.collection.Buffer;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.guess.GUESSStatistics;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/** Starts a BackEnd (which should have a .props file configured to be an
 *  Ultrapeer) and any time it gets a pong from a GUESS Ultrapeer, it sends
 *  it a query and take statistics on how many acks it recieves.
 *  It seems like you need to start this guy from a directory which has a com
 *  underneath it.
 *  If you run main, this will stop when you enter anything and press RETURN.
 */
@SuppressWarnings("unchecked")
public class GUESSMonitor extends LimeTestCase {

    public final static String INSTRUCTIONS = 
        "? - Help; verbose - switch verbose on/off; connect - start the " +
        "backend; disconnect - stop the backend; stats - show stats";

    private RouterService _backend;
    private MyMessageRouter _messageRouter;

    public GUESSMonitor() {
        super("GUESS MONITOR");
        setStandardSettings();
        // make my own MessageRouter....            
        ActivityCallback stub = new ActivityCallbackStub();
        _messageRouter = new MyMessageRouter();
        _backend = new RouterService(stub, _messageRouter);
        //_backend = Backend.createLongLivedBackend(stub, _messageRouter);
        _backend.start();
        //RouterService.forceKeepAlive(8);
        //_backend.getRouterService().forceKeepAlive(5);
    }

    public void shutdown() {
        _messageRouter.shutdown();
        _messageRouter.join();
        RouterService.shutdown();
    }

    public void connect() {
        //_backend.getRouterService().connect();
        //_backend.getRouterService().forceKeepAlive(5);
        RouterService.connect();
        //RouterService.forceKeepAlive(5);
    }

    public void disconnect() {
    //_backend.getRouterService().disconnect();
        RouterService.disconnect();
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

    
    private class MyMessageRouter extends HackMessageRouter {

        private List _guessPongs = new Vector();
        private Set  _uniqueHosts = Collections.synchronizedSet(new HashSet());
        private Set  _badHosts = Collections.synchronizedSet(new HashSet());
        private Buffer _lastFiveHosts = new Buffer(5);

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

        public MyMessageRouter() {
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
            if (!NetworkUtils.isPrivateAddress(reply.getIPBytes()) &&
                notMe(reply.getInetAddress(), reply.getPort()) &&
                reply.supportsUnicast()) {
                synchronized (_guessPongs) {
                    _guessPongs.add(reply);
                    _guessPongs.notify();
                }
            }
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
                    if (_badHosts.contains(currPong.getInetAddress()))
                        continue;
                    {
                        // don't hit the same guys too often.....
                        if (_lastFiveHosts.contains(currPong.getInetAddress()))
                            continue;
                        _lastFiveHosts.addFirst(currPong.getInetAddress());
                    }
                    debug("guessPongLoop(): consuming Pong = " + currPong);
                    Object[] retObjs = 
                        GUESSStatistics.getAckStatistics(currPong.getAddress(),
                                                         currPong.getPort());
                    float numSent = ((Float)retObjs[1]).floatValue();
                    float numGot = ((Float)retObjs[0]).floatValue();
                    float averageTime = ((Float)retObjs[2]).floatValue();
                    float successRate = (numGot/numSent)*100;

                    tallyStats(numGot, averageTime, successRate);
                    // also keep track of unique tests done....
                    if (!_uniqueHosts.contains(currPong.getInetAddress())) {
                        _uniqueHosts.add(currPong.getInetAddress());
                        if (numGot == 0)
                            _badHosts.add(currPong.getInetAddress());
                        else
                            goodGUESSers++;
                    }

                    debug("Sent Queries to " + currPong.getInetAddress() + ":" +
                          currPong.getPort() + " . " + "Success Rate = " +
                          successRate + " at an average of " +
                          averageTime + " ms per Query.");
                    try {
                        Thread.sleep(500); // wait a little, don't kill guys....
                    }
                    catch (InterruptedException ignored) {}
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

    private synchronized void tallyStats(float numGot,
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

        if ((port == ProviderHacks.getNetworkManager().getPort()) &&
				 Arrays.equals(address.getAddress(), 
				         ProviderHacks.getNetworkManager().getAddress())) {			
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
