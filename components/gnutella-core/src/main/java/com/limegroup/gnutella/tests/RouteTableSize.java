package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.IOException;
import java.util.*;

/** 
 * A quick dirty test to estimate the amount of memory used by route tables. 
 * May require changes to access of ReplyHandler and RouteTable to compile.
 */
public class RouteTableSize {
    public static void main(String args[]) {
        //Measure memory.
        System.gc();
        long memoryBefore=Runtime.getRuntime().totalMemory();

        //Make route table, fill with entries.
        int size=50000;
        ReplyHandler handler=new StubReplyHandler();
        RouteTable rt=new RouteTable(60);
        for (int i=0; i<2*size; i++) {
            byte[] guid=GUID.makeGuid();
            rt.tryToRouteReply(guid, handler);
        }

        //Measure memory
        System.gc();
        long memoryAfter=Runtime.getRuntime().totalMemory();
        long used=memoryAfter-memoryBefore;
        System.out.println("Total memory used: "+used);
        System.out.println("Memory per element: "+((float)used/(float)size));
        System.out.println("Ensuring rt still live: "+rt.hashCode());
    }
}


//Make route table, fill it with data.        
class StubReplyHandler implements ReplyHandler {
    public boolean isOpen() {
        return true;
    }
    public void handlePingReply(PingReply pingReply, 
                                ManagedConnection receivingConnection) {
    }
    public void handlePushRequest(PushRequest pushRequest, 
                                  ManagedConnection receivingConnection) {
    }
    public void handleQueryReply(QueryReply queryReply, 
                                 ManagedConnection receivingConnection) {
    }
}
