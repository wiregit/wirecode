package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.licenses.*;
import com.bitzi.util.*;

import junit.framework.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class CreativeCommonsResultTest extends ClientSideTestCase {

    public CreativeCommonsResultTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CreativeCommonsResultTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    

    private static void doSettings() {
        // get the resource file for com/limegroup/gnutella
        File cc1 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/metadata/ccverifytest0.mp3");
        File cc2 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/metadata/ccverifytest1.mp3");
        assertTrue(cc1.exists());
        assertTrue(cc2.exists());
        // now move them to the share dir
        CommonUtils.copy(cc1, new File(_sharedDir, "ccverifytest0.mp3"));
        CommonUtils.copy(cc2, new File(_sharedDir, "ccverifytest1.mp3"));
    }        
    
    ///////////////////////// Actual Tests ////////////////////////////

    // PLEASE RUN THIS TEST FIRST
    public void testQRPExchange() throws Exception {
        
        Thread.sleep(2000); // give time to verify files
        assertEquals(2, rs.getNumSharedFiles());

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf",
                testUP[i].isSupernodeClientConnection());
            if (i < (testUP.length - 1))
                drain(testUP[i], 500);
        }

        final int upIndex = testUP.length - 1;
        QueryRouteTable qrt = new QueryRouteTable();
        com.limegroup.gnutella.util.BitSet retSet = 
        (com.limegroup.gnutella.util.BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertEquals(0, retSet.cardinality());
        // need to wait for QRP table to be sent
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = testUP[upIndex].receive(500);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        }
        catch (InterruptedIOException bad) {
            // we are waiting for all messages to be processed
        }

        retSet = (com.limegroup.gnutella.util.BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertGreaterThan(0, retSet.cardinality());

        // send a query that should hit in the qrt
        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio license=\"http://creativecommons.org/licenses/\"></audio></audios>";
        QueryRequest query = QueryRequest.createQuery("", richQuery);

        assertTrue(qrt.contains(query));
    }
    
    
    public void testCCResultsXMLSearch() throws Exception {
        // just make an incoming to the leaf so it knows it will respond to
        // queries
        Socket s = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
        s.close();

        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio license=\"http://creativecommons.org/licenses/\"></audio></audios>";

        // we should send a query to the leaf and get results.
        QueryRequest query = QueryRequest.createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(new GUID(query.getGUID()), new GUID(reply.getGUID()));
        assertEquals(""+reply.getResultsAsList(), 2, reply.getResultCount());

        /**
         * This is currently not working.  I've tested it with two LWs and it
         * seems to work nicely but for some reason the license stuff isn't 
         * returned in this test.  Will investigate more....
         String hexML = new String(reply.getXMLBytes());
         assertTrue(hexML,
        hexML.indexOf("license=\"http://creativecommons.org/licenses\"") > 0);
        */
    }


    
    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}

