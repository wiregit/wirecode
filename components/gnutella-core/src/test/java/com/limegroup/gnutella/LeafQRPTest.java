package com.limegroup.gnutella;

import java.io.InterruptedIOException;

import org.limewire.collection.BitSet;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class LeafQRPTest extends ClientSideTestCase {
    public LeafQRPTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LeafQRPTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    ///////////////////////// Actual Tests ////////////////////////////
    
    // the only test - make sure the QRP table sent by the leaf is send and is
    // valid.
    public void testQRPExchange() throws Exception {
        // set up the connection
        QueryRouteTable qrt = new QueryRouteTable();
        BitSet retSet = (BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertEquals(0, retSet.cardinality());
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = testUP[0].receive(1000);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        }
        catch (InterruptedIOException bad) {
            // we are waiting for all messages to be processed
        }

        // send a query that should hit in the qrt
        QueryRequest query = QueryRequest.createQuery("berkeley");
        QueryRequest query2 = QueryRequest.createQuery("susheel");

        assertTrue("qrt did not contain: " + query, qrt.contains(query));
        assertTrue("qrt did not contain: " + query2, qrt.contains(query2));
   }

}
