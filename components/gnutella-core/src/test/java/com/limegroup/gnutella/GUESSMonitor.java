package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import java.io.*;

/** Starts a BackEnd (which should have a .props file configured to be an
 *  Ultrapeer) and any time it gets a pong from a GUESS Ultrapeer, it sends
 *  it a query and take statistics on how many acks it recieves.
 *  It seems like you need to start this guy from a directory which has a com
 *  underneath it.
 *  If you run main, this will stop when you enter anything and press RETURN.
 */
public class GUESSMonitor {

    private Backend _backend;

    public GUESSMonitor() {
        // make my own MessageRouter....            
        ActivityCallback stub = new ActivityCallbackStub();
        FileManager staticFM = RouterService.getFileManager();
        MyMessageRouter messageRouter = new MyMessageRouter(stub, staticFM);
        _backend = Backend.createLongLivedBackend(stub, messageRouter);
    }

    public void shutdown() {
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
        public MyMessageRouter(ActivityCallback ac, FileManager fm) {
            super(ac, fm);
        }
        
        protected void handlePingReply(PingReply reply,
                                       ReplyHandler handler) {
            super.handlePingReply(reply, handler);
            System.out.println("GUESSMonitor.MyMessageRouter.handlePingReply()"
                               + ": got a pong!!");
        }

    }

}
