package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import java.util.Iterator;

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

        // get the URNS for the files
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        Iterator iter = FileDesc.calculateAndCacheURN(berkeley).iterator();
        URN berkeleyURN = (URN) iter.next();
        iter = FileDesc.calculateAndCacheURN(susheel).iterator();
        URN susheelURN = (URN) iter.next();

        // send a query that should hit in the qrt
        QueryRequest query = QueryRequest.createQuery("berkeley");
        QueryRequest query2 = QueryRequest.createQuery("susheel");
        QueryRequest queryURN = QueryRequest.createQuery(berkeleyURN);
        QueryRequest queryURN2 = QueryRequest.createQuery(susheelURN);

        assertTrue("qrt did not contain: " + query, qrt.contains(query));
        assertTrue("qrt did not contain: " + query2, qrt.contains(query2));
        assertTrue("qrt did not contain: " + queryURN, qrt.contains(queryURN));
        assertTrue("qrt did not contain: "+queryURN2, qrt.contains(queryURN2));

        /* //TODO: investigate why this isn't working....
        retSet = (BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertEquals(4, retSet.cardinality());
        */
   }

}
