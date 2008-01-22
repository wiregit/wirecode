package com.limegroup.gnutella;

import java.io.File;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 *  Tests that a Ultrapeer correctly sends XML Replies.  
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
 */
public final class ServerSideXMLReplyTest extends ServerSideTestCase {

    private QueryRequestFactory queryRequestFactory;

    public ServerSideXMLReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideXMLReplyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	public int getNumberOfUltrapeers() {
	    return 1;
    }

	@Override
	public int getNumberOfLeafpeers() {
	    return 1;
    }
	
    @Override
    public void setSettings() {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;");
        // get the resource file for com/limegroup/gnutella
        File mp3 = 
            TestUtils.getResourceFile("com/limegroup/gnutella/metadata/mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3");
        assertTrue(mp3.exists());
        // now move them to the share dir        
        FileUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
    }

    @Override
    public void setUpQRPTables() throws Exception {
        //3. routed leaf, with route table for "test"
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
        }
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        super.setUp(injector);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
    
    public void testXMLReturned1() throws Exception {
        drainAll();

        // send a query
        QueryRequest query = queryRequestFactory.createQuery("metadata");
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // wait for processing
        Thread.sleep(750);

        // confirm that result has heXML.
        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
        assertNotNull(reply);
        assertNotNull(reply.getXMLBytes());
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

    public void testXMLReturned2() throws Exception {
        drainAll();

        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Ambient\"></audio></audios>";

        // send a query
        QueryRequest query = queryRequestFactory.createQuery("Ambient", richQuery);
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // wait for processing
        Thread.sleep(750);


        // confirm that result has heXML.
        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
        assertNotNull(reply);
        assertNotNull(reply.getXMLBytes());
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

    public void testBitrateExclusion() throws Exception {
        // test that a mismatching artist name doesn't return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"junk\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("junk 16", richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNull(reply);
        }        

        // test that a matching artist name does return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("Test 16", richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // test that a null price value doesn't return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("$19.99 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNull(reply);
        }        

        // 3 fields - bitrate matches, but only one other, so no return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" title=\"junk\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("Test junk 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNull(reply);
        }        

        // 3 fields - all match, should return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" title=\"Test mpg\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("Test mpg 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // 3 fields - 1 match, 1 null, should return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" type=\"Audiobook\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("Test Audiobook 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // 3 fields - 2 null, should not return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\" type=\"Audiobook\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("$19.99 Audiobook 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNull(reply);
        }        

        // 3 fields - 1 null, 1 mismatch, should not return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\" artist=\"Tester\"></audio></audios>";

            // send a query
            QueryRequest query = queryRequestFactory.createQuery("$19.99 Tester 16", 
                                                          richQuery);
            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[0]);
            assertNull(reply);
        }        


    }
    

}
