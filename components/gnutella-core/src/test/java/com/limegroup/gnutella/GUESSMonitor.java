package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import java.io.*;
import java.util.*;

/** Starts a BackEnd (which should have a .props file configured to be an
 *  Ultrapeer) and any time it gets a pong from a GUESS Ultrapeer, it sends
 *  it a query and take statistics on how many acks it recieves.
 *  It seems like you need to start this guy from a directory which has a com
 *  underneath it.
 *  If you run main, this will stop when you enter anything and press RETURN.
 */
public class GUESSMonitor {

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


    public static void main(String argv[]) throws Exception {
        System.out.println("Type Anything to Exit....");
        GUESSMonitor guessMon = new GUESSMonitor();
        //  open up standard input
        BufferedReader br = 
        new BufferedReader(new InputStreamReader(System.in));
        try {
            br.readLine(); // just wait for input...
        } 
        catch (IOException ioe) {
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
                if (reply.supportsUnicast()) {
                    synchronized (_guessPongs) {
                        _guessPongs.add(reply);
                        _guessPongs.notify();
                    }
                }
            }
            catch (BadPacketException ignored) {}
        }
                
        private void guessPongLoop() {
            System.out.println("guessPongLoop(): starting.");
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
                    System.out.println("guessPongLoop(): consuming Pong = " + 
                                       currPong);
                }
            }
            System.out.println("guessPongLoop(): returning.");
        }

    }

}
