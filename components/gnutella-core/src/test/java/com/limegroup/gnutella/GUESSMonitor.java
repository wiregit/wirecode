package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import java.io.*;

/** Starts a BackEnd (which should have a .props file configured to be an
 *  Ultrapeer) and any time it gets a pong from a GUESS Ultrapeer, it sends
 *  it a query and take statistics on how many acks it recieves.
 *  It seems like you need to start this guy from a directory which has a com
 *  underneath it.
 *  This class will stop when you enter anything and press RETURN.
 */
public class GUESSMonitor {

    private Backend _backend;

    public GUESSMonitor() {
        _backend = Backend.createLongLivedBackend();
        try {
            // get the router....
            RouterService router = (RouterService) 
            PrivilegedAccessor.getValue(_backend, "ROUTER_SERVICE");
            // get the ActivityCallback (could do this easier, but whatever)
            ActivityCallback callback = (ActivityCallback)
            PrivilegedAccessor.getValue(router, "callback");
            // get the FileManager            
            FileManager fm = (FileManager)
            PrivilegedAccessor.getValue(router, "fileManager");
            // make my own MessageRouter....            
            MyMessageRouter messageRouter = new MyMessageRouter(callback, fm);
            messageRouter.initialize();
            // set my MessageRouter as THE MessageRouter
            PrivilegedAccessor.setValue(router, "router", messageRouter);
        }
        catch (Exception e) {
            System.out.println("Could not set up Backend appropriately!!");
            e.printStackTrace();
            _backend.shutdown("GUESSMonitor exiting!");
        }
    }

    public void shutdown() {
        _backend.shutdown("GUESSMonitor exiting!");
    }


    public static void main(String argv[]) {
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
