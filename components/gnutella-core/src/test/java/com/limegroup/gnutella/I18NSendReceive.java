package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.*;

import java.io.*;
import com.sun.java.util.collections.*;

public class I18NSendReceive extends com.limegroup.gnutella.util.BaseTestCase {

    private static Connection CONN_1;
    private static final int PORT = 6667;
    private static final int TIMEOUT = 5*1000;
    private static RouterService ROUTER_SERVICE;



    //test file names that should be in the shared dir and returned as
    //replies

    private static final String FILE_0 = "hello_0.txt";
    private static final String FILE_1 = "\uff8a\uff9b\uff70\u5143\u6c17\u3067\u3059\u304b\uff1f_\u30d5\u30a3\u30b7\u30e5_1.txt";
    private static final String FILE_2 = "\uff34\uff25\uff33\uff34\uff34\uff28\uff29\uff33\uff3f\uff26\uff29\uff2c\uff25\uff3f\uff2e\uff21\uff2d\uff25_2.txt";
    private static final String FILE_3 = "\u7206\u98a8\uff3ftestth\u00ccs_\uff27\uff2f_3.txt";
    private static final String FILE_4 = "t\u00e9stthis_\u334d_\uff2d\uff21\uff2c\uff23\uff2f\uff2d\u3000\uff38\uff3f\uff8f\uff99\uff7a\uff91_4.txt";

    private static final String[] FILES = {FILE_0, FILE_1, FILE_2, FILE_3, FILE_4};

    private static final String meter_j = "\u30e1\u30fc\u30c8\u30eb";


    public I18NSendReceive(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(I18NSendReceive.class);
    }

    
    private static void doSettings() throws Exception {
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(4);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);

        setUpFiles();
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }

    private static void setUpFiles() throws Exception {
        for(int i = 0; i < 5; i++) {
            File f = 
                new File("com/limegroup/gnutella/testfiles/" + FILES[i]);
            if(!f.exists()) {
                f.createNewFile();
                //make sure its not 0kb
                FileOutputStream fo = new FileOutputStream(f);
                BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(fo));
                buf.write("a");
                buf.flush();
                buf.close();
            }
            CommonUtils.copy(f, new File(_sharedDir, FILES[i]));
        }
    }

    public static void globalSetUp() throws Exception {
        doSettings();
        ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
        ROUTER_SERVICE.start();
        ROUTER_SERVICE.connect();
        connect();
    }

    public static void globalTearDown() throws Exception {
        drain(CONN_1);
        CONN_1.close();
        ROUTER_SERVICE.disconnect();
    }


    private static void connect() throws Exception {
        CONN_1 = new Connection("localhost", PORT,
                                new UltrapeerHeaders("localhost"),
                                new EmptyResponder());
        CONN_1.initialize();
        drain(CONN_1);
    }

	private void sleep() {
		try {Thread.sleep(5000);}catch(InterruptedException e) {}
	}

    /*
     * tests that we get a query reply from a file with the normalized
     * name and also that we receive the actual file name in the queryreply
     */
    public void testSendReceive() throws Exception {        

        QueryRequest qr;
        QueryReply rp;
        List expectedReply = new ArrayList();
        int size;

        //test random query 
        qr = QueryRequest.createQuery("asdfadf", (byte)2);
        CONN_1.send(qr);
        CONN_1.flush();
        
        rp = getFirstQueryReply(CONN_1);
        assertTrue("should not have received a QueryReply", !drain(CONN_1));
        drain(CONN_1);

        //should find FILE_0
        expectedReply.add(FILE_0);
        sendCheckQuery(expectedReply, "hello");

        //should find FILE_2, FILE_3, FILE_4
        expectedReply.add(FILE_2);
        expectedReply.add(FILE_3);
        expectedReply.add(FILE_4);
        sendCheckQuery(expectedReply, "testthis");
        
        //should find FILE_3
        expectedReply.add(FILE_3);
        sendCheckQuery(expectedReply, "\u7206\u98a8");

        //should find FILE_1
        expectedReply.add(FILE_1);
        sendCheckQuery(expectedReply, "\u5143\u6c17");
        
        //should find FILE_4
        expectedReply.add(FILE_4);
        sendCheckQuery(expectedReply, "malcom testthis \u30e1\u30fc\u30c8\u30eb");


    }

    //call if you expect a reply 
    private void sendCheckQuery(List expectedReply, String q) throws Exception {
        int size = expectedReply.size();

        QueryRequest qr = QueryRequest.createQuery(q, (byte)2);
        CONN_1.send(qr);
        CONN_1.flush();
        
        QueryReply rp = getFirstQueryReply(CONN_1);
        
        assertTrue("we should of received a QueryReply", rp != null);
        assertEquals("should have " + size + " result(s)", 
                     size,
                     rp.getResultCount());

        for(Iterator iter = rp.getResults(); iter.hasNext(); ) {
            Response res = (Response)iter.next();
            assertTrue("QueryReply : " + res.getName() + " not expected",
                       expectedReply.remove(res.getName()));
        }

        expectedReply.clear();
    }


}






