package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.routing.*;

import junit.framework.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Test to make sure that query routing tables are correctly exchanged between
 * Ultrapeers.
 *
 * ULTRAPEER_1  ----  ULTRAPEER_2
 */
public final class UltrapeerQueryRouteTableTest extends BaseTestCase {

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ULTRAPEER_1 = 
		new RouterService(new TestCallback());

	/**
	 * The central Ultrapeer used in the test.
	 */
	//private static final RouterService ULTRAPEER_2 = 
    //new RouterService(new TestCallback());

    //private static final ReplyHandler REPLY_HANDLER =
    //  new TestReplyHandler();

    private static List REPLIES = new LinkedList();

    public UltrapeerQueryRouteTableTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UltrapeerQueryRouteTableTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public static void globalSetUp() throws Exception {
        setStandardSettings();
        //SearchSettings.PROBE_TTL.setValue((byte)1);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);
        SettingsManager settings = SettingsManager.instance();
        settings.setPort(6332);
        launchBackend();    
        ULTRAPEER_1.start();
		RouterService.connectToHostAsynchronously("localhost", Backend.PORT);    
        Thread.sleep(3000);
        assertTrue("should be connected", RouterService.isConnected());
    }

	public void setUp() throws Exception {
	}

	public void tearDown() throws Exception {
	}

    /**
     * Test to make sure that queries with one more hop to go are
     * properly routed when the last hop is an Ultrapeer.
     */
    public void testLastHopQueryRouting() throws Exception {
        assertTrue("should be connected", RouterService.isConnected());
        // first make sure it gets through on NOT last hop...
        QueryRequest qr = QueryRequest.createQuery("junkie junk", (byte)1);
        sendQuery(qr);
        
        Thread.sleep(2000);
        assertEquals("should not have received any replies", 0, REPLIES.size());

        qr = QueryRequest.createQuery("FileManager."+Backend.SHARED_EXTENSION, (byte)1);
        sendQuery(qr);        

        Thread.sleep(4000);
        assertTrue("should have received replies", !REPLIES.isEmpty());
    }


    private static void sendQuery(QueryRequest qr) {
        MessageRouter mr = RouterService.getMessageRouter();
        mr.sendDynamicQuery(qr);
        //mr.broadcastQueryRequest(qr, REPLY_HANDLER);
    }

    private static class TestCallback extends ActivityCallbackStub {
        public void handleQueryResult(HostData hd, Response res, List docs) {
            REPLIES.add(new Object());
        }
    }
}
